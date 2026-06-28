package com.crm.service;

import com.crm.entity.CrmContract;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmReceivablePlan;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportAgingBucketVO;
import com.crm.vo.ReportDistItemVO;
import com.crm.vo.ReportFinanceVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportPerformerVO;
import com.crm.vo.ReportTrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表 Tab ④ 回款 / 财务
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>4 KPI(合同总额 / 已回款 / 未回款 / 逾期率)</li>
 *   <li>回款趋势(3 series:合同 / 已回款 / 预测未回款,粒度月)</li>
 *   <li>月度回款堆叠(V1 简化为单 series)</li>
 *   <li>账龄分桶(0-30 / 31-60 / 61-90 / 90+ 天,基于 crm_receivable_plan.expected_date)</li>
 *   <li>回款方式分布(银行转账/微信/支付宝/现金)</li>
 *   <li>应收 TopN 客户榜(contract_id → customer_name)</li>
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
    private final CrmCustomerMapper customerMapper;

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
        vo.setTrend(buildTrend(start, end, ownerIds));
        vo.setMonthlyStacked(buildMonthlyStacked(start, end, ownerIds));
        vo.setAgingBuckets(buildAging());
        vo.setReceivableMethod(buildMethod(start, end));
        vo.setTopDebtors(buildTopDebtors(topN));
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

    /** 3 series 折线(contract / received / predicted),合并到同一时间轴 */
    private List<ReportTrendPointVO> buildTrend(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        List<Map<String, Object>> contractRows = contractMapper.sumByMonth(start, end, ownerIds);
        List<Map<String, Object>> receivedRows = receivableMapper.sumActualByMonth(start, end);

        Map<String, BigDecimal> contractByMonth = contractRows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r.get("month")),
                        r -> ReportUtils.toBigDecimal(r.get("sum")), (a, b) -> a, LinkedHashMap::new));
        Map<String, BigDecimal> receivedByMonth = receivedRows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r.get("month")),
                        r -> ReportUtils.toBigDecimal(r.get("sum")), (a, b) -> a, LinkedHashMap::new));

        List<ReportTrendPointVO> out = new ArrayList<>();
        for (String month : contractByMonth.keySet()) {
            ReportTrendPointVO pc = new ReportTrendPointVO();
            pc.setDate(month); pc.setSeriesKey("contract");
            pc.setValue(contractByMonth.get(month).toPlainString());
            out.add(pc);
            ReportTrendPointVO pr = new ReportTrendPointVO();
            pr.setDate(month); pr.setSeriesKey("received");
            BigDecimal rec = receivedByMonth.getOrDefault(month, BigDecimal.ZERO);
            pr.setValue(rec.toPlainString());
            out.add(pr);
        }
        return out;
    }

    private List<ReportTrendPointVO> buildMonthlyStacked(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        // V1 简化:用 contract 趋势作为堆叠主轴,V2 拆 已回款/计划未回款
        return buildTrend(start, end, ownerIds);
    }

    private List<ReportAgingBucketVO> buildAging() {
        List<CrmReceivablePlan> plans = planMapper.listUnreceivedForAging();
        LocalDate today = LocalDate.now();
        Map<String, long[]> buckets = new LinkedHashMap<>();
        buckets.put("0-30",  new long[]{0, 0});
        buckets.put("31-60", new long[]{0, 0});
        buckets.put("61-90", new long[]{0, 0});
        buckets.put("90+",   new long[]{0, 0});

        for (CrmReceivablePlan p : plans) {
            if (p.getExpectedDate() == null || p.getExpectedAmount() == null) continue;
            long days = ChronoUnit.DAYS.between(p.getExpectedDate(), today);
            String key;
            if (days <= 30)      key = "0-30";
            else if (days <= 60) key = "31-60";
            else if (days <= 90) key = "61-90";
            else                 key = "90+";
            long[] slot = buckets.get(key);
            slot[0] += 1;
            slot[1] += p.getExpectedAmount().longValue();
        }
        long totalCount = buckets.values().stream().mapToLong(b -> b[0]).sum();
        List<ReportAgingBucketVO> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            ReportAgingBucketVO b = new ReportAgingBucketVO();
            b.setKey(e.getKey());
            b.setLabel(e.getKey() + " 天");
            b.setCount(e.getValue()[0]);
            b.setAmount(BigDecimal.valueOf(e.getValue()[1]).toPlainString());
            if (totalCount > 0) {
                BigDecimal pct = new BigDecimal(e.getValue()[0] * 100).divide(new BigDecimal(totalCount), 1, RoundingMode.HALF_UP);
                b.setPercent(pct.toPlainString() + "%");
            } else b.setPercent("0%");
            out.add(b);
        }
        return out;
    }

    private List<ReportDistItemVO> buildMethod(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = receivableMapper.groupByMethod(start, end);
        long total = rows.stream().mapToLong(r -> ReportUtils.toLong(r.get("cnt"))).sum();
        return rows.stream().map(r -> {
            ReportDistItemVO d = new ReportDistItemVO();
            d.setKey(String.valueOf(r.get("k")));
            d.setCount(ReportUtils.toLong(r.get("cnt")));
            d.setAmount(ReportUtils.toBigDecimal(r.get("sum")).toPlainString());
            if (total > 0) {
                BigDecimal pct = new BigDecimal(ReportUtils.toLong(r.get("cnt")) * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
                d.setPercent(pct.toPlainString() + "%");
            } else d.setPercent("0%");
            return d;
        }).collect(Collectors.toList());
    }

    private List<ReportPerformerVO> buildTopDebtors(int topN) {
        List<Map<String, Object>> rows = planMapper.topByUnreceived(topN);
        if (rows.isEmpty()) return List.of();
        List<Long> contractIds = rows.stream().map(r -> ReportUtils.toLong(r.get("contract_id"))).collect(Collectors.toList());
        // contract → customer_name
        List<CrmContract> contracts = contractMapper.selectBatchIds(contractIds);
        Map<Long, Long> cidToCustomerId = contracts.stream()
                .collect(Collectors.toMap(CrmContract::getId, CrmContract::getCustomerId, (a, b) -> a));
        List<Long> custIds = cidToCustomerId.values().stream().distinct().collect(Collectors.toList());
        Map<Long, String> custNameMap = custIds.isEmpty()
                ? Map.of()
                : customerMapper.selectBatchIds(custIds).stream()
                    .collect(Collectors.toMap(CrmCustomer::getId, CrmCustomer::getCustomerName, (a, b) -> a));

        List<ReportPerformerVO> out = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> r : rows) {
            ReportPerformerVO p = new ReportPerformerVO();
            p.setRank(rank++);
            Long contractId = ReportUtils.toLong(r.get("contract_id"));
            Long custId = cidToCustomerId.get(contractId);
            p.setName(custId == null ? "客户" + contractId : custNameMap.getOrDefault(custId, "客户" + custId));
            p.setCount(ReportUtils.toLong(r.get("cnt")).intValue());
            p.setAmount(ReportUtils.toBigDecimal(r.get("sum")).toPlainString());
            out.add(p);
        }
        return out;
    }
}
