import apiClient from './client'

// 点赞接口：内容点赞开关与点赞状态查询。
export const toggleContentLike = (contentId) =>
  apiClient.post('/like/toggle', null, { params: { contentId } })

export const getContentLikeStatus = (contentId) =>
  apiClient.get('/like/status', { params: { contentId } })
