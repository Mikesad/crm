package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收明细中的单个客户条目
 */
@Data
@Schema(description = "回收明细")
public class RecycledCustomerVO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名")
    private String customerName;

    @Schema(description = "原 owner 用户 ID(回收后置 NULL)")
    private Long ownerUserId;

    @Schema(description = "最后跟进时间(回收判定依据)")
    private LocalDateTime lastFollowTime;
}
