package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同明细响应
 */
@Data
@Schema(description = "合同明细响应")
public class ContractItemVO {

    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String spec;
    private String unit;

    private Integer count;

    @Schema(description = "标准单价(快照)")
    private BigDecimal standardPrice;

    @Schema(description = "实际成交单价 = standardPrice * discount / 10")
    private BigDecimal salesPrice;

    @Schema(description = "折扣,如 9.5 折存 9.50")
    private BigDecimal discount;

    @Schema(description = "小计 = salesPrice * count")
    private BigDecimal subtotal;
}
