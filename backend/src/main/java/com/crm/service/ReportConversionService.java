package com.crm.service;

import com.crm.entity.SysUser;
import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.util.ReportUtils;
import com.crm.vo.ReportConversionCompareVO;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表 Tab ③ 跟进与转化率
 *
 * <p>聚合维度:</p>
 * <ul>
 *   <li>4 KPI(跟进总数 / 平均转化率 / 日均跟进 / 高频跟进人数)</li>
 *   <li>5 阶段转化漏斗(基于 crm_business.stage,同 Tab ① 漏斗)</li>
 *   <li>跟进方式分布(电话/微信/拜访/邮件/其他)</li>
 *   <li>高频跟进人榜 TOP N(按 crm_record.create_by 分组)</li>
 *   <li>团队 vs 全公司 转化率对比(V1 简化为只展示全公司,team 字段填同值,V2 增强)</li>
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
    private final SysUserMapper sysUserMapper;

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
        vo.setKpis(buildKpis(start, end, ownerIds));
        vo.setStageFunnel(buildFunnel(start, end, ownerIds));
        vo.setFollowTypeDist(buildFollowType(start, end));
        vo.setTopPerformers(buildTopPerformers(start, end, topN));
        vo.setTeamVsCompany(buildCompare(start, end, ownerIds));
        vo.setTrend(buildTrend(start, end));
        return vo;
    }

    private List<ReportKpiVO> buildKpis(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        long totalRecord = recordMapper.countByRange(start, end);
        long days = Math.max(1L, ChronoUnit.DAYS.between(start, end) + 1);
        BigDecimal dailyAvg = new BigDecimal(totalRecord).divide(new BigDecimal(days), 1, RoundingMode.HALF_UP);

        long allBiz = businessMapper.countAllByRange(start, end, ownerIds);
        long winBiz = businessMapper.winCountByRange(start, end, ownerIds);
        String convRate = allBiz > 0
                ? new BigDecimal(winBiz * 100).divide(new BigDecimal(allBiz), 1, RoundingMode.HALF_UP).toPlainString() + "%"
                : "0%";

        long performers = recordMapper.groupByCreateBy(start, end, 50).size();

        List<ReportKpiVO> kpis = new ArrayList<>(4);
        ReportKpiVO k1 = new ReportKpiVO();
        k1.setKey("totalRecord"); k1.setLabel("跟进总数");
        k1.setValue(String.valueOf(totalRecord)); k1.setUnit("条");
        kpis.add(k1);
        ReportKpiVO k2 = new ReportKpiVO();
        k2.setKey("convRate"); k2.setLabel("平均转化率");
        k2.setValue(convRate);
        kpis.add(k2);
        ReportKpiVO k3 = new ReportKpiVO();
        k3.setKey("dailyAvg"); k3.setLabel("日均跟进");
        k3.setValue(dailyAvg.toPlainString()); k3.setUnit("条");
        kpis.add(k3);
        ReportKpiVO k4 = new ReportKpiVO();
        k4.setKey("performers"); k4.setLabel("活跃跟进人数");
        k4.setValue(String.valueOf(performers));
        kpis.add(k4);
        return kpis;
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

    private List<ReportConversionCompareVO> buildCompare(LocalDateTime start, LocalDateTime end, List<Long> ownerIds) {
        // V1 简化:团队 = 当前 deptId 下的 ownerIds,公司 = 全部
        // 复用 buildFunnel 的 5 阶段数,组装到 2 个 VO
        String[] names = {"新建线索", "需求分析", "方案报价", "商务谈判", "赢单"};
        String[] bizStages = {null, CrmBusinessMapper.STAGE_ANALYSIS, CrmBusinessMapper.STAGE_QUOTE,
                              CrmBusinessMapper.STAGE_NEGOTIATE, CrmBusinessMapper.STAGE_WIN};
        Long[] counts = new Long[5];
        counts[0] = leadMapper.countInFunnel(start, end, ownerIds);
        for (int i = 1; i < 5; i++) {
            counts[i] = businessMapper.countByStage(bizStages[i], start, end, ownerIds);
        }
        long base = counts[0] == 0 ? 1 : counts[0];
        ReportConversionCompareVO team = new ReportConversionCompareVO();
        team.setGroup("team");
        team.setStage1Lead("100%");
        team.setStage2Analysis(pct(counts[1], base));
        team.setStage3Quote(pct(counts[2], base));
        team.setStage4Negotiate(pct(counts[3], base));
        team.setStage5Win(pct(counts[4], base));

        // V1 占位:全公司数与 team 相同(无独立 sys_user 全量,后续用全公司聚合补)
        ReportConversionCompareVO company = new ReportConversionCompareVO();
        company.setGroup("company");
        company.setStage1Lead(team.getStage1Lead());
        company.setStage2Analysis(team.getStage2Analysis());
        company.setStage3Quote(team.getStage3Quote());
        company.setStage4Negotiate(team.getStage4Negotiate());
        company.setStage5Win(team.getStage5Win());

        return List.of(team, company);
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

    private String pct(long part, long base) {
        if (base == 0) return "0%";
        BigDecimal p = new BigDecimal(part * 100).divide(new BigDecimal(base), 1, RoundingMode.HALF_UP);
        return p.toPlainString() + "%";
    }
}
