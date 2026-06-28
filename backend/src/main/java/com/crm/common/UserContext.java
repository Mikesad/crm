package com.crm.common;

import cn.dev33.satoken.stp.StpUtil;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;

import java.util.Collections;
import java.util.List;

/**
 * 当前用户上下文（基于 Sa-Token tokenSession）
 *
 * <p>由 {@code AuthService.login()} 写入，{@code StpInterfaceImpl} 与
 * {@code DataPermissionHandler} 读取。整个登录态生命周期内 <b>0 次 DB 命中</b>。</p>
 */
public final class UserContext {

    private UserContext() {}

    /** 当前登录用户 ID，未登录抛 {@link BusinessException} */
    public static long requireUserId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return StpUtil.getLoginIdAsLong();
    }

    public static Long currentUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }

    public static Long currentDeptId() {
        return StpUtil.getTokenSession().getLong(SessionKeys.DEPT_ID);
    }

    public static String currentUsername() {
        return (String) StpUtil.getTokenSession().get(SessionKeys.USERNAME);
    }

    public static String currentNickname() {
        return (String) StpUtil.getTokenSession().get(SessionKeys.NICKNAME);
    }

    /**
     * 当前用户在 crm_record / 业务表审计字段中的"作者键"——nickname 优先,回退 username。
     *
     * <p>与 {@code RecordService.append()} 写入 create_by 的策略对齐,凡写 crm_record 必走此方法,
     * 与后续按 create_by 统计/过滤的查询(RecordService.todoCount / mine 等)保持一致。</p>
     */
    public static String currentAuthor() {
        String nickname = currentNickname();
        return (nickname != null && !nickname.isEmpty()) ? nickname : currentUsername();
    }

    /** 当前用户的有效数据范围（1/3/4/5） */
    public static int currentDataScope() {
        Integer scope = StpUtil.getTokenSession().getInt(SessionKeys.DATA_SCOPE);
        return scope == null ? 5 : scope;
    }

    @SuppressWarnings("unchecked")
    public static List<String> currentRoleKeys() {
        Object v = StpUtil.getTokenSession().get(SessionKeys.ROLE_KEYS);
        return v == null ? Collections.emptyList() : (List<String>) v;
    }

    @SuppressWarnings("unchecked")
    public static List<String> currentPermissions() {
        Object v = StpUtil.getTokenSession().get(SessionKeys.PERMISSIONS);
        return v == null ? Collections.emptyList() : (List<String>) v;
    }
}