package com.crm.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "请求资源不存在"),

    PARAM_ERROR(1001, "参数校验失败"),
    DATA_EXISTS(1002, "数据已存在"),
    DATA_NOT_FOUND(1003, "数据不存在"),

    USER_DISABLED(2001, "账号已停用"),
    USER_PASSWORD_ERROR(2002, "用户名或密码错误"),
    USER_NOT_EXISTS(2003, "用户不存在"),

    BUSINESS_ERROR(3001, "业务异常");

    private final Integer code;
    private final String message;
}
