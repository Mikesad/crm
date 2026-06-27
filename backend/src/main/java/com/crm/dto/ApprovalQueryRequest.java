package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批分页查询请求
 */
@Data
@Schema(description = "审批分页查询请求")
public class ApprovalQueryRequest extends PageQuery {

    @Schema(description = "状态:0 待审 / 1 通过 / 2 驳回 / 3 撤回;null 返回全部")
    private Integer status;

    @Schema(description = "合同 ID 精确过滤")
    private Long contractId;

    @Schema(description = "申请人 ID 精确过滤(销售看自己)")
    private Long applicantId;

    @Schema(description = "审批人 ID 精确过滤")
    private Long approverId;
}
