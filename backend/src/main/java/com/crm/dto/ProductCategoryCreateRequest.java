package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建产品分类请求
 */
@Data
@Schema(description = "创建产品分类请求")
public class ProductCategoryCreateRequest {

    @Schema(description = "父分类 ID(V1 暂不开放多级,固定传 0)", example = "0")
    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 50, message = "分类名称 2-50 字符")
    private String categoryName;
}
