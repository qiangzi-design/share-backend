import axios from 'axios'
import router from '../router'

// 全局 HTTP 客户端：统一 baseURL、JWT 注入、401/封禁状态处理。
const apiClient = axios.create({
  baseURL: '/api'
})

apiClient.interceptors.request.use(
  (config) => {
    // 所有请求自动携带会话 token，避免页面层重复拼接 header。
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一清理登录态，避免残留缓存导致 UI 状态错误。
    const clearAuth = () => {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('userId')
      sessionStorage.removeItem('admin_me_cache')
      sessionStorage.removeItem('admin_permissions')
      window.dispatchEvent(new Event('auth-state-changed'))
      window.dispatchEvent(new Event('admin-state-changed'))
    }

    const response = error.response
    const status = response?.status
    const message = response?.data?.message
    const accountStatus = response?.headers?.['x-account-status']
    const encodedBlockReason = response?.headers?.['x-block-reason']

    const blockedByHeader = accountStatus === 'blocked'
    const blockedByMessage = typeof message === 'string' && message.startsWith('ACCOUNT_BLOCKED')

    // 封禁场景优先处理：立即清理会话并跳转封禁提示页。
    if (blockedByHeader || blockedByMessage) {
      let reason = ''
      if (encodedBlockReason) {
        try {
          reason = decodeURIComponent(encodedBlockReason)
        } catch (_) {
          reason = encodedBlockReason
        }
      } else if (blockedByMessage) {
        const parts = String(message).split('|')
        reason = parts.length > 1 ? parts.slice(1).join('|') : ''
      }
      if (reason) {
        sessionStorage.setItem('blocked_reason', reason)
      } else {
        sessionStorage.setItem('blocked_reason', '你的账号已被封禁，请联系管理员')
      }
      clearAuth()
      if (router.currentRoute.value.path !== '/account-blocked') {
        router.push('/account-blocked')
      }
      return Promise.reject(error)
    }

    // 401 统一回收登录态，并根据路由域跳到普通登录或管理登录。
    if (status === 401) {
      clearAuth()
      const currentPath = router.currentRoute.value.path || '/'
      const loginPath = currentPath.startsWith('/admin') ? '/admin/login' : '/login'
      if (currentPath !== loginPath) {
        router.push(loginPath)
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
