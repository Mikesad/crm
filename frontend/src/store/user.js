import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'

/**
 * 用户状态
 *
 * 字段对齐后端 LoginResponse / CurrentUserVO：
 * - token       string  Sa-Token
 * - userId      number
 * - username    string
 * - nickname    string
 * - deptId      number
 * - roleKeys    string[]
 * - dataScope   number  1/3/4/5
 * - permissions string[]
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userId: null,
    username: '',
    nickname: '',
    deptId: null,
    roleKeys: [],
    dataScope: 5,
    permissions: []
  }),
  persist: {
    key: 'crm-user',
    storage: localStorage,
    pick: [
      'token',
      'userId',
      'username',
      'nickname',
      'deptId',
      'roleKeys',
      'dataScope',
      'permissions'
    ]
  },
  actions: {
    /**
     * 登录：后端一次返回 token + 用户信息 + 权限，避免二次 /me 调用
     */
    async login(form) {
      const { data } = await loginApi(form)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.nickname = data.nickname
      this.deptId = data.deptId
      this.roleKeys = data.roleKeys || []
      this.dataScope = data.dataScope ?? 5
      this.permissions = data.permissions || []
      return data
    },
    /**
     * 拉取当前用户（页面刷新后用）
     */
    async fetchMe() {
      const { data } = await getMe()
      this.userId = data.userId
      this.username = data.username
      this.nickname = data.nickname
      this.deptId = data.deptId
      this.roleKeys = data.roleKeys || []
      this.dataScope = data.dataScope ?? 5
      this.permissions = data.permissions || []
      return data
    },
    /**
     * 登出：清本地状态 + 调后端注销 token
     */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略后端错误，本地状态也要清
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.deptId = null
      this.roleKeys = []
      this.dataScope = 5
      this.permissions = []
    },
    /**
     * 权限判断：hasPermission('crm:customer:edit')
     */
    hasPermission(code) {
      return this.permissions.includes(code)
    }
  }
})