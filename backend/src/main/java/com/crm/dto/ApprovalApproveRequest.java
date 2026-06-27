package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批通过请求
 */
@Data
@Schema(description = "审批通过请求")
public class ApprovalApproveRequest {

    @NotNull(message = "审批 ID 不能为空")
    private Long id;

    @Size(max = 500, message = "审批意见最长 500 字符")
    @Schema(description = "审批意见,选填")
    private String comment;
}
