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

/** 线索转客户（核心业务：@Transactional 双写 customer + contact + record + 物理迁移跟进） */
export const convertLead = (id, data) => request.post(`/crm/lead/${id}/convert`, data)

// ---------- 阶段五:标记死线索 ----------
/**
 * 标记线索为死线索（仅 owner 可调;死因可选）
 * @param {number} id 线索 ID
 * @param {{deadReason?: string}} data
 */
export const markDead = (id, data) => request.post(`/crm/lead/${id}/markDead`, data || {})

// ---------- 阶段四:EasyExcel 导入导出 ----------

/**
 * 下载线索 Excel（返回 blob 供 a[download] 用）
 */
export const exportLeadExcel = () => request.get('/crm/lead/export', { responseType: 'blob' })

/**
 * 上传线索 Excel
 * @param {File} file
 * @returns {Promise<{totalRows, successRows, failRows, errors}>}
 */
export const importLeadExcel = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/crm/lead/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
