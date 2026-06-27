import request from '@/utils/request'

/**
 * 回款管理接口（阶段三）
 *
 * 路径前缀：/crm/receivable
 * 权限：crm:receivable:list（查询）/ crm:receivable:edit（录入）
 *
 * 核心：财务录入 → applicationEventPublisher.publishEvent(ReceivableRecordedEvent) →
 * @TransactionalEventListener(AFTER_COMMIT) 监听器更新 plan.status / contract.status。
 *
 * 计划外回款：planId 传 null，不影响 plan/contract 状态。
 */

/** 回款分页（按 contractId/planId/paymentMethod/returnDate 过滤） */
export const pageReceivable = (params) => request.get('/crm/receivable/page', { params })

/** 回款详情 */
export const getReceivable = (id) => request.get(`/crm/receivable/${id}`)

/** 录入回款（planId 可空 = 计划外回款） */
export const createReceivable = (data) => request.post('/crm/receivable', data)
