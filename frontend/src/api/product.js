import request from '@/utils/request'

/**
 * 产品接口（阶段三）
 *
 * 路径前缀：/crm/product
 * 权限：crm:product:list / crm:product:edit
 *
 * 产品为公共资源，无数据权限拦截。
 */

/** 产品分页（按关键字/分类/状态过滤） */
export const pageProduct = (params) => request.get('/crm/product/page', { params })

/** 产品详情 */
export const getProduct = (id) => request.get(`/crm/product/${id}`)

/** 新增产品 */
export const addProduct = (data) => request.post('/crm/product', data)

/** 修改产品 */
export const updateProduct = (data) => request.put('/crm/product', data)

/** 删除产品（逻辑删除） */
export const deleteProduct = (id) => request.delete(`/crm/product/${id}`)
