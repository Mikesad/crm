package com.crm.service;

import com.crm.mapper.CrmCustomerMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportActivityVO;
import com.crm.vo.ReportCustomerVO;
import com.crm.vo.ReportDistItemVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportTrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表 Tab ② 客户分布
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>4 KPI(客户总数 / 行业数 / 公海占比 / 沉睡客户数)</li>
 *   <li>主分布(按 dim=industry/level/source 切换,LIMIT 500 兜底)</li>
 *   <li>活跃/沉睡/公海/总数 + 占比</li>
 *   <li>等级分布(常显,饼图)</li>
 *   <li>地区分布(V1 暂无 region 字段,用行业替代,V2 增强)</li>
 *   <li>活跃 vs 沉睡 6 月趋势(简化为按月统计新增活跃客户数)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCustomerService {

    private final ReportCacheService cache;
    private final ReportQueryHelper helper;
    private final CrmCustomerMapper customerMapper;

    public ReportCustomerVO buildCustomerReport(String range, String startDate, String endDate,
                                                 Long deptId, Long userId, String dim) {
        String key = helper.cacheKey("customer", range, deptId, userId) + ":dim=" + dim;
        return cache.getOrLoad(key, () -> doBuild(range, startDate, endDate, deptId, userId, dim));
    }

    private ReportCustomerVO doBuild(String range, String startDate, String endDate,
                                      Long deptId, Long userId, String dim) {
        LocalDateTime[] r = helper.resolveRange(range, startDate, endDate);
        List<Long> ownerIds = helper.resolveOwnerIds(deptId, userId);

        ReportCustomerVO vo = new ReportCustomerVO();
        vo.setKpis(buildKpis(ownerIds));
        vo.setDistribution(buildMainDist(ownerIds, dim));
        vo.setActivity(buildActivity(ownerIds));
        vo.setLevelDistribution(buildLevelDist(ownerIds));
        vo.setRegionDistribution(buildRegionDist(ownerIds));
        vo.setActivityTrend(buildActivityTrend(r[0], r[1], ownerIds));
        return vo;
    }

    private List<ReportKpiVO> buildKpis(List<Long> ownerIds) {
        long total = customerMapper.countByOwner(ownerIds);
        long industryCount = customerMapper.countDistinctIndustry(ownerIds);
        long publicCount = customerMapper.countPublic();
        long dormant = customerMapper.countDormant(30, ownerIds);
        String publicPct = total > 0
                ? new BigDecimal(publicCount * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";
        String dormantPct = total > 0
                ? new BigDecimal(dormant * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";
        List<ReportKpiVO> kpis = new ArrayList<>(4);
        ReportKpiVO k1 = new ReportKpiVO();
        k1.setKey("total"); k1.setLabel("客户总数");
        k1.setValue(String.valueOf(total)); k1.setUnit(null);
        kpis.add(k1);
        ReportKpiVO k2 = new ReportKpiVO();
        k2.setKey("industryCount"); k2.setLabel("行业数");
        k2.setValue(String.valueOf(industryCount));
        kpis.add(k2);
        ReportKpiVO k3 = new ReportKpiVO();
        k3.setKey("publicPct"); k3.setLabel("公海占比");
        k3.setValue(publicPct); k3.setUnit(null);
        kpis.add(k3);
        ReportKpiVO k4 = new ReportKpiVO();
        k4.setKey("dormantCount"); k4.setLabel("沉睡客户数");
        k4.setValue(String.valueOf(dormant));
        k4.setFootnote(dormantPct + " 客户超 30 天未跟进");
        kpis.add(k4);
        return kpis;
    }

    private List<ReportDistItemVO> buildMainDist(List<Long> ownerIds, String dim) {
        List<Map<String, Object>> rows;
        int limit = 500;
        if ("level".equalsIgnoreCase(dim)) {
            rows = customerMapper.groupByLevel(ownerIds);
        } else {
            // industry / source 都走 groupByIndustry(V1 暂用 crm_customer.industry;source 暂退化为 industry 兜底)
            rows = customerMapper.groupByIndustry(ownerIds, limit);
        }
        long total = rows.stream().mapToLong(r -> ReportUtils.toLong(r.get("cnt"))).sum();
        return rows.stream().map(r -> {
            ReportDistItemVO d = new ReportDistItemVO();
            d.setKey(String.valueOf(r.get("k")));
            long cnt = ReportUtils.toLong(r.get("cnt"));
            d.setCount(cnt);
            if (total > 0) {
                BigDecimal pct = new BigDecimal(cnt * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
                d.setPercent(pct.toPlainString() + "%");
            } else d.setPercent("0%");
            return d;
        }).collect(Collectors.toList());
    }

    private ReportActivityVO buildActivity(List<Long> ownerIds) {
        ReportActivityVO a = new ReportActivityVO();
        long total = customerMapper.countByOwner(ownerIds);
        long active = customerMapper.countActive(30, ownerIds);
        long dormant = customerMapper.countDormant(30, ownerIds);
        long publicCount = customerMapper.countPublic();
        a.setTotal(total);
        a.setActive(active);
        a.setDormant(dormant);
        a.setPublicPool(publicCount);
        a.setActivePercent(pct(active, total));
        a.setDormantPercent(pct(dormant, total));
        a.setPublicPercent(pct(publicCount, total));
        return a;
    }

    private List<ReportDistItemVO> buildLevelDist(List<Long> ownerIds) {
        return buildMainDist(ownerIds, "level");
    }

    /** V1 占位:地区分布暂用行业分布替代 */
    private List<ReportDistItemVO> buildRegionDist(List<Long> ownerIds) {
        return buildMainDist(ownerIds, "industry");
    }

    /** 活跃 vs 沉睡 6 月趋势(V1 简化:按月统计"近 30 天有跟进"客户数) */
    private List<ReportTrendPointVO> buildActivityTrend(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        // 6 月度桶:每月取一次 countActive(简化版,精确版需用 DATE_FORMAT 拉历史快照)
        java.time.LocalDate today = java.time.LocalDate.now();
        List<ReportTrendPointVO> out = new ArrayList<>(6);
        for (int i = 5; i >= 0; i--) {
            java.time.LocalDate monthEnd = today.minusMonths(i);
            // 简化:每月月末往后看 30 天有跟进即计入
            // 因 crm_customer.last_follow_time 是当前快照,无法回溯,V1 用"近 30 天"作为单桶
            ReportTrendPointVO p = new ReportTrendPointVO();
            p.setDate(String.format("%d-%02d", monthEnd.getYear(), monthEnd.getMonthValue()));
            p.setValue(String.valueOf(customerMapper.countActive(30, ownerIds)));
            p.setSeriesKey("active");
            out.add(p);
        }
        return out;
    }

    private String pct(long part, long total) {
        if (total == 0) return "0%";
        BigDecimal p = new BigDecimal(part * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
        return p.toPlainString() + "%";
    }
}
