package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新产品请求
 */
@Data
@Schema(description = "更新产品请求")
public class ProductUpdateRequest {

    @NotNull(message = "产品 ID 不能为空")
    private Long id;

    private Long categoryId;

    @Size(max = 50, message = "产品编码最长 50 字符")
    private String productCode;

    @Size(max = 100, message = "产品名称最长 100 字符")
    private String productName;

    @Size(max = 100, message = "规格最长 100 字符")
    private String spec;

    @DecimalMin(value = "0.00", message = "标准售价不能小于 0")
    private BigDecimal price;

    @Size(max = 10, message = "单位最长 10 字符")
    private String unit;

    @Schema(description = "状态：0 下架 / 1 上架")
    private Integer status;
}
