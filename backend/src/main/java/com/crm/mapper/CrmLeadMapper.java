package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmLead;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 线索 Mapper
 *
 * <p>基础 CRUD 由 MyBatis-Plus {@code BaseMapper} 提供；复杂查询（按 dataScope 拼接条件、
 * 跨表 join）放在 XML 文件中。</p>
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 *
 * <p>报表 Tab ① 的"客户来源分布"实际统计的是 crm_lead.source(线索来源),
 * 语义:本月新增的线索来自哪些渠道,转客户后该信息在 crm_lead 历史中保留。</p>
 */
@Mapper
public interface CrmLeadMapper extends BaseMapper<CrmLead> {

    /** 线索数(全量,不限 owner) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countAllByRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmLead> w = new QueryWrapper<>();
        w.ge("create_time", start);
        w.le("create_time", end);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 死线索数(status=4) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countDeadByRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmLead> w = new QueryWrapper<>();
        w.eq("status", 4);
        w.ge("create_time", start);
        w.le("create_time", end);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 按来源分组(source, count) */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupBySource(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds, int limit) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmLead> w = new QueryWrapper<>();
        w.select("source AS k", "COUNT(*) AS cnt");
        w.isNotNull("source");
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        w.groupBy("source");
        w.orderByDesc("cnt");
        w.last("LIMIT " + Math.min(limit, 50));
        return selectMaps(w);
    }

    /**
     * 转客户线索数(status=3)
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countConvertedByRange(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmLead> w = new QueryWrapper<>();
        w.eq("status", 3);
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }

    /**
     * 漏斗首阶段"新建线索"统计:status IN (1未跟进, 2跟进中)
     * <p>排除了 3已转客户(已沉淀到 crm_business)和 4已死线索(决策 A.2)。</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default Long countInFunnel(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return 0L;
        QueryWrapper<CrmLead> w = new QueryWrapper<>();
        w.in("status", 1, 2);
        w.ge("create_time", start);
        w.le("create_time", end);
        if (ownerIds != null) w.in("owner_user_id", ownerIds);
        return ReportUtils.toLong(selectCount(w));
    }
}
