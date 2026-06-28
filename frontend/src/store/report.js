import { defineStore } from 'pinia'
import {
  getFunnel,
  getCustomer,
  getConversion,
  getFinance,
  clearCache as apiClearCache
} from '@/api/report'

/**
 * 报表中心 Pinia Store（阶段五 commit 2）
 *
 * 5 分钟内存缓存(对齐后端 ReportCacheService):
 * - key = `${tabKey}:${cacheKeyFromFilters(filters)}`
 * - value = { data, expireAt }
 * - 命中:expireAt > Date.now(),直接返回 data
 * - 未命中/过期:调对应 API,塞回缓存
 *
 * Pinia 持久化默认不开启(本 store 是纯运行时缓存,跨会话无意义)。
 */

/** 5 分钟 TTL(ms) */
const TTL_MS = 5 * 60 * 1000

/**
 * 简化版 filter hash:把对象按键排序后 JSON.stringify
 * <p>实际项目若 filter 字段膨胀可换 murmurhash/sha1,本阶段 5-7 字段够用</p>
 */
function hashFilters(filters) {
  if (!filters || Object.keys(filters).length === 0) return 'default'
  const sorted = Object.keys(filters).sort().reduce((acc, k) => {
    const v = filters[k]
    if (v !== null && v !== undefined && v !== '') acc[k] = v
    return acc
  }, {})
  return JSON.stringify(sorted)
}

export const useReportStore = defineStore('report', {
  state: () => ({
    /** key → { data, expireAt } */
    cache: new Map(),
    /** 当前激活的 Tab key */
    activeTab: 'funnel',
    /** 全局筛选条件(4 Tab 共享) */
    filters: {
      range: 'month',
      deptId: null,
      userId: null
    },
    /** 最后更新时间戳 */
    lastUpdatedAt: 0,
    /** 当前正在加载的 key(用于 loading 状态) */
    loadingKey: ''
  }),

  actions: {
    /**
     * 按 tab + filters 拉取报表数据,5 分钟内复用缓存。
     *
     * @param {string} tabKey funnel / customer / conversion / finance
     * @param {object} extraFilters  临时筛选覆盖(本接口特有,如 dim / topN)
     * @returns {Promise<object>}
     */
    async fetchReport(tabKey, extraFilters = {}) {
      const allFilters = { ...this.filters, ...extraFilters }
      const key = `${tabKey}:${hashFilters(allFilters)}`
      const now = Date.now()
      const cached = this.cache.get(key)
      if (cached && cached.expireAt > now) {
        return cached.data
      }
      this.loadingKey = key
      try {
        const { data } = await this._callApi(tabKey, allFilters)
        this.cache.set(key, { data, expireAt: now + TTL_MS })
        this.lastUpdatedAt = now
        return data
      } finally {
        this.loadingKey = ''
      }
    },

    /**
     * 按 tab 路由到对应 API 函数。
     * <p>把 filters 拆给对应 query 参数,保持和后端 13 个接口对齐。</p>
     */
    async _callApi(tabKey, filters) {
      const base = {
        range: filters.range,
        startDate: filters.startDate,
        endDate: filters.endDate,
        deptId: filters.deptId,
        userId: filters.userId,
        topN: filters.topN
      }
      switch (tabKey) {
        case 'funnel':     return getFunnel(base)
        case 'customer':   return getCustomer({ ...base, dim: filters.dim })
        case 'conversion': return getConversion(base)
        case 'finance':    return getFinance(base)
        default:
          throw new Error(`未知 tabKey: ${tabKey}`)
      }
    },

    /** 设置全局筛选 + 自动清缓存(让 4 Tab 重新拉) */
    setFilters(patch) {
      this.filters = { ...this.filters, ...patch }
      this.cache.clear()
    },

    /** 切换 Tab,filter 不变 */
    setActiveTab(tabKey) {
      this.activeTab = tabKey
    },

    /** 强制重算当前 Tab(清掉对应 key) */
    invalidateTab(tabKey) {
      for (const k of this.cache.keys()) {
        if (k.startsWith(`${tabKey}:`)) this.cache.delete(k)
      }
    },

    /** 调后端 /cache/clear,清空所有报表缓存 */
    async clearAll() {
      this.cache.clear()
      try {
        await apiClearCache()
      } catch (e) {
        // 即使后端清失败,前端也清了,不影响用户
        console.warn('[useReportStore] clearCache API 调用失败,前端缓存已清', e)
      }
    }
  }
})
