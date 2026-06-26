import request from '@/utils/request'

/** 客户分页 */
export const pageCustomer = (params) => request.get('/customer/page', { params })

/** 客户详情 */
export const getCustomer = (id) => request.get(`/customer/${id}`)

/** 新增客户 */
export const addCustomer = (data) => request.post('/customer', data)

/** 修改客户 */
export const updateCustomer = (data) => request.put('/customer', data)

/** 删除客户（逻辑删除） */
export const deleteCustomer = (id) => request.delete(`/customer/${id}`)

/** 公海池列表 */
export const pagePublicPool = (params) => request.get('/customer/public/page', { params })

/** 领取公海客户 */
export const claimCustomer = (id) => request.post(`/customer/claim/${id}`)

/** 共享客户 */
export const shareCustomer = (data) => request.post('/customer/share', data)
