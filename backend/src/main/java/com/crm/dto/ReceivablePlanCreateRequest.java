package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量创建回款计划请求
 *
 * <p>合同审批通过后(contract.status=1),销售可一次性录入多条回款计划。
 * 期数(period) 需递增,重复期数会被拒绝。</p>
 */
@Data
@Schema(description = "批量创建回款计划请求")
public class ReceivablePlanCreateRequest {

    @NotNull(message = "合同 ID 不能为空")
    private Long contractId;

    @NotEmpty(message = "回款计划不能为空,至少 1 期")
    @Valid
    private List<ReceivablePlanItemRequest> plans;
}
