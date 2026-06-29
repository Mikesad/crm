package com.crm.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.SysRoleCreateRequest;
import com.crm.dto.SysRoleUpdateRequest;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.SysRoleMapper;
import com.crm.mapper.SysRoleMenuMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.mapper.SysUserRoleMapper;
import com.crm.vo.SysRoleVO;
import com.crm.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统角色服务
 *
 * <p>核心约束：</p>
 * <ul>
 *   <li>5 个种子角色不可删除（roleKey 白名单）</li>
 *   <li>删除前校验无用户绑定</li>
 *   <li>角色变更时所有绑定此角色的用户需重新登录（Sa-Token session 失效）</li>
 *   <li>菜单绑定走"全量重绑"：DELETE + INSERT WHERE role_id=?</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    /** 内置 5 角色不可删 */
    private static final Set<String> PROTECTED_ROLE_KEYS = Set.of(
            "admin", "sales_director", "sales_lead", "sales", "finance"
    );

    public IPage<SysRoleVO> page(String keyword, int pageNum, int pageSize) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getRoleName, keyword)
                    .or().like(SysRole::getRoleKey, keyword));
        }
        wrapper.orderByAsc(SysRole::getId);
        IPage<SysRole> result = roleMapper.selectPage(page, wrapper);
        IPage<SysRoleVO> voPage = result.convert(this::toVO);
        // 填充 userCount
        for (SysRoleVO vo : voPage.getRecords()) {
            vo.setUserCount(countUserBinding(vo.getId()));
        }
        return voPage;
    }

    /**
     * 全量角色下拉(status=1)
     */
    public List<SysRoleVO> listAll() {
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId));
        return roles.stream().map(this::toVO).toList();
    }

    public SysRoleVO detail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        SysRoleVO vo = toVO(role);
        vo.setMenuIds(roleMenuMapper.selectMenuIdsByRoleId(id));
        vo.setUserCount(countUserBinding(id));
        return vo;
    }

    @Transactional
    public Long create(SysRoleCreateRequest req) {
        // roleKey 唯一性
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, req.getRoleKey()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "roleKey 已存在");
        }
        SysRole role = new SysRole();
        BeanUtil.copyProperties(req, role);
        if (req.getStatus() == null) role.setStatus(1);
        role.setCreateBy(UserContext.currentUsername());
        roleMapper.insert(role);
        // 菜单绑定
        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(role.getId(), req.getMenuIds());
        }
        log.info("创建角色: id={}, key={}", role.getId(), role.getRoleKey());
        return role.getId();
    }

    @Transactional
    public void update(SysRoleUpdateRequest req) {
        SysRole role = roleMapper.selectById(req.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (StringUtils.hasText(req.getRoleName())) role.setRoleName(req.getRoleName());
        if (req.getDataScope() != null) role.setDataScope(req.getDataScope());
        if (req.getStatus() != null) role.setStatus(req.getStatus());
        role.setUpdateBy(UserContext.currentUsername());
        roleMapper.updateById(role);
        // 菜单绑定
        if (req.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(role.getId());
            if (!req.getMenuIds().isEmpty()) {
                roleMenuMapper.batchInsert(role.getId(), req.getMenuIds());
            }
            // 角色权限变化 → 踢所有绑定此角色的用户下线
            kickRoleUsersOffline(role.getId());
        }
        log.info("更新角色: id={}", role.getId());
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        // 内置 5 角色保护
        if (PROTECTED_ROLE_KEYS.contains(role.getRoleKey())) {
            throw new BusinessException("内置角色「" + role.getRoleName() + "」不可删除");
        }
        // 校验无用户绑定
        if (countUserBinding(id) > 0) {
            throw new BusinessException("存在用户绑定此角色,不能删除");
        }
        roleMapper.deleteById(id);  // 逻辑删除
        roleMenuMapper.deleteByRoleId(id);
        log.info("删除角色: id={}", id);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleId, menuIds);
        }
        // 踢所有绑定此角色的用户下线
        kickRoleUsersOffline(roleId);
        log.info("分配角色菜单: roleId={}, menuIds.size={}", roleId,
                menuIds == null ? 0 : menuIds.size());
    }

    /**
     * 阶段六 commit 1 收尾:批量给角色加成员
     *
     * <p>行为:</p>
     * <ol>
     *   <li>逐个 userId 用 batchInsert(单元素列表)插入 sys_user_role</li>
     *   <li>重复绑定走 INSERT IGNORE / MyBatis-Plus 自带 SELECT IGNORE 兼容;
     *       重复时 batchInsert 走 INSERT IGNORE 不会抛异常(见 SysUserRoleMapper @Insert)</li>
     *   <li>把所有受影响用户踢下线(Sa-Token session 失效),他们下次请求会触发 401</li>
     * </ol>
     */
    @Transactional
    public void addMembers(Long roleId, List<Long> userIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            userRoleMapper.batchInsert(userId, Collections.singletonList(roleId));
        }
        kickUsersOffline(userIds);
        log.info("添加角色成员: roleId={}, userCount={}", roleId, userIds.size());
    }

    /**
     * 阶段六 commit 1 收尾:把单个用户从角色移除
     *
     * <p>admin 自保护:若是 admin 角色且绑这个 admin 的活跃用户数 = 1,拒绝移除</p>
     */
    @Transactional
    public void removeMember(Long roleId, Long userId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        if ("admin".equals(role.getRoleKey())) {
            int count = userRoleMapper.countByRoleId(roleId);
            if (count <= 1) {
                throw new BusinessException("admin 至少 1 人,不能移除最后 1 个 admin");
            }
        }
        userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
        kickUsersOffline(Collections.singletonList(userId));
        log.info("移除角色成员: roleId={}, userId={}", roleId, userId);
    }

    /**
     * 阶段六 commit 1 收尾:列出某角色下的所有用户(分页)
     *
     * <p>返回 SysUserVO(含 roleIds / roleNames)。roleNames 字段刻意**不包含**当前 roleId,
     * 因为 UI 的"其他角色"列要展示除本角色之外的角色,避免冗余。</p>
     */
    public IPage<SysUserVO> listMembers(Long roleId, int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return page.convert(this::toUserVO);
        }
        IPage<SysUser> result = userMapper.selectPage(page,
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
        IPage<SysUserVO> voPage = result.convert(this::toUserVO);
        // 填充 otherRoles(排除当前 roleId 本身)
        for (SysUserVO vo : voPage.getRecords()) {
            List<Long> others = userRoleMapper.selectRoleIdsByUserId(vo.getId())
                    .stream().filter(id -> !id.equals(roleId)).toList();
            vo.setRoleIds(others);
        }
        fillRoleNames(voPage.getRecords());
        return voPage;
    }

    /**
     * SysUser -> SysUserVO 转换(v0.3:不查 deptName)
     */
    private SysUserVO toUserVO(SysUser user) {
        SysUserVO vo = BeanUtil.copyProperties(user, SysUserVO.class);
        vo.setSexText(user.getSex() == null ? "未知"
                : (user.getSex() == 0 ? "男" : (user.getSex() == 1 ? "女" : "未知")));
        vo.setStatusText(user.getStatus() != null && user.getStatus() == 1 ? "正常" : "停用");
        return vo;
    }

    private void fillRoleNames(List<SysUserVO> vos) {
        if (vos.isEmpty()) return;
        List<Long> allRoleIds = vos.stream()
                .flatMap(vo -> vo.getRoleIds().stream())
                .distinct().toList();
        if (allRoleIds.isEmpty()) return;
        Map<Long, String> roleNameMap = roleMapper.selectBatchIds(allRoleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));
        for (SysUserVO vo : vos) {
            List<String> names = vo.getRoleIds().stream()
                    .map(roleNameMap::get).filter(java.util.Objects::nonNull).toList();
            vo.setRoleNames(names);
        }
    }

    private void kickUsersOffline(List<Long> userIds) {
        for (Long uid : userIds) {
            try { StpUtil.logout(uid); } catch (Exception ignored) {}
        }
    }

    private void kickRoleUsersOffline(Long roleId) {
        try {
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            if (userIds != null) {
                for (Long uid : userIds) {
                    try { cn.dev33.satoken.stp.StpUtil.logout(uid); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("踢下线失败: roleId={}", roleId, e);
        }
    }

    private int countUserBinding(Long roleId) {
        return userRoleMapper.countByRoleId(roleId);
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = BeanUtil.copyProperties(role, SysRoleVO.class);
        vo.setStatusText(role.getStatus() != null && role.getStatus() == 1 ? "正常" : "停用");
        vo.setDataScopeText(dataScopeText(role.getDataScope()));
        return vo;
    }

    private String dataScopeText(Integer scope) {
        if (scope == null) return "未知";
        return switch (scope) {
            case 1 -> "全部";
            case 2 -> "自定义";
            case 3 -> "本部门";
            case 4 -> "本部门及以下";
            case 5 -> "仅本人";
            default -> "未知";
        };
    }
}
