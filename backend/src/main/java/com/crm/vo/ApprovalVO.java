package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批响应
 */
@Data
@Schema(description = "审批响应")
public class ApprovalVO {

    private Long id;
    private Long contractId;
    private String contractNum;
    private String contractName;

    private Long applicantId;
    private String applicantName;

    private Long approverId;
    private String approverName;

    @Schema(description = "状态:0 待审 / 1 通过 / 2 驳回 / 3 撤回")
    private Integer status;
    private String statusText;

    private String triggerReason;
    private String comment;

    private LocalDateTime createTime;
    private LocalDateTime finishTime;

    /** 合同总金额,审批列表显示用 */
    private BigDecimal contractTotalAmount;
    /** 合同最低折扣(触发审批的折扣率) */
    private BigDecimal minDiscount;
}
