package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商机阶段推进请求
 *
 * <p>用于 {@code PUT /api/crm/business/{id}/stage}。</p>
 *
 * <p>目标阶段：需求分析 / 方案报价 / 商务谈判 / 赢单 / 输单。</p>
 *
 * <p>严格单向校验：</p>
 * <ul>
 *   <li>赢单 / 输单 → 不可再变更</li>
 *   <li>任意阶段 → 输单：允许（放弃）</li>
 *   <li>需求分析 → 方案报价 → 商务谈判 → 赢单：单向推进，不允许跳级</li>
 * </ul>
 */
@Data
@Schema(description = "商机阶段变更请求")
public class BusinessStageUpdateRequest {

    @NotBlank(message = "目标阶段不能为空")
    private String stage;

    /**
     * 跟进内容（阶段变更时强制要求）
     * <p>由 Service 层同步写入 crm_record 时间轴。</p>
     */
    private String followContent;
}
