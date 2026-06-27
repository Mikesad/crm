import request from '@/utils/request'

/**
 * 跟进记录接口（阶段二）
 * 路径前缀：/crm/record
 *
 * 跟进记录是 append-only，无 update / delete 接口
 */

/** 拉取时间轴（relatedType: lead/customer/business, relatedId） */
export const getTimeline = (params) => request.get('/crm/record/timeline', { params })

/** 新增跟进（手动追加；业务变更时由后端自动埋点） */
export const addRecord = (data) => request.post('/crm/record', data)
