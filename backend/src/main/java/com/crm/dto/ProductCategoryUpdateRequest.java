package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新产品分类请求
 */
@Data
@Schema(description = "更新产品分类请求")
public class ProductCategoryUpdateRequest {

    @NotNull(message = "分类 ID 不能为空")
    private Long id;

    @Schema(description = "父分类 ID(V1 暂不开放多级,固定传 0)", example = "0")
    private Long parentId;

    @Size(min = 2, max = 50, message = "分类名称 2-50 字符")
    private String categoryName;
}
