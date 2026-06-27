package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户共享列表查询请求
 */
@Data
@Schema(description = "客户共享列表查询")
public class CustomerShareListRequest {

    @NotNull(message = "customerId 不能为空")
    @Min(value = 1, message = "customerId 必须 > 0")
    @Schema(description = "客户 ID")
    private Long customerId;
}
