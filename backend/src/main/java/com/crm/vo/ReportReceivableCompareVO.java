package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表 Tab ④ "实际回款 vs 理应回款" 饼图数据(阶段八 commit 4·2026-06-30)
 *
 * <p>核心 3 个数:</p>
 * <ul>
 *   <li>{@code actualAmount}  实际回款(crm_receivable.actual_amount 按 return_date 区间 SUM)</li>
 *   <li>{@code plannedAmount} 理应回款(crm_receivable_plan.expected_amount 按 expected_date 区间 SUM,过滤 is_deleted=0)</li>
 *   <li>{@code completionRate} 完成率 = actualAmount / plannedAmount × 100%,保留 1 位小数 + %</li>
 * </ul>
 *
 * <p>衍生字段(前端饼图 / 完成率展示):</p>
 * <ul>
 *   <li>{@code gapAmount} 应回未回(plannedAmount - actualAmount,可能为负数表示已超收)</li>
 *   <li>{@code actualPercent} / {@code gapPercent} 饼图两块各自的百分比(完成率 100% 时 = 1.00)</li>
 * </ul>
 */
@Data
@Schema(description = "实际回款 vs 理应回款 饼图数据")
public class ReportReceivableCompareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实际回款金额(已收) */
    private String actualAmount;

    /** 理应回款金额(计划) */
    private String plannedAmount;

    /** 应回未回(差额,正数=欠款,负数=超收) */
    private String gapAmount;

    /** 完成率(百分比字符串,1 位小数 + %) */
    private String completionRate;

    /** 饼图"实际回款"占比(小数 0~1) */
    private String actualPercent;

    /** 饼图"应回未回"占比(小数 0~1) */
    private String gapPercent;
}