package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款计划响应
 */
@Data
@Schema(description = "回款计划响应")
public class ReceivablePlanVO {

    private Long id;
    private Long contractId;

    private Integer period;
    private BigDecimal expectedAmount;
    private LocalDate expectedDate;

    @Schema(description = "状态:0 未到期 / 1 催款中 / 2 已回款")
    private Integer status;
    private String statusText;

    private String remark;

    @Schema(description = "该计划累计实收(由 ReceivableEventListener 维护)")
    private BigDecimal receivedAmount;

    private LocalDateTime createTime;
}
