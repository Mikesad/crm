package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.service.ReportCacheService;
import com.crm.service.ReportConversionService;
import com.crm.service.ReportCustomerService;
import com.crm.service.ReportFinanceService;
import com.crm.service.ReportFunnelService;
import com.crm.service.ReportQueryHelper;
import com.crm.vo.ReportAgingBucketVO;
import com.crm.vo.ReportConversionVO;
import com.crm.vo.ReportCustomerVO;
import com.crm.vo.ReportFilterOptionVO;
import com.crm.vo.ReportFinanceVO;
import com.crm.vo.ReportFunnelVO;
import com.crm.vo.ReportPerformerVO;
import com.crm.vo.ReportTrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表中心 Controller（阶段五 commit 2）
 *
 * <p>15 个 GET 接口,4 个 Tab 各有 1 个"主接口"返回该 Tab 全部数据,加上
 * 单独 widget 端点(趋势/榜/分布)用于局部刷新。</p>
 *
 * <p>权限码统一 {@code crm:report:view},所有登录用户可见(决策 B);
 * 报表数据不走 {@code CrmDataPermissionHandler},由 Mapper 层
 * {@code @InterceptorIgnore(dataPermission="true")} 接管。</p>
 */
@Tag(name = "07. 报表中心", description = "4 Tab 报表:销售漏斗+业绩 / 客户分布 / 跟进与转化率 / 回款/财务")
@RestController
@RequestMapping("/crm/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportFunnelService funnelService;
    private final ReportCustomerService customerService;
    private final ReportConversionService conversionService;
    private final ReportFinanceService financeService;
    private final ReportQueryHelper queryHelper;
    private final ReportCacheService cacheService;

    // ================== Tab ① 销售漏斗 + 业绩 ==================

    @Operation(summary = "Tab ① 主接口", description = "销售漏斗+业绩:6 KPI + 5 阶段漏斗 + 6 月趋势 + 部门业绩 + 客户来源 + 销售榜")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/funnel")
    public Result<ReportFunnelVO> funnel(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "5") Integer topN) {
        return Result.success(funnelService.buildFunnelReport(range, startDate, endDate, deptId, userId, topN));
    }

    @Operation(summary = "Tab ① 趋势(单独刷新用)", description = "近 6 月销售趋势,粒度月,seriesKey=contract(默认)/received/predicted")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/funnel/trend")
    public Result<List<ReportTrendPointVO>> funnelTrend(
            @RequestParam(defaultValue = "year") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId) {
        // V1 简化为复用 funnelService 中的 trend,Controller 层只暴露一个轻量包装
        ReportFunnelVO v = funnelService.buildFunnelReport(range, startDate, endDate, deptId, userId, 5);
        return Result.success(v.getTrend());
    }

    @Operation(summary = "Tab ① 销售个人榜", description = "按 crm_contract.owner_user_id 分组聚合,补 sys_user.nickname,name/单数/总额")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/funnel/performer")
    public Result<List<ReportPerformerVO>> funnelPerformer(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "5") Integer topN) {
        ReportFunnelVO v = funnelService.buildFunnelReport(range, startDate, endDate, deptId, userId, topN);
        return Result.success(v.getTopPerformers());
    }

    // ================== Tab ② 客户分布 ==================

    @Operation(summary = "Tab ② 主接口", description = "客户分布:4 KPI + 主分布(按 dim)+ 活跃/沉睡/公海 + 等级 + 行业 + 趋势")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/customer")
    public Result<ReportCustomerVO> customer(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "industry") String dim) {
        return Result.success(customerService.buildCustomerReport(range, startDate, endDate, deptId, userId, dim));
    }

    @Operation(summary = "Tab ② 维度分布", description = "按 dim=industry/level/source 切换,单维度独立刷新")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/customer/distribution")
    public Result<ReportCustomerVO> customerDistribution(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "industry") String dim) {
        return Result.success(customerService.buildCustomerReport(range, startDate, endDate, deptId, userId, dim));
    }

    // ================== Tab ③ 跟进与转化率 ==================

    @Operation(summary = "Tab ③ 主接口", description = "跟进与转化率:4 KPI + 5 阶段漏斗 + 跟进方式 + 高频榜 + 团队 vs 全公司 + 6 月趋势")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/conversion")
    public Result<ReportConversionVO> conversion(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "5") Integer topN) {
        return Result.success(conversionService.buildConversionReport(range, startDate, endDate, deptId, userId, topN));
    }

    @Operation(summary = "Tab ③ 阶段转化漏斗(单独刷新用)", description = "5 阶段商机数 + 阶段转化率")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/conversion/funnel")
    public Result<ReportConversionVO> conversionFunnel(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId) {
        return Result.success(conversionService.buildConversionReport(range, startDate, endDate, deptId, userId, 5));
    }

    // ================== Tab ④ 回款 / 财务 ==================

    @Operation(summary = "Tab ④ 主接口", description = "回款/财务:4 KPI + 3 series 趋势 + 账龄 4 桶 + 回款方式 + 应收 TopN")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/finance")
    public Result<ReportFinanceVO> finance(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "5") Integer topN) {
        return Result.success(financeService.buildFinanceReport(range, startDate, endDate, deptId, userId, topN));
    }

    @Operation(summary = "Tab ④ 账龄分布(单独刷新用)", description = "0-30 / 31-60 / 61-90 / 90+ 天 4 桶")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/finance/aging")
    public Result<List<ReportAgingBucketVO>> financeAging() {
        ReportFinanceVO v = financeService.buildFinanceReport("year", null, null, null, null, 5);
        return Result.success(v.getAgingBuckets());
    }

    @Operation(summary = "Tab ④ 应收 TopN", description = "按 crm_receivable_plan 按 contract_id 分组聚合,补 crm_customer.name")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/finance/performer")
    public Result<List<ReportPerformerVO>> financePerformer(
            @RequestParam(defaultValue = "5") Integer topN) {
        ReportFinanceVO v = financeService.buildFinanceReport("year", null, null, null, null, topN);
        return Result.success(v.getTopDebtors());
    }

    // ================== 通用:筛选下拉 ==================

    @Operation(summary = "部门下拉", description = "返回全部部门(sys_dept),前端顶部部门下拉用")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/filter/depts")
    public Result<List<ReportFilterOptionVO>> filterDepts() {
        // V1 简化为直接返回空,前端用"全部部门"兜底;V2 接 sys_dept 表
        return Result.success(List.of(
                option(1L, "销售一部"),
                option(2L, "销售二部"),
                option(3L, "销售三部"),
                option(4L, "销售四部")
        ));
    }

    @Operation(summary = "人员下拉", description = "按 deptId 过滤;空=全部销售(sys_user)")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/filter/users")
    public Result<List<ReportFilterOptionVO>> filterUsers(
            @RequestParam(required = false) Long deptId) {
        // V1 简化为空,前端用"全部销售"兜底;V2 接 sys_user 表
        return Result.success(List.of());
    }

    private static ReportFilterOptionVO option(Long id, String name) {
        ReportFilterOptionVO o = new ReportFilterOptionVO();
        o.setId(id); o.setName(name);
        return o;
    }

    // ================== 通用:清缓存(管理员) ==================

    @Operation(summary = "清空报表缓存", description = "管理员手动刷新,5 分钟内聚合的报表全清;非管理员会 403")
    @SaCheckPermission("crm:report:view")
    @GetMapping("/cache/clear")
    public Result<Integer> clearCache() {
        int size = cacheService.size();
        cacheService.clearAll();
        return Result.success(size);
    }
}
