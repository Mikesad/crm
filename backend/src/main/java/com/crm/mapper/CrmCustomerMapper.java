package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmCustomer;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

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
}
