package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索响应
 */
@Data
@Schema(description = "线索响应")
public class LeadVO {

    private Long id;
    private String leadName;
    private String contactName;
    private String phone;
    private String source;

    @Schema(description = "1 未跟进 / 2 跟进中 / 3 已转客户 / 4 已死线索")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    private Long ownerUserId;
    private String ownerName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
