package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 跟进时间轴查询
 *
 * <p>用于 {@code GET /api/crm/record/timeline}，按 relatedType + relatedId 拉取时间轴，
 * 按 {@code create_time DESC} 倒序返回。</p>
 */
@Data
@Schema(description = "跟进时间轴查询")
public class RecordQueryRequest {

    @NotBlank(message = "关联类型不能为空")
    private String relatedType;

    @NotNull(message = "关联主体 ID 不能为空")
    private Long relatedId;
}
