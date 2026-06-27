package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线索查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "线索查询")
public class LeadQueryRequest extends PageQuery {

    @Schema(description = "关键字（模糊匹配 leadName / contactName / phone）")
    private String keyword;

    @Schema(description = "状态过滤：1 未跟进 / 2 跟进中 / 3 已转客户 / 4 已死线索")
    private Integer status;

    @Schema(description = "线索来源")
    private String source;
}
