package com.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmBusiness;
import com.crm.util.ReportUtils;
import org.apache.ibatis.annotations.Mapper;

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
}
