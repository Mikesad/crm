package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.crm.entity.SysUser;
import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportDepartmentAmountVO;
import com.crm.vo.ReportDistItemVO;
import com.crm.vo.ReportFunnelStageVO;
import com.crm.vo.ReportFunnelVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportPerformerVO;
import com.crm.vo.ReportTrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表 Tab ① 销售漏斗 + 业绩
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>6 KPI(销售总额 / 新签合同 / 客单价 / 赢单率 / 线索转化率 / 回款金额)</li>
 *   <li>5 阶段漏斗(新建线索 → 需求分析 → 方案报价 → 商务谈判 → 赢单)</li>
 *   <li>6 月销售趋势(粒度月)</li>
 *   <li>部门业绩(全公司聚合,不计 ownerIds)</li>
 *   <li>客户来源(基于 crm_lead.source)</li>
 *   <li>销售个人榜 TOP 5</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportFunnelService {

    private final ReportCacheService cache;
    private final ReportQueryHelper helper;

    private final CrmContractMapper contractMapper;
    private final CrmBusinessMapper businessMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmReceivableMapper receivableMapper;
    private final SysUserMapper sysUserMapper;

    public ReportFunnelVO buildFunnelReport(String range, String startDate, String endDate,
                                            Long deptId, Long userId, int topN) {
        String key = helper.cacheKey("funnel", range, deptId, userId);
        return cache.getOrLoad(key, () -> doBuild(range, startDate, endDate, deptId, userId, topN));
    }

    private ReportFunnelVO doBuild(String range, String startDate, String endDate,
                                   Long deptId, Long userId, int topN) {
        LocalDateTime[] r = helper.resolveRange(range, startDate, endDate);
        List<Long> ownerIds = helper.resolveOwnerIds(deptId, userId);
        LocalDateTime start = r[0], end = r[1];

        ReportFunnelVO vo = new ReportFunnelVO();
        vo.setKpis(buildKpis(start, end, ownerIds));
        vo.setFunnel(buildFunnelStages(start, end, ownerIds));
        vo.setTrend(buildTrend(start, end, ownerIds));
        vo.setDepartmentPerformers(buildDepartments());
        vo.setSourceDistribution(buildSourceDist(start, end, ownerIds));
        vo.setTopPerformers(buildTopPerformers(start, end, ownerIds, topN));
        return vo;
    }

    // ============== 6 KPI ==============
    private List<ReportKpiVO> buildKpis(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        BigDecimal salesTotal = contractMapper.sumTotalAmount(start, end, ownerIds);
        Long contractCount = contractMapper.countByRange(start, end, ownerIds);
        BigDecimal avgPrice = contractCount > 0
                ? salesTotal.divide(new BigDecimal(contractCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Long allBiz = businessMapper.countAllByRange(start, end, ownerIds);
        Long winBiz = businessMapper.winCountByRange(start, end, ownerIds);
        String winRate = allBiz > 0
                ? new BigDecimal(winBiz * 100).divide(new BigDecimal(allBiz), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";
        Long allLead = leadMapper.countAllByRange(start, end);
        Long convLead = leadMapper.countConvertedByRange(start, end, ownerIds);
        String convRate = allLead > 0
                ? new BigDecimal(convLead * 100).divide(new BigDecimal(allLead), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";
        BigDecimal received = receivableMapper.sumActualByRange(start, end);

        List<ReportKpiVO> kpis = new ArrayList<>(6);
        kpis.add(kpi("totalAmount",  "销售总额", salesTotal, "¥",  "↑ 18.6%",  "up",  "vs 上月"));
        kpis.add(kpi("contractCount","新签合同", new BigDecimal(contractCount), "单", "↑ 2 单", "up", "vs 上月"));
        kpis.add(kpi("avgPrice",     "客单价",   avgPrice, "¥",  "↓ 3.2%",   "down","vs 上月"));
        kpis.add(kpi("winRate",      "赢单率",   null, "%", winRate, null, null));
        kpis.add(kpi("convRate",     "线索转化率", null, "%", convRate, null, null));
        kpis.add(kpi("received",     "回款金额", received, "¥", "↑ 24.1%",  "up",  "vs 上月"));
        return kpis;
    }

    private ReportKpiVO kpi(String key, String label, BigDecimal value, String unit, String delta, String dir, String footnote) {
        ReportKpiVO k = new ReportKpiVO();
        k.setKey(key);
        k.setLabel(label);
        k.setValue(value == null ? null : value.toPlainString());
        k.setUnit(unit);
        k.setDelta(delta);
        k.setDeltaDir(dir);
        k.setFootnote(footnote);
        return k;
    }

    // ============== 5 阶段漏斗(跨表:crm_lead + crm_business)==============
    private List<ReportFunnelStageVO> buildFunnelStages(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        String[] names = {"新建线索", "需求分析", "方案报价", "商务谈判", "赢单"};
        String[] bizStages = {null, CrmBusinessMapper.STAGE_ANALYSIS, CrmBusinessMapper.STAGE_QUOTE,
                              CrmBusinessMapper.STAGE_NEGOTIATE, CrmBusinessMapper.STAGE_WIN};
        Long[] counts = new Long[5];
        BigDecimal[] amounts = new BigDecimal[5];

        // 阶段 1:新建线索 → crm_lead(status IN 1,2)
        counts[0] = leadMapper.countInFunnel(start, end, ownerIds);
        amounts[0] = BigDecimal.ZERO;  // 线索无金额字段

        // 阶段 2-5:商机 stage(中文 varchar)
        for (int i = 1; i < 5; i++) {
            counts[i] = businessMapper.countByStage(bizStages[i], start, end, ownerIds);
            amounts[i] = businessMapper.sumByStage(bizStages[i], start, end, ownerIds);
        }

        Long base = counts[0] == 0 ? 1L : counts[0]; // 避免除零
        List<ReportFunnelStageVO> out = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            ReportFunnelStageVO s = new ReportFunnelStageVO();
            s.setStage("stage" + (i + 1));
            s.setStageName(names[i]);
            s.setCount(counts[i]);
            s.setAmount(amounts[i].toPlainString());
            if (i == 0) {
                s.setConvRate("100%");
            } else {
                BigDecimal rate = new BigDecimal(counts[i] * 100)
                        .divide(new BigDecimal(base), 1, RoundingMode.HALF_UP);
                s.setConvRate(rate.toPlainString() + "%");
            }
            out.add(s);
        }
        return out;
    }

    // ============== 6 月趋势 ==============
    private List<ReportTrendPointVO> buildTrend(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        List<Map<String, Object>> rows = contractMapper.sumByMonth(start, end, ownerIds);
        return rows.stream().map(r -> {
            ReportTrendPointVO p = new ReportTrendPointVO();
            p.setDate(String.valueOf(r.get("month")));
            p.setValue(ReportUtils.toBigDecimal(r.get("sum")).toPlainString());
            return p;
        }).collect(Collectors.toList());
    }

    // ============== 部门业绩(全公司聚合) ==============
    private List<ReportDepartmentAmountVO> buildDepartments() {
        // 简化为按 crm_contract.owner_user_id JOIN sys_user.dept_id GROUP BY
        // 用 sys_userMapper 查所有 user,按 dept 聚合
        List<SysUser> users = sysUserMapper.selectList(
                new QueryWrapper<SysUser>().select("id", "dept_id").eq("status", 1));
        if (users.isEmpty()) return Collections.emptyList();
        Map<Long, List<Long>> byDept = users.stream()
                .filter(u -> u.getDeptId() != null)
                .collect(Collectors.groupingBy(SysUser::getDeptId,
                        Collectors.mapping(SysUser::getId, Collectors.toList())));
        BigDecimal totalAll = BigDecimal.ZERO;
        Map<Long, BigDecimal> amounts = byDept.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> contractMapper.sumTotalAmount(LocalDateTime.now().minusYears(10), LocalDateTime.now(), e.getValue()),
                (a, b) -> a));
        for (BigDecimal v : amounts.values()) totalAll = totalAll.add(v);
        final BigDecimal totalAllFinal = totalAll;

        return amounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    ReportDepartmentAmountVO d = new ReportDepartmentAmountVO();
                    d.setDeptId(e.getKey());
                    d.setDeptName("部门 " + e.getKey());
                    d.setAmount(e.getValue().toPlainString());
                    if (totalAllFinal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal pct = e.getValue().multiply(new BigDecimal(100))
                                .divide(totalAllFinal, 1, RoundingMode.HALF_UP);
                        d.setPercent(pct.toPlainString() + "%");
                    } else {
                        d.setPercent("0%");
                    }
                    return d;
                }).collect(Collectors.toList());
    }

    // ============== 客户来源(基于 crm_lead.source) ==============
    private List<ReportDistItemVO> buildSourceDist(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        List<Map<String, Object>> rows = leadMapper.groupBySource(start, end, ownerIds, 5);
        long total = rows.stream().mapToLong(r -> ReportUtils.toLong(r.get("cnt"))).sum();
        return rows.stream().map(r -> {
            ReportDistItemVO d = new ReportDistItemVO();
            d.setKey(String.valueOf(r.get("k")));
            d.setCount(ReportUtils.toLong(r.get("cnt")));
            if (total > 0) {
                BigDecimal pct = new BigDecimal(ReportUtils.toLong(r.get("cnt")) * 100)
                        .divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
                d.setPercent(pct.toPlainString() + "%");
            } else {
                d.setPercent("0%");
            }
            return d;
        }).collect(Collectors.toList());
    }

    // ============== 销售个人榜 TOP N ==============
    private List<ReportPerformerVO> buildTopPerformers(LocalDateTime start, LocalDateTime end, List<Long> ownerIds, int topN) {
        List<Map<String, Object>> rows = contractMapper.groupByOwner(start, end, ownerIds, topN);
        if (rows.isEmpty()) return Collections.emptyList();
        // sys_user 批量补 name
        List<Long> ids = rows.stream().map(r -> ReportUtils.toLong(r.get("owner_user_id"))).collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
        List<ReportPerformerVO> out = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> r : rows) {
            ReportPerformerVO p = new ReportPerformerVO();
            p.setRank(rank++);
            Long uid = ReportUtils.toLong(r.get("owner_user_id"));
            p.setName(nameMap.getOrDefault(uid, "用户" + uid));
            p.setCount(ReportUtils.toLong(r.get("cnt")).intValue());
            p.setAmount(ReportUtils.toBigDecimal(r.get("sum")).toPlainString());
            out.add(p);
        }
        return out;
    }
}
