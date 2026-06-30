package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.crm.entity.SysDept;
import com.crm.entity.SysUser;
import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.SysDeptMapper;
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
 *   <li>部门业绩(阶段八 commit 2 重写:接 range/ownerIds/真 deptName/2 口径·C2-D1~D4)</li>
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
    private final SysDeptMapper sysDeptMapper;

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
        vo.setTrendReceived(buildTrendReceived(start, end, ownerIds));
        vo.setDepartmentPerformers(buildDepartments(start, end, ownerIds, deptId));
        vo.setSourceDistribution(buildSourceDist(start, end, ownerIds));
        vo.setTopPerformers(buildTopPerformers(start, end, ownerIds, topN));
        vo.setTopPerformersReceived(buildTopPerformersReceived(start, end, ownerIds, topN));
        return vo;
    }

    // ============== 4 KPI(P19 精简: 删客单价/赢单率/线索转化率; 加新增客户数; 去掉下方小字) ==============
    private List<ReportKpiVO> buildKpis(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        BigDecimal salesTotal = contractMapper.sumTotalAmount(start, end, ownerIds);
        Long contractCount = contractMapper.countByRange(start, end, ownerIds);
        Long newCustomers = customerMapper.countNewCustomers(start, end, ownerIds);
        BigDecimal received = receivableMapper.sumActualByRange(start, end);

        List<ReportKpiVO> kpis = new ArrayList<>(4);
        kpis.add(kpi("totalAmount",   "销售总额",   salesTotal, "¥"));
        kpis.add(kpi("contractCount", "新签合同",   new BigDecimal(contractCount), "单"));
        kpis.add(kpi("newCustomers",  "新增客户数", new BigDecimal(newCustomers), "个"));
        kpis.add(kpi("received",      "回款金额",   received, "¥"));
        return kpis;
    }

    /**
     * KPI 工厂(P19 简化版,4 个 KPI 不带 delta/footnote 小字)
     */
    private ReportKpiVO kpi(String key, String label, BigDecimal value, String unit) {
        ReportKpiVO k = new ReportKpiVO();
        k.setKey(key);
        k.setLabel(label);
        k.setValue(value == null ? null : value.toPlainString());
        k.setUnit(unit);
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

    /**
     * 6 月实际回款趋势(阶段八 P6·2026-06-29 新增,chip tab 切换"实际回款"口径用)
     * <p>基于 crm_receivable.return_date 按月分桶 SUM(actual_amount)。</p>
     */
    private List<ReportTrendPointVO> buildTrendReceived(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        List<Map<String, Object>> rows = receivableMapper.sumActualByMonth(start, end);
        return rows.stream().map(r -> {
            ReportTrendPointVO p = new ReportTrendPointVO();
            p.setDate(String.valueOf(r.get("month")));
            p.setValue(ReportUtils.toBigDecimal(r.get("sum")).toPlainString());
            p.setSeriesKey("received");
            return p;
        }).collect(Collectors.toList());
    }

    // ============== 部门业绩(阶段八 commit 2 重写·C2-D1~D4) ==============
    /**
     * 部门业绩卡片:拆 2 口径(合同业绩 / 实际回款)chip tab 切换。
     *
     * <p>阶段八 commit 2 改造点:</p>
     * <ul>
     *   <li>C2-D1 部门名接 sys_dept.dept_name 真名(原"部门 + id"写死)</li>
     *   <li>C2-D2 时间范围接前端 range(原写死近 10 年)</li>
     *   <li>C2-D3 接 ownerIds(原全公司聚合,sales 也看别人部门)</li>
     *   <li>C2-D4 同时算合同业绩 + 实际回款 2 个口径,前端 chip tab 切换</li>
     *   <li>C2-D5 合同侧过滤 status IN (1,2)(已在 Mapper 层 default 方法加,这里透传 ownerIds 即可)</li>
     *   <li>C2-D6 deptId 认子部门(走 helper.selectDescendantIds)</li>
     * </ul>
     */
    private List<ReportDepartmentAmountVO> buildDepartments(
            LocalDateTime start, LocalDateTime end, List<Long> ownerIds, Long filterDeptId) {

        // 1) 确定"要看哪些部门":
        //    filterDeptId 非空 → 该部门 + 所有后代(走 ancestors,见 SysDeptMapper.selectDescendantIds)
        //    filterDeptId 空 → 全公司 status=1 部门,但要排除"总公司/财务部/叶子节点"(业务部门横向对比原则)
        //    过滤规则(P1·2026-06-29):
        //      - 排除总公司(parent_id = 0)
        //      - 排除财务部(硬编码 dept_name = '财务部')
        //      - 排除叶子节点(没人挂在它下面 = 没人在 dept_id 字段引用它作 parent)
        List<Long> targetDeptIds;
        if (filterDeptId != null) {
            targetDeptIds = sysDeptMapper.selectDescendantIds(filterDeptId);
        } else {
            targetDeptIds = sysDeptMapper.selectList(
                    new QueryWrapper<SysDept>().select("id")
                            .eq("status", 1).eq("is_deleted", 0)
                            .ne("parent_id", 0)                            // 排除总公司
                            .ne("dept_name", "财务部")                       // 排除财务部
                            .apply("id NOT IN (SELECT DISTINCT parent_id FROM sys_dept WHERE parent_id IS NOT NULL AND is_deleted = 0)")  // 排除叶子
                    ).stream().map(SysDept::getId).collect(Collectors.toList());
        }
        if (targetDeptIds.isEmpty()) return Collections.emptyList();

        // 2) 算每个部门的"合同业绩"(crm_contract JOIN sys_user,Mapper 内已过滤 status IN(1,2))
        Map<Long, BigDecimal> contractAmounts = contractMapper.sumByDeptIds(start, end, ownerIds, targetDeptIds);

        // 3) 算每个部门的"实际回款"(crm_receivable JOIN crm_contract JOIN sys_user)
        Map<Long, BigDecimal> receivedAmounts = receivableMapper.sumActualByDeptIds(start, end, targetDeptIds);

        // 4) 批量查部门名,避免 N+1
        Map<Long, String> nameMap = sysDeptMapper.selectBatchIds(targetDeptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        // 5) 算总金额用于占比
        BigDecimal contractTotal = contractAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedTotal = receivedAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6) 拼 VO + 按合同业绩降序(财务 KPI 时点优先看合同)
        return targetDeptIds.stream()
                .map(d -> {
                    ReportDepartmentAmountVO vo = new ReportDepartmentAmountVO();
                    vo.setDeptId(d);
                    vo.setDeptName(nameMap.getOrDefault(d, "部门 " + d));
                    BigDecimal cAmt = contractAmounts.getOrDefault(d, BigDecimal.ZERO);
                    BigDecimal rAmt = receivedAmounts.getOrDefault(d, BigDecimal.ZERO);
                    vo.setAmount(cAmt.toPlainString());
                    vo.setReceivedAmount(rAmt.toPlainString());
                    vo.setPercent(contractTotal.compareTo(BigDecimal.ZERO) > 0
                            ? cAmt.multiply(BigDecimal.valueOf(100))
                                  .divide(contractTotal, 1, RoundingMode.HALF_UP).toPlainString() + "%"
                            : "0%");
                    vo.setReceivedPercent(receivedTotal.compareTo(BigDecimal.ZERO) > 0
                            ? rAmt.multiply(BigDecimal.valueOf(100))
                                  .divide(receivedTotal, 1, RoundingMode.HALF_UP).toPlainString() + "%"
                            : "0%");
                    return vo;
                })
                .sorted((a, b) -> new BigDecimal(b.getAmount()).compareTo(new BigDecimal(a.getAmount())))
                .collect(Collectors.toList());
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

    // ============== 销售个人榜 TOP N(合同业绩口径) ==============
    private List<ReportPerformerVO> buildTopPerformers(LocalDateTime start, LocalDateTime end, List<Long> ownerIds, int topN) {
        List<Map<String, Object>> rows = contractMapper.groupByOwner(start, end, ownerIds, topN);
        if (rows.isEmpty()) return Collections.emptyList();
        // 算每位销售的"实际回款"金额(填到 receivedAmount,合同业绩口径下也带上供对比)
        Map<Long, BigDecimal> receivedByOwner = receivableMapper.sumActualByOwnerIds(start, end, ownerIds);
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
            p.setReceivedAmount(receivedByOwner.getOrDefault(uid, BigDecimal.ZERO).toPlainString());
            out.add(p);
        }
        return out;
    }

    /**
     * 销售个人榜 TOP N(实际回款口径·阶段八 P3·2026-06-29)
     * <p>按 owner_user_id 分组聚合 crm_receivable.actual_amount,合同侧 status IN (1,2)。
     * 复用合同业绩榜的 nameMap,但 count + amount 换成回款指标(单数=有回款的合同数,amount=实收总额)。</p>
     */
    private List<ReportPerformerVO> buildTopPerformersReceived(LocalDateTime start, LocalDateTime end, List<Long> ownerIds, int topN) {
        Map<Long, BigDecimal> receivedByOwner = receivableMapper.sumActualByOwnerIds(start, end, ownerIds);
        if (receivedByOwner.isEmpty()) return Collections.emptyList();
        // 按实收金额降序取 TOP N
        List<Map.Entry<Long, BigDecimal>> sorted = receivedByOwner.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(Math.min(topN, 50))
                .collect(Collectors.toList());
        // 同时算每个 owner 的合同数(关联 crm_contract,展示用)
        // 简化:从合同业绩榜的 count 复用,先查合同业绩榜取 ownerIds + count
        Map<Long, Long> contractCountByOwner = contractMapper.groupByOwner(start, end, ownerIds, 50).stream()
                .collect(Collectors.toMap(
                        r -> ReportUtils.toLong(r.get("owner_user_id")),
                        r -> ReportUtils.toLong(r.get("cnt"))));
        // 批量查 name
        List<Long> ids = sorted.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
        List<ReportPerformerVO> out = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, BigDecimal> e : sorted) {
            ReportPerformerVO p = new ReportPerformerVO();
            p.setRank(rank++);
            p.setName(nameMap.getOrDefault(e.getKey(), "用户" + e.getKey()));
            p.setCount(contractCountByOwner.getOrDefault(e.getKey(), 0L).intValue());
            p.setAmount(e.getValue().toPlainString());
            // 回款口径下:receivedAmount 也填 amount(前端按 mode 渲染二选一)
            p.setReceivedAmount(e.getValue().toPlainString());
            out.add(p);
        }
        return out;
    }
}
