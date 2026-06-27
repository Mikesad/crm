package com.crm.job;

import com.crm.service.CustomerPublicPoolService;
import com.crm.vo.RecycleResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 公海池自动回收 Job
 *
 * <p>每天凌晨 2 点整触发,扫描"超过阈值未跟进"的客户,自动回收至公海。
 * 共用 {@link CustomerPublicPoolService#recycle} 方法,与手动触发接口规则一致。</p>
 *
 * <p><b>调度框架</b>:Spring {@code @Scheduled} + 启动类
 * {@code @EnableScheduling} 已就位,无第三方依赖。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerPublicPoolJob {

    private final CustomerPublicPoolService poolService;

    /**
     * 每天凌晨 2:00 触发
     * <p>Cron:秒 分 时 日 月 周 → {@code 0 0 2 * * ?}</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledRecycle() {
        long t0 = System.currentTimeMillis();
        log.info("[公海回收 Job] 开始,默认阈值={}s", poolService.getDefaultThresholdSeconds());
        try {
            RecycleResultVO r = poolService.recycle(null, null, false);
            log.info("[公海回收 Job] 完成 scanned={} recycled={} duration={}ms",
                    r.getScanned(), r.getRecycled(), r.getDurationMs());
        } catch (Exception e) {
            // Job 失败不影响下次调度,只记日志
            log.error("[公海回收 Job] 执行异常,耗时={}ms", System.currentTimeMillis() - t0, e);
        }
    }
}
