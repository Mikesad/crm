import request from '@/utils/request'

/**
 * 跟进记录接口（阶段二 + 阶段五）
 * 路径前缀：/crm/record
 *
 * 跟进记录是 append-only，无 update / delete 接口
 */

/** 拉取时间轴（relatedType: lead/customer/business/contract, relatedId） */
export const getTimeline = (params) => request.get('/crm/record/timeline', { params })

/** 新增跟进（手动追加；业务变更时由后端自动埋点） */
export const addRecord = (data) => request.post('/crm/record', data)

// ---------- 阶段五:跟进中心 ----------

/**
 * 待办数量统计(顶部铃铛用)
 * @returns {Promise<{today, week, overdue, total}>}
 */
export const todoCount = () => request.get('/crm/record/todo/count')

/**
 * 待办列表(跟进中心今日/本周 Tab)
 * @param {{range: 'today'|'week', pageNum: number, pageSize: number}} params
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export const todoList = (params) => request.get('/crm/record/todo/list', { params })

/**
 * 我的历史(跟进中心"我的历史" Tab)
 * @param {{pageNum: number, pageSize: number, keyword?: string}} params
 *   keyword:可选,匹配跟进内容(content)或跟进方式(follow_type)
 */
export const myRecords = (params) => request.get('/crm/record/mine', { params })

/**
 * 近 7 日跟进频次(跟进中心 sparkline)
 * @returns {Promise<{date: string, weekday: string, count: number}[]>} 长度固定 7,从 6 天前到今天
 */
export const last7Days = () => request.get('/crm/record/stats/last7days')
