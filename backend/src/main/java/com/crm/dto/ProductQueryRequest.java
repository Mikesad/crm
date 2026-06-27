package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 产品分页查询请求
 */
@Data
@Schema(description = "产品分页查询请求")
public class ProductQueryRequest extends PageQuery {

    @Schema(description = "关键字 (产品编码 / 名称 模糊匹配)", example = "CRM")
    private String keyword;

    @Schema(description = "产品分类 ID")
    private Long categoryId;

    @Schema(description = "状态：0 下架 / 1 上架")
    private Integer status;
}
