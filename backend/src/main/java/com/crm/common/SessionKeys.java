package com.crm.common;

/**
 * Sa-Token Session 中的字段 key 集中定义
 *
 * <p>所有写入 {@code StpUtil.getTokenSession()} 的字段必须在此声明，禁止散落魔法字符串。</p>
 */
public final class SessionKeys {

    private SessionKeys() {}

    /** 当前用户 ID 冗余存一份，方便拦截器直接读，避免类型转换 */
    public static final String USER_ID     = "userId";

    /** 用户账号 */
    public static final String USERNAME    = "username";

    /** 昵称 */
    public static final String NICKNAME    = "nickname";

    /** 部门 ID */
    public static final String DEPT_ID     = "deptId";

    /** 角色 key 列表，如 ["admin", "sales"] */
    public static final String ROLE_KEYS   = "roleKeys";

    /**
     * 有效数据范围（取该用户所有角色的最宽）
     * <p>1 全部 / 3 本部门组(我的部门+同parent兄弟) / 5 仅本人 (phase8 commit1 拆档:删 4)</p>
     */
    public static final String DATA_SCOPE  = "dataScope";

    /** 功能权限码列表 */
    public static final String PERMISSIONS = "perms";
}