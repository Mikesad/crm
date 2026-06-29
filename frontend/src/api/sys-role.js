import request from '@/utils/request'

/**
 * 系统角色接口（阶段六 commit 1）
 *
 * 路径前缀：/sys/role
 * 权限：sys:role:list / sys:role:edit / sys:role:assign_menu
 * 角色：admin + sales_director
 *
 * 内置 5 角色(roleKey: admin/sales_director/sales_lead/sales/finance)不可删。
 */

/** 角色分页 */
export const pageRole = (params) => request.get('/sys/role/page', { params })

/** 全部启用角色下拉(供用户编辑/角色分配) */
export const listAllRoles = () => request.get('/sys/role/all')

/** 角色详情(含 menuIds + userCount) */
export const getRole = (id) => request.get(`/sys/role/${id}`)

/** 新建角色 */
export const addRole = (data) => request.post('/sys/role', data)

/** 更新角色(roleKey 不可改;传 menuIds 时全量重绑 + 踢所有用户下线) */
export const updateRole = (data) => request.put('/sys/role', data)

/** 删除角色(内置 5 角色不可删) */
export const deleteRole = (id) => request.delete(`/sys/role/${id}`)

/** 分配菜单(全量重绑 sys_role_menu + 踢所有用户下线) */
export const assignMenus = (id, menuIds) =>
  request.put(`/sys/role/${id}/menus`, menuIds)

// ========== 阶段六 commit 1 收尾:成员管理 3 接口 ==========

/**
 * 成员列表(分页,后端走 sys_user_role → sys_user JOIN)
 * @param {number} roleId
 * @param {{pageNum?:number, pageSize?:number}} params
 */
export const listMembers = (roleId, params) =>
  request.get(`/sys/role/${roleId}/members`, { params })

/**
 * 添加成员(批量 userIds → 已绑定走 INSERT IGNORE 跳过;踢受影响用户下线)
 * @param {number} roleId
 * @param {number[]} userIds
 */
export const addMembers = (roleId, userIds) =>
  request.post(`/sys/role/${roleId}/members`, userIds)

/**
 * 移除成员(admin 至少 1 人兜底;踢该用户下线)
 * @param {number} roleId
 * @param {number} userId
 */
export const removeMember = (roleId, userId) =>
  request.delete(`/sys/role/${roleId}/members/${userId}`)
