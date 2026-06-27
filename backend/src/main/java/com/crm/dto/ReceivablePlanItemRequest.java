package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款计划明细项(批量创建时嵌套使用)
 */
@Data
@Schema(description = "回款计划明细项")
public class ReceivablePlanItemRequest {

    @NotNull(message = "期数不能为空")
    @Min(value = 1, message = "期数必须 >= 1")
    private Integer period;

    @NotNull(message = "预计金额不能为空")
    @DecimalMin(value = "0.01", message = "预计金额必须 > 0")
    private BigDecimal expectedAmount;

    @NotNull(message = "预计日期不能为空")
    private LocalDate expectedDate;

    @Schema(description = "备注(可选,如 '首款 40%')")
    private String remark;
}
