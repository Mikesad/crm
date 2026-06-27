package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户查询")
public class CustomerQueryRequest extends PageQuery {

    @Schema(description = "关键字（模糊匹配 customerName）")
    private String keyword;

    @Schema(description = "客户级别过滤")
    private String level;

    @Schema(description = "行业过滤")
    private String industry;

    /**
     * 是否查询公海池：0/null 私海 / 1 公海
     * <p>私海仅返回当前用户作为 owner 的客户；公海返回 ownerUserId IS NULL 的客户。</p>
     */
    private Integer isPublic;
}
