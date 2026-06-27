package com.crm.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 与 Web MVC 配置
 *
 * <p>注册 Sa-Token 拦截器并开启 CORS 跨域（方便 Vue 前端调试）</p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 注册 Sa-Token 拦截器，打开注解鉴权 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 注意:路径要含 /api 前缀(application.yml context-path=/api),
                        // 否则 Sa-Token 仍会校验旧 token,导致 /auth/login 报 401 "未登录访问"
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/captcha",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico"
                );
    }

    /** CORS 跨域配置 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
