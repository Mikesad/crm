import request from '@/utils/request'

/** 登录 */
export const login = (data) => request.post('/auth/login', data)

/** 登出 */
export const logout = () => request.post('/auth/logout')

/** 获取当前登录用户信息（包含权限标识） */
export const getUserInfo = () => request.get('/auth/info')

/** 获取动态菜单 / 路由 */
export const getRouters = () => request.get('/auth/routers')
