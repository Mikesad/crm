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

    /**
     * 当前登录用户是否被共享了此客户（crm_customer_share 命中）
     * <p>phase8 commit1 修复:前端 ownerBadgeText 兜底分支错把所有"非自己 own"标成共享,改为按此标志位渲染。</p>
     */
    @Schema(description = "是否被共享给当前登录用户")
    private Boolean sharedToMe;

    private LocalDateTime lastFollowTime;
    private LocalDateTime createTime;
}
