package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公海池回收请求（手动触发 / 调度统一入口）
 *
 * <p>三个参数均可空，空时回退到 yml 默认或安全上限。</p>
 */
@Data
@Schema(description = "公海池回收请求")
public class RecycleRequest {

    /**
     * 阈值秒数
     * <p>lastFollowTime 距今超过该秒数的客户会被回收。范围 1~7,776,000(1秒~90天)。</p>
     * <p>为空时取 yml {@code crm.customer.public-pool-days × 86400}。</p>
     */
    @Schema(description = "阈值秒数,范围 1~7776000;空=读 yml 默认(15天)")
    private Long thresholdSeconds;

    /**
     * 本次最多回收的条数,防误伤
     * <p>范围 1~10000,空=1000。</p>
     */
    @Schema(description = "本次最多回收条数,范围 1~10000;空=1000")
    private Integer limit;

    /**
     * 是否 dry run
     * <p>true 时只统计不真回收,返回的 details 是将被回收的列表。</p>
     */
    @Schema(description = "true=只统计不真回收")
    private Boolean dryRun;
}
