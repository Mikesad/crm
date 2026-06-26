import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import router from '@/router'
import { useUserStore } from '@/store/user'

nprogress.configure({ showSpinner: false })

const service = axios.create({
  baseURL: import.meta.env.VITE_API_PREFIX || '/api',
  timeout: 30000
})

// 请求拦截器：附加 Sa-Token
service.interceptors.request.use(
  (config) => {
    nprogress.start()
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = userStore.token
    }
    return config
  },
  (error) => {
    nprogress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理业务码
service.interceptors.response.use(
  (response) => {
    nprogress.done()
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 401：未登录
    if (res.code === 401) {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      })
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    nprogress.done()
    console.error('请求错误', error)
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
