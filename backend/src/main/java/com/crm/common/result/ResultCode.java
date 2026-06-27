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

    // ---- 阶段三合同/回款专用业务码 (3001-3099) ----
    BUSINESS_ERROR(3001, "业务异常"),
    AMOUNT_MISMATCH(3002, "合同金额与明细不符"),
    PLAN_NOT_BELONG_CONTRACT(3003, "回款计划不属于该合同"),
    CONTRACT_NOT_IN_EXECUTION(3004, "合同非执行中,不能录入"),
    PLAN_ALREADY_PAID(3005, "已回款的计划不能操作"),
    APPROVAL_NOT_PENDING(3006, "该审批单已处理,不能重复操作"),
    CONTRACT_CANNOT_UPDATE(3007, "已结束/已作废的合同不能修改");

    private final Integer code;
    private final String message;
}
