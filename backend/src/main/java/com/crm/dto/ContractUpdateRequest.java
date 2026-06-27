package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新合同请求
 *
 * <p>V1 仅允许修改合同名称/起止日期，明细不允许修改（防止金额被绕过重算）。
 * 状态流转由 {@code ApprovalService}（审批通过/驳回）和 {@code ReceivableEventListener}（全部回款完成）触发。</p>
 */
@Data
@Schema(description = "更新合同请求")
public class ContractUpdateRequest {

    @NotNull(message = "合同 ID 不能为空")
    private Long id;

    @Size(max = 100, message = "合同名称最长 100 字符")
    private String contractName;

    private LocalDate startDate;
    private LocalDate endDate;
}
