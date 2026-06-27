package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新回款计划请求
 *
 * <p>已回款(status=2)的计划不允许修改,需先撤回回款(财务录入反向冲销,V1 未实现)。</p>
 */
@Data
@Schema(description = "更新回款计划请求")
public class ReceivablePlanUpdateRequest {

    @NotNull(message = "计划 ID 不能为空")
    private Long id;

    @DecimalMin(value = "0.01", message = "预计金额必须 > 0")
    private BigDecimal expectedAmount;

    private LocalDate expectedDate;

    @Schema(description = "状态:0 未到期 / 1 催款中 / 2 已回款(通常由事件自动维护,前端可手动改 1 催款中)")
    private Integer status;

    private String remark;
}
