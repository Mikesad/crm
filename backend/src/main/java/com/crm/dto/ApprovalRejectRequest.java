package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批驳回请求
 */
@Data
@Schema(description = "审批驳回请求")
public class ApprovalRejectRequest {

    @NotNull(message = "审批 ID 不能为空")
    private Long id;

    @NotBlank(message = "驳回原因必填")
    @Size(max = 500, message = "驳回原因最长 500 字符")
    @Schema(description = "驳回原因,必填,用于告知申请人")
    private String comment;
}
