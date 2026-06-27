import request from '@/utils/request'

/**
 * 客户接口（阶段二 + 阶段四）
 *
 * 路径前缀：/crm/customer 与 /customer/public-pool 与 /customer/share
 * 实际请求：/api/crm/customer/... 与 /api/customer/public-pool/... 与 /api/customer/share/...
 *
 * 阶段二实现：分页 / 详情 / 创建 / 更新 / 删除 / 公海池（用 isPublic=1 复用 page 接口）
 * 阶段四补：claimCustomer / recyclePublicPool / shareCustomer / revokeShare / listShares
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

// ---------- 阶段四:公海池 ----------

/**
 * 公海认领:把公海客户捞到自己名下
 * @param {number|string} id 客户 ID
 */
export const claimCustomer = (id) => request.post(`/crm/customer/public-pool/claim/${id}`)

/**
 * 手动触发公海回收
 * @param {object|null} body { thresholdSeconds?, limit?, dryRun? }
 * @returns {Promise<{thresholdSeconds,limit,dryRun,scanned,recycled,durationMs,details:Array}>}
 */
export const recyclePublicPool = (body) => request.post('/customer/public-pool/recycle', body || {})

// ---------- 阶段四:客户共享 ----------

/**
 * 发起共享(已存在则覆盖 authType)
 * @param {{customerId:number, userId:number, authType:1|2}} data
 * @returns {Promise<{id:number}>}
 */
export const shareCustomer = (data) => request.post('/customer/share', data)

/**
 * 撤销共享(仅 owner 可撤销)
 * @param {number|string} id crm_customer_share 主键
 */
export const revokeShare = (id) => request.delete(`/customer/share/${id}`)

/**
 * 查看某客户的共享名单(仅 owner)
 * @param {number|string} customerId
 * @returns {Promise<Array<{id,customerId,userId,userNickname,userDeptName,authType,authTypeText,createBy,createTime}>>}
 */
export const listShares = (customerId) => request.get('/customer/share/list', { params: { customerId } })
