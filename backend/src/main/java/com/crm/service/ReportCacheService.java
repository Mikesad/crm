package com.crm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 报表中心 5 分钟内存缓存（阶段五 commit 2）
 *
 * <p>key = {@code reportKey + ":" + filterHash},value = (data, expireAt)。
 * 命中:expireAt &gt; now,直接返回 data;未命中或已过期:跑 supplier,塞回缓存。</p>
 *
 * <p>设计取舍:</p>
 * <ul>
 *   <li>用 {@link ConcurrentHashMap} 而非 Caffeine/Spring Cache — 报表模块独立,
 *       不依赖额外组件,代码量少 30 行,够用</li>
 *   <li>无淘汰策略 — 5 分钟过期是软删除(Lazy),缓存条目数随 filter 组合增长
 *       但每个用户会话最多 14 Tab × 6 range × 2 dept × N user = 数百条,可控</li>
 *   <li>无 LRU 兜底 — 阶段六若报表维度爆炸再升级 Caffeine</li>
 * </ul>
 *
 * <p>命中日志:debug 级,生产默认不刷屏;未命中日志:info 级,便于观察
 * 实际请求模式与缓存效果。</p>
 */
@Slf4j
@Service
public class ReportCacheService {

    /** 5 分钟 TTL */
    private static final Duration TTL = Duration.ofMinutes(5);

    /** key → (data, expireAtEpochMs) */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 按 key 取数据,未命中或过期则调用 loader 加载并塞回缓存。
     *
     * @param key    缓存键,建议 {@code reportKey + ":" + filterHash}
     * @param loader 数据加载函数(可能为 null,返回 null 时不缓存)
     * @param <T>    数据类型
     * @return 缓存或刚加载的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expireAt > now) {
            log.debug("[ReportCache] HIT key={}", key);
            return (T) entry.data;
        }
        T fresh = loader.get();
        if (fresh != null) {
            cache.put(key, new CacheEntry(fresh, now + TTL.toMillis()));
            log.info("[ReportCache] MISS key={}, loaded size={}", key, approxSize(fresh));
        } else {
            log.info("[ReportCache] MISS key={}, loader returned null (skip cache)", key);
        }
        return fresh;
    }

    /** 清空全部缓存(管理员手动刷新用) */
    public void clearAll() {
        int size = cache.size();
        cache.clear();
        log.info("[ReportCache] CLEARED, dropped {} entries", size);
    }

    /** 当前缓存条数(监控用) */
    public int size() {
        return cache.size();
    }

    private static int approxSize(Object data) {
        if (data == null) return 0;
        if (data instanceof java.util.Collection<?> c) return c.size();
        return 1;
    }

    /** 缓存条目,不可变 */
    private record CacheEntry(Object data, long expireAt) {}
}
