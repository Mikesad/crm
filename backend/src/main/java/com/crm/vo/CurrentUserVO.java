package com.crm.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前用户信息（{@code GET /api/auth/me} 响应）
 *
 * <p>与 {@link LoginResponse} 结构基本一致但不含 token，用于 token 续期或刷新页面时复用。</p>
 */
@Data
@Builder
public class CurrentUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
    private List<String> roleKeys;
    private Integer dataScope;
    private List<String> permissions;
}