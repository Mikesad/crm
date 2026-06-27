package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线索转客户请求
 *
 * <p>用于 {@code POST /api/crm/lead/{id}/convert}。</p>
 *
 * <p>该接口在 {@code @Transactional} 内双写 {@code crm_customer} + {@code crm_contact}，
 * 客户级别、行业、负责人 ID 可在转客户时一并设置；联系人姓名/电话/职务由请求体提供。</p>
 */
@Data
@Schema(description = "线索转客户请求")
public class LeadConvertRequest {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称最长 100 字符")
    private String customerName;

    @Size(max = 50, message = "行业最长 50 字符")
    private String industry;

    /**
     * 客户级别
     * <p>A 重要客户 / B 普通客户 / C 意向客户（默认 C）</p>
     */
    private String level = "C";

    /** 联系人职务/职位（可选） */
    @Size(max = 50, message = "职务最长 50 字符")
    private String post;

    /** 联系人手机（默认沿用线索的电话） */
    @Size(max = 20, message = "手机号最长 20 字符")
    private String phone;

    /**
     * 决策权重
     * <p>1 核心决策者 / 2 弱影响者 / 3 普通职员（默认 1）</p>
     */
    private Integer decisionWeight = 1;
}
