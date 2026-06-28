import { defineStore } from 'pinia'
import { todoCount } from '@/api/record'

/**
 * 跟进待办 store（阶段五新增）
 *
 * <p>被 NotificationBell（顶部铃铛）+ record/center.vue（跟进中心 KPI）共享。</p>
 *
 * <p>持久化策略：仅持久化 counts 数字（用户刷新页面后铃铛数字不丢）；lastFetched 时间戳不持久化,
 * 每次刷新页面会重新拉一次（避免 stale 数据）。</p>
 */
export const useTodoStore = defineStore('todo', {
  state: () => ({
    counts: { today: 0, week: 0, overdue: 0, total: 0 },
    lastFetched: 0,
  }),
  getters: {
    hasTodo: (s) => (s.counts?.total || 0) > 0,
    badgeNumber: (s) => Math.min(s.counts?.overdue || 0, 99),
  },
  persist: {
    key: 'crm-todo',
    pick: ['counts'],
  },
  actions: {
    /**
     * 拉取待办计数。60 秒内不重复请求（避免轮询风暴）。
     */
    async fetchCount(force = false) {
      const now = Date.now()
      if (!force && now - this.lastFetched < 60_000) return
      try {
        const { data } = await todoCount()
        this.counts = data || { today: 0, week: 0, overdue: 0, total: 0 }
        this.lastFetched = now
      } catch (e) {
        // 静默失败:不阻塞 UI;5 分钟后再试
        console.warn('[todoStore] fetchCount failed:', e?.message)
      }
    },
    clearCache() {
      this.lastFetched = 0
    },
  },
})