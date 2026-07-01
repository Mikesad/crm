package com.crm.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应
 *
 * <p>登录成功后一次性把鉴权所需的全部信息返回给前端，避免前端再次请求 {@code /me}。</p>
 */
@Data
@Builder
public class LoginResponse {

    /** Sa-Token 的 token，前端存入 localStorage，请求时放入 Authorization header */
    private String token;

    private Long userId;

    private String username;

    /** 昵称（来自 sys_user.nickname） */
    private String nickname;

    private Long deptId;

    /** 角色 key 列表，如 ["admin"] */
    private List<String> roleKeys;

    /**
     * 有效数据范围（取该用户所有角色的最宽）
     * <p>1 全部 / 3 本部门组 / 5 仅本人 (phase8 commit1 拆档:删 4)</p>
     */
    private Integer dataScope;

    /** 功能权限码列表，如 ["crm:customer:list", "crm:customer:edit"] */
    private List<String> permissions;
}