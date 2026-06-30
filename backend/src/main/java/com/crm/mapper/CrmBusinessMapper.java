package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmBusiness;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商机 Mapper
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 *
 * <p><b>stage 字段约定(crm_business.stage varchar(20))</b>:实际存中文,不是 int 1-5!
 * 实际枚举:</p>
 * <ul>
 *   <li>需求分析(默认)</li>
 *   <li>方案报价</li>
 *   <li>商务谈判</li>
 *   <li>赢单</li>
 *   <li>输单(决策 B 不计入漏斗)</li>
 * </ul>
 *
 * <p>报表漏斗 5 阶段约定(跨表):</p>
 * <ol>
 *   <li>新建线索 → 走 crm_lead(status IN 1,2)</li>
 *   <li>需求分析 → crm_business.stage='需求分析'</li>
 *   <li>方案报价 → crm_business.stage='方案报价'</li>
 *   <li>商务谈判 → crm_business.stage='商务谈判'</li>
 *   <li>赢单 → crm_business.stage='赢单'</li>
 * </ol>
 *
 * <p>Service 端 {@code buildFunnelStages} 调本接口 4 次(后 4 阶段),首阶段走
 * {@code CrmLeadMapper.countInFunnel}。</p>
 */
@Mapper
public interface CrmBusinessMapper extends BaseMapper<CrmBusiness> {

    /** stage 中文名常量,统一漏斗引用 */
    String STAGE_ANALYSIS  = "需求分析";
    String STAGE_QUOTE     = "方案报价";
    String STAGE_NEGOTIATE = "商务谈判";
    String STAGE_WIN       = "赢单";
    String STAGE_LOSE      = "输单";

    /**
     * 单个阶段商机数(漏斗用,Service 调 4 次)
     * @param stage 中文 stage 名(STAGE_ANALYSIS 等)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByStage(String stage, LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmBusiness> w = new QueryWrapper<>();
        w.eq("stage", stage);
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 单个阶段累计金额(漏斗辅助)
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumByStage(String stage, LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return BigDecimal.ZERO;
        QueryWrapper<CrmBusiness> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(expected_amount),0) AS total");
        w.eq("stage", stage);
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * 总商机数(含所有 stage)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countAllByRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmBusiness> w = new QueryWrapper<>();
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 赢单数(stage='赢单')
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long winCountByRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        return countByStage(STAGE_WIN, start, end, ownerIds);
    }

    /**
     * 期内新增商机总数(阶段八 commit 6·2026-06-30,Tab ③"商机转换率"分子)
     * <p>JOIN sys_user 按 {@code crm_business.create_by → sys_user.dept_id} 链路过滤部门;
     * 与 {@link #countAllByRange} 的 owner_user_id 维度不同 — 此处取"创建人部门"
     * 以与 {@link CrmCustomerMapper#countNewCustomersByDeptIds} 的口径保持一致。</p>
     *
     * <p>典型 SQL:
     * <pre>
     * SELECT COUNT(*) FROM crm_business b
     *   JOIN sys_user u ON u.username = b.create_by
     *  WHERE b.create_time BETWEEN #{start} AND #{end}
     *    AND b.is_deleted = 0
     *    AND u.status = 1 AND u.is_deleted = 0
     *    AND u.dept_id IN (...deptIds...)
     * </pre>
     * </p>
     */
    @Select("""
            <script>
            SELECT COUNT(*)
              FROM crm_business b
              JOIN sys_user u ON u.username = b.create_by
             WHERE b.create_time BETWEEN #{start} AND #{end}
               AND b.is_deleted = 0
               AND u.status    = 1
               AND u.is_deleted = 0
               AND u.dept_id IN
               <foreach collection="deptIds" item="id" open="(" separator="," close=")">
                 #{id}
               </foreach>
            </script>
            """)
    @InterceptorIgnore(dataPermission = "true")
    Long countAllByRangeByCreateByDeptIdsRaw(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("deptIds") Collection<Long> deptIds);

    /**
     * 包装方法:deptIds 为 null/空 时走全公司口径(走基类 countByTimeRange 路径),
     * 否则走 create_by → dept_id JOIN 链路。
     */
    default Long countAllByRangeByCreateByDeptIds(LocalDateTime start, LocalDateTime end, Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            QueryWrapper<CrmBusiness> w = new QueryWrapper<>();
            w.ge("create_time", start);
            w.le("create_time", end);
            w.eq("is_deleted", 0);
            return ReportUtils.toLong(selectCount(w));
        }
        return countAllByRangeByCreateByDeptIdsRaw(start, end, deptIds);
    }
}
