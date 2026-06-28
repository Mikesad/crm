package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmReceivable;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}
