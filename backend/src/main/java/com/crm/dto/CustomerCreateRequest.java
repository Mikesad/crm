package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建客户请求
 */
@Data
@Schema(description = "创建客户请求")
public class CustomerCreateRequest {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称最长 100 字符")
    private String customerName;

    @Size(max = 50, message = "行业最长 50 字符")
    private String industry;

    /**
     * 客户级别：A 重要客户 / B 普通客户 / C 意向客户
     */
    private String level = "C";
}
