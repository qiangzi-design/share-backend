import apiClient from './client'

// 收藏接口：收藏开关、状态与计数。
export const toggleContentCollection = (contentId) =>
  apiClient.post('/collection/toggle', null, { params: { contentId } })

export const getContentCollectionStatus = (contentId) =>
  apiClient.get('/collection/status', { params: { contentId } })

export const getContentCollectionCount = (contentId) =>
  apiClient.get('/collection/count', { params: { contentId } })
