package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建合同请求
 *
 * <p>前端传 {@code totalAmount}，后端按明细实时重算（{@code sales_price = standard_price * discount / 10}），
 * 误差 > 0.01 视为篡改直接拒绝。
 * 最低折扣 &lt; 8.5 折时合同 status=0 (审批中) 并自动创建 crm_approval 待总监审批。</p>
 */
@Data
@Schema(description = "创建合同请求")
public class ContractCreateRequest {

    @Size(max = 100, message = "合同名称最长 100 字符")
    private String contractName;

    @NotNull(message = "客户 ID 不能为空")
    private Long customerId;

    @Schema(description = "商机 ID,从商机赢单跳转时填入")
    private Long businessId;

    @Schema(description = "前端传来的合同总金额,后端会按明细重算并校验")
    @NotNull(message = "合同总金额不能为空")
    private BigDecimal totalAmount;

    private LocalDate startDate;
    private LocalDate endDate;

    @NotEmpty(message = "合同明细不能为空")
    @Valid
    private List<ContractItemRequest> items;
}
