import request from '@/utils/request'

/**
 * 系统部门接口（阶段七 commit）
 *
 * 路径前缀：/sys/dept
 * 权限：sys:dept:list / sys:dept:edit
 * 角色：admin + sales_director
 *
 * 3 类删除保护：顶级不可删 / 有子部门拒绝 / 有启用用户拒绝
 * 父变更会触发祖先链重建（事务内刷所有后代 ancestors）
 */

/** 全量部门列表（前端 el-tree 自组织）+ parentName + childCount + userCount */
export const listAllDept = () => request.get('/sys/dept/all')

/** 部门分页（搜索/筛选） */
export const pageDept = (params) => request.get('/sys/dept/page', { params })

/** 部门详情（含 userCount + childCount + parentName） */
export const getDept = (id) => request.get(`/sys/dept/${id}`)

/** 新建部门（V1 暂不允许新建顶级） */
export const addDept = (data) => request.post('/sys/dept', data)

/** 更新部门（父变更会触发祖先链重建） */
export const updateDept = (data) => request.put('/sys/dept', data)

/** 删除部门（顶级 / 有子 / 有用户 三类拒绝） */
export const deleteDept = (id) => request.delete(`/sys/dept/${id}`)

/** 启停用 */
export const toggleDeptStatus = (id, status) =>
  request.put(`/sys/dept/${id}/status`, null, { params: { status } })
