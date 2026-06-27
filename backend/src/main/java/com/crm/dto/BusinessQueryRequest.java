package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商机查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商机查询")
public class BusinessQueryRequest extends PageQuery {

    @Schema(description = "关键字（模糊匹配 businessName）")
    private String keyword;

    @Schema(description = "关联客户 ID")
    private Long customerId;

    @Schema(description = "商机阶段：需求分析 / 方案报价 / 商务谈判 / 赢单 / 输单")
    private String stage;
}
