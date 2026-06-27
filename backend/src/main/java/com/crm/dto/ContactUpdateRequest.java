package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新联系人请求
 */
@Data
@Schema(description = "更新联系人请求")
public class ContactUpdateRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    private String contactName;
    private String post;
    private String phone;
    private Integer isMaster;
    private Integer decisionWeight;
}
