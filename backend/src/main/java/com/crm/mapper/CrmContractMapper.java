package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmContract;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同 Mapper
 *
 * <p>有 owner_user_id,受 {@code CrmDataPermissionHandler} 拦截。
 * 报表聚合方法全部 {@code @InterceptorIgnore(dataPermission="true")} 绕过,
 * 与决策 B 保持一致(报表不叠加 owner 拦截,所有角色看全量)。</p>
 *
 * <p>阶段八 commit 2 升级:所有金额聚合加 {@code status IN (1,2)} 过滤(C2-D5);
 * 新增 {@link #sumByDeptIds} 按部门聚合(crm_contract JOIN sys_user),给部门业绩卡片用(C2-D4)。</p>
 */
@Mapper
public interface CrmContractMapper extends BaseMapper<CrmContract> {

    /** 报表通用前缀(子查询别名 _t) */
    String T = "_t";

    // ================== 阶段五 commit 2:报表聚合(全部 bypass 数据权限) ==================

    /**
     * 按签约日期区间 + ownerIds 过滤,累加合同总金额(只算 status IN (1,2)·阶段八 commit 2·C2-D5)
     * <p>ownerIds 为 null 表示不限人员(全部);空集合返回 ZERO(无数据)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumTotalAmount(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return BigDecimal.ZERO;
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(total_amount),0) AS total");
        w.in("status", 1, 2);   // C2-D5:排除审批中(0)/已作废(3)
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * phase8 commit1 修复:合同总额全量累计口径 — 排除 status=3(已作废),保留 0/1/2
     * <p>适用场景:Tab ④ 回款/财务的"合同总额" KPI,需要累计含"审批中"和"执行中/已结束"全部状态。</p>
     * <p>phase8 commit1 修订:**完全去掉 start_date 过滤**。原版用 start_date 范围,会漏掉
     * start_date 在今天之后的合同(测试合同 start_date='2026-07-01' 被 LocalDateTime.now()='2026-06-30 12:46:xx'
     * 误排)。改为"无时间范围 = 全量累计"。</p>
     * <p>与 {@link #sumTotalAmount} 区别:旧版本 status IN (1,2) 排除了"审批中(0)",
     * 新口径与 Tab ① 销售漏斗解耦,只服务财务 Tab 的 KPI 卡片(累计全量)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumTotalAmountAll(Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return BigDecimal.ZERO;
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(total_amount),0) AS total");
        w.ne("status", 3);   // 仅排除已作废,保留 0/1/2
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * phase8 commit1 新增:按 status 分组统计合同数 + 金额(4 状态全集合)
     * <p>返回 [{status:0, count:3, amount:'150000'}, ...],status 枚举:0 审批中 / 1 执行中 / 2 已结束 / 3 已作废</p>
     * <p>用于 Tab ④ "合同状态分布"图。包含 status=3 已作废(让财务看到作废数据)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByStatus(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("status",
                "COUNT(*) AS cnt",
                "COALESCE(SUM(total_amount),0) AS sum");
        if (ownerIds != null && !ownerIds.isEmpty()) {
            w.in("owner_user_id", ownerIds);
        } else if (ownerIds != null) {
            // ownerIds 非 null 且为空 → 无数据
            return List.of();
        }
        if (start != null) w.ge("start_date", start);
        if (end != null) w.le("start_date", end);
        w.groupBy("status");
        w.orderByAsc("status");
        return selectMaps(w);
    }

    /**
     * 合同数(同区间,status IN (1,2)·C2-D5)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.in("status", 1, 2);   // C2-D5
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 6 月度趋势:按月 SUM(total_amount),返回 [{month:'2026-01', sum:'1420000'}, ...]
     * <p>粒度固定 month,与决策 B 保持一致(报表趋势用月粒度,日报用天粒度)。</p>
     * <p>SQL 字段 {@code start_date} (合同开始日期) — 表无 sign_date。</p>
     * <p>阶段八 commit 2:加 status IN (1,2)·C2-D5。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> sumByMonth(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("DATE_FORMAT(start_date,'%Y-%m') AS month", "COALESCE(SUM(total_amount),0) AS sum");
        w.in("status", 1, 2);   // C2-D5
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.groupBy("DATE_FORMAT(start_date,'%Y-%m')");
        w.orderByAsc("month");
        return selectMaps(w);
    }

    /**
     * 按 owner_user_id 分组聚合(销售个人榜用,status IN (1,2)·C2-D5)
     * <p>返回 [{owner_user_id, owner_name, contract_count, total_amount}, ...] 按金额降序</p>
     * <p>注:owner_name 由 Service 层 sys_user JOIN 补充(Mapper 不跨表)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByOwner(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds, int topN) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("owner_user_id", "COUNT(*) AS cnt", "COALESCE(SUM(total_amount),0) AS sum");
        w.in("status", 1, 2);   // C2-D5
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.isNotNull("owner_user_id");
        w.groupBy("owner_user_id");
        w.orderByDesc("sum");
        w.last("LIMIT " + Math.min(topN, 50));
        return selectMaps(w);
    }

    /**
     * 客单价(已废弃,改用 sumTotalAmount / countByRange 在 Service 算比值)
     * 保留空方法以兼容旧调用,实际不使用。
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> sumByOwner(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        return groupByOwner(start, end, ownerIds, 50);
    }

    // ================== 阶段八 commit 2:按部门聚合(C2-D4) ==================

    /**
     * 按部门聚合合同业绩(阶段八 commit 2·C2-D4)
     * <p>crm_contract JOIN sys_user,按 {@code u.dept_id} 分组 SUM(total_amount),
     * 过滤 status IN (1,2)·C2-D5。</p>
     *
     * <p>典型 SQL:
     * <pre>
     * SELECT u.dept_id AS dept_id, COALESCE(SUM(c.total_amount),0) AS sum
     *   FROM crm_contract c JOIN sys_user u ON u.id = c.owner_user_id
     *  WHERE c.status IN (1, 2)
     *    AND c.start_date BETWEEN #{start} AND #{end}
     *    [AND c.owner_user_id IN (...)]   // ownerIds 不为 null 时
     *    AND u.dept_id IN (...{deptIds}...)
     *  GROUP BY u.dept_id
     * </pre>
     * </p>
     *
     * @return deptId → 合同业绩金额,无数据返回空 Map
     */
    @Select("""
            <script>
            SELECT u.dept_id              AS dept_id,
                   COALESCE(SUM(c.total_amount), 0) AS sum
              FROM crm_contract c
              JOIN sys_user    u ON u.id = c.owner_user_id
             WHERE c.status     IN (1, 2)
               AND c.is_deleted  = 0
               AND u.status      = 1
               AND u.is_deleted  = 0
               AND c.start_date BETWEEN #{start} AND #{end}
               <if test="ownerIds != null">
                 AND c.owner_user_id IN
                 <foreach collection="ownerIds" item="id" open="(" separator="," close=")">
                   #{id}
                 </foreach>
               </if>
               AND u.dept_id IN
               <foreach collection="deptIds" item="id" open="(" separator="," close=")">
                 #{id}
               </foreach>
             GROUP BY u.dept_id
            </script>
            """)
    @InterceptorIgnore(dataPermission = "true")
    List<Map<String, Object>> sumByDeptIdsRaw(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("ownerIds") Collection<Long> ownerIds,
                                              @Param("deptIds") Collection<Long> deptIds);

    /**
     * 按部门聚合合同业绩,返回 {@code Map<Long, BigDecimal> deptId → 金额}
     */
    default Map<Long, BigDecimal> sumByDeptIds(LocalDateTime start, LocalDateTime end,
                                              Collection<Long> ownerIds, Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyMap();
        if (ownerIds != null && ownerIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = sumByDeptIdsRaw(start, end, ownerIds, deptIds);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long deptId = ReportUtils.toLong(r.get("dept_id"));
            BigDecimal sum = ReportUtils.toBigDecimal(r.get("sum"));
            out.put(deptId, sum);
        }
        return out;
    }
}