package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户响应
 */
@Data
@Schema(description = "客户响应")
public class CustomerVO {

    private Long id;
    private String customerName;
    private String industry;
    private String level;
    private String levelText;

    private Long ownerUserId;
    private String ownerName;

    @Schema(description = "0 私海 / 1 公海")
    private Integer isPublic;

    private LocalDateTime lastFollowTime;
    private LocalDateTime createTime;
}
