package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmReceivable;
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

/**
 * 回款记录 Mapper
 *
 * <p>append-only,无 is_deleted,不走逻辑删除拦截器。
 * 受 {@code CrmDataPermissionHandler.MANAGED_TABLES} 拦截(本表 owner_user_id 由 contract 决定,
 * V1 直接当受控表处理:如果 SQL 不带 owner 列会自动跳过,见 Task #7 V2 优化)。</p>
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 *
 * <p>阶段八 commit 2 扩展:新增 {@link #sumActualByDeptIds(LocalDateTime, LocalDateTime, Collection)}
 * 按部门聚合回款金额(crm_receivable JOIN crm_contract JOIN sys_user),给部门业绩卡片实际回款口径用(C2-D4)。</p>
 */
@Mapper
public interface CrmReceivableMapper extends BaseMapper<CrmReceivable> {

    /**
     * 已回款金额总和(按 return_date 区间)
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumActualByRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmReceivable> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(actual_amount),0) AS total");
        w.ge("return_date", start);
        w.le("return_date", end);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * 6 月度趋势:按月 SUM(actual_amount)
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> sumActualByMonth(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmReceivable> w = new QueryWrapper<>();
        w.select("DATE_FORMAT(return_date,'%Y-%m') AS month", "COALESCE(SUM(actual_amount),0) AS sum");
        w.ge("return_date", start);
        w.le("return_date", end);
        w.groupBy("DATE_FORMAT(return_date,'%Y-%m')");
        w.orderByAsc("month");
        return selectMaps(w);
    }

    /**
     * 总回款数(append-only,无 is_deleted 过滤)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countAll() {
        return ReportUtils.toLong(selectCount(null));
    }

    /**
     * 按支付方式分组(payment_method, count, sum)
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByMethod(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmReceivable> w = new QueryWrapper<>();
        w.select("payment_method AS k", "COUNT(*) AS cnt", "COALESCE(SUM(actual_amount),0) AS sum");
        w.isNotNull("payment_method");
        w.ge("return_date", start);
        w.le("return_date", end);
        w.groupBy("payment_method");
        w.orderByDesc("sum");
        return selectMaps(w);
    }

    // ================== 阶段八 P3:按 owner 聚合回款(销售个人榜回款口径) ==================

    /**
     * 按 owner_user_id 分组聚合实际回款(P3·2026-06-29,给销售个人榜回款口径用)
     * <p>crm_receivable JOIN crm_contract JOIN sys_user,按 {@code c.owner_user_id} 分组
     * SUM(actual_amount),过滤 contract status IN (1,2)·与 C2-D5 一致。</p>
     *
     * <p>典型 SQL:
     * <pre>
     * SELECT c.owner_user_id AS owner_id, COALESCE(SUM(r.actual_amount),0) AS sum
     *   FROM crm_receivable r JOIN crm_contract c ON c.id = r.contract_id
     *  WHERE r.return_date BETWEEN #{start} AND #{end}
     *    AND c.status IN (1, 2) AND c.is_deleted = 0
     *    [AND c.owner_user_id IN (...)]  // ownerIds 不为 null 时
     *  GROUP BY c.owner_user_id
     * </pre>
     * </p>
     *
     * @return ownerUserId → 实收金额 Map
     */
    @Select("""
            <script>
            SELECT c.owner_user_id                AS owner_id,
                   COALESCE(SUM(r.actual_amount), 0) AS sum
              FROM crm_receivable r
              JOIN crm_contract   c ON c.id = r.contract_id
             WHERE r.return_date BETWEEN #{start} AND #{end}
               AND c.status      IN (1, 2)
               AND c.is_deleted   = 0
               <if test="ownerIds != null">
                 AND c.owner_user_id IN
                 <foreach collection="ownerIds" item="id" open="(" separator="," close=")">
                   #{id}
                 </foreach>
               </if>
             GROUP BY c.owner_user_id
            </script>
            """)
    @InterceptorIgnore(dataPermission = "true")
    List<Map<String, Object>> sumActualByOwnerIdsRaw(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end,
                                                     @Param("ownerIds") Collection<Long> ownerIds);

    /**
     * 按 owner 聚合回款,返回 {@code Map<Long, BigDecimal>}
     */
    default Map<Long, BigDecimal> sumActualByOwnerIds(LocalDateTime start, LocalDateTime end,
                                                      Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = sumActualByOwnerIdsRaw(start, end, ownerIds);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long ownerId = ReportUtils.toLong(r.get("owner_id"));
            BigDecimal sum = ReportUtils.toBigDecimal(r.get("sum"));
            out.put(ownerId, sum);
        }
        return out;
    }

    // ================== 阶段八 commit 2:按部门聚合回款(C2-D4) ==================

    /**
     * 按部门聚合实际回款(阶段八 commit 2·C2-D4)
     * <p>crm_receivable JOIN crm_contract JOIN sys_user,按 {@code u.dept_id} 分组 SUM(actual_amount)。
     * 合同侧过滤 status IN (1,2)(C2-D5 一致性)。</p>
     *
     * @return deptId → 回款金额 Map;无数据返回空 Map
     */
    @Select("""
            <script>
            SELECT u.dept_id                       AS dept_id,
                   COALESCE(SUM(r.actual_amount), 0) AS sum
              FROM crm_receivable r
              JOIN crm_contract   c ON c.id = r.contract_id
              JOIN sys_user       u ON u.id = c.owner_user_id
             WHERE r.return_date BETWEEN #{start} AND #{end}
               AND c.status      IN (1, 2)
               AND c.is_deleted   = 0
               AND u.status       = 1
               AND u.is_deleted   = 0
               AND u.dept_id IN
               <foreach collection="deptIds" item="id" open="(" separator="," close=")">
                 #{id}
               </foreach>
             GROUP BY u.dept_id
            </script>
            """)
    @InterceptorIgnore(dataPermission = "true")
    List<Map<String, Object>> sumActualByDeptIdsRaw(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end,
                                                    @Param("deptIds") Collection<Long> deptIds);

    /**
     * 按部门聚合回款金额,返回 {@code Map<Long, BigDecimal> deptId → 金额}
     */
    default Map<Long, BigDecimal> sumActualByDeptIds(LocalDateTime start, LocalDateTime end,
                                                     Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = sumActualByDeptIdsRaw(start, end, deptIds);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long deptId = ReportUtils.toLong(r.get("dept_id"));
            BigDecimal sum = ReportUtils.toBigDecimal(r.get("sum"));
            out.put(deptId, sum);
        }
        return out;
    }
}