package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 标记线索为死线索 请求 DTO（阶段五新增）
 *
 * <p>死因可选,前端表单显示"建议填写"提示,后端不强制必填。</p>
 */
@Data
@Schema(description = "标记线索为死线索")
public class LeadMarkDeadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 死因(可选,最长 500 字符) */
    @Schema(description = "死因备注,可空,最长 500 字符")
    @Size(max = 500, message = "死因最长 500 字符")
    private String deadReason;
}