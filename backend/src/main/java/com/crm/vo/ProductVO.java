package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品响应
 */
@Data
@Schema(description = "产品响应")
public class ProductVO {

    private Long id;
    private Long categoryId;

    private String productCode;
    private String productName;
    private String spec;

    @Schema(description = "标准售价")
    private BigDecimal price;

    private String unit;

    @Schema(description = "状态：0 下架 / 1 上架")
    private Integer status;
    private String statusText;

    // v0.7:移除套餐线 / 计费周期字段(撤销 D4 中度 SaaS 升级)

    @Schema(description = "产品分类名称(批量填充,阶段六 commit 2 新增)")
    private String categoryName;

    private LocalDateTime createTime;
}
