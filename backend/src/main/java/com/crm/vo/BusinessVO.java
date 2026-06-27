package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机响应
 */
@Data
@Schema(description = "商机响应")
public class BusinessVO {

    private Long id;
    private Long customerId;
    private String customerName;
    private String businessName;
    private BigDecimal expectedAmount;
    private LocalDate expectedDealDate;

    @Schema(description = "需求分析 / 方案报价 / 商务谈判 / 赢单 / 输单")
    private String stage;

    private Long ownerUserId;
    private String ownerName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
