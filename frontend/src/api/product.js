import request from '@/utils/request'

/**
 * 产品接口（阶段三；阶段六 commit 2 扩展 productLine / billingCycle + categoryName）
 *
 * 路径前缀：/crm/product
 * 权限：crm:product:list / crm:product:edit
 *
 * 产品为公共资源，无数据权限拦截。
 */

/**
 * 产品分页（按 关键字/分类ID/套餐线/计费周期/状态 过滤）
 * 阶段六 commit 2 新增 productLine / billingCycle / categoryId 三种过滤;
 * 列表 VO 含 categoryName（批量填充）。
 */
export const pageProduct = (params) => request.get('/crm/product/page', { params })

/** 产品详情 */
export const getProduct = (id) => request.get(`/crm/product/${id}`)

/** 新增产品（支持 productLine / billingCycle / categoryId） */
export const addProduct = (data) => request.post('/crm/product', data)

/** 修改产品（支持 productLine / billingCycle / categoryId） */
export const updateProduct = (data) => request.put('/crm/product', data)

/** 删除产品（逻辑删除） */
export const deleteProduct = (id) => request.delete(`/crm/product/${id}`)
