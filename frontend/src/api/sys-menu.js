import request from '@/utils/request'

/**
 * 系统菜单接口（阶段六 commit 1）
 *
 * 路径前缀：/sys/menu
 * 权限：sys:menu:list / sys:menu:edit
 * 角色：admin + sales_director
 *
 * 关键能力菜单(perms LIKE 'sys:system:view' 等)不可删。
 */

/** 全量菜单平铺(前端组装树) */
export const listAllMenus = () => request.get('/sys/menu/all')

/** 菜单树(后端组装好) */
export const getMenuTree = () => request.get('/sys/menu/tree')

/** 菜单详情 */
export const getMenu = (id) => request.get(`/sys/menu/${id}`)

/** 新建菜单(M/C/F 类型严格校验) */
export const addMenu = (data) => request.post('/sys/menu', data)

/** 更新菜单 */
export const updateMenu = (data) => request.put('/sys/menu', data)

/** 删除菜单(关键能力菜单不可删;存在子菜单/角色绑定时拒绝) */
export const deleteMenu = (id) => request.delete(`/sys/menu/${id}`)
