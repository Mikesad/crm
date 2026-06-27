import request from '@/utils/request'

/**
 * 回款计划接口（阶段三）
 *
 * 路径前缀：/crm/receivable-plan
 * 权限：crm:contract:list（查询）/ crm:receivable_plan:edit（创建/更新/删除）
 *
 * V1 用 list 替代 page（单合同计划数通常 3~10）。
 * 状态 0 未到期 → 2 已回款 由 ReceivableEventListener 监听 ReceivableRecordedEvent 自动联动。
 */

/** 按合同查询回款计划（contractId 必传） */
export const listReceivablePlan = (params) => request.get('/crm/receivable-plan/list', { params })

/** 计划详情 */
export const getReceivablePlan = (id) => request.get(`/crm/receivable-plan/${id}`)

/** 批量创建回款计划（合同 status=1 才能录，期数 period 不能重复） */
export const createReceivablePlanBatch = (data) => request.post('/crm/receivable-plan', data)

/** 更新回款计划（已回款 status=2 不能改） */
export const updateReceivablePlan = (data) => request.put('/crm/receivable-plan', data)

/** 删除回款计划（逻辑删除，已回款不能删） */
export const deleteReceivablePlan = (id) => request.delete(`/crm/receivable-plan/${id}`)
