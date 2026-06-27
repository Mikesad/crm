package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户共享响应
 */
@Data
@Schema(description = "客户共享响应")
public class CustomerShareVO {

    private Long id;
    private Long customerId;
    private Long userId;
    private String userNickname;
    private String userDeptName;
    private Integer authType;
    private String authTypeText;
    private String createBy;
    private LocalDateTime createTime;
}
