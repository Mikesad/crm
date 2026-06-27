import request from '@/utils/request'

/**
 * 合同接口（阶段三）
 *
 * 路径前缀：/crm/contract
 * 权限：crm:contract:list / crm:contract:edit
 *
 * 核心业务：create() 后端按明细实时核算金额；最低折扣 < 8.5 折时 contract.status=0 + 写 crm_approval 自动进入审批。
 */

/** 合同分页 */
export const pageContract = (params) => request.get('/crm/contract/page', { params })

/** 合同详情（含明细 items） */
export const getContract = (id) => request.get(`/crm/contract/${id}`)

/** 新建合同（金额重算 + 折扣审批触发） */
export const createContract = (data) => request.post('/crm/contract', data)

/** 更新合同（仅合同名称/起止日期） */
export const updateContract = (data) => request.put('/crm/contract', data)

/** 删除合同（逻辑删除） */
export const deleteContract = (id) => request.delete(`/crm/contract/${id}`)
