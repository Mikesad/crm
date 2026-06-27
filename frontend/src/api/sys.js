import request from '@/utils/request'

/**
 * 系统接口(阶段四新增:用户下拉)
 *
 * 路径:/sys/user
 * 实际:/api/sys/user/list
 */

/**
 * 查询用户列表(供前端下拉/选择器用,最大 200 条)
 * @param {{keyword?:string, deptId?:number}} params
 * @returns {Promise<Array<{id,username,nickname,deptId}>>}
 */
export const listUsers = (params) => request.get('/sys/user/list', { params })
