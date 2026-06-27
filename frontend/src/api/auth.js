import request from '@/utils/request'

/**
 * 登录
 * @param {{username:string, password:string, remember?:boolean}} data
 * @returns {Promise<{token:string, userId:number, username:string, nickname:string, deptId:number, roleKeys:string[], dataScope:number, permissions:string[]}>}
 */
export const login = (data) => request.post('/auth/login', data)

/** 登出（幂等） */
export const logout = () => request.post('/auth/logout')

/**
 * 当前用户信息（从 tokenSession 读取，0 DB 命中）
 * 用于页面刷新时恢复 store
 */
export const getMe = () => request.get('/auth/me')