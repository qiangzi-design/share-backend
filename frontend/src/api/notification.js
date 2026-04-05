import apiClient from './client'

// 互动通知分页列表（点赞/评论/收藏等）。
export const getNotificationList = (params) =>
  apiClient.get('/notifications/list', { params })

// 互动未读总数：用于顶部“消息”角标。
export const getNotificationUnreadCount = () =>
  apiClient.get('/notifications/unread-count')

// 未读汇总：一次返回互动/私聊/公告三类未读，减少前端多接口并发轮询。
export const getUnreadSummary = () =>
  apiClient.get('/notifications/unread-summary')

// 一键已读互动通知。
export const markAllNotificationsRead = () =>
  apiClient.post('/notifications/read-all')

// 单条互动通知已读。
export const markNotificationRead = (id) =>
  apiClient.post('/notifications/read', null, { params: { id } })
