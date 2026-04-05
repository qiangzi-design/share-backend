import apiClient from './client'

// 公告接口：用户端公告拉取、未读统计与已读回执。
export const getActiveAnnouncements = () => apiClient.get('/announcements/active')

export const getAnnouncementList = (params) => apiClient.get('/announcements/list', { params })

export const getAnnouncementUnreadCount = () => apiClient.get('/announcements/unread-count')

export const markAnnouncementRead = (id) => apiClient.post('/announcements/read', null, { params: { id } })

export const markAllAnnouncementsRead = () => apiClient.post('/announcements/read-all')
