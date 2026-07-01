package com.crm.service;

import com.crm.entity.CrmReceivablePlan;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportDistItemVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表 Tab ④ 回款 / 财务
 *
 * <p>聚合维度(phase8 commit1 第 3 次修订):</p>
 * <ul>
 *   <li>2 KPI:合同总额(status IN (1,2) + range) / 已回款(按 range)</li>
 *   <li>回款率 = 已回款 / 合同总额 × 100%(分母=执行中+已结束的合同总额,分子=已回款)</li>
 *   <li>合同状态分布:全量聚合 4 状态(不受 range 影响,让"执行中"显示真实全量)</li>
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

        // 合同总额按 range 过滤(口径:status IN (1,2) + start_date ∈ [range])
        BigDecimal contractTotal = contractMapper.sumTotalAmount(start, end, ownerIds);
        BigDecimal received = receivableMapper.sumActualByRange(start, end);

        ReportFinanceVO vo = new ReportFinanceVO();
        vo.setKpis(buildKpis(contractTotal, received));
        vo.setReceivableCompare(buildReceivableCompare(contractTotal, received));
        vo.setContractStatusDistribution(buildContractStatusDistribution(ownerIds));
        return vo;
    }

    /**
     * 2 KPI:合同总额 / 已回款(phase8 commit1 第 3 次修订,删未回款+逾期率)
     */
    private List<ReportKpiVO> buildKpis(BigDecimal contractTotal, BigDecimal received) {
        List<ReportKpiVO> kpis = new ArrayList<>(2);
        ReportKpiVO k1 = new ReportKpiVO();
        k1.setKey("contractTotal"); k1.setLabel("合同总额");
        k1.setValue(contractTotal.toPlainString()); k1.setUnit("¥");
        kpis.add(k1);
        ReportKpiVO k2 = new ReportKpiVO();
        k2.setKey("received"); k2.setLabel("已回款");
        k2.setValue(received.toPlainString()); k2.setUnit("¥");
        kpis.add(k2);
        return kpis;
    }

    /**
     * 回款率(phase8 commit1 第 3 次修订):
     * <ul>
     *   <li>分母 = 执行中 + 已结束的合同总额(SUM crm_contract.total_amount WHERE status IN (1,2) AND start_date IN [range])</li>
     *   <li>分子 = 已回款(SUM crm_receivable.actual_amount WHERE return_date IN [range])</li>
     *   <li>VO 字段复用:actualAmount=已回款 / plannedAmount=合同总额 / gapAmount=未回款(合同-已回)</li>
     * </ul>
     */
    private ReportReceivableCompareVO buildReceivableCompare(BigDecimal contractTotal, BigDecimal received) {
        BigDecimal unreceived = contractTotal.subtract(received);

        String completionRate;
        if (contractTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = received.multiply(BigDecimal.valueOf(100))
                    .divide(contractTotal, 1, RoundingMode.HALF_UP);
            completionRate = rate.toPlainString() + "%";
        } else {
            completionRate = "0%";
        }

        ReportReceivableCompareVO vo = new ReportReceivableCompareVO();
        vo.setActualAmount(received.toPlainString());
        vo.setPlannedAmount(contractTotal.toPlainString());   // 复用字段:合同总额
        vo.setGapAmount(unreceived.toPlainString());           // 未回款 = 合同 - 已回
        vo.setCompletionRate(completionRate);

        // 饼图占比:已回 vs 未回(分母=合同总额,不再是 已回+未回 合计)
        if (contractTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actualPercent = received.divide(contractTotal, 4, RoundingMode.HALF_UP);
            BigDecimal gapPercent = BigDecimal.ONE.subtract(actualPercent);
            // 超收(>100%)归一化
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

    /** 合同状态枚举 → 中文 label */
    private static final Map<Integer, String> CONTRACT_STATUS_LABEL = Map.of(
            0, "审批中",
            1, "执行中",
            2, "已结束",
            3, "已作废"
    );

    /**
     * 合同状态分布:全量聚合(phase8 commit1 第 3 次修订,传 null 范围让 groupByStatus 全量)
     * <p>这样"执行中"显示真实全量(14 份 ¥2.6M),不被 range 范围切割</p>
     */
    private List<ReportDistItemVO> buildContractStatusDistribution(List<Long> ownerIds) {
        List<Map<String, Object>> rows = contractMapper.groupByStatus(null, null, ownerIds);
        Map<Integer, long[]> agg = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object statusObj = r.get("status");
            Integer status = null;
            if (statusObj instanceof Number) status = ((Number) statusObj).intValue();
            else if (statusObj != null) {
                try { status = Integer.parseInt(statusObj.toString()); } catch (NumberFormatException ignore) {}
            }
            if (status == null) continue;
            long cnt = ReportUtils.toLong(r.get("cnt"));
            BigDecimal sum = ReportUtils.toBigDecimal(r.get("sum"));
            agg.put(status, new long[]{cnt, sum.multiply(BigDecimal.valueOf(100)).longValue()});
        }
        long totalCnt = agg.values().stream().mapToLong(a -> a[0]).sum();

        List<ReportDistItemVO> out = new ArrayList<>(4);
        for (int s = 0; s <= 3; s++) {
            ReportDistItemVO item = new ReportDistItemVO();
            item.setKey(CONTRACT_STATUS_LABEL.get(s));
            long[] a = agg.getOrDefault(s, new long[]{0L, 0L});
            item.setCount(a[0]);
            item.setAmount(BigDecimal.valueOf(a[1]).divide(BigDecimal.valueOf(100)).toPlainString());
            String percent = totalCnt > 0
                    ? new BigDecimal(a[0] * 100).divide(new BigDecimal(totalCnt), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                    : "0%";
            item.setPercent(percent);
            out.add(item);
        }
        return out;
    }
}