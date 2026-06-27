package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新线索请求
 */
@Data
@Schema(description = "更新线索请求")
public class LeadUpdateRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    private String leadName;
    private String contactName;
    private String phone;
    private String source;

    /**
     * 状态变更：1 未跟进 / 2 跟进中 / 4 已死线索
     * <p>注意 3 已转客户 由 {@code LeadService.convertToCustomer()} 单独管理，不允许通过此字段修改。</p>
     */
    private Integer status;

    private String remark;
}
