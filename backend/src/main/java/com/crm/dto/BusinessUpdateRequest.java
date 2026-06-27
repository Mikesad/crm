package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新商机请求
 *
 * <p>注意：stage 字段不在此处修改，阶段流转走专用端点
 * {@code PUT /api/crm/business/{id}/stage}，由 {@code BusinessService.updateStage()} 校验单向性。</p>
 */
@Data
@Schema(description = "更新商机请求")
public class BusinessUpdateRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    private String businessName;
    private BigDecimal expectedAmount;
    private LocalDate expectedDealDate;
}
