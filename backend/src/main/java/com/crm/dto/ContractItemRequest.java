package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同商品明细请求项（嵌套在 {@link ContractCreateRequest} 中）
 *
 * <p>前端传 {@code productId + count + discount}，后端按
 * {@code sales_price = standard_price * discount / 10} 反推并落库，
 * 防止前端篡改金额。</p>
 */
@Data
@Schema(description = "合同商品明细请求项")
public class ContractItemRequest {

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须 >= 1")
    private Integer count;

    @NotNull(message = "折扣不能为空")
    @DecimalMin(value = "0.01", message = "折扣必须 > 0")
    private BigDecimal discount;
}
