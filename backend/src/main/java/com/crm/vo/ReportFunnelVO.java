package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报表 Tab ① 销售漏斗 + 业绩(主 VO)
 *
 * <p>聚合字段:</p>
 * <ul>
 *   <li>{@code kpis} 6 个核心 KPI(销售总额 / 新签合同 / 客单价 / 赢单率 / 转化率 / 回款金额)</li>
 *   <li>{@code funnel} 漏斗 5 阶段(每阶段:阶段名 / 商机数 / 平均金额 / 阶段转化率)</li>
 *   <li>{@code trend} 近 6 月销售总额折线(粒度月)</li>
 *   <li>{@code departmentPerformers} 部门业绩柱状(部门名 / 总额)</li>
 *   <li>{@code sourceDistribution} 客户来源饼图(来源 / 客户数)</li>
 *   <li>{@code topPerformers} 销售个人榜 TOP N(默认 5)</li>
 * </ul>
 */
@Data
@Schema(description = "销售漏斗 + 业绩报表")
public class ReportFunnelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 6 KPI 单元 */
    private List<ReportKpiVO> kpis;

    /** 漏斗 5 阶段(新建线索 / 需求分析 / 方案报价 / 商务谈判 / 赢单) */
    private List<ReportFunnelStageVO> funnel;

    /** 近 6 月销售趋势(粒度月;granularity=day 时返回近 30 天日数据) */
    private List<ReportTrendPointVO> trend;

    /** 部门业绩(按销售总额排序) */
    private List<ReportDepartmentAmountVO> departmentPerformers;

    /** 客户来源分布(用于饼图) */
    private List<ReportDistItemVO> sourceDistribution;

    /** 销售个人榜 TOP N */
    private List<ReportPerformerVO> topPerformers;
}
