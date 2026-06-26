import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    permissions: [],
    roles: []
  }),
  persist: {
    key: 'crm-user',
    storage: localStorage,
    pick: ['token', 'userInfo', 'permissions', 'roles']
  },
  actions: {
    async login(form) {
      const { data } = await loginApi(form)
      this.token = data.token
      this.userInfo = data.user
      return data
    },
    async fetchUserInfo() {
      const { data } = await getUserInfo()
      this.userInfo = data.user
      this.permissions = data.permissions || []
      this.roles = data.roles || []
      return data
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // ignore
      }
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
    }
  }
})
