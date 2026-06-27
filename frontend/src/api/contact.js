import request from '@/utils/request'

/**
 * 联系人接口（阶段二）
 * 路径前缀：/crm/contact
 *
 * 联系人查询强依赖 customerId，不分页
 */

/** 按客户查询联系人列表（GET /list?customerId=xxx&keyword=yyy） */
export const listContact = (params) => request.get('/crm/contact/list', { params })

/** 新增联系人 */
export const addContact = (data) => request.post('/crm/contact', data)

/** 更新联系人 */
export const updateContact = (data) => request.put('/crm/contact', data)

/** 删除联系人（逻辑删除） */
export const deleteContact = (id) => request.delete(`/crm/contact/${id}`)
