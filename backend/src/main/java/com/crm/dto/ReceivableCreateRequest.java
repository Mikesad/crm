package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录入回款请求
 *
 * <p>财务录入实际回款,planId 可空(计划外回款),提交后异步发
 * {@code ReceivableRecordedEvent} 联动 plan/contract 状态。</p>
 */
@Data
@Schema(description = "录入回款请求")
public class ReceivableCreateRequest {

    @NotNull(message = "合同 ID 不能为空")
    private Long contractId;

    @Schema(description = "对应回款计划 ID,可空(计划外回款,如押金/多打款)")
    private Long planId;

    @NotNull(message = "回款金额不能为空")
    @DecimalMin(value = "0.01", message = "回款金额必须 > 0")
    private BigDecimal actualAmount;

    @NotNull(message = "回款日期不能为空")
    private LocalDate returnDate;

    @Schema(description = "支付方式:银行转账/微信/支付宝/现金,默认 银行转账")
    private String paymentMethod = "银行转账";
}
