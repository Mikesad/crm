package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户活跃度单元(报表 Tab ②)
 *
 * <p>定义:活跃 = last_follow_time 在 [now-30d, now];沉睡 = 超过 30d;
 * 公海 = is_public=1;总数 = 全部有效(is_deleted=0)。</p>
 */
@Data
@Schema(description = "客户活跃度")
public class ReportActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 客户总数 */
    private Long total;

    /** 活跃数(近 30 天有跟进) */
    private Long active;

    /** 沉睡数(超 30 天无跟进) */
    private Long dormant;

    /** 公海数(is_public=1) */
    private Long publicPool;

    /** 活跃占比 */
    private String activePercent;

    /** 沉睡占比 */
    private String dormantPercent;

    /** 公海占比 */
    private String publicPercent;
}
