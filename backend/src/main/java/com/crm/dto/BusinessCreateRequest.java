package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建商机请求
 */
@Data
@Schema(description = "创建商机请求")
public class BusinessCreateRequest {

    @NotNull(message = "客户 ID 不能为空")
    private Long customerId;

    @NotBlank(message = "商机名称不能为空")
    @Size(max = 100, message = "商机名称最长 100 字符")
    private String businessName;

    @PositiveOrZero(message = "预计金额必须为非负数")
    private BigDecimal expectedAmount;

    private LocalDate expectedDealDate;
}
