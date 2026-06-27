package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录响应
 */
@Data
@Schema(description = "跟进记录响应")
public class RecordVO {

    private Long id;
    private String relatedType;
    private Long relatedId;
    private String content;
    private String followType;
    private LocalDateTime nextFollowTime;
    private String createBy;
    private LocalDateTime createTime;
}
