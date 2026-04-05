import apiClient from './client'

// 举报创建：后端会执行频控、重复举报校验与目标快照记录。
export const createReport = (payload) => apiClient.post('/reports', payload)
// 举报模板：供前端下拉展示，不在前端写死文案。
export const getReportTemplates = () => apiClient.get('/reports/templates')

// 我的举报记录：用于用户端闭环追踪处理状态。
export const getMyReports = (params) => apiClient.get('/reports/my', { params })
