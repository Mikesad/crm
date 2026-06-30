package com.crm.service;

import com.crm.mapper.CrmCustomerMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportActivityVO;
import com.crm.vo.ReportCustomerVO;
import com.crm.vo.ReportDistItemVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportTrendPointVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class ReportCustomerService {

    private final ReportCacheService cache;
    private final ReportQueryHelper helper;
    private final CrmCustomerMapper customerMapper;

    /**
     * 沉睡阈值(天)— 与公海池回收共用同一配置 {@code crm.customer.public-pool-days},默认 15 天。
     * <p>语义:超过该天数无跟进的客户视为「沉睡」,与公海回收口径一致,
     * 改 yml 一处即可同步调整「沉睡客户数」KPI 与「活跃/沉睡」分布。</p>
     * <p>注意:与「活跃(30 天)」是独立的另一窗口(体现近期互动强度),保留 30 天不变。</p>
     */
    @Value("${crm.customer.public-pool-days:15}")
    private int publicPoolDays;

    public ReportCustomerService(ReportCacheService cache, ReportQueryHelper helper, CrmCustomerMapper customerMapper) {
        this.cache = cache;
        this.helper = helper;
        this.customerMapper = customerMapper;
    }

    public ReportCustomerVO buildCustomerReport(String range, String startDate, String endDate,
                                                 Long deptId, Long userId, String dim) {
        String key = helper.cacheKey("customer", range, deptId, userId) + ":dim=" + dim;
        return cache.getOrLoad(key, () -> doBuild(range, startDate, endDate, deptId, userId, dim));
    }

    private ReportCustomerVO doBuild(String range, String startDate, String endDate,
                                      Long deptId, Long userId, String dim) {
        LocalDateTime[] r = helper.resolveRange(range, startDate, endDate);
        LocalDateTime start = r[0], end = r[1];
        // 阶段八 commit 9 回滚:Tab ② 走 owner_user_id 链路(反映"客户当前归属")
        // helper.resolveOwnerIds(deptId) → deptId + 子部门 → 这些部门下所有 user.id
        // crm_customer.owner_user_id IN ownerIds = "该部门所有者的客户"
        List<Long> ownerIds = helper.resolveOwnerIds(deptId, userId);

        ReportCustomerVO vo = new ReportCustomerVO();
        vo.setKpis(buildKpis(ownerIds));
        vo.setDistribution(buildMainDist(ownerIds, dim));
        vo.setActivity(buildActivity(start, end, ownerIds));
        vo.setLevelDistribution(buildLevelDist(ownerIds));
        vo.setRegionDistribution(buildRegionDist(ownerIds));
        vo.setActivityTrend(buildActivityTrend(start, end, ownerIds));
        return vo;
    }

    /**
     * KPI 统计(owner_user_id 链路)
     * <p>总数 = 当前归属于该部门的客户数;
     * 行业数 = 当前归属客户中不同 industry 数。</p>
     */
    private List<ReportKpiVO> buildKpis(List<Long> ownerIds) {
        long total = customerMapper.countByOwner(ownerIds);
        long industryCount = customerMapper.countDistinctIndustry(ownerIds);
        long publicCount = customerMapper.countPublic(); // 全量公海(is_public=1)
        String publicPct = total > 0
                ? new BigDecimal(publicCount * 100).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";
        // 阶段八 commit 8:删"沉睡客户数" KPI,3 KPI(总数 / 行业数 / 公海占比)
        List<ReportKpiVO> kpis = new ArrayList<>(3);
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
        return kpis;
    }

    /** 主分布(owner_user_id 链路) */
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

    /**
     * 活跃度(阶段八 commit 9·2026-06-30 重构,owner_user_id 链路)
     * <p>总数 / 活跃都按 crm_customer.owner_user_id → sys_user.dept_id 链路过滤,
     * 反映"归属于该部门的客户的活跃度";公海单独统计(期内新增到公海的)。</p>
     */
    private ReportActivityVO buildActivity(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        ReportActivityVO a = new ReportActivityVO();
        long total = customerMapper.countByOwner(ownerIds);
        long active = customerMapper.countActiveInRange(start, end, ownerIds);
        long publicCount = customerMapper.countPublicInRange(start, end);
        a.setTotal(total);
        a.setActive(active);
        a.setPublicPool(publicCount);
        a.setActivePercent(pct(active, total));
        a.setPublicPercent(pct(publicCount, total));
        return a;
    }

    /** 等级分布(owner_user_id 链路,常显维度,不受 dim 切换影响) */
    private List<ReportDistItemVO> buildLevelDist(List<Long> ownerIds) {
        return buildMainDist(ownerIds, "level");
    }

    /** V1 占位:地区分布暂用行业分布替代(owner_user_id 链路) */
    private List<ReportDistItemVO> buildRegionDist(List<Long> ownerIds) {
        return buildMainDist(ownerIds, "industry");
    }

    /**
     * 活跃 6 月趋势(owner_user_id 链路)
     * <p>按月统计"近 30 天有跟进"客户数,因为 last_follow_time 是当前快照无法回溯,V1 单桶值相同。</p>
     */
    private List<ReportTrendPointVO> buildActivityTrend(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<ReportTrendPointVO> out = new ArrayList<>(6);
        for (int i = 5; i >= 0; i--) {
            java.time.LocalDate monthEnd = today.minusMonths(i);
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
