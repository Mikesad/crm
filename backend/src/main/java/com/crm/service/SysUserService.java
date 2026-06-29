package com.crm.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.SysUserCreateRequest;
import com.crm.dto.SysUserUpdateRequest;
import com.crm.entity.SysDept;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.SysDeptMapper;
import com.crm.mapper.SysRoleMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.mapper.SysUserRoleMapper;
import com.crm.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户服务
 *
 * <p>核心约束：</p>
 * <ul>
 *   <li>admin 至少 1 人（防止全员禁用锁死系统）</li>
 *   <li>admin 不能操作自己（防止误把自己禁用）</li>
 *   <li>{@code username} 不可修改（作为登录账号稳定不变）</li>
 *   <li>新建用户默认密码 123456（BCrypt 加密落库）</li>
 *   <li>角色绑定走"全量重绑"：DELETE + INSERT WHERE user_id=?</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Set<String> PROTECTED_KEYS = Set.of("admin");
    private static final Long DEFAULT_ADMIN_ROLE_ID = 1L;
    private static final String DEFAULT_PASSWORD = "123456";

    public IPage<SysUserVO> page(String keyword, Long deptId, Integer status,
                                  int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getId);
        IPage<SysUser> result = userMapper.selectPage(page, wrapper);
        IPage<SysUserVO> voPage = result.convert(this::toVO);
        // v0.11:补 roleIds + roleNames 填充(原 page 漏掉,导致前端列表全显示"未分配")
        // 阶段七 commit:v0.12 补 deptName 填充
        List<SysUserVO> records = voPage.getRecords();
        if (!records.isEmpty()) {
            for (SysUserVO vo : records) {
                vo.setRoleIds(userRoleMapper.selectRoleIdsByUserId(vo.getId()));
            }
            fillRoleNames(records);
            fillDeptNames(records);
        }
        return voPage;
    }

    public SysUserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        SysUserVO vo = toVO(user);
        vo.setRoleIds(userRoleMapper.selectRoleIdsByUserId(id));
        fillRoleNames(Collections.singletonList(vo));
        fillDeptNames(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public Long create(SysUserCreateRequest req) {
        // 账号唯一性
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "账号已存在");
        }
        SysUser user = new SysUser();
        BeanUtil.copyProperties(req, user);
        // 密码处理
        String rawPwd = StringUtils.hasText(req.getPassword()) ? req.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(rawPwd));
        if (req.getStatus() == null) user.setStatus(1);
        if (req.getSex() == null) user.setSex(0);
        user.setCreateBy(UserContext.currentUsername());
        userMapper.insert(user);
        // 角色绑定
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), req.getRoleIds());
        }
        log.info("创建用户: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Transactional
    public void update(SysUserUpdateRequest req) {
        SysUser user = userMapper.selectById(req.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // admin 自保护：不能停用自己
        if (req.getStatus() != null && req.getStatus() == 0 && req.getId().equals(UserContext.requireUserId())) {
            throw new BusinessException("不能停用自己");
        }
        if (StringUtils.hasText(req.getNickname())) user.setNickname(req.getNickname());
        if (req.getDeptId() != null) user.setDeptId(req.getDeptId());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getSex() != null) user.setSex(req.getSex());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        user.setUpdateBy(UserContext.currentUsername());
        userMapper.updateById(user);
        // 角色绑定（仅在 roleIds 非 null 时重绑）
        if (req.getRoleIds() != null) {
            assignRoles(user.getId(), req.getRoleIds());
        }
        // 停用时立即踢下线
        if (req.getStatus() != null && req.getStatus() == 0) {
            try { StpUtil.logout(user.getId()); } catch (Exception ignored) {}
        }
        log.info("更新用户: id={}", user.getId());
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // admin 自保护
        if (id.equals(UserContext.requireUserId())) {
            throw new BusinessException("不能删除自己");
        }
        // admin 至少 1 人校验
        ensureAtLeastOneAdmin(id, null);
        // 逻辑删除 + 踢下线
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
        try { StpUtil.logout(id); } catch (Exception ignored) {}
        log.info("删除用户: id={}", id);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        String pwd = StringUtils.hasText(newPassword) ? newPassword : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setUpdateBy(UserContext.currentUsername());
        userMapper.updateById(user);
        // 重置后踢下线,迫使用户重新登录
        try { StpUtil.logout(id); } catch (Exception ignored) {}
        log.info("重置用户密码: id={}", id);
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户 ID 不能为空");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // admin 自保护：清空 admin 角色前确保至少剩 1 人
        if (roleIds == null || !roleIds.contains(DEFAULT_ADMIN_ROLE_ID)) {
            ensureAtLeastOneAdmin(userId, null);
        }
        // 全量重绑
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.batchInsert(userId, roleIds);
        }
        // 角色变了 → 踢下线让 Sa-Token session 重新加载
        try { StpUtil.logout(userId); } catch (Exception ignored) {}
        log.info("分配角色: userId={}, roleIds={}", userId, roleIds);
    }

    @Transactional
    public void toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0 或 1");
        }
        if (status == 0 && id.equals(UserContext.requireUserId())) {
            throw new BusinessException("不能停用自己");
        }
        ensureAtLeastOneAdmin(id, status);
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdateBy(UserContext.currentUsername());
        userMapper.updateById(user);
        if (status == 0) {
            try { StpUtil.logout(id); } catch (Exception ignored) {}
        }
        log.info("切换用户状态: id={}, status={}", id, status);
    }

    /**
     * 校验"admin 至少 1 人"约束。
     *
     * @param excludeUserId 排除的用户（即将被操作的用户）
     * @param targetStatus  该用户即将变更到的状态，null 表示即将被删除
     */
    private void ensureAtLeastOneAdmin(Long excludeUserId, Integer targetStatus) {
        // 只在用户被停用/删除/剥离 admin 角色时才校验
        if (targetStatus != null && targetStatus == 1) return;  // 启用不校验
        // 查当前 admin（role_id=1）下的活跃用户数,排除掉当前用户
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .ne(SysUser::getId, excludeUserId)
                .inSql(SysUser::getId, "SELECT user_id FROM sys_user_role WHERE role_id = " + DEFAULT_ADMIN_ROLE_ID));
        if (count == 0) {
            throw new BusinessException("系统至少保留 1 个启用的 admin 账号");
        }
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = BeanUtil.copyProperties(user, SysUserVO.class);
        vo.setSexText(user.getSex() == null ? "未知" : (user.getSex() == 0 ? "男" : (user.getSex() == 1 ? "女" : "未知")));
        vo.setStatusText(user.getStatus() != null && user.getStatus() == 1 ? "正常" : "停用");
        return vo;
    }

    private void fillRoleNames(List<SysUserVO> vos) {
        if (vos.isEmpty()) return;
        // 收集所有 roleId
        List<Long> allRoleIds = vos.stream()
                .flatMap(vo -> vo.getRoleIds().stream())
                .distinct().toList();
        if (allRoleIds.isEmpty()) return;
        Map<Long, String> roleNameMap = roleMapper.selectBatchIds(allRoleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));
        for (SysUserVO vo : vos) {
            List<String> names = vo.getRoleIds().stream()
                    .map(roleNameMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            vo.setRoleNames(names);
        }
    }

    /**
     * 阶段七 commit:批量填充 {@code deptName}
     *
     * <p>从 vos 中收集非空 deptId,走 {@code SysDeptMapper.selectBatchIds} 一次取回,
     * 避免 N+1。模式与 {@link #fillRoleNames} 一致。
     * deptId 为空的用户保持 deptName = null(前端显示"— 未分配 —")。</p>
     */
    private void fillDeptNames(List<SysUserVO> vos) {
        if (vos.isEmpty()) return;
        List<Long> allDeptIds = vos.stream()
                .map(SysUserVO::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (allDeptIds.isEmpty()) return;
        Map<Long, String> deptNameMap = deptMapper.selectBatchIds(allDeptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        for (SysUserVO vo : vos) {
            if (vo.getDeptId() != null) {
                vo.setDeptName(deptNameMap.get(vo.getDeptId()));
            }
        }
    }
}
