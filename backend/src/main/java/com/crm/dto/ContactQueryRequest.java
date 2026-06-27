package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 联系人查询
 *
 * <p>联系人查询强依赖 customerId（必须指定看哪个客户的联系人），不需要分页。</p>
 */
@Data
@Schema(description = "联系人查询")
public class ContactQueryRequest {

    @NotNull(message = "客户 ID 不能为空")
    private Long customerId;

    @Schema(description = "关键字（模糊匹配 contactName / phone）")
    private String keyword;
}
