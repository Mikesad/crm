/**
 * 演示账号（开发环境专用）
 *
 * 仅在 import.meta.env.DEV 时挂载到登录页 quick-fill 区域。
 * 严禁在生产环境引入此文件做真实账号提示。
 *
 * 种子数据来源：crm_full.sql 中的 sys_user 表
 * 密码统一为 123456（BCrypt 加密值已写入种子）
 */

export interface DemoAccount {
  /** 登录账号 */
  username: string
  /** 登录密码（明文仅本地 autofill 用，不参与网络传输） */
  password: string
  /** 角色 key，与后端 sys_role.role_key 对齐 */
  roleKey: 'admin' | 'sales_director' | 'sales_lead' | 'sales' | 'finance'
  /** 前端展示用角色名 */
  roleLabel: string
  /** 显示顺序 */
  order: number
}

export const demoAccounts: DemoAccount[] = [
  { username: 'admin',       password: '123456', roleKey: 'admin',          roleLabel: '管理员', order: 1 },
  { username: 'sales_li',    password: '123456', roleKey: 'sales',          roleLabel: '销售',   order: 2 },
  { username: 'finance',     password: '123456', roleKey: 'finance',        roleLabel: '财务',   order: 3 }
]