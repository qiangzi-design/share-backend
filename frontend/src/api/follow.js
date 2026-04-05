import apiClient from './client'

// 关注接口：关注关系、粉丝/关注计数与增长趋势。
export const toggleFollowUser = (targetUserId) =>
  apiClient.post('/follow/toggle', null, { params: { targetUserId } })

export const getFollowStatus = (targetUserId) =>
  apiClient.get('/follow/status', { params: { targetUserId } })

export const getFollowerCount = (userId) =>
  apiClient.get('/follow/follower-count', { params: { userId } })

export const getFollowingCount = (userId) =>
  apiClient.get('/follow/following-count', { params: { userId } })

export const getMyFollowSummary = () => apiClient.get('/follow/my/summary')

export const getMyFollowerGrowth = (params) =>
  apiClient.get('/follow/my/follower-growth', { params })

export const getMyFollowers = (params) =>
  apiClient.get('/follow/my/followers', { params })

export const getMyFollowing = (params) =>
  apiClient.get('/follow/my/following', { params })
