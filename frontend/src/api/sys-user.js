import request from '@/utils/request'

/**
 * 系统用户接口（阶段六 commit 1）
 *
 * 路径前缀：/sys/user
 * 权限：sys:user:list / sys:user:edit / sys:user:reset_pwd / sys:user:assign_role
 * 角色：admin + sales_director
 *
 * 阶段六:CRUD + 重置密码 + 分配角色 + 启停用;
 * 既有的轻量 /list(被共享人下拉用)保留。
 */

/** 用户分页 */
export const pageUser = (params) => request.get('/sys/user/page', { params })

/** 用户详情(含 roleIds) */
export const getUser = (id) => request.get(`/sys/user/${id}`)

/** 新建用户(默认密码 123456) */
export const addUser = (data) => request.post('/sys/user', data)

/** 更新用户(username 不可改;传 roleIds 时全量重绑) */
export const updateUser = (data) => request.put('/sys/user', data)

/** 删除用户(逻辑删除 + 踢下线) */
export const deleteUser = (id) => request.delete(`/sys/user/${id}`)

/** 重置密码(默认 123456;重置后踢下线) */
export const resetPassword = (id, newPassword) =>
  request.post(`/sys/user/${id}/resetPassword`, null, { params: { newPassword } })

/** 分配角色(全量重绑 sys_user_role + 踢下线) */
export const assignRoles = (id, roleIds) =>
  request.put(`/sys/user/${id}/roles`, roleIds)

/** 启停用(status 切换 + 停用时踢下线) */
export const toggleStatus = (id, status) =>
  request.put(`/sys/user/${id}/status`, null, { params: { status } })
