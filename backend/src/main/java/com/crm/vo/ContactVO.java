package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人响应
 */
@Data
@Schema(description = "联系人响应")
public class ContactVO {

    private Long id;
    private Long customerId;
    private String contactName;
    private String post;
    private String phone;

    @Schema(description = "0 否 / 1 是")
    private Integer isMaster;

    @Schema(description = "1 核心决策者 / 2 弱影响者 / 3 普通职员")
    private Integer decisionWeight;

    private String decisionWeightText;
    private LocalDateTime createTime;
}
