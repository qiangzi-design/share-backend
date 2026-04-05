import apiClient from './client'

// 用户接口：个人资料、公开资料、个人内容与互动列表。
export const getMyProfile = () => apiClient.get('/users/me')

export const updateMyProfile = (payload) => apiClient.put('/users/me', payload)

export const getUserContents = (userId, params) => apiClient.get(`/users/${userId}/contents`, { params })

export const getPublicUserProfile = (userId) => apiClient.get(`/users/${userId}/public`)

export const getMyCollections = (params) => apiClient.get('/users/me/collections', { params })

export const getMyLikes = (params) => apiClient.get('/users/me/likes', { params })
