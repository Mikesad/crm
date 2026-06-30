package com.crm.service;

import com.crm.entity.CrmReceivablePlan;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import com.crm.vo.ReportFinanceVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportReceivableCompareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报表 Tab ④ 回款 / 财务
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>4 KPI(合同总额 / 已回款 / 未回款 / 逾期率)</li>
 *   <li>实际回款 vs 理应回款 饼图(完成率口径)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportFinanceService {

    private final ReportCacheService cache;
    private final ReportQueryHelper helper;

    private final CrmContractMapper contractMapper;
    private final CrmReceivableMapper receivableMapper;
    private final CrmReceivablePlanMapper planMapper;

    public ReportFinanceVO buildFinanceReport(String range, String startDate, String endDate,
                                               Long deptId, Long userId, int topN) {
        String key = helper.cacheKey("finance", range, deptId, userId);
        return cache.getOrLoad(key, () -> doBuild(range, startDate, endDate, deptId, userId, topN));
    }

    private ReportFinanceVO doBuild(String range, String startDate, String endDate,
                                     Long deptId, Long userId, int topN) {
        LocalDateTime[] r = helper.resolveRange(range, startDate, endDate);
        List<Long> ownerIds = helper.resolveOwnerIds(deptId, userId);
        LocalDateTime start = r[0], end = r[1];

        ReportFinanceVO vo = new ReportFinanceVO();
        vo.setKpis(buildKpis(start, end, ownerIds));
        vo.setReceivableCompare(buildReceivableCompare(start, end));
        return vo;
    }

    private List<ReportKpiVO> buildKpis(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        BigDecimal contractTotal = contractMapper.sumTotalAmount(
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now(), ownerIds);
        BigDecimal received = receivableMapper.sumActualByRange(start, end);
        BigDecimal unreceived = planMapper.sumUnreceived();
        // 逾期率 = 逾期 plan 数 / 未回款 plan 数
        List<CrmReceivablePlan> plans = planMapper.listUnreceivedForAging();
        long overdue = plans.stream()
                .filter(p -> p.getExpectedDate() != null && p.getExpectedDate().isBefore(LocalDate.now()))
                .count();
        String overdueRate = plans.size() > 0
                ? new BigDecimal(overdue * 100).divide(new BigDecimal(plans.size()), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";

        List<ReportKpiVO> kpis = new ArrayList<>(4);
        ReportKpiVO k1 = new ReportKpiVO();
        k1.setKey("contractTotal"); k1.setLabel("合同总额");
        k1.setValue(contractTotal.toPlainString()); k1.setUnit("¥");
        kpis.add(k1);
        ReportKpiVO k2 = new ReportKpiVO();
        k2.setKey("received"); k2.setLabel("已回款");
        k2.setValue(received.toPlainString()); k2.setUnit("¥");
        kpis.add(k2);
        ReportKpiVO k3 = new ReportKpiVO();
        k3.setKey("unreceived"); k3.setLabel("未回款");
        k3.setValue(unreceived.toPlainString()); k3.setUnit("¥");
        kpis.add(k3);
        ReportKpiVO k4 = new ReportKpiVO();
        k4.setKey("overdueRate"); k4.setLabel("逾期率");
        k4.setValue(overdueRate);
        kpis.add(k4);
        return kpis;
    }

    /**
     * 实际回款 vs 理应回款 饼图(阶段八 commit 4·2026-06-30)
     * <ul>
     *   <li>实际回款 = SUM(crm_receivable.actual_amount) WHERE return_date IN [start, end]</li>
     *   <li>理应回款 = SUM(crm_receivable_plan.expected_amount) WHERE expected_date IN [start, end] AND is_deleted=0</li>
     *   <li>完成率 = actual / planned × 100%(planned=0 时返回 "0%")</li>
     *   <li>应回未回 = planned - actual(负数表示已超收)</li>
     * </ul>
     */
    private ReportReceivableCompareVO buildReceivableCompare(LocalDateTime start, LocalDateTime end) {
        BigDecimal actual = receivableMapper.sumActualByRange(start, end);
        BigDecimal planned = planMapper.sumExpectedByRange(start, end);

        BigDecimal gap = planned.subtract(actual);

        String completionRate;
        if (planned.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = actual.multiply(BigDecimal.valueOf(100))
                    .divide(planned, 1, RoundingMode.HALF_UP);
            completionRate = rate.toPlainString() + "%";
        } else {
            completionRate = "0%";
        }

        ReportReceivableCompareVO vo = new ReportReceivableCompareVO();
        vo.setActualAmount(actual.toPlainString());
        vo.setPlannedAmount(planned.toPlainString());
        vo.setGapAmount(gap.toPlainString());
        vo.setCompletionRate(completionRate);

        // 饼图占比:分母 = max(actual + max(0, gap), actual)
        // 简化口径:饼图 = actual vs (planned - actual 的绝对值),planned 为 0 时 actual=100%
        if (planned.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actualPercent = actual.divide(planned, 4, RoundingMode.HALF_UP);
            BigDecimal gapPercent = BigDecimal.ONE.subtract(actualPercent);
            // 超收(>100%)时归一化
            if (actualPercent.compareTo(BigDecimal.ONE) > 0) {
                actualPercent = BigDecimal.ONE;
                gapPercent = BigDecimal.ZERO;
            } else if (gapPercent.compareTo(BigDecimal.ZERO) < 0) {
                gapPercent = BigDecimal.ZERO;
            }
            vo.setActualPercent(actualPercent.toPlainString());
            vo.setGapPercent(gapPercent.toPlainString());
        } else {
            vo.setActualPercent("1.00");
            vo.setGapPercent("0.00");
        }
        return vo;
    }
}