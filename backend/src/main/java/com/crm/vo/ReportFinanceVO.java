package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报表 Tab ④ 回款 / 财务(主 VO)
 *
 * <p>聚合字段:</p>
 * <ul>
 *   <li>{@code kpis} 4 个核心 KPI(合同总额 / 已回款 / 未回款 / 逾期率)</li>
 *   <li>{@code trend} 回款趋势(合同 / 已回款 / 预测未回款 3 series,粒度月)</li>
 *   <li>{@code monthlyStacked} 月度回款堆叠柱(各月已回款 / 计划未回款)</li>
 *   <li>{@code agingBuckets} 账龄分布(0-30 / 31-60 / 61-90 / 90+ 天,横向柱状)</li>
 *   <li>{@code receivableMethod} 回款方式分布(银行转账 / 现金 / 票据 / 支付宝微信)</li>
 *   <li>{@code topDebtors} 应收 TopN 客户榜(按未回款金额排序)</li>
 * </ul>
 */
@Data
@Schema(description = "回款 / 财务报表")
public class ReportFinanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 4 KPI 单元 */
    private List<ReportKpiVO> kpis;

    /** 回款趋势(3 series:contract / received / predicted,粒度月) */
    private List<ReportTrendPointVO> trend;

    /** 月度回款堆叠(用于堆叠柱状图) */
    private List<ReportTrendPointVO> monthlyStacked;

    /** 账龄分布(0-30 / 31-60 / 61-90 / 90+ 天) */
    private List<ReportAgingBucketVO> agingBuckets;

    /** 回款方式分布 */
    private List<ReportDistItemVO> receivableMethod;

    /** 应收 TopN 客户榜 */
    private List<ReportPerformerVO> topDebtors;
}
