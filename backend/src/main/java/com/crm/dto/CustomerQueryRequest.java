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

    /**
     * 是否仅查询"被共享给我"的客户(阶段四新增):1 = 只返回 crm_customer_share.user_id = 当前用户
     * <p>与 isPublic 互斥:若 sharedToMeOnly=1,无论 isPublic 是什么,只返回共享表命中的客户。</p>
     */
    private Integer sharedToMeOnly;

    // v0.16:排序字段(可选 lastFollowTime)
    @Schema(description = "排序字段:lastFollowTime(默认)")
    private String sortBy;

    @Schema(description = "排序方向:asc / desc(默认 desc)", example = "desc")
    private String order;
}
