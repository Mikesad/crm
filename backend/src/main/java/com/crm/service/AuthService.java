package com.crm.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.SessionKeys;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.LoginRequest;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.SysMenuMapper;
import com.crm.mapper.SysRoleMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.vo.CurrentUserVO;
import com.crm.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 登录鉴权服务
 *
 * <p>登录流程（一次性查完三个维度，全部写入 {@code StpUtil.getTokenSession()}）：</p>
 * <ol>
 *   <li>按用户名查 sys_user</li>
 *   <li>BCrypt 验密</li>
 *   <li>查该用户所有启用的角色 + data_scope</li>
 *   <li>查该用户所有功能权限码（去重）</li>
 *   <li>计算有效数据范围（多角色取最宽）</li>
 *   <li>{@code StpUtil.login(userId)}</li>
 *   <li>写 tokenSession：userId / username / nickname / deptId / roleKeys / dataScope / perms</li>
 * </ol>
 *
 * <p>失效策略：默认不主动失效，靠 token 过期（默认 30 天，application.yml）自然重登。
 * 管理员改了角色后，受影响用户最坏 30 天内权限滞后。如需立即生效，加 role_version 字段做比对。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /** 默认 30 天，对应 application.yml 中 sa-token.timeout；rememberMe 延长到 90 天 */
    private static final long DEFAULT_TIMEOUT_SEC = 30L * 24 * 3600;
    private static final long REMEMBER_TIMEOUT_SEC = 90L * 24 * 3600;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        // 1. 查用户
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (user == null) {
            // 用户不存在与密码错误返回相同文案，避免账号枚举
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        // 2. 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        // 3. BCrypt 验密
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 4. 一次性加载角色与权限
        List<SysRole> roles = roleMapper.selectActiveRolesByUserId(user.getId());
        if (roles.isEmpty()) {
            // 没有启用角色不允许登录
            throw new BusinessException(ResultCode.FORBIDDEN, "账号未分配角色，无法登录");
        }
        List<String> roleKeys = roles.stream().map(SysRole::getRoleKey).toList();
        List<String> perms    = menuMapper.selectPermsByUserId(user.getId());

        // 5. 有效数据范围 = 多角色取最宽（数值最小）
        int effectiveScope = roles.stream()
                .map(SysRole::getDataScope)
                .filter(java.util.Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(5);

        // 6. Sa-Token 登录
        long timeout = Boolean.TRUE.equals(req.getRemember()) ? REMEMBER_TIMEOUT_SEC : DEFAULT_TIMEOUT_SEC;
        StpUtil.login(user.getId(), timeout);

        // 7. 写 tokenSession（绑定当前 token，非全局）
        StpUtil.getTokenSession().set(SessionKeys.USER_ID, user.getId());
        StpUtil.getTokenSession().set(SessionKeys.USERNAME, user.getUsername());
        StpUtil.getTokenSession().set(SessionKeys.NICKNAME, user.getNickname());
        StpUtil.getTokenSession().set(SessionKeys.DEPT_ID, user.getDeptId());
        StpUtil.getTokenSession().set(SessionKeys.ROLE_KEYS, roleKeys);
        StpUtil.getTokenSession().set(SessionKeys.DATA_SCOPE, effectiveScope);
        StpUtil.getTokenSession().set(SessionKeys.PERMISSIONS, perms);

        log.info("用户登录成功: id={}, username={}, roles={}, scope={}, perms={}",
                user.getId(), user.getUsername(), roleKeys, effectiveScope, perms.size());

        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .deptId(user.getDeptId())
                .roleKeys(roleKeys)
                .dataScope(effectiveScope)
                .permissions(perms)
                .build();
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            log.info("用户登出: id={}", StpUtil.getLoginIdAsLong());
            StpUtil.logout();
        }
    }

    public CurrentUserVO me() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return CurrentUserVO.builder()
                .userId(StpUtil.getLoginIdAsLong())
                .username((String) StpUtil.getTokenSession().get(SessionKeys.USERNAME))
                .nickname((String) StpUtil.getTokenSession().get(SessionKeys.NICKNAME))
                .deptId(StpUtil.getTokenSession().getLong(SessionKeys.DEPT_ID))
                .roleKeys((List<String>) StpUtil.getTokenSession().get(SessionKeys.ROLE_KEYS))
                .dataScope(StpUtil.getTokenSession().getInt(SessionKeys.DATA_SCOPE))
                .permissions((List<String>) StpUtil.getTokenSession().get(SessionKeys.PERMISSIONS))
                .build();
    }
}