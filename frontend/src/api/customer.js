import request from '@/utils/request'

/**
 * 客户接口（阶段二）
 *
 * 路径前缀：/crm/customer（application.yml context-path=/api）
 * 实际请求：/api/crm/customer/...
 *
 * 阶段二实现：分页 / 详情 / 创建 / 更新 / 删除 / 公海池（用 isPublic=1 复用 page 接口）
 * 阶段四补：claimCustomer / shareCustomer（依赖 crm_customer_share 表）
 */

/** 客户分页（isPublic=1 时返回公海池，否则受 dataScope 拦截） */
export const pageCustomer = (params) => request.get('/crm/customer/page', { params })

/** 客户详情 */
export const getCustomer = (id) => request.get(`/crm/customer/${id}`)

/** 新增客户 */
export const addCustomer = (data) => request.post('/crm/customer', data)

/** 修改客户 */
export const updateCustomer = (data) => request.put('/crm/customer', data)

/** 删除客户（逻辑删除） */
export const deleteCustomer = (id) => request.delete(`/crm/customer/${id}`)

// 阶段四预留（依赖 crm_customer_share）
// export const claimCustomer = (id) => request.post(`/crm/customer/claim/${id}`)
// export const shareCustomer = (data) => request.post('/crm/customer/share', data)
