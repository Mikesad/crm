package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户共享创建请求
 */
@Data
@Schema(description = "客户共享创建请求")
public class CustomerShareCreateRequest {

    @NotNull(message = "customerId 不能为空")
    @Min(value = 1, message = "customerId 必须 > 0")
    @Schema(description = "客户 ID,必须是当前用户的私海客户")
    private Long customerId;

    @NotNull(message = "userId 不能为空")
    @Min(value = 1, message = "userId 必须 > 0")
    @Schema(description = "被共享人用户 ID")
    private Long userId;

    @NotNull(message = "authType 不能为空")
    @Schema(description = "权限类型:1 只读 / 2 读写", example = "2")
    private Integer authType;
}
