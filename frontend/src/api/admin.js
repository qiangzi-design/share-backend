import apiClient from './client'

// 管理端当前登录信息（角色 + 权限集合）。
export const getAdminMe = () => apiClient.get('/admin/me')

// 仪表盘总览与趋势。
export const getAdminDashboardOverview = () => apiClient.get('/admin/dashboard/overview')

export const getAdminDashboardTrends = (params) => apiClient.get('/admin/dashboard/trends', { params })

// 手动刷新每日 AI 快讯，支持可选 date=yyyy-MM-dd 指定补跑日期。
export const refreshAdminAiBrief = (params) => apiClient.post('/admin/ai-brief/refresh', null, { params })

// 用户治理（查询/封禁/禁言/风险标记）。
export const getAdminUsers = (params) => apiClient.get('/admin/users', { params })

export const getAdminUserDetail = (id) => apiClient.get(`/admin/users/${id}/detail`)

export const banAdminUser = (id, payload) => apiClient.post(`/admin/users/${id}/ban`, payload || {})

export const unbanAdminUser = (id) => apiClient.post(`/admin/users/${id}/unban`)

export const muteAdminUser = (id, payload) => apiClient.post(`/admin/users/${id}/mute`, payload || {})

export const unmuteAdminUser = (id) => apiClient.post(`/admin/users/${id}/unmute`)

export const markAdminUserRisk = (id, payload) => apiClient.post(`/admin/users/${id}/risk-mark`, payload || {})

export const unmarkAdminUserRisk = (id) => apiClient.post(`/admin/users/${id}/risk-unmark`)

// 内容与评论审核。
export const getAdminContents = (params) => apiClient.get('/admin/contents', { params })

export const offShelfAdminContent = (id, payload) => apiClient.post(`/admin/contents/${id}/off-shelf`, payload || {})

export const restoreAdminContent = (id, payload) => apiClient.post(`/admin/contents/${id}/restore`, payload || {})

export const getAdminComments = (params) => apiClient.get('/admin/comments', { params })

export const hideAdminComment = (id, payload) => apiClient.post(`/admin/comments/${id}/hide`, payload || {})

export const restoreAdminComment = (id, payload) => apiClient.post(`/admin/comments/${id}/restore`, payload || {})

// 举报中心与模板。
export const getAdminReports = (params) => apiClient.get('/admin/reports', { params })

export const getAdminReportTargetPreview = (id) => apiClient.get(`/admin/reports/${id}/target-preview`)

export const assignAdminReport = (id, payload) => apiClient.post(`/admin/reports/${id}/assign`, payload)
export const handleAdminReport = (id, payload) => apiClient.post(`/admin/reports/${id}/handle`, payload || {})
export const getAdminReportTemplates = (params) => apiClient.get('/admin/report-templates', { params })
export const createAdminReportTemplate = (payload) => apiClient.post('/admin/report-templates', payload || {})
export const updateAdminReportTemplate = (id, payload) =>
  apiClient.put(`/admin/report-templates/${id}`, payload || {})
export const enableAdminReportTemplate = (id) => apiClient.post(`/admin/report-templates/${id}/enable`)
export const disableAdminReportTemplate = (id) => apiClient.post(`/admin/report-templates/${id}/disable`)

export const resolveAdminReport = (id, payload) => apiClient.post(`/admin/reports/${id}/resolve`, payload || {})

export const rejectAdminReport = (id, payload) => apiClient.post(`/admin/reports/${id}/reject`, payload || {})

// 审计日志。
export const getAdminAuditLogs = (params) => apiClient.get('/admin/audit-logs', { params })

// 分类与标签运营。
export const getAdminCategories = (params) => apiClient.get('/admin/categories', { params })

export const createAdminCategory = (payload) => apiClient.post('/admin/categories', payload)

export const updateAdminCategory = (id, payload) => apiClient.put(`/admin/categories/${id}`, payload)

export const enableAdminCategory = (id) => apiClient.post(`/admin/categories/${id}/enable`)

export const disableAdminCategory = (id) => apiClient.post(`/admin/categories/${id}/disable`)

export const getAdminTags = (params) => apiClient.get('/admin/tags', { params })

export const createAdminTag = (payload) => apiClient.post('/admin/tags', payload)

export const updateAdminTag = (id, payload) => apiClient.put(`/admin/tags/${id}`, payload)

export const enableAdminTag = (id) => apiClient.post(`/admin/tags/${id}/enable`)

export const disableAdminTag = (id) => apiClient.post(`/admin/tags/${id}/disable`)

export const getAdminTagContents = (id, params) => apiClient.get(`/admin/tags/${id}/contents`, { params })

// 公告管理与系统消息模板。
export const getAdminAnnouncements = (params) => apiClient.get('/admin/announcements', { params })

export const createAdminAnnouncement = (payload) => apiClient.post('/admin/announcements', payload)

export const updateAdminAnnouncement = (id, payload) => apiClient.put(`/admin/announcements/${id}`, payload)

export const publishAdminAnnouncement = (id) => apiClient.post(`/admin/announcements/${id}/publish`)

export const offlineAdminAnnouncement = (id) => apiClient.post(`/admin/announcements/${id}/offline`)

export const getAdminNotificationTemplates = () => apiClient.get('/admin/notification-templates')

export const updateAdminNotificationTemplate = (code, payload) =>
  apiClient.put(`/admin/notification-templates/${code}`, payload)

// 运营分析三件套。
export const getAdminContentQualityAnalytics = (params) => apiClient.get('/admin/analytics/content-quality', { params })

export const getAdminUserGrowthAnalytics = (params) => apiClient.get('/admin/analytics/user-growth', { params })

export const getAdminModerationEfficiencyAnalytics = (params) =>
  apiClient.get('/admin/analytics/moderation-efficiency', { params })
