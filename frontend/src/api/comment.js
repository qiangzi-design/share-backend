import apiClient from './client'

// 评论接口：评论增删、回复读取与评论点赞能力。
export const createComment = (payload) => apiClient.post('/comment/create', payload)

export const getCommentList = (contentId, page = 1, pageSize = 10) =>
  apiClient.get('/comment/list', { params: { contentId, page, pageSize } })

export const getCommentReplies = (commentId) =>
  apiClient.get('/comment/replies', { params: { commentId } })

export const toggleCommentLike = (commentId) =>
  apiClient.post('/comment/like/toggle', null, { params: { commentId } })

export const getCommentLikeStatus = (commentId) =>
  apiClient.get('/comment/like/status', { params: { commentId } })

export const deleteComment = (id) => apiClient.delete(`/comment/delete/${id}`)
