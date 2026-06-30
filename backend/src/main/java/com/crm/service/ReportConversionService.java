package com.crm.service;

import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.mapper.SysDeptMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportConversionMetricsVO;
import com.crm.vo.ReportConversionMetricsVO.ConversionMetric;
import com.crm.vo.ReportConversionVO;
import com.crm.vo.ReportDistItemVO;
import com.crm.vo.ReportFunnelStageVO;
import com.crm.vo.ReportKpiVO;
import com.crm.vo.ReportPerformerVO;
import com.crm.vo.ReportTrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表 Tab ③ 跟进与转化率
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>4 KPI(跟进总数 / 平均转化率 / 日均跟进 / 高频跟进人数)</li>
 *   <li>4 个切换指标(阶段八 commit 4):跟进率 / 客户转换率 / 商机转换率 / 合同转换率</li>
 *   <li>5 阶段转化漏斗(基于 crm_business.stage,同 Tab ① 漏斗)</li>
 *   <li>跟进方式分布(电话/微信/拜访/邮件/其他)</li>
 *   <li>高频跟进人榜 TOP N(按 crm_record.create_by 分组)</li>
 *   <li>6 月跟进频次趋势(按月统计)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportConversionService {

    private final ReportCacheService cache;
    private final ReportQueryHelper helper;

    private final CrmRecordMapper recordMapper;
    private final CrmBusinessMapper businessMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmContractMapper contractMapper;
    private final CrmCustomerMapper customerMapper;
    private final SysDeptMapper sysDeptMapper;

    public ReportConversionVO buildConversionReport(String range, String startDate, String endDate,
                                                     Long deptId, Long userId, int topN) {
        String key = helper.cacheKey("conversion", range, deptId, userId);
        return cache.getOrLoad(key, () -> doBuild(range, startDate, endDate, deptId, userId, topN));
    }

    private ReportConversionVO doBuild(String range, String startDate, String endDate,
                                       Long deptId, Long userId, int topN) {
        LocalDateTime[] r = helper.resolveRange(range, startDate, endDate);
        List<Long> ownerIds = helper.resolveOwnerIds(deptId, userId);
        LocalDateTime start = r[0], end = r[1];

        ReportConversionVO vo = new ReportConversionVO();
        vo.setKpis(buildKpis(start, end, ownerIds, deptId));
        vo.setMetrics(buildMetrics(start, end, ownerIds, deptId));
        vo.setStageFunnel(buildFunnel(start, end, ownerIds));
        vo.setFollowTypeDist(buildFollowType(start, end));
        vo.setTopPerformers(buildTopPerformers(start, end, topN));
        vo.setTrend(buildTrend(start, end));
        return vo;
    }

    private List<ReportKpiVO> buildKpis(LocalDateTime start, LocalDateTime end, List<Long> ownerIds, Long deptId) {
        // 阶段八 commit 6 调整:3 KPI 紧凑展示(跟进总数 + 2 个转换率),删"合同转换率"
        //   客户转换率: 期内该部门内新增客户数 / 期内全公司新增线索数
        //   商机转换率: 期内该部门内新增商机数 / 期内该部门内新增客户数(同 create_by 部门链口径)
        long totalRecord = recordMapper.countByRange(start, end);

        ReportConversionMetricsVO metrics = buildMetrics(start, end, ownerIds, deptId);

        List<ReportKpiVO> kpis = new ArrayList<>(3);

        // 1) 跟进总数
        ReportKpiVO k1 = new ReportKpiVO();
        k1.setKey("totalRecord"); k1.setLabel("跟进总数");
        k1.setValue(String.valueOf(totalRecord)); k1.setUnit("条");
        kpis.add(k1);

        // 2) 客户转换率(value 带 "%" 后缀,unit=null 让 KpiStrip 紧凑显示)
        addKpiFromMetric(kpis, "leadToCustomerRate", "客户转换率", metrics.getLeadToCustomerRate());
        // 3) 商机转换率
        addKpiFromMetric(kpis, "leadToBusinessRate", "商机转换率", metrics.getLeadToBusinessRate());

        return kpis;
    }

    /** 把单个指标灌进 KPI 列表(value="88.9%", unit=null,KpiStrip 直接紧凑显示) */
    private void addKpiFromMetric(List<ReportKpiVO> kpis, String key, String label, ConversionMetric m) {
        if (m == null) return;
        ReportKpiVO k = new ReportKpiVO();
        k.setKey(key);
        k.setLabel(label);
        k.setValue(m.getRate());
        k.setUnit(null);
        kpis.add(k);
    }

    private List<ReportFunnelStageVO> buildFunnel(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        String[] names = {"新建线索", "需求分析", "方案报价", "商务谈判", "赢单"};
        String[] bizStages = {null, CrmBusinessMapper.STAGE_ANALYSIS, CrmBusinessMapper.STAGE_QUOTE,
                              CrmBusinessMapper.STAGE_NEGOTIATE, CrmBusinessMapper.STAGE_WIN};
        Long[] counts = new Long[5];
        // 阶段 1:新建线索(走 crm_lead)
        counts[0] = leadMapper.countInFunnel(start, end, ownerIds);
        // 阶段 2-5:商机 stage(中文)
        for (int i = 1; i < 5; i++) {
            counts[i] = businessMapper.countByStage(bizStages[i], start, end, ownerIds);
        }
        long base = counts[0] == 0 ? 1 : counts[0];
        List<ReportFunnelStageVO> out = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            ReportFunnelStageVO s = new ReportFunnelStageVO();
            s.setStage("stage" + (i + 1));
            s.setStageName(names[i]);
            s.setCount(counts[i]);
            if (i == 0) s.setConvRate("100%");
            else {
                BigDecimal rate = new BigDecimal(counts[i] * 100).divide(new BigDecimal(base), 1, RoundingMode.HALF_UP);
                s.setConvRate(rate.toPlainString() + "%");
            }
            out.add(s);
        }
        return out;
    }

    private List<ReportDistItemVO> buildFollowType(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = recordMapper.groupByFollowType(start, end);
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

    private List<ReportPerformerVO> buildTopPerformers(LocalDateTime start, LocalDateTime end, int topN) {
        List<Map<String, Object>> rows = recordMapper.groupByCreateBy(start, end, topN);
        // createBy 是 nickname/username,直接当 name 用
        List<ReportPerformerVO> out = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> r : rows) {
            ReportPerformerVO p = new ReportPerformerVO();
            p.setRank(rank++);
            p.setName(String.valueOf(r.get("create_by")));
            p.setCount(ReportUtils.toLong(r.get("cnt")).intValue());
            out.add(p);
        }
        return out;
    }

    private List<ReportTrendPointVO> buildTrend(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = recordMapper.groupByMonth(start, end);
        return rows.stream().map(r -> {
            ReportTrendPointVO p = new ReportTrendPointVO();
            p.setDate(String.valueOf(r.get("month")));
            p.setValue(String.valueOf(ReportUtils.toLong(r.get("cnt"))));
            return p;
        }).collect(Collectors.toList());
    }

    /**
     * 4 个切换指标(阶段八 commit 4·2026-06-30)
     * <p>全部接受 ownerIds 参数,自动支持部门维度(ownerIds 由 ReportQueryHelper.resolveOwnerIds 解出);
     * start/end 由 ReportQueryHelper.resolveRange 解出,支持页面时间维度。</p>
     *
     * <p>阶段八 commit 5(2026-06-30)调整:
     * 客户转换率 公式改为 "期内新增客户数 / 期内新增线索数"(产出/投入 比,语义更直观);
     * 部门维度 改为 "客户 create_by → sys_user.dept_id" 链路(反映销售员产出,而不是当前客户归属)。</p>
     */
    private ReportConversionMetricsVO buildMetrics(LocalDateTime start, LocalDateTime end, List<Long> ownerIds, Long deptId) {
        ReportConversionMetricsVO m = new ReportConversionMetricsVO();

        // 1) 跟进率:期内 last_follow_time ∈ [start,end] 的客户数 / 全量在管客户数
        long followed = customerMapper.countFollowedInRange(start, end, ownerIds);
        long totalCustomers = customerMapper.countByOwner(ownerIds);
        m.setFollowRate(metric("followRate",
                followed, "期内被跟进客户数",
                totalCustomers, "在管客户总数",
                "客户的最近一次跟进时间落在所选时间区间内 / 全量在管客户数"));

        // 2) 客户转换率(线索 → 客户):期内新增客户数 / 期内新增线索数
        //    部门维度:分子按 crm_customer.create_by → sys_user.dept_id(销售员产出);
        //    分母 全公司(用户反馈:分母保持全公司口径)
        //    注意:deptId==null 时走 countNewCustomers(null) 全公司路径,
        //          必须避开 countNewCustomersByDeptIds 的 default(null → 0) 截断
        long customerNew;
        String departmentDesc;
        if (deptId == null) {
            customerNew = customerMapper.countNewCustomers(start, end, null);
            departmentDesc = "全公司(crm_customer.create_time 区间)";
        } else {
            List<Long> deptIdsForCustomer = sysDeptMapper.selectDescendantIds(deptId);
            customerNew = customerMapper.countNewCustomersByDeptIds(start, end, deptIdsForCustomer);
            departmentDesc = "create_by 部门维度(deptId=" + deptId + ")";
        }
        long leadTotal = leadMapper.countAllByRange(start, end);
        m.setLeadToCustomerRate(metric("leadToCustomerRate",
                customerNew, "期内新增客户数",
                leadTotal, "期内新增线索数",
                "期内新增客户数 / 期内新增线索数 · 分子" + departmentDesc + ",分母全公司"));

        // 3) 商机转换率(阶段八 commit 6·2026-06-30 重构):期内新增商机数 / 期内新增客户数
        //    口径:分子 + 分母 都按 crm_*.create_by → sys_user.dept_id 链(同口径)
        //    deptId==null → 全公司(无部门过滤)
        //    deptId!=null → 部门下创建人产生的商机/客户
        long businessNew;
        if (deptId == null) {
            businessNew = businessMapper.countAllByRangeByCreateByDeptIds(start, end, null);
        } else {
            List<Long> deptIdsForBusiness = sysDeptMapper.selectDescendantIds(deptId);
            businessNew = businessMapper.countAllByRangeByCreateByDeptIds(start, end, deptIdsForBusiness);
        }
        m.setLeadToBusinessRate(metric("leadToBusinessRate",
                businessNew, "期内新增商机数",
                customerNew, "期内新增客户数",
                "期内新增商机数 / 期内新增客户数(同 create_by 部门链口径) · " + departmentDesc));

        return m;
    }

    /** 构造单指标 VO(分子 / 分母 / 比率 / 标签) */
    private ConversionMetric metric(String key,
                                    long num, String numLabel,
                                    long den, String denLabel,
                                    String description) {
        ConversionMetric m = new ConversionMetric();
        m.setKey(key);
        m.setNumerator(num);
        m.setNumeratorLabel(numLabel);
        m.setDenominator(den);
        m.setDenominatorLabel(denLabel);
        m.setDescription(description);
        if (den > 0) {
            BigDecimal rate = new BigDecimal(num * 100)
                    .divide(new BigDecimal(den), 1, RoundingMode.HALF_UP);
            m.setRate(rate.toPlainString() + "%");
        } else {
            m.setRate("0%");
        }
        return m;
    }
}
