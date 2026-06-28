package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmContract;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 合同 Mapper
 *
 * <p>有 owner_user_id,受 {@code CrmDataPermissionHandler} 拦截。
 * 报表聚合方法全部 {@code @InterceptorIgnore(dataPermission="true")} 绕过,
 * 与决策 B 保持一致(报表不叠加 owner 拦截,所有角色看全量)。</p>
 */
@Mapper
public interface CrmContractMapper extends BaseMapper<CrmContract> {

    /** 报表通用前缀(子查询别名 _t) */
    String T = "_t";

    // ================== 阶段五 commit 2:报表聚合(全部 bypass 数据权限) ==================

    /**
     * 按签约日期区间 + ownerIds 过滤,累加合同总金额。
     * <p>ownerIds 为 null 表示不限人员(全部);空集合返回 ZERO(无数据)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumTotalAmount(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return BigDecimal.ZERO;
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(total_amount),0) AS total");
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * 合同数(同区间)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 6 月度趋势:按月 SUM(total_amount),返回 [{month:'2026-01', sum:'1420000'}, ...]
     * <p>粒度固定 month,与决策 B 保持一致(报表趋势用月粒度,日报用天粒度)。</p>
     * <p>SQL 字段 {@code start_date} (合同开始日期) — 表无 sign_date。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> sumByMonth(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("DATE_FORMAT(start_date,'%Y-%m') AS month", "COALESCE(SUM(total_amount),0) AS sum");
        w.ge("start_date", start);
        w.le("start_date", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.groupBy("DATE_FORMAT(start_date,'%Y-%m')");
        w.orderByAsc("month");
        return selectMaps(w);
    }

    /**
     * 按 owner_user_id 分组聚合(销售个人榜用)
     * <p>返回 [{owner_user_id, owner_name, contract_count, total_amount}, ...] 按金额降序</p>
     * <p>注:owner_name 由 Service 层 sys_user JOIN 补充(Mapper 不跨表)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByOwner(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds, int topN) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmContract> w = new QueryWrapper<>();
        w.select("owner_user_id", "COUNT(*) AS cnt", "COALESCE(SUM(total_amount),0) AS sum");
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
}
