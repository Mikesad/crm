import request from '@/utils/request'

/**
 * 产品分类接口（阶段六 commit 2 新增）
 *
 * 路径前缀：/crm/product/category
 * 权限：crm:product:category:list / crm:product:category:edit
 *
 * 分类为公共资源，无数据权限拦截;D7 v0.4 5 角色全员可见。
 */

/** 分页查询产品分类（VO 含 productCount 关联产品数） */
export const pageCategory = (params) => request.get('/crm/product/category/page', { params })

/** 全量查询产品分类（产品表单下拉用） */
export const listCategory = () => request.get('/crm/product/category/all')

/** 创建产品分类 */
export const addCategory = (data) => request.post('/crm/product/category', data)

/** 修改产品分类 */
export const updateCategory = (data) => request.put('/crm/product/category', data)

/** 删除产品分类（被产品引用时返回 DATA_EXISTS） */
export const deleteCategory = (id) => request.delete(`/crm/product/category/${id}`)
