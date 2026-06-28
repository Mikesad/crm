package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报表 Tab ③ 跟进与转化率(主 VO)
 *
 * <p>聚合字段:</p>
 * <ul>
 *   <li>{@code kpis} 4 个核心 KPI(跟进总数 / 平均转化率 / 日均跟进 / 逾期数)</li>
 *   <li>{@code stageFunnel} 阶段转化漏斗(从新建线索到赢单,含每阶段转化率)</li>
 *   <li>{@code followTypeDist} 跟进方式分布(电话/微信/拜访/邮件/其他 饼图)</li>
 *   <li>{@code topPerformers} 高频跟进人榜(按跟进条数排序)</li>
 *   <li>{@code teamVsCompany} 本团队 vs 全公司 转化率对比(柱状)</li>
 *   <li>{@code trend} 6 月跟进频次趋势(粒度月)</li>
 * </ul>
 */
@Data
@Schema(description = "跟进与转化率报表")
public class ReportConversionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 4 KPI 单元 */
    private List<ReportKpiVO> kpis;

    /** 阶段转化漏斗(新建线索 → 需求分析 → 方案报价 → 商务谈判 → 赢单) */
    private List<ReportFunnelStageVO> stageFunnel;

    /** 跟进方式分布 */
    private List<ReportDistItemVO> followTypeDist;

    /** 高频跟进人榜 TOP N */
    private List<ReportPerformerVO> topPerformers;

    /** 本团队 vs 全公司 转化率对比(2 条 series 的柱状,key="team"/"company") */
    private List<ReportConversionCompareVO> teamVsCompany;

    /** 6 月跟进频次趋势 */
    private List<ReportTrendPointVO> trend;
}
