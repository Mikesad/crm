import request from '@/utils/request'

/**
 * 商机接口（阶段二）
 * 路径前缀：/crm/business
 */

/** 商机分页（keyword / customerId / stage / pageNum / pageSize） */
export const pageBusiness = (params) => request.get('/crm/business/page', { params })

/** 商机详情 */
export const getBusiness = (id) => request.get(`/crm/business/${id}`)

/** 新增商机（默认 stage=需求分析） */
export const addBusiness = (data) => request.post('/crm/business', data)

/** 更新商机（stage 字段不在此处修改） */
export const updateBusiness = (data) => request.put('/crm/business', data)

/** 删除商机（逻辑删除） */
export const deleteBusiness = (id) => request.delete(`/crm/business/${id}`)

/**
 * 商机阶段变更（核心业务：严格单向校验）
 * body: { stage: '方案报价', followContent: '...' }
 * 响应失败：3001（跳级/回退/终态再变更）
 */
export const updateBusinessStage = (id, data) =>
  request.put(`/crm/business/${id}/stage`, data)
