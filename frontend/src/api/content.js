import apiClient from './client'
import { getViewerKey } from '../utils/viewerKey'

export const getCategories = () => apiClient.get('/content/categories')

export const getTags = () => apiClient.get('/content/tags')

export const getContentList = (params) => apiClient.get('/content/list', { params })

export const getContentDetail = (id) => apiClient.get(`/content/detail/${id}`)

export const publishContent = (payload) => apiClient.post('/content/publish', payload)

export const updateContent = (id, payload) => apiClient.put(`/content/${id}`, payload)

export const deleteContent = (id) => apiClient.delete(`/content/${id}`)

export const reportContentView = (id) =>
  apiClient.post(`/content/view/${id}`, null, {
    headers: {
      'X-Viewer-Id': getViewerKey()
    }
  })

