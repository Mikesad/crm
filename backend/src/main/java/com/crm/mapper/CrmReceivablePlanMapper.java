package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmReceivablePlan;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 回款计划 Mapper
 *
 * <p>无 owner_user_id,V1 不入 CrmDataPermissionHandler.MANAGED_TABLES,
 * 权限靠 {@code @SaCheckPermission} 兜底。</p>
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 */
@Mapper
public interface CrmReceivablePlanMapper extends BaseMapper<CrmReceivablePlan> {

    /**
     * 未回款计划总额(status != 2)
     */
    @InterceptorIgnore(dataPermission = "true")
    default BigDecimal sumUnreceived() {
        QueryWrapper<CrmReceivablePlan> w = new QueryWrapper<>();
        w.select("COALESCE(SUM(expected_amount),0) AS total");
        w.ne("status", 2);
        List<Map<String, Object>> rows = selectMaps(w);
        return ReportUtils.toBigDecimal(rows.isEmpty() ? null : rows.get(0).get("total"));
    }

    /**
     * 拉出未回款计划(供 Service 端按 expected_date 算账龄分桶)。
     * <p>返回字段:contractId / expectedDate / expectedAmount / status。
     * V1 简化为全量拉(N 通常 1000 以内),Service 端按 DATEDIFF 分桶。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<CrmReceivablePlan> listUnreceivedForAging() {
        QueryWrapper<CrmReceivablePlan> w = new QueryWrapper<>();
        w.ne("status", 2);
        w.isNotNull("expected_date");
        w.isNotNull("expected_amount");
        return selectList(w);
    }

    /**
     * 按客户(合同)分组的应收 TopN
     * <p>返回 [{contract_id, cnt, sum}, ...] 按 sum 降序;
     * customerName 由 Service 端 crm_contract JOIN crm_customer 补全。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> topByUnreceived(int topN) {
        QueryWrapper<CrmReceivablePlan> w = new QueryWrapper<>();
        w.select("contract_id", "COUNT(*) AS cnt", "COALESCE(SUM(expected_amount),0) AS sum");
        w.ne("status", 2);
        w.groupBy("contract_id");
        w.orderByDesc("sum");
        w.last("LIMIT " + Math.min(topN, 50));
        return selectMaps(w);
    }

    /**
     * 按状态统计计划数(用于账龄时进一步分桶)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByStatus(int status) {
        QueryWrapper<CrmReceivablePlan> w = new QueryWrapper<>();
        w.eq("status", status);
        return ReportUtils.toLong(selectCount(w));
    }
}
