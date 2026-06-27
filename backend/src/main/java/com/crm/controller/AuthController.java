package com.crm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.crm.common.result.Result;
import com.crm.dto.LoginRequest;
import com.crm.service.AuthService;
import com.crm.vo.CurrentUserVO;
import com.crm.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录鉴权接口
 *
 * <p>路径前缀 /api/auth（application.yml 已配置 context-path=/api）。</p>
 * <p>/login 与 /logout 已在 SaTokenConfig 中排除鉴权，/me 需要登录。</p>
 */
@Tag(name = "01. 登录鉴权", description = "登录 / 登出 / 当前用户")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "账号密码登录。成功返回 Sa-Token，后续请求放 Authorization header。")
    @SaIgnore
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @Operation(summary = "登出", description = "注销当前 token。")
    @SaIgnore
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "当前用户信息", description = "从 tokenSession 读取，0 DB 命中。可用于前端刷新页面时恢复状态。")
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.me());
    }
}