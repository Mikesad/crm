import request from '@/utils/request'

/**
 * 线索接口（阶段二）
 * 路径前缀：/crm/lead
 */

/** 线索分页（keyword / status / source / pageNum / pageSize） */
export const pageLead = (params) => request.get('/crm/lead/page', { params })

/** 线索详情 */
export const getLead = (id) => request.get(`/crm/lead/${id}`)

/** 新增线索 */
export const addLead = (data) => request.post('/crm/lead', data)

/** 更新线索 */
export const updateLead = (data) => request.put('/crm/lead', data)

/** 删除线索（逻辑删除） */
export const deleteLead = (id) => request.delete(`/crm/lead/${id}`)

/** 线索转客户（核心业务：@Transactional 双写 customer + contact + record） */
export const convertLead = (id, data) => request.post(`/crm/lead/${id}/convert`, data)
