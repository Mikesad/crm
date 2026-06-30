package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmCustomer;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 客户 Mapper
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 *
 * <p>活跃定义:last_follow_time &gt;= (now - 30d);
 * 沉睡:last_follow_time &lt; (now - 30d) 或 NULL;
 * 公海:is_public=1(LIMIT 不限 owner)。</p>
 */
@Mapper
public interface CrmCustomerMapper extends BaseMapper<CrmCustomer> {

    /** 客户总数(全量,不过滤 owner) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countAll() {
        return ReportUtils.toLong(selectCount(null));
    }

    /**
     * 新增客户数(P19·2026-06-30,KPI 用)
     * <p>按 create_time 区间 + ownerIds 过滤,统计新增客户数(排除逻辑删除)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countNewCustomers(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 客户总数(按 ownerIds 过滤,ownerIds=null=全部) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByOwner(Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 不同行业数(COUNT DISTINCT industry) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countDistinctIndustry(Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.select("COUNT(DISTINCT industry) AS cnt");
        w.isNotNull("industry");
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toLong(rows.isEmpty() ? null : rows.get(0).get("cnt"));
    }

    /** 按行业分组聚合(industry, count) LIMIT 500 兜底 */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByIndustry(Collection<Long> ownerIds, int limit) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.select("industry AS k", "COUNT(*) AS cnt");
        w.isNotNull("industry");
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.groupBy("industry");
        w.orderByDesc("cnt");
        w.last("LIMIT " + Math.min(limit, 500));
        return selectMaps(w);
    }

    /** 按客户等级分组(A/B/C) */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByLevel(Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.select("level AS k", "COUNT(*) AS cnt");
        w.isNotNull("level");
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.groupBy("level");
        w.orderByDesc("cnt");
        return selectMaps(w);
    }

    /** 活跃数(last_follow_time >= now-30d) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countActive(int days, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.ge("last_follow_time", since);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 沉睡数(last_follow_time < now-30d 或 NULL) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countDormant(int days, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.and(qw -> qw.lt("last_follow_time", since).or().isNull("last_follow_time"));
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 公海数(is_public=1),ownerIds 强制失效(公海无 owner) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countPublic() {
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.eq("is_public", 1);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 期内被跟进客户数(阶段八 commit 4·2026-06-30,Tab ③"跟进率"分子)
     * <p>语义:客户的最近一次跟进时间 {@code last_follow_time} 落在 [start, end] 区间内
     * 视为"期内被跟进";与活跃/沉睡定义(last_follow_time vs today-N days)解耦,
     * 支持页面时间维度查询。</p>
     *
     * <p>注意:不传 ownerIds 时即"全公司"统计,符合报表决策 B。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countFollowedInRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.ge("last_follow_time", start);
        w.le("last_follow_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 期内新增客户数(阶段八 commit 5·2026-06-30,Tab ③"客户转换率"分子)
     * <p>按 {@code crm_customer.create_by → sys_user.username → sys_user.dept_id} 链路过滤部门;
     * 与 owner_user_id 维度的 {@link #countNewCustomers} 不同 — 此处取"创建人部门",
     * 用于反映销售员的产出(谁创建了这个客户),而不是客户的当前归属。</p>
     *
     * <p>典型 SQL:
     * <pre>
     * SELECT COUNT(*) FROM crm_customer c
     *   JOIN sys_user u ON u.username = c.create_by
     *  WHERE c.create_time BETWEEN #{start} AND #{end}
     *    AND u.status = 1 AND u.is_deleted = 0
     *    AND u.dept_id IN (...deptIds...)
     * </pre>
     * </p>
     */
    @Select("""
            <script>
            SELECT COUNT(*)
              FROM crm_customer c
              JOIN sys_user u ON u.username = c.create_by
             WHERE c.create_time BETWEEN #{start} AND #{end}
               AND u.status    = 1
               AND u.is_deleted = 0
               AND u.dept_id IN
               <foreach collection="deptIds" item="id" open="(" separator="," close=")">
                 #{id}
               </foreach>
            </script>
            """)
    @InterceptorIgnore(dataPermission = "true")
    Long countNewCustomersByDeptIdsRaw(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("deptIds") Collection<Long> deptIds);

    /**
     * 包装方法:deptIds 为空时返回 0
     */
    default Long countNewCustomersByDeptIds(LocalDateTime start, LocalDateTime end, Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return 0L;
        return countNewCustomersByDeptIdsRaw(start, end, deptIds);
    }

    /**
     * 期内活跃客户数(阶段八 commit 8·2026-06-30,Tab ②"活跃"维度,owner_user_id 链路)
     * <p>语义:last_follow_time ∈ [start, end] 且 is_public=0(未入公海)的客户数;
     * 支持页面时间维度 + 部门维度(ownerIds)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countActiveInRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.ge("last_follow_time", start);
        w.le("last_follow_time", end);
        w.eq("is_public", 0);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 期内新增到公海的客户数(阶段八 commit 8·Tab ②"公海"维度)
     * <p>语义:is_public=1 且 create_time ∈ [start, end]。
     * 公海无 owner,ownerIds 暂不参与过滤(全公司口径,符合决策 B)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countPublicInRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmCustomer> w = new QueryWrapper<>();
        w.eq("is_public", 1);
        w.ge("create_time", start);
        w.le("create_time", end);
        return ReportUtils.toLong(selectCount(w));
    }
}
