import apiClient from './client'

// 会话列表：包含会话摘要与未读数。
export const getChatConversations = (params) =>
  apiClient.get('/chat/conversations', { params })

// 消息列表：按会话拉取分页消息。
export const getChatMessages = (params) =>
  apiClient.get('/chat/messages', { params })

// 私聊总未读：用于顶部私聊角标。
export const getChatUnreadCount = () =>
  apiClient.get('/chat/unread-count')

// 发信资格校验：返回是否可发与限制原因（单向/互关规则）。
export const getChatAllowance = (targetUserId) =>
  apiClient.get('/chat/allowance', { params: { targetUserId } })

export const sendChatMessage = (payload) =>
  apiClient.post('/chat/messages', payload)

// 图片消息上传（multipart），成功后再走 sendChatMessage 发送消息体。
export const uploadChatImage = (formData) =>
  apiClient.post('/chat/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

// 打开会话即已读：按 targetUserId 批量置已读。
export const markChatRead = (targetUserId) =>
  apiClient.post('/chat/read', null, { params: { targetUserId } })
