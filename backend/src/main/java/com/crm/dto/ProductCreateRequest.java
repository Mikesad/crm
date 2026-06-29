package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建产品请求
 */
@Data
@Schema(description = "创建产品请求")
public class ProductCreateRequest {

    @Schema(description = "产品分类 ID")
    private Long categoryId;

    @NotBlank(message = "产品编码不能为空")
    @Size(max = 50, message = "产品编码最长 50 字符")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称最长 100 字符")
    private String productName;

    @Size(max = 100, message = "规格最长 100 字符")
    private String spec;

    @NotNull(message = "标准售价不能为空")
    @DecimalMin(value = "0.00", message = "标准售价不能小于 0")
    private BigDecimal price;

    @Size(max = 10, message = "单位最长 10 字符")
    private String unit = "个";

    // v0.7:移除套餐线 / 计费周期字段(撤销 D4 中度 SaaS 升级)

    @Schema(description = "状态：0 下架 / 1 上架", example = "1")
    private Integer status = 1;
}
