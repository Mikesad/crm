package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品分类响应
 */
@Data
@Schema(description = "产品分类响应")
public class ProductCategoryVO {

    private Long id;
    private Long parentId;
    private String categoryName;

    @Schema(description = "关联产品数(批量填充,删除前引用校验也用)")
    private Long productCount;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
