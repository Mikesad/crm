package com.crm.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.SysDeptCreateRequest;
import com.crm.dto.SysDeptUpdateRequest;
import com.crm.entity.SysDept;
import com.crm.mapper.SysDeptMapper;
import com.crm.vo.SysDeptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统部门服务
 *
 * <p>核心约束:</p>
 * <ul>
 *   <li>同级 deptName 唯一(后端校验)</li>
 *   <li>parent_id=0 顶级不可删除</li>
 *   <li>删除前校验:无直接子部门 / 无启用用户</li>
 *   <li>ancestors 维护:新建时按 parent.ancestors + parent.id 拼接;父变更时事务内刷所有后代</li>
 *   <li>上级选择控件(el-cascader)后端再校验剔除"自己/自己后代",防死循环</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    /** 顶级部门 sentinel */
    private static final Long ROOT_PARENT_ID = 0L;
    private static final String ROOT_ANCESTORS = "0";

    // ========== 列表/树 ==========

    /**
     * 全量部门列表(平铺,前端 el-tree 自组织树形)
     *
     * <p>包含 {@code parentName} / {@code childCount},前端选中节点后可直接渲染右栏。</p>
     */
    public List<SysDeptVO> listAll() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .orderByAsc(SysDept::getOrderNum)
                        .orderByAsc(SysDept::getId));
        return toVOList(all);
    }

    /**
     * 部门分页(平铺)
     *
     * <p>支持按 deptName / status / parentId 过滤。</p>
     */
    public IPage<SysDeptVO> page(String keyword, Integer status, Long parentId,
                                 int pageNum, int pageSize) {
        Page<SysDept> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysDept::getDeptName, keyword);
        }
        if (status != null) wrapper.eq(SysDept::getStatus, status);
        if (parentId != null) wrapper.eq(SysDept::getParentId, parentId);
        wrapper.orderByAsc(SysDept::getOrderNum).orderByAsc(SysDept::getId);
        IPage<SysDept> result = deptMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    /**
     * 部门详情(供 right-side 详情卡用)
     *
     * <p>包含 parentName / childCount / userCount。</p>
     */
    public SysDeptVO detail(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        SysDeptVO vo = toVO(dept);
        if (dept.getParentId() != null && dept.getParentId() > 0) {
            SysDept parent = deptMapper.selectById(dept.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getDeptName());
            }
        }
        vo.setChildCount(deptMapper.selectChildrenById(id).size());
        vo.setUserCount((int) deptMapper.countActiveUsersByDeptId(id));
        return vo;
    }

    // ========== 写操作 ==========

    @Transactional
    public Long create(SysDeptCreateRequest req) {
        // 1. 上级合法性:0=顶级 不允许新建(避免孤儿顶级),否则必须存在
        SysDept parent;
        if (req.getParentId() == null || req.getParentId() == 0L) {
            throw new BusinessException("V1 暂不允许新建顶级部门");
        } else {
            parent = deptMapper.selectById(req.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "上级部门不存在");
            }
        }
        // 2. 同级 deptName 唯一
        if (deptMapper.countByParentAndName(req.getParentId(), req.getDeptName(), 0L) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS,
                    "同级已存在同名部门「" + req.getDeptName() + "」");
        }
        SysDept dept = new SysDept();
        BeanUtil.copyProperties(req, dept);
        if (dept.getStatus() == null) dept.setStatus(1);
        // 3. ancestors = parent.ancestors + "," + parent.id
        dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        dept.setCreateBy(UserContext.currentUsername());
        deptMapper.insert(dept);
        log.info("新建部门: id={}, name={}, ancestors={}", dept.getId(), dept.getDeptName(), dept.getAncestors());
        return dept.getId();
    }

    /**
     * 更新部门。父变更会触发祖先链重建,事务内递归刷所有后代。
     */
    @Transactional
    public void update(SysDeptUpdateRequest req) {
        SysDept dept = deptMapper.selectById(req.getId());
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        // 同级 deptName 唯一(如果改了 deptName)
        if (StringUtils.hasText(req.getDeptName()) && !req.getDeptName().equals(dept.getDeptName())) {
            long parentId = req.getParentId() != null ? req.getParentId() : dept.getParentId();
            if (deptMapper.countByParentAndName(parentId, req.getDeptName(), dept.getId()) > 0) {
                throw new BusinessException(ResultCode.DATA_EXISTS,
                        "同级已存在同名部门「" + req.getDeptName() + "」");
            }
            dept.setDeptName(req.getDeptName());
        }
        boolean parentChanged = false;
        Long oldParentId = dept.getParentId();
        String oldAncestors = dept.getAncestors();
        if (req.getParentId() != null && !req.getParentId().equals(oldParentId)) {
            // 父变更 校验
            if (req.getParentId() == 0L) {
                throw new BusinessException("V1 暂不允许将部门改为顶级");
            }
            // 不能选自己或自己的后代作为父级(防死循环)
            if (req.getParentId().equals(dept.getId())) {
                throw new BusinessException("上级部门不能是本部门");
            }
            List<SysDept> allDescendants = deptMapper.selectDescendantsByAncestors(oldAncestors);
            if (allDescendants.stream().anyMatch(d -> d.getId().equals(req.getParentId()))) {
                throw new BusinessException("上级部门不能是本部门或本部门的后代");
            }
            SysDept newParent = deptMapper.selectById(req.getParentId());
            if (newParent == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "新上级部门不存在");
            }
            dept.setParentId(req.getParentId());
            dept.setAncestors(newParent.getAncestors() + "," + newParent.getId());
            parentChanged = true;
        }
        if (req.getOrderNum() != null) dept.setOrderNum(req.getOrderNum());
        if (req.getStatus() != null) dept.setStatus(req.getStatus());
        dept.setUpdateBy(UserContext.currentUsername());
        deptMapper.updateById(dept);
        // 父变更 → 事务内刷所有后代的 ancestors
        if (parentChanged) {
            rebuildDescendantAncestors(dept.getId(), oldAncestors, dept.getAncestors());
        }
        log.info("更新部门: id={}, parentChanged={}", dept.getId(), parentChanged);
    }

    /**
     * 删除部门。3 类保护:
     * <ol>
     *   <li>顶级(parent_id=0)不可删</li>
     *   <li>有直接子部门 → 拒绝</li>
     *   <li>有启用用户 → 拒绝</li>
     * </ol>
     */
    @Transactional
    public void delete(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        if (ROOT_PARENT_ID.equals(dept.getParentId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "顶级部门不可删除");
        }
        if (!deptMapper.selectChildrenById(id).isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "部门「" + dept.getDeptName() + "」下存在子部门,请先删除子部门");
        }
        long userCount = deptMapper.countActiveUsersByDeptId(id);
        if (userCount > 0) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "部门「" + dept.getDeptName() + "」下存在 " + userCount + " 名启用用户,请先转移用户");
        }
        deptMapper.deleteById(id);
        log.info("删除部门: id={}, name={}", id, dept.getDeptName());
    }

    @Transactional
    public void toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0 或 1");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        // 顶级部门不可停用(管理上保持顶级永远在岗)
        if (ROOT_PARENT_ID.equals(dept.getParentId()) && status == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "顶级部门不可停用");
        }
        dept.setStatus(status);
        dept.setUpdateBy(UserContext.currentUsername());
        deptMapper.updateById(dept);
        // 部门不在 Sa-Token session 缓存中(用户登录是按角色 / dept_id 是用户字段),
        // 部门停用对已登录用户**不**触发踢下线——但引用此部门的用户在下次登录时不会
        // 被任何 Sa-Token 拦截器拒绝;部门只是组织架构标签,与 Sa-Token 鉴权解耦。
        log.info("切换部门状态: id={}, status={}", id, status);
    }

    // ========== 内部:祖先链重建 ==========

    /**
     * 父变更后,事务内刷新所有后代的 ancestors 字符串
     *
     * <p>{@code ancestors} 字段约定 = 父级链 id 列表(不含自身),例:id=2 dept 的 ancestors='0,1'。</p>
     * <p>父变更时,把后代的 ancestors 字符串中"self 的旧 ancestors"前缀替换为"self 的新 ancestors"前缀:</p>
     * <pre>
     *   self.id=2, old ancestors='0,1', new ancestors='0,5'
     *   direct child d=8   old='0,1,2'   new='0,5,2'
     *   2nd-level  d=9     old='0,1,2,8' new='0,5,2,8'
     * </pre>
     */
    private void rebuildDescendantAncestors(Long selfId, String oldAncestorsPrefix, String newAncestorsPrefix) {
        List<SysDept> descendants = deptMapper.selectDescendantsByAncestors(oldAncestorsPrefix);
        for (SysDept d : descendants) {
            if (d.getId().equals(selfId)) continue;  // 自己已在 updateById 中更新
            // substring 跳过 oldAncestorsPrefix + ',' 之间的连接符,只保留 selfId 之后的后缀
            String suffix = d.getAncestors().substring(oldAncestorsPrefix.length() + 1);
            d.setAncestors(newAncestorsPrefix + "," + suffix);
            d.setUpdateBy(UserContext.currentUsername());
            deptMapper.updateById(d);
        }
    }

    // ========== 内部:VO 转换 / 批量 parentName 填充 ==========

    private SysDeptVO toVO(SysDept dept) {
        SysDeptVO vo = BeanUtil.copyProperties(dept, SysDeptVO.class);
        vo.setStatusText(dept.getStatus() != null && dept.getStatus() == 1 ? "正常" : "停用");
        return vo;
    }

    private List<SysDeptVO> toVOList(List<SysDept> depts) {
        if (depts.isEmpty()) return new ArrayList<>();
        // 批量填充 parentName / childCount / userCount
        Map<Long, String> idNameMap = depts.stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        Map<Long, Long> childCountMap = depts.stream()
                .collect(Collectors.toMap(SysDept::getId, d -> (long) deptMapper.selectChildrenById(d.getId()).size(), (a, b) -> a));
        Map<Long, Long> userCountMap = depts.stream()
                .collect(Collectors.toMap(SysDept::getId, d -> deptMapper.countActiveUsersByDeptId(d.getId()), (a, b) -> a));

        return depts.stream().map(d -> {
            SysDeptVO vo = BeanUtil.copyProperties(d, SysDeptVO.class);
            vo.setStatusText(d.getStatus() != null && d.getStatus() == 1 ? "正常" : "停用");
            if (d.getParentId() != null && d.getParentId() > 0) {
                vo.setParentName(idNameMap.getOrDefault(d.getParentId(), ""));
            }
            vo.setChildCount(childCountMap.getOrDefault(d.getId(), 0L).intValue());
            vo.setUserCount(userCountMap.getOrDefault(d.getId(), 0L).intValue());
            return vo;
        }).sorted(Comparator.comparing(SysDeptVO::getOrderNum,
                Comparator.nullsLast(Comparator.naturalOrder())))
          .toList();
    }
}
