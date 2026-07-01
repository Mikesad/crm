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
 *   <li>{@code contractStatusDistribution} 合同状态分布(phase8 commit1 新增,4 状态:审批中/执行中/已结束/已作废)</li>
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

    /**
     * phase8 commit1 新增:合同状态分布(4 状态:0 审批中 / 1 执行中 / 2 已结束 / 3 已作废)
     * <p>Tab ④ 新增"合同状态分布"图;字段含 key=状态枚举 / count=合同数 / amount=金额合计 / percent=占比</p>
     */
    private List<ReportDistItemVO> contractStatusDistribution;
}
