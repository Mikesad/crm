package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款响应
 */
@Data
@Schema(description = "回款响应")
public class ReceivableVO {

    private Long id;
    private String receivableNum;

    private Long contractId;
    private String contractNum;
    private String contractName;

    private Long planId;
    @Schema(description = "对应期数(planId 空时为 null)")
    private Integer planPeriod;
    @Schema(description = "true 计划外回款 / false 关联计划")
    private Boolean planExtra;

    private BigDecimal actualAmount;
    private LocalDate returnDate;
    private String paymentMethod;

    private String createBy;
    private LocalDateTime createTime;
}
