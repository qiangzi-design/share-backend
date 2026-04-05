import apiClient from './client'

// 认证接口：注册、登录、登出统一由此模块调用。
export const register = (payload) => apiClient.post('/auth/register', payload)

export const login = (payload) => apiClient.post('/auth/login', payload)

export const logout = () => apiClient.post('/auth/logout')
