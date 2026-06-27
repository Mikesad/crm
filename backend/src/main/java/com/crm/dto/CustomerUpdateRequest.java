package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新客户请求
 */
@Data
@Schema(description = "更新客户请求")
public class CustomerUpdateRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    private String customerName;
    private String industry;
    private String level;
}
