package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建联系人请求
 */
@Data
@Schema(description = "创建联系人请求")
public class ContactCreateRequest {

    @NotNull(message = "客户 ID 不能为空")
    private Long customerId;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 30, message = "姓名最长 30 字符")
    private String contactName;

    @Size(max = 50, message = "职务最长 50 字符")
    private String post;

    @Size(max = 20, message = "手机号最长 20 字符")
    private String phone;

    /** 是否主联系人：0 否 / 1 是（默认 0） */
    private Integer isMaster = 0;

    /** 决策权重：1 核心 / 2 弱影响 / 3 普通（默认 3） */
    private Integer decisionWeight = 3;
}
