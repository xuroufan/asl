import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '/api/v1/admin',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 注入 JWT Token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = 'Bearer ' + userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 — 统一错误处理 + Token 刷新
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新 Token，将请求加入队列等待
        return new Promise((resolve) => {
          pendingRequests.push((token: string) => {
            originalRequest.headers.Authorization = 'Bearer ' + token
            resolve(request(originalRequest))
          })
        })
      }
      originalRequest._retry = true
      isRefreshing = true
      const userStore = useUserStore()
      try {
        const res = await axios.post('/api/v1/admin/auth/refresh', null, {
          params: { refreshToken: userStore.refreshToken },
        })
        const data = res.data.data
        if (data && data.accessToken) {
          userStore.setToken(data.accessToken)
          userStore.setRefreshToken(data.refreshToken)
          userStore.setUserInfo(data.user)
          // 重放等待中的请求
          pendingRequests.forEach(cb => cb(data.accessToken))
          pendingRequests = []
          originalRequest.headers.Authorization = 'Bearer ' + data.accessToken
          return request(originalRequest)
        }
      } catch {
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } finally {
        isRefreshing = false
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request