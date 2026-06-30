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
 *   <li>{@code receivableCompare} 实际回款 vs 理应回款 饼图(完成率口径)</li>
 * </ul>
 */
@Data
@Schema(description = "回款 / 财务报表")
public class ReportFinanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 4 KPI 单元 */
    private List<ReportKpiVO> kpis;

    /**
     * 实际回款 vs 理应回款 饼图数据(阶段八 commit 4·2026-06-30,折线图 → 饼图)
     * <p>单对象,内部含实际金额 / 计划金额 / 完成率 / 饼图占比。</p>
     */
    private ReportReceivableCompareVO receivableCompare;
}
