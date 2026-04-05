import apiClient from './client'

// 用户分析接口：仅查询当前登录用户本人数据，不支持跨用户查看。
export const getMyInsightsOverview = (params) => apiClient.get('/analytics/me/overview', { params })

export const getMyInsightsTopContents = (params) => apiClient.get('/analytics/me/top-contents', { params })

export const getMyInsightsTrend = (params) => apiClient.get('/analytics/me/trend', { params })

export const getMyInsightsTaxonomy = (params) => apiClient.get('/analytics/me/taxonomy', { params })

export const getMyInsightsPublishTime = (params) => apiClient.get('/analytics/me/publish-time', { params })

// 治理提醒口径与筛选条件无关，直接返回当前账号治理状态快照。
export const getMyInsightsGovernance = () => apiClient.get('/analytics/me/governance')
