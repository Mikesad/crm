package com.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 *
 * <p>POST /api/auth/login 的 body。</p>
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 30, message = "账号长度需在 2-30 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    /** 记住我（延长 token 有效期）。非必填，默认 false。 */
    private Boolean remember;
}