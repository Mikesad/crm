package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报表 Tab ② 客户分布(主 VO)
 *
 * <p>聚合字段:</p>
 * <ul>
 *   <li>{@code kpis} 4 个核心 KPI(客户总数 / 行业数 / 公海占比 / 沉睡客户数)</li>
 *   <li>{@code distribution} 维度分布(按 {@code dim} 切换:industry/region/level/source),LIMIT 500 兜底</li>
 *   <li>{@code activity} 活跃/沉睡/公海/总数 + 趋势(粒度月)</li>
 *   <li>{@code levelDistribution} 客户等级饼图(独立于 {@code dim} 切换,常显)</li>
 *   <li>{@code regionDistribution} 地区分布(简化为 TOP 10 城市,避免 GeoJSON 复杂化)</li>
 * </ul>
 */
@Data
@Schema(description = "客户分布报表")
public class ReportCustomerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 4 KPI 单元 */
    private List<ReportKpiVO> kpis;

    /** 主维度分布(按 dim 切换,LIMIT 500) */
    private List<ReportDistItemVO> distribution;

    /** 客户活跃度(活跃 / 沉睡 / 公海 / 总数) */
    private ReportActivityVO activity;

    /** 客户等级分布(常显,饼图) */
    private List<ReportDistItemVO> levelDistribution;

    /** 地区分布 TOP 10(横向柱状) */
    private List<ReportDistItemVO> regionDistribution;

    /** 活跃 vs 沉睡 6 月趋势 */
    private List<ReportTrendPointVO> activityTrend;
}
