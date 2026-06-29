package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 产品分类分页查询请求
 */
@Data
@Schema(description = "产品分类分页查询请求")
public class ProductCategoryQueryRequest extends PageQuery {

    @Schema(description = "关键字(分类名称模糊匹配)", example = "核心")
    private String keyword;
}
