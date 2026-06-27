package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmRecord;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.vo.RecycledCustomerVO;
import com.crm.vo.RecycleResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 公海池回收服务
 *
 * <p>阶段四职责：① 自动回收 Job 与手动触发接口共用本 Service；② 公海认领在
 * {@link CustomerService#claim} 中实现,本类不重复。</p>
 *
 * <p><b>回收规则</b>:{@code isPublic=0} 且 {@code lastFollowTime < now - thresholdSeconds}
 * 的客户,被回收至公海({@code isPublic=1}, {@code ownerUserId=NULL})并写入
 * 一条系统跟进记录。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPublicPoolService {

    /** 阈值秒数下限(1 秒,便于联调) */
    private static final long MIN_THRESHOLD_SECONDS = 1L;
    /** 阈值秒数上限(90 天 = 7,776,000 秒) */
    private static final long MAX_THRESHOLD_SECONDS = 90L * 86400L;
    /** limit 下限 */
    private static final int MIN_LIMIT = 1;
    /** limit 上限(单次最多 1 万条) */
    private static final int MAX_LIMIT = 10_000;
    /** 默认 limit */
    private static final int DEFAULT_LIMIT = 1000;

    private final CrmCustomerMapper customerMapper;
    private final CrmRecordMapper recordMapper;

    /** yml: crm.customer.public-pool-days,默认 15 天 */
    @Value("${crm.customer.public-pool-days:15}")
    private int defaultPublicPoolDays;

    /**
     * 执行一次回收(供 @Scheduled 与手动触发接口共用)
     *
     * @param thresholdSeconds 阈值秒数,null 时取 yml 默认(15 天)
     * @param limit            本次最多回收条数,null 时取 1000
     * @param dryRun           true=只统计不真回收
     * @return 回收结果(含 scanned/recycled/details)
     */
    @Transactional
    public RecycleResultVO recycle(Long thresholdSeconds, Integer limit, Boolean dryRun) {
        long t0 = System.currentTimeMillis();
        long resolvedThreshold = resolveThreshold(thresholdSeconds);
        int resolvedLimit = resolveLimit(limit);
        boolean resolvedDryRun = dryRun != null && dryRun;

        // 1) 扫描候选
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(resolvedThreshold);
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmCustomer::getIsPublic, 0)
               .isNotNull(CrmCustomer::getLastFollowTime)
               .lt(CrmCustomer::getLastFollowTime, cutoff)
               .orderByAsc(CrmCustomer::getLastFollowTime)
               .last("LIMIT " + resolvedLimit);
        List<CrmCustomer> candidates = customerMapper.selectList(wrapper);

        // 2) 准备 details
        List<RecycledCustomerVO> details = new ArrayList<>(candidates.size());
        for (CrmCustomer c : candidates) {
            RecycledCustomerVO d = new RecycledCustomerVO();
            d.setCustomerId(c.getId());
            d.setCustomerName(c.getCustomerName());
            d.setOwnerUserId(c.getOwnerUserId());
            d.setLastFollowTime(c.getLastFollowTime());
            details.add(d);
        }

        int recycled = 0;
        if (!resolvedDryRun) {
            // 3) 逐条回收(用 LambdaUpdateWrapper + .set(field, null) 才能把 owner_user_id 置 NULL)
            //    MyBatis-Plus updateById 默认忽略 null 字段,必须用 update(wrapper) 才能写 NULL
            String operator = UserContext.currentUsername() != null
                    ? UserContext.currentUsername() : "system";
            for (CrmCustomer c : candidates) {
                LambdaUpdateWrapper<CrmCustomer> uw = new LambdaUpdateWrapper<>();
                uw.eq(CrmCustomer::getId, c.getId())
                  .set(CrmCustomer::getOwnerUserId, null)
                  .set(CrmCustomer::getIsPublic, 1)
                  .set(CrmCustomer::getUpdateBy, operator);
                int n = customerMapper.update(null, uw);
                if (n > 0) {
                    recycled++;
                    // 4) 写一条系统跟进记录
                    CrmRecord r = new CrmRecord();
                    r.setRelatedType("customer");
                    r.setRelatedId(c.getId());
                    r.setContent(String.format(
                            "由于超过 %d 天未跟进,系统自动将该客户回收至公海池。",
                            resolvedThreshold / 86400L));
                    r.setFollowType("系统");
                    r.setCreateBy("system");
                    r.setCreateTime(LocalDateTime.now());
                    recordMapper.insert(r);
                }
            }
        }

        RecycleResultVO vo = new RecycleResultVO();
        vo.setThresholdSeconds(resolvedThreshold);
        vo.setLimit(resolvedLimit);
        vo.setDryRun(resolvedDryRun);
        vo.setScanned(candidates.size());
        vo.setRecycled(resolvedDryRun ? 0 : recycled);
        vo.setDurationMs(System.currentTimeMillis() - t0);
        vo.setDetails(details);

        log.info("[公海回收] threshold={}s limit={} dryRun={} scanned={} recycled={} duration={}ms",
                resolvedThreshold, resolvedLimit, resolvedDryRun,
                candidates.size(), recycled, vo.getDurationMs());
        return vo;
    }

    private long resolveThreshold(Long thresholdSeconds) {
        if (thresholdSeconds == null) {
            return (long) defaultPublicPoolDays * 86400L;
        }
        if (thresholdSeconds < MIN_THRESHOLD_SECONDS || thresholdSeconds > MAX_THRESHOLD_SECONDS) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "thresholdSeconds 必须在 " + MIN_THRESHOLD_SECONDS + "~" + MAX_THRESHOLD_SECONDS + " 之间");
        }
        return thresholdSeconds;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "limit 必须在 " + MIN_LIMIT + "~" + MAX_LIMIT + " 之间");
        }
        return limit;
    }

    /**
     * 校验当前用户是 admin 或 sales_director
     * <p>用于手动回收接口的二次校验,即使权限码被绑错角色也兜底。</p>
     */
    public void requireRecyclePermission() {
        List<String> roleKeys = UserContext.currentRoleKeys();
        boolean ok = roleKeys != null && (roleKeys.contains("admin") || roleKeys.contains("sales_director"));
        if (!ok) {
            throw new BusinessException(ResultCode.FORBIDDEN, "手动回收仅限 admin / sales_director 角色");
        }
    }

    /**
     * 提供给外部复用的"当前阈值秒数"读取
     */
    public long getDefaultThresholdSeconds() {
        return (long) defaultPublicPoolDays * 86400L;
    }

    /**
     * 占位:用于暴露配置读取的辅助方法(若有日志需要)
     */
    public String describeConfig() {
        return "public-pool-days=" + defaultPublicPoolDays
                + " (" + StringUtils.hasText(String.valueOf(defaultPublicPoolDays)) + "d)";
    }
}
