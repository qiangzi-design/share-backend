import apiClient from './client'

// AI快讯接口：用于首页/快讯页展示每日热点与历史摘要。
export const getTodayAiBrief = () => apiClient.get('/ai-brief/today')

export const getAiBriefHistory = (params) => apiClient.get('/ai-brief/history', { params })
