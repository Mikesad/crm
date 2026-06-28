package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmRecord;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 跟进记录 Mapper
 *
 * <p>报表聚合方法 bypass 数据权限(决策 B)。</p>
 */
@Mapper
public interface CrmRecordMapper extends BaseMapper<CrmRecord> {

    /** 跟进总数(全量,不限 owner) */
    @InterceptorIgnore(dataPermission = "true")
    default Long countByRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmRecord> w = new QueryWrapper<>();
        w.ge("create_time", start);
        w.le("create_time", end);
        return ReportUtils.toLong(selectCount(w));
    }

    /** 按跟进方式分组(电话/微信/拜访/邮件/其他) */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByFollowType(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmRecord> w = new QueryWrapper<>();
        w.select("follow_type AS k", "COUNT(*) AS cnt");
        w.isNotNull("follow_type");
        w.ge("create_time", start);
        w.le("create_time", end);
        w.groupBy("follow_type");
        w.orderByDesc("cnt");
        return selectMaps(w);
    }

    /**
     * 高频跟进人榜:按 create_by 分组统计,按条数降序
     * <p>返回 [{create_by, cnt}, ...];name 由 Service 层 sys_user JOIN 补</p>
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByCreateBy(LocalDateTime start, LocalDateTime end, int topN) {
        QueryWrapper<CrmRecord> w = new QueryWrapper<>();
        w.select("create_by", "COUNT(*) AS cnt");
        w.isNotNull("create_by");
        w.ge("create_time", start);
        w.le("create_time", end);
        w.groupBy("create_by");
        w.orderByDesc("cnt");
        w.last("LIMIT " + Math.min(topN, 50));
        return selectMaps(w);
    }

    /**
     * 按月趋势:跟进条数(DATE_FORMAT)
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByMonth(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmRecord> w = new QueryWrapper<>();
        w.select("DATE_FORMAT(create_time,'%Y-%m') AS month", "COUNT(*) AS cnt");
        w.ge("create_time", start);
        w.le("create_time", end);
        w.groupBy("DATE_FORMAT(create_time,'%Y-%m')");
        w.orderByAsc("month");
        return selectMaps(w);
    }

    /**
     * 按关联类型分组的跟进数(转化漏斗辅助,按 relatedType 聚合)
     */
    @InterceptorIgnore(dataPermission = "true")
    default List<Map<String, Object>> groupByRelatedType(LocalDateTime start, LocalDateTime end, Collection<Long> ownerIds) {
        if (ownerIds != null && ownerIds.isEmpty()) return List.of();
        QueryWrapper<CrmRecord> w = new QueryWrapper<>();
        w.select("related_type AS k", "COUNT(*) AS cnt");
        w.isNotNull("related_type");
        w.ge("create_time", start);
        w.le("create_time", end);
        w.groupBy("related_type");
        w.orderByDesc("cnt");
        return selectMaps(w);
    }
}
