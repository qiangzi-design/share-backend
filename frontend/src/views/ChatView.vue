<template>
  <section class="chat-page">
    <el-card class="chat-card motion-fade-up">
      <div class="chat-layout">
        <aside class="conversation-panel">
          <div class="panel-head">
            <h3>私聊会话</h3>
          </div>

          <el-empty
            v-if="!conversationState.loading && conversationState.list.length === 0"
            description="还没有会话"
          />

          <div v-else class="conversation-list">
            <div
              v-for="item in conversationState.list"
              :key="item.conversationId || item.peerUserId"
              class="conversation-item"
              :class="{ active: Number(item.peerUserId) === Number(activePeer?.peerUserId || 0) }"
              @click="selectConversation(item)"
            >
              <button class="avatar-link" type="button" @click.stop="goUserProfile(item.peerUserId)">
                <UserAvatar :size="40" :src="normalizeFileUrl(item.peerAvatar || '')" :fallback-text="getPeerName(item)" />
              </button>
              <div class="conversation-meta">
                <div class="name-row">
                  <strong class="name-link" @click.stop="goUserProfile(item.peerUserId)">
                    {{ getPeerName(item) }}
                  </strong>
                  <div class="meta-right">
                    <span class="time">{{ formatTime(item.lastMessageTime || item.createTime) }}</span>
                    <span v-if="normalizeUnreadCount(item.unreadCount) > 0" class="unread-badge">
                      {{ formatUnreadCount(item.unreadCount) }}
                    </span>
                  </div>
                </div>
                <p class="preview">{{ getConversationPreview(item) }}</p>
              </div>
            </div>
          </div>
        </aside>

        <main class="message-panel">
          <template v-if="activePeer">
            <div class="message-head">
              <div class="peer">
                <button class="avatar-link" type="button" @click="goUserProfile(activePeer.peerUserId)">
                  <UserAvatar :size="36" :src="normalizeFileUrl(activePeer.peerAvatar || '')" :fallback-text="getPeerName(activePeer)" />
                </button>
                <div>
                  <strong class="name-link" @click="goUserProfile(activePeer.peerUserId)">
                    {{ getPeerName(activePeer) }}
                  </strong>
                  <p>{{ allowance.reason }}</p>
                </div>
              </div>
            </div>

            <div class="message-list-wrap">
              <el-empty v-if="!messageState.loading && messageState.list.length === 0" description="暂无聊天记录" />
              <div v-else class="message-list">
                <template v-for="item in messageRenderList" :key="item.key">
                  <div v-if="item.type === 'divider'" class="time-divider">
                    {{ item.label }}
                  </div>
                  <div v-else class="message-item" :class="{ mine: Number(item.message.senderId) === currentUserId }">
                    <div class="message-row" :class="{ mine: Number(item.message.senderId) === currentUserId }">
                      <button
                        class="avatar-link message-avatar"
                        type="button"
                        @click="goUserProfile(item.message.senderId)"
                      >
                        <UserAvatar :size="40" :src="normalizeFileUrl(item.message.senderAvatar || '')" :fallback-text="getMessageSenderName(item.message)" />
                      </button>
                      <div class="message-main" :class="{ mine: Number(item.message.senderId) === currentUserId }">
                        <div class="bubble" :class="{ mine: Number(item.message.senderId) === currentUserId }">
                          <el-image
                            v-if="Number(item.message.messageType) === IMAGE_MESSAGE_TYPE"
                            :src="normalizeFileUrl(item.message.content)"
                            :preview-src-list="[normalizeFileUrl(item.message.content)]"
                            fit="cover"
                            class="chat-image"
                            preview-teleported
                          />
                          <p v-else>{{ item.message.content }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>

            <div class="composer">
              <el-input
                v-model="draft"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                maxlength="1000"
                show-word-limit
                :placeholder="allowance.canSend ? '输入消息，按 Enter 发送（Shift+Enter 换行）' : allowance.reason"
                :disabled="!allowance.canSend"
                @keydown.enter.exact.prevent="sendCurrentMessage"
              />
              <div class="composer-actions">
                <EmojiPicker :disabled="!allowance.canSend" @select="appendChatEmoji" />
                <el-upload
                  action="/api/chat/upload-image"
                  :show-file-list="false"
                  :before-upload="beforeChatImageUpload"
                  :on-success="handleChatImageUploadSuccess"
                  :on-error="handleChatImageUploadError"
                  :headers="uploadHeaders"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  :disabled="!allowance.canSend || sending"
                >
                  <el-button :icon="Picture" :disabled="!allowance.canSend || sending">图片</el-button>
                </el-upload>
                <el-button @click="refreshCurrent">刷新</el-button>
                <el-button type="primary" :loading="sending" :disabled="!allowance.canSend" @click="sendCurrentMessage">
                  发送
                </el-button>
              </div>
            </div>
          </template>

          <el-empty v-else description="请选择左侧会话，或从作者卡片点击‘私聊’发起会话" />
        </main>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmojiPicker from '../components/EmojiPicker.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { getChatAllowance, getChatConversations, getChatMessages, markChatRead, sendChatMessage } from '../api/chat'
import { getPublicUserProfile } from '../api/user'
import { compressImageFile, formatBytes } from '../utils/media'

/**
 * 私聊页职责：
 * 1. 展示会话列表与消息流，并维护会话级未读数。
 * 2. 承载发信规则提示（单向各1条、互关不限）与发送入口（文字/图片）。
 * 3. 统一处理“打开会话即已读”和实时事件同步，保持角标与列表一致。
 */
const route = useRoute()
const router = useRouter()
const currentUserId = computed(() => Number(sessionStorage.getItem('userId') || 0))

const sending = ref(false)
const draft = ref('')
const activePeer = ref(null)
const realtimeWsConnected = ref(Boolean(window.__shareRealtimeWsConnected))
let pollingTimer = null

const conversationState = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0,
  loading: false
})

const messageState = reactive({
  list: [],
  page: 1,
  pageSize: 50,
  total: 0,
  loading: false
})

const allowance = reactive({
  canSend: false,
  isFollowing: false,
  isMutual: false,
  oneWayMessageUsed: false,
  reason: '请先选择会话'
})

const THIRTY_MINUTES_MS = 30 * 60 * 1000
const TEXT_MESSAGE_TYPE = 1
const IMAGE_MESSAGE_TYPE = 2
const MAX_CHAT_IMAGE_BYTES = 8 * 1024 * 1024
const CHAT_IMAGE_TARGET_BYTES = Math.floor(1.2 * 1024 * 1024)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${sessionStorage.getItem('token') || ''}`
}))

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const getPeerName = (peer) => {
  if (!peer) return '用户'
  return peer.peerNickname || peer.peerUsername || `用户${peer.peerUserId || ''}`
}

const getMessageSenderName = (message) => {
  if (!message) return '用户'
  return message.senderNickname || message.senderUsername || `用户${message.senderId || ''}`
}

const getConversationPreview = (item) => {
  if (!item) return '还没有消息，开始打个招呼吧'
  if (Number(item.lastMessageType) === IMAGE_MESSAGE_TYPE) return '[图片]'
  return item.lastMessage || '还没有消息，开始打个招呼吧'
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN')
}

const formatDividerTime = (timeString) => {
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return ''
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${hour}:${minute}:${second}`
}

const normalizeUnreadCount = (value) => {
  const count = Number(value || 0)
  return Number.isFinite(count) && count > 0 ? count : 0
}

const formatUnreadCount = (value) => {
  const count = normalizeUnreadCount(value)
  if (count <= 0) return ''
  return count > 99 ? '99+' : String(count)
}

const getTimestamp = (timeString) => {
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return Date.now()
  return date.getTime()
}

// 时间分隔规则：首条必显示，之后仅在与上次分隔间隔 >=30 分钟时插入分隔条。
const messageRenderList = computed(() => {
  const result = []
  let lastDividerTime = 0
  for (const message of messageState.list) {
    const currentTime = getTimestamp(message.createTime)
    if (lastDividerTime === 0 || currentTime - lastDividerTime >= THIRTY_MINUTES_MS) {
      result.push({
        type: 'divider',
        key: `divider-${message.id || currentTime}`,
        label: formatDividerTime(message.createTime)
      })
      lastDividerTime = currentTime
    }
    result.push({
      type: 'message',
      key: `message-${message.id}`,
      message
    })
  }
  return result
})

const appendChatEmoji = (emoji) => {
  if (!emoji || !allowance.canSend) return
  draft.value = `${draft.value || ''}${emoji}`
}

// 聊天图片上传前压缩：优先保证可发送，再尽量减少传输体积。
const beforeChatImageUpload = async (file) => {
  if (!allowance.canSend) {
    ElMessage.warning(allowance.reason || '当前不可发送消息')
    return false
  }
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、GIF、WEBP')
    return false
  }

  const compressed = await compressImageFile(file, {
    minBytesToCompress: 500 * 1024,
    targetBytes: CHAT_IMAGE_TARGET_BYTES,
    maxDimension: 1600,
    qualityList: [0.84, 0.76, 0.68, 0.62],
    pngToWebpQuality: 0.78,
    allowedTypes: new Set(['image/jpeg', 'image/png', 'image/webp'])
  }).catch(() => ({
    file,
    compressed: false,
    beforeBytes: file.size,
    afterBytes: file.size
  }))

  const uploadFile = compressed.file || file
  if (uploadFile.size > MAX_CHAT_IMAGE_BYTES) {
    ElMessage.error('图片大小不能超过 8MB')
    return false
  }

  if (compressed.compressed) {
    const savedPercent = ((compressed.beforeBytes - compressed.afterBytes) / compressed.beforeBytes) * 100
    ElMessage.success(
      `已压缩图片：${formatBytes(compressed.beforeBytes)} -> ${formatBytes(compressed.afterBytes)}（-${savedPercent.toFixed(1)}%）`
    )
  }
  return uploadFile
}

const handleChatImageUploadSuccess = async (response) => {
  if (response?.code !== 200 || !response?.data) {
    ElMessage.error(response?.message || '图片上传失败')
    return
  }
  if (!activePeer.value?.peerUserId) {
    ElMessage.warning('请先选择会话')
    return
  }
  try {
    sending.value = true
    const sendResp = await sendChatMessage({
      targetUserId: activePeer.value.peerUserId,
      content: response.data,
      messageType: IMAGE_MESSAGE_TYPE
    })
    if (sendResp.data?.code !== 200) {
      ElMessage.error(sendResp.data?.message || '图片发送失败')
      return
    }
    await Promise.all([loadConversations(), refreshCurrent()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '图片发送失败')
  } finally {
    sending.value = false
  }
}

const handleChatImageUploadError = (error) => {
  const status = Number(error?.status || 0)
  if (status === 413) {
    ElMessage.error('图片超过上传限制（最大 8MB）')
    return
  }
  ElMessage.error('图片上传失败，请稍后重试')
}

const goUserProfile = (userId) => {
  if (!userId) return
  router.push(`/users/${userId}`)
}

const emitUnreadSync = (unreadCount) => {
  const detail = Number.isFinite(Number(unreadCount)) ? { unreadCount: Number(unreadCount) } : {}
  window.dispatchEvent(new CustomEvent('chat-unread-sync', { detail }))
}

const getQueryTargetUserId = () => {
  const value = Number(route.query.targetUserId || 0)
  return value > 0 ? value : 0
}

const setConversationUnreadCount = (peerUserId, unreadCount) => {
  const nextUnread = normalizeUnreadCount(unreadCount)
  conversationState.list = conversationState.list.map((item) => {
    if (Number(item.peerUserId) !== Number(peerUserId)) {
      return item
    }
    return {
      ...item,
      unreadCount: nextUnread
    }
  })
  if (activePeer.value && Number(activePeer.value.peerUserId) === Number(peerUserId)) {
    activePeer.value = {
      ...activePeer.value,
      unreadCount: nextUnread
    }
  }
}

// 支持从 URL 参数直接拉起会话（例如“从作者卡片点击私聊”场景）。
const ensureActivePeerFromQuery = async () => {
  const targetUserId = getQueryTargetUserId()
  if (!targetUserId) return

  const exists = conversationState.list.find((item) => Number(item.peerUserId) === targetUserId)
  if (exists) {
    activePeer.value = exists
    return
  }

  try {
    const response = await getPublicUserProfile(targetUserId)
    if (response.data?.code !== 200 || !response.data?.data) return
    const user = response.data.data
    activePeer.value = {
      conversationId: null,
      peerUserId: user.id,
      peerUsername: user.username || '',
      peerNickname: user.nickname || '',
      peerAvatar: user.avatar || '',
      lastMessage: '',
      lastMessageTime: null,
      unreadCount: 0
    }
  } catch (_) {
    // ignore
  }
}

// 会话列表是右侧消息区的索引源，刷新时优先保持当前激活会话不丢失。
const loadConversations = async () => {
  conversationState.loading = true
  try {
    const response = await getChatConversations({
      page: conversationState.page,
      pageSize: conversationState.pageSize
    })
    const data = response.data?.data || {}
    conversationState.list = (data.list || []).map((item) => ({
      ...item,
      unreadCount: normalizeUnreadCount(item.unreadCount)
    }))
    conversationState.page = data.page || conversationState.page
    conversationState.pageSize = data.pageSize || conversationState.pageSize
    conversationState.total = Number(data.total || 0)

    if (activePeer.value?.peerUserId) {
      const matched = conversationState.list.find((item) => Number(item.peerUserId) === Number(activePeer.value.peerUserId))
      if (matched) {
        activePeer.value = matched
      }
    }
  } catch (_) {
    ElMessage.error('加载会话失败')
  } finally {
    conversationState.loading = false
  }
}

// 由后端统一判定发信资格，前端只负责展示可发送状态与原因文案。
const loadAllowance = async () => {
  if (!activePeer.value?.peerUserId) return
  try {
    const response = await getChatAllowance(activePeer.value.peerUserId)
    if (response.data?.code !== 200 || !response.data?.data) return
    const data = response.data.data
    allowance.canSend = Boolean(data.canSend)
    allowance.isFollowing = Boolean(data.isFollowing)
    allowance.isMutual = Boolean(data.isMutual)
    allowance.oneWayMessageUsed = Boolean(data.oneWayMessageUsed)
    allowance.reason = data.reason || (allowance.canSend ? '可以发送消息' : '暂不可发送')
  } catch (error) {
    allowance.canSend = false
    allowance.reason = error?.response?.data?.message || '暂不可发送消息'
  }
}

const loadMessages = async () => {
  if (!activePeer.value?.peerUserId) {
    messageState.list = []
    messageState.total = 0
    return false
  }
  messageState.loading = true
  try {
    const response = await getChatMessages({
      targetUserId: activePeer.value.peerUserId,
      page: messageState.page,
      pageSize: messageState.pageSize
    })
    const data = response.data?.data || {}
    messageState.list = data.list || []
    messageState.total = Number(data.total || 0)
    messageState.page = data.page || messageState.page
    messageState.pageSize = data.pageSize || messageState.pageSize
    return true
  } catch (_) {
    ElMessage.error('加载消息失败')
    return false
  } finally {
    messageState.loading = false
  }
}

// 打开会话并成功拉到消息后立即置已读，保证“角标-会话-消息页”三方同步。
const markActiveConversationRead = async () => {
  if (!activePeer.value?.peerUserId) return
  try {
    const response = await markChatRead(activePeer.value.peerUserId)
    if (response.data?.code !== 200 || !response.data?.data) return
    const data = response.data.data
    setConversationUnreadCount(activePeer.value.peerUserId, 0)
    emitUnreadSync(data.unreadCount)
  } catch (_) {
    // ignore
  }
}

const selectConversation = async (item) => {
  activePeer.value = item
  if (Number(route.query.targetUserId || 0) !== Number(item.peerUserId || 0)) {
    router.replace({ path: '/chat', query: { targetUserId: String(item.peerUserId) } })
  }
  await refreshCurrent()
}

const refreshCurrent = async () => {
  const messagesLoaded = await loadMessages()
  await loadAllowance()
  if (messagesLoaded) {
    await markActiveConversationRead()
  }
}

// 实时事件命中当前会话时全量刷新，避免局部 patch 造成顺序/未读不一致。
const handleRealtimeChatEvent = async (event) => {
  const payload = event?.detail || {}
  const senderId = Number(payload.senderId || 0)
  if (!senderId) {
    return
  }
  if (activePeer.value?.peerUserId && Number(activePeer.value.peerUserId) === senderId) {
    await Promise.all([loadConversations(), refreshCurrent()])
    return
  }
  await loadConversations()
}

const handleRealtimeWsState = (event) => {
  // 接收全局 WebSocket 状态广播：在线时仅走实时消息，断线时才启用轮询兜底。
  realtimeWsConnected.value = Boolean(event?.detail?.connected)
}

const sendCurrentMessage = async () => {
  if (!activePeer.value?.peerUserId) return
  const text = draft.value.trim()
  if (!text) {
    ElMessage.warning('消息内容不能为空')
    return
  }
  if (!allowance.canSend) {
    ElMessage.warning(allowance.reason || '当前不可发送消息')
    return
  }

  sending.value = true
  try {
    const response = await sendChatMessage({
      targetUserId: activePeer.value.peerUserId,
      content: text,
      messageType: TEXT_MESSAGE_TYPE
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '发送失败')
      return
    }
    draft.value = ''
    await Promise.all([loadConversations(), refreshCurrent()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

// 轮询兜底用于覆盖 WebSocket 短暂断连场景，页面可见时才执行减少无效请求。
const startPolling = () => {
  stopPolling()
  pollingTimer = window.setInterval(async () => {
    if (document.visibilityState !== 'visible') return
    if (realtimeWsConnected.value) return
    await loadConversations()
    if (activePeer.value?.peerUserId) {
      await refreshCurrent()
    }
  }, 8000)
}

const stopPolling = () => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const initPage = async () => {
  await loadConversations()
  await ensureActivePeerFromQuery()
  if (!activePeer.value && conversationState.list.length > 0) {
    activePeer.value = conversationState.list[0]
  }
  await refreshCurrent()
}

watch(
  () => route.query.targetUserId,
  async () => {
    await loadConversations()
    await ensureActivePeerFromQuery()
    await refreshCurrent()
  }
)

onMounted(async () => {
  await initPage()
  startPolling()
  window.addEventListener('chat-realtime-event', handleRealtimeChatEvent)
  window.addEventListener('realtime-ws-state', handleRealtimeWsState)
})

onBeforeUnmount(() => {
  stopPolling()
  window.removeEventListener('chat-realtime-event', handleRealtimeChatEvent)
  window.removeEventListener('realtime-ws-state', handleRealtimeWsState)
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
}

.chat-card {
  border-radius: 18px;
}

.chat-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 14px;
  min-height: 600px;
}

.conversation-panel,
.message-panel {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.panel-head h3 {
  margin: 4px 4px 10px;
  font-size: 16px;
  color: #1f2d3d;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: 540px;
  padding-right: 2px;
}

.conversation-item {
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(248, 252, 255, 0.9);
  border-radius: 12px;
  padding: 8px;
  cursor: pointer;
  display: flex;
  gap: 10px;
  align-items: center;
  text-align: left;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  border-color: rgba(14, 165, 164, 0.44);
  background: rgba(236, 253, 245, 0.88);
}

.avatar-link {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  border-radius: 999px;
}

.conversation-meta {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.name-row strong {
  color: #23364c;
  font-size: 14px;
}

.meta-right {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.name-link {
  cursor: pointer;
}

.name-link:hover {
  color: #0c8c8b;
}

.time {
  color: #7f8a9e;
  font-size: 12px;
  white-space: nowrap;
}

.unread-badge {
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: #fff;
  background: #f56c6c;
}

.preview {
  margin-top: 4px;
  color: #5d7189;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-panel {
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.message-head {
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.peer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.peer strong {
  color: #1f2d3d;
}

.peer p {
  margin-top: 3px;
  color: #63778f;
  font-size: 12px;
}

.message-list-wrap {
  flex: 1;
  min-height: 300px;
  max-height: 430px;
  overflow-y: auto;
  padding: 16px 12px;
  background: #f3f3f3;
  border-radius: 10px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.time-divider {
  align-self: center;
  font-size: 13px;
  color: #8a8f98;
  line-height: 1;
}

.message-item {
  display: flex;
  justify-content: flex-start;
}

.message-item.mine {
  justify-content: flex-end;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: min(680px, 92%);
}

.message-row.mine {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-main {
  min-width: 0;
  max-width: min(560px, 78vw);
}

.message-main.mine {
  display: flex;
  justify-content: flex-end;
}

.bubble {
  position: relative;
  max-width: 100%;
  border-radius: 10px;
  padding: 11px 14px;
  background: #e0e2e6;
  color: #0f172a;
  box-shadow: 0 1px 1px rgba(15, 23, 42, 0.06);
}

.bubble.mine {
  background: #28abe8;
  color: #fff;
}

.bubble::after {
  content: '';
  position: absolute;
  left: -6px;
  bottom: 10px;
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  border-right: 6px solid #e0e2e6;
}

.bubble.mine::after {
  left: auto;
  right: -6px;
  border-right: 0;
  border-left: 6px solid #28abe8;
}

.bubble p {
  margin: 0;
  line-height: 1.5;
  font-size: 16px;
  word-break: break-word;
}

.chat-image {
  width: 220px;
  max-width: min(240px, 66vw);
  border-radius: 8px;
  overflow: hidden;
}

.composer {
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  padding-top: 10px;
}

.composer-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .chat-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .conversation-list {
    max-height: 260px;
  }

  .message-list-wrap {
    max-height: 380px;
  }

  .bubble {
    max-width: min(420px, 76vw);
  }
}
</style>
