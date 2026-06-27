package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增跟进记录请求
 *
 * <p>跟进记录只能新增，不能修改或删除（CRM 行业惯例）。</p>
 */
@Data
@Schema(description = "新增跟进记录请求")
public class RecordCreateRequest {

    /**
     * 关联类型
     * <p>lead / customer / business</p>
     */
    @NotBlank(message = "关联类型不能为空")
    @Size(max = 20, message = "关联类型最长 20 字符")
    private String relatedType;

    @NotNull(message = "关联主体 ID 不能为空")
    private Long relatedId;

    @NotBlank(message = "跟进内容不能为空")
    private String content;

    @Schema(description = "跟进方式：电话 / 微信 / 上门拜访 / 邮件（默认 电话）")
    private String followType = "电话";

    /** 下次跟进时间（可选） */
    private LocalDateTime nextFollowTime;
}
