package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 回款计划查询请求
 *
 * <p>V1 用 list 替代 page(单合同计划数通常 3~10),仅支持按合同 ID 过滤 + 状态过滤。</p>
 */
@Data
@Schema(description = "回款计划查询请求")
public class ReceivablePlanQueryRequest {

    @Schema(description = "合同 ID 必传", example = "1")
    private Long contractId;

    @Schema(description = "状态:0 未到期 / 1 催款中 / 2 已回款;null 返回全部")
    private Integer status;
}
