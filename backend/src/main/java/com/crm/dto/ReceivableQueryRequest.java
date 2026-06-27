package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 回款分页查询请求
 */
@Data
@Schema(description = "回款分页查询请求")
public class ReceivableQueryRequest extends PageQuery {

    @Schema(description = "合同 ID 精确过滤")
    private Long contractId;

    @Schema(description = "对应计划 ID 过滤")
    private Long planId;

    @Schema(description = "支付方式过滤")
    private String paymentMethod;

    @Schema(description = "回款起始日期")
    private LocalDate returnDateStart;

    @Schema(description = "回款结束日期")
    private LocalDate returnDateEnd;
}
