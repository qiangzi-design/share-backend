<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, ChatDotRound, Compass, House, InfoFilled, Plus, Setting, SwitchButton, UserFilled, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import { logout } from './api/auth'
import { getActiveAnnouncements, getAnnouncementUnreadCount } from './api/announcement'
import { reportSiteUvPing } from './api/analytics'
import { getUnreadSummary } from './api/notification'
import { clearAdminCache, getAdminPermissionsFromCache, refreshAdminMe } from './utils/adminAuth'

const route = useRoute()
const router = useRouter()

const activePath = computed(() => {
  if (route.path.startsWith('/content/') && route.path.endsWith('/edit')) return '/publish'
  if (route.path.startsWith('/content/')) return '/'
  if (route.path.startsWith('/ai-brief')) return '/ai-brief'
  if (route.path.startsWith('/users/')) return '/'
  if (route.path.startsWith('/my-works')) return '/profile'
  if (route.path.startsWith('/chat')) return '/chat'
  if (route.path.startsWith('/notifications')) return '/notifications'
  if (route.path.startsWith('/my-reports')) return '/my-reports'
  if (route.path.startsWith('/admin')) return '/admin'
  return route.path
})

const readAuthState = () => ({
  token: sessionStorage.getItem('token') || '',
  username: sessionStorage.getItem('username') || '',
  nickname: sessionStorage.getItem('nickname') || ''
})

const authState = ref(readAuthState())
const adminPermissions = ref(getAdminPermissionsFromCache())
const chatUnreadCount = ref(0)
const interactionUnreadCount = ref(0)
const systemUnreadCount = ref(0)
const announcementUnreadCount = ref(0)
const totalUnreadCount = computed(() => chatUnreadCount.value + interactionUnreadCount.value + systemUnreadCount.value + announcementUnreadCount.value)
const wsConnected = ref(false)
// 未读轮询仅作为 WebSocket 断线兜底，避免高并发下频繁打到后端数据库。
const UNREAD_POLL_INTERVAL_MS = 20000
// 公告轮询改为低频触发，先查未读数，有变化时再拉详情列表，降低后端压力。
const ANNOUNCEMENT_POLL_INTERVAL_MS = 30000
let unreadPollingTimer = null
let announcementPollingTimer = null
let wsConnection = null
let wsReconnectTimer = null
let manualCloseWs = false
const announcementPopupSeenIds = ref(new Set())

const syncAuthState = () => {
  authState.value = readAuthState()
  adminPermissions.value = getAdminPermissionsFromCache()
  if (authState.value.token && !wsConnection) {
    connectRealtimeSocket()
  }
}

const isLoggedIn = computed(() => Boolean(authState.value.token))
const hasAdminEntrance = computed(() => adminPermissions.value.length > 0)
const displayName = computed(() => authState.value.nickname || authState.value.username || '我的主页')
const theme = ref(localStorage.getItem('ui-theme') || 'teal')
const motion = ref(localStorage.getItem('ui-motion') || 'standard')

const applyTheme = (value) => {
  const normalized = value === 'sunset' ? 'sunset' : 'teal'
  document.documentElement.setAttribute('data-theme', normalized)
}

const applyMotion = (value) => {
  const normalized = ['standard', 'soft', 'off'].includes(value) ? value : 'standard'
  document.documentElement.setAttribute('data-motion', normalized)
}

const normalizeUnreadCount = (value) => {
  const count = Number(value || 0)
  return Number.isFinite(count) && count >= 0 ? count : 0
}

const emitInteractionUnreadSync = (count) => {
  window.dispatchEvent(new CustomEvent('interaction-unread-sync', { detail: { unreadCount: normalizeUnreadCount(count) } }))
}

const emitSystemUnreadSync = (count) => {
  // 管理端动作触发的系统通知与互动通知分栏展示，需要单独同步未读数。
  window.dispatchEvent(new CustomEvent('system-unread-sync', { detail: { unreadCount: normalizeUnreadCount(count) } }))
}

const emitChatUnreadSync = (count) => {
  window.dispatchEvent(new CustomEvent('chat-unread-sync', { detail: { unreadCount: normalizeUnreadCount(count) } }))
}

const emitAnnouncementUnreadSync = (count) => {
  window.dispatchEvent(new CustomEvent('announcement-unread-sync', { detail: { unreadCount: normalizeUnreadCount(count) } }))
}

const emitRealtimeWsStateSync = (connected) => {
  // 统一广播 WS 连接状态，便于业务页在“实时模式/轮询兜底模式”间切换。
  const next = Boolean(connected)
  window.__shareRealtimeWsConnected = next
  window.dispatchEvent(new CustomEvent('realtime-ws-state', { detail: { connected: next } }))
}

const extractRealtimeUnreadCount = (value) => {
  const count = Number(value)
  if (!Number.isFinite(count) || count < 0) {
    return null
  }
  return count
}

const applyUnreadSummary = (summary) => {
  // 统一消费后端聚合结果，保证三类角标同一时刻更新，避免页面出现“数字不同步”。
  interactionUnreadCount.value = normalizeUnreadCount(summary?.interactionUnreadCount)
  systemUnreadCount.value = normalizeUnreadCount(summary?.systemUnreadCount)
  chatUnreadCount.value = normalizeUnreadCount(summary?.chatUnreadCount)
  announcementUnreadCount.value = normalizeUnreadCount(summary?.announcementUnreadCount)
  emitInteractionUnreadSync(interactionUnreadCount.value)
  emitSystemUnreadSync(systemUnreadCount.value)
  emitChatUnreadSync(chatUnreadCount.value)
  emitAnnouncementUnreadSync(announcementUnreadCount.value)
}

const refreshUnreadSummary = async () => {
  if (!isLoggedIn.value) {
    applyUnreadSummary({
      interactionUnreadCount: 0,
      systemUnreadCount: 0,
      chatUnreadCount: 0,
      announcementUnreadCount: 0
    })
    return
  }
  try {
    const response = await getUnreadSummary()
    if (response.data?.code === 200 && response.data?.data) {
      applyUnreadSummary(response.data.data)
    }
  } catch (_) {
    // 聚合接口失败时保持现有显示值，避免 UI 抖动。
  }
}

const refreshAllUnread = async () => {
  await refreshUnreadSummary()
}

const reportSiteUvSafely = async () => {
  try {
    await reportSiteUvPing()
  } catch (_) {
    // uv 上报失败不阻断页面加载
  }
}

const stopUnreadPolling = () => {
  if (unreadPollingTimer) {
    window.clearInterval(unreadPollingTimer)
    unreadPollingTimer = null
  }
}

const startUnreadPolling = () => {
  stopUnreadPolling()
  unreadPollingTimer = window.setInterval(async () => {
    if (document.visibilityState !== 'visible') return
    // WebSocket 在线时由实时推送驱动未读更新，停止主动轮询以减轻接口压力。
    if (wsConnected.value) return
    await refreshAllUnread()
  }, UNREAD_POLL_INTERVAL_MS)
}

const getAnnouncementPopupStoreKey = () => {
  const userId = sessionStorage.getItem('userId') || 'guest'
  return `announcement_popup_seen_${userId}`
}

const loadAnnouncementPopupSeenIds = () => {
  const raw = sessionStorage.getItem(getAnnouncementPopupStoreKey())
  if (!raw) {
    announcementPopupSeenIds.value = new Set()
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      announcementPopupSeenIds.value = new Set(parsed.map((item) => Number(item)).filter((item) => Number.isFinite(item) && item > 0))
      return
    }
  } catch (_) {
    // ignore parse error
  }
  announcementPopupSeenIds.value = new Set()
}

const saveAnnouncementPopupSeenIds = () => {
  try {
    sessionStorage.setItem(getAnnouncementPopupStoreKey(), JSON.stringify([...announcementPopupSeenIds.value]))
  } catch (_) {
    // ignore storage failure
  }
}

const stopAnnouncementPolling = () => {
  if (announcementPollingTimer) {
    window.clearInterval(announcementPollingTimer)
    announcementPollingTimer = null
  }
}

const sortAnnouncementList = (list) =>
  [...list].sort((a, b) => {
    const pinnedDiff = Number(Boolean(b?.isPinned)) - Number(Boolean(a?.isPinned))
    if (pinnedDiff !== 0) return pinnedDiff
    const timeA = new Date(a?.publishTime || a?.createTime || 0).getTime() || 0
    const timeB = new Date(b?.publishTime || b?.createTime || 0).getTime() || 0
    return timeB - timeA
  })

const showAnnouncementPopup = (item) => {
  const title = item?.title || '站内公告'
  const body = String(item?.body || '').trim()
  const summary = body.length > 28 ? `${body.slice(0, 28)}...` : body
  ElNotification({
    title: item?.isPinned ? '新置顶公告' : '新公告',
    message: summary ? `${title}：${summary}` : title,
    type: 'info',
    duration: 6000,
    onClick: () => {
      router.push('/notifications')
    }
  })
}

const checkAnnouncementUpdates = async (initialize = false, shouldRefreshUnread = true) => {
  if (!isLoggedIn.value) return
  try {
    const response = await getActiveAnnouncements()
    if (response.data?.code !== 200 || !Array.isArray(response.data?.data)) {
      return
    }
    const sortedList = sortAnnouncementList(response.data.data)
    if (sortedList.length === 0) {
      return
    }

    const currentIds = new Set(
      sortedList.map((item) => Number(item?.id)).filter((id) => Number.isFinite(id) && id > 0)
    )

    if (initialize && announcementPopupSeenIds.value.size === 0) {
      announcementPopupSeenIds.value = currentIds
      saveAnnouncementPopupSeenIds()
      return
    }

    const newItems = sortedList.filter((item) => {
      const id = Number(item?.id)
      return Number.isFinite(id) && id > 0 && !announcementPopupSeenIds.value.has(id)
    })

    if (newItems.length === 0) {
      return
    }

    for (const item of newItems) {
      const id = Number(item?.id)
      announcementPopupSeenIds.value.add(id)
      showAnnouncementPopup(item)
      window.dispatchEvent(new CustomEvent('announcement-realtime-event', { detail: item }))
    }
    if (shouldRefreshUnread) {
      // 公告弹窗后刷新聚合未读，保证消息/私聊/公告角标保持一致。
      await refreshUnreadSummary()
    }
    saveAnnouncementPopupSeenIds()
  } catch (_) {
    // keep silent on announcement polling failure
  }
}

const startAnnouncementPolling = () => {
  stopAnnouncementPolling()
  announcementPollingTimer = window.setInterval(async () => {
    if (document.visibilityState !== 'visible') return
    // WebSocket 在线时公告改为实时事件驱动，轮询仅在断线兜底时启用。
    if (wsConnected.value) return
    try {
      const response = await getAnnouncementUnreadCount()
      if (response.data?.code !== 200 || !response.data?.data) {
        return
      }
      const nextUnread = normalizeUnreadCount(response.data.data.unreadCount)
      const previousUnread = announcementUnreadCount.value

      // 未读数总是先同步到全局角标，保证导航数字及时更新。
      announcementUnreadCount.value = nextUnread
      emitAnnouncementUnreadSync(nextUnread)

      // 仅在“未读增加”时才拉 active 公告详情并弹提醒，避免每轮都拉大列表。
      if (nextUnread > previousUnread) {
        await checkAnnouncementUpdates(false, false)
      }
    } catch (_) {
      // keep silent on announcement polling failure
    }
  }, ANNOUNCEMENT_POLL_INTERVAL_MS)
}

const handleChatUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (Number.isFinite(incoming) && incoming >= 0) {
    chatUnreadCount.value = incoming
    return
  }
  // 兜底时改为一次拉取聚合未读，避免单项接口高频请求。
  refreshUnreadSummary()
}

const handleInteractionUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (Number.isFinite(incoming) && incoming >= 0) {
    interactionUnreadCount.value = incoming
    return
  }
  // 兜底时改为一次拉取聚合未读，避免单项接口高频请求。
  refreshUnreadSummary()
}

const handleSystemUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (Number.isFinite(incoming) && incoming >= 0) {
    systemUnreadCount.value = incoming
    return
  }
  // 兜底时改为一次拉取聚合未读，避免单项接口高频请求。
  refreshUnreadSummary()
}

const handleAnnouncementUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (Number.isFinite(incoming) && incoming >= 0) {
    announcementUnreadCount.value = incoming
    return
  }
  // 兜底时改为一次拉取聚合未读，避免单项接口高频请求。
  refreshUnreadSummary()
}

const buildWebSocketUrl = () => {
  const token = sessionStorage.getItem('token')
  if (!token) return ''
  if (import.meta.env.DEV) {
    return `ws://localhost:8081/ws/notifications?token=${encodeURIComponent(token)}`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws/notifications?token=${encodeURIComponent(token)}`
}

const clearWsReconnect = () => {
  if (wsReconnectTimer) {
    window.clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
}

const closeRealtimeSocket = () => {
  manualCloseWs = true
  clearWsReconnect()
  if (wsConnection) {
    wsConnection.close()
    wsConnection = null
  }
  wsConnected.value = false
  emitRealtimeWsStateSync(false)
}

const handleRealtimeEvent = (raw) => {
  if (!raw || !raw.eventType || !raw.payload) {
    return
  }
  if (raw.eventType === 'interaction') {
    const nextUnread = extractRealtimeUnreadCount(raw.payload.unreadCount)
    // 系统通知(type=system_notice)单独归档到“系统通知”页签，互动通知保持原有口径。
    const isSystemNotice = String(raw.payload?.type || '').toLowerCase() === 'system_notice'
    if (isSystemNotice) {
      if (nextUnread !== null) {
        // 实时载荷中的 unreadCount 是“全部通知未读”，这里采用聚合同步避免口径误差。
        refreshUnreadSummary()
      } else {
        systemUnreadCount.value += 1
        emitSystemUnreadSync(systemUnreadCount.value)
      }
    } else {
      if (nextUnread !== null) {
        interactionUnreadCount.value = nextUnread
      } else {
        interactionUnreadCount.value += 1
      }
      emitInteractionUnreadSync(interactionUnreadCount.value)
    }
    ElNotification({
      title: raw.payload.title || '新互动',
      message: raw.payload.body || '你收到了新的互动消息',
      type: 'info',
      duration: 3500
    })
    window.dispatchEvent(new CustomEvent('interaction-realtime-event', { detail: raw.payload }))
    return
  }
  if (raw.eventType === 'chat') {
    const nextUnread = extractRealtimeUnreadCount(raw.payload.unreadCount)
    if (nextUnread !== null) {
      chatUnreadCount.value = nextUnread
    } else {
      chatUnreadCount.value += 1
    }
    emitChatUnreadSync(chatUnreadCount.value)
    const chatPreview = Number(raw.payload.messageType) === 2
      ? '[图片]'
      : (raw.payload.content || '你收到了一个新私聊消息')
    ElNotification({
      title: '新私聊消息',
      message: chatPreview,
      type: 'success',
      duration: 2800
    })
    window.dispatchEvent(new CustomEvent('chat-realtime-event', { detail: raw.payload }))
    return
  }
  if (raw.eventType === 'announcement') {
    // 公告实时事件：发布时走“弹窗+角标+列表刷新”，更新/下线走“静默同步”。
    const action = String(raw.payload?.action || '').toLowerCase()
    const activeNow = Boolean(raw.payload?.activeNow)
    if (action === 'publish' && activeNow) {
      showAnnouncementPopup(raw.payload)
      announcementUnreadCount.value += 1
      emitAnnouncementUnreadSync(announcementUnreadCount.value)
    } else {
      // 更新/下线或未到生效时间的发布，统一触发一次聚合同步避免本地状态漂移。
      refreshUnreadSummary()
    }
    window.dispatchEvent(new CustomEvent('announcement-realtime-event', { detail: raw.payload }))
  }
}

const connectRealtimeSocket = () => {
  if (!isLoggedIn.value || wsConnection) {
    return
  }
  const url = buildWebSocketUrl()
  if (!url) return

  manualCloseWs = false
  const socket = new WebSocket(url)
  wsConnection = socket

  socket.onopen = () => {
    wsConnected.value = true
    emitRealtimeWsStateSync(true)
    clearWsReconnect()
    refreshAllUnread()
  }

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleRealtimeEvent(data)
    } catch (_) {
      // ignore non-JSON payload
    }
  }

  socket.onclose = (event) => {
    wsConnection = null
    wsConnected.value = false
    emitRealtimeWsStateSync(false)
    if (manualCloseWs || !isLoggedIn.value) return
    clearWsReconnect()
    wsReconnectTimer = window.setTimeout(() => {
      connectRealtimeSocket()
    }, 3000)
  }

  socket.onerror = () => {
    wsConnected.value = false
    emitRealtimeWsStateSync(false)
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close()
    }
  }
}

const handleWindowFocus = () => {
  if (!isLoggedIn.value) return
  refreshAllUnread()
  checkAnnouncementUpdates(false)
  if (!wsConnected.value && !wsConnection) {
    connectRealtimeSocket()
  }
}

const refreshAdminState = async () => {
  if (!isLoggedIn.value) {
    adminPermissions.value = []
    clearAdminCache()
    return
  }

  try {
    const me = await refreshAdminMe()
    adminPermissions.value = Array.isArray(me?.permissions) ? me.permissions : []
  } catch (_) {
    clearAdminCache()
    adminPermissions.value = []
  }
}

watch(theme, (value) => {
  applyTheme(value)
  localStorage.setItem('ui-theme', value)
})

watch(motion, (value) => {
  applyMotion(value)
  localStorage.setItem('ui-motion', value)
})

watch(
  isLoggedIn,
  async (loggedIn) => {
    if (!loggedIn) {
      stopUnreadPolling()
      stopAnnouncementPolling()
      closeRealtimeSocket()
      chatUnreadCount.value = 0
      interactionUnreadCount.value = 0
      systemUnreadCount.value = 0
      announcementUnreadCount.value = 0
      announcementPopupSeenIds.value = new Set()
      clearAdminCache()
      adminPermissions.value = []
      return
    }
    loadAnnouncementPopupSeenIds()
    await refreshAllUnread()
    await refreshAdminState()
    await checkAnnouncementUpdates(true)
    startUnreadPolling()
    startAnnouncementPolling()
    connectRealtimeSocket()
  },
  { immediate: true }
)

if (typeof document !== 'undefined') {
  applyTheme(theme.value)
  applyMotion(motion.value)
}

onMounted(() => {
  reportSiteUvSafely()
  window.addEventListener('auth-state-changed', syncAuthState)
  window.addEventListener('storage', syncAuthState)
  window.addEventListener('admin-state-changed', syncAuthState)
  window.addEventListener('chat-unread-sync', handleChatUnreadSync)
  window.addEventListener('interaction-unread-sync', handleInteractionUnreadSync)
  window.addEventListener('system-unread-sync', handleSystemUnreadSync)
  window.addEventListener('announcement-unread-sync', handleAnnouncementUnreadSync)
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleWindowFocus)
  // 页面挂载时同步一次当前连接态，避免子页面初次进入时拿不到状态。
  emitRealtimeWsStateSync(wsConnected.value)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth-state-changed', syncAuthState)
  window.removeEventListener('storage', syncAuthState)
  window.removeEventListener('admin-state-changed', syncAuthState)
  window.removeEventListener('chat-unread-sync', handleChatUnreadSync)
  window.removeEventListener('interaction-unread-sync', handleInteractionUnreadSync)
  window.removeEventListener('system-unread-sync', handleSystemUnreadSync)
  window.removeEventListener('announcement-unread-sync', handleAnnouncementUnreadSync)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleWindowFocus)
  stopUnreadPolling()
  stopAnnouncementPolling()
  closeRealtimeSocket()
})

const routeTransitionName = computed(() => {
  if (motion.value === 'off') return ''
  return motion.value === 'soft' ? 'fade-slide-soft' : 'fade-slide'
})

const handleLogout = async () => {
  try {
    await logout()
  } catch (_) {
    // ignore
  } finally {
    closeRealtimeSocket()
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('blocked_reason')
    clearAdminCache()
    adminPermissions.value = []
    chatUnreadCount.value = 0
    interactionUnreadCount.value = 0
    systemUnreadCount.value = 0
    announcementUnreadCount.value = 0
    syncAuthState()
    window.dispatchEvent(new Event('auth-state-changed'))
    window.dispatchEvent(new Event('admin-state-changed'))
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<template>
  <div class="layout">
    <header class="app-header">
      <div class="brand" @click="router.push('/')">
        <div class="brand-mark">生</div>
        <div class="brand-text">
          <strong>生活分享</strong>
          <span>记录灵感，连接彼此</span>
        </div>
      </div>

      <div class="header-tools">
        <div class="tool-item">
          <span class="theme-label">配色</span>
          <el-radio-group v-model="theme" size="small" class="theme-switch">
            <el-radio-button label="teal">清新青橙</el-radio-button>
            <el-radio-button label="sunset">暖阳珊瑚</el-radio-button>
          </el-radio-group>
        </div>
        <div class="tool-item">
          <span class="theme-label">动效</span>
          <el-radio-group v-model="motion" size="small" class="theme-switch">
            <el-radio-button label="standard">标准</el-radio-button>
            <el-radio-button label="soft">柔和</el-radio-button>
            <el-radio-button label="off">关闭</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 导航保持单行紧凑排列，菜单过长时仅自身横向滚动，不与其他区域重叠。 -->
      <el-menu class="top-nav" mode="horizontal" :default-active="activePath" :ellipsis="false" router>
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item v-if="isLoggedIn" index="/publish">
          <el-icon><Plus /></el-icon>
          <span>发布</span>
        </el-menu-item>

        <el-menu-item v-if="isLoggedIn" index="/profile">
          <el-icon><UserFilled /></el-icon>
          <span>{{ displayName }}</span>
        </el-menu-item>

        <el-menu-item v-if="isLoggedIn" index="/notifications">
          <el-badge :value="totalUnreadCount" :max="99" :hidden="totalUnreadCount <= 0" class="message-menu-badge">
            <span class="message-menu-label">
              <el-icon><Bell /></el-icon>
              <span>消息</span>
            </span>
          </el-badge>
        </el-menu-item>

        <el-menu-item v-if="isLoggedIn" index="/my-reports">
          <el-icon><Warning /></el-icon>
          <span>举报</span>
        </el-menu-item>

        <el-menu-item v-if="isLoggedIn" index="/chat">
          <el-badge :value="chatUnreadCount" :max="99" :hidden="chatUnreadCount <= 0" class="chat-menu-badge">
            <span class="chat-menu-label">
              <el-icon><ChatDotRound /></el-icon>
              <span>私聊</span>
            </span>
          </el-badge>
        </el-menu-item>

        <el-menu-item v-if="!isLoggedIn" index="/login">
          <span>登录</span>
        </el-menu-item>

        <!-- 次级入口折叠到“更多”菜单，缓解主导航拥挤。 -->
        <el-sub-menu index="more">
          <template #title>
            <span class="more-title">...</span>
          </template>

          <el-menu-item index="/ai-brief">
            <el-icon><Compass /></el-icon>
            <span>AI快讯</span>
          </el-menu-item>

          <el-menu-item v-if="isLoggedIn && hasAdminEntrance" index="/admin">
            <el-icon><Setting /></el-icon>
            <span>管理</span>
          </el-menu-item>

          <el-menu-item index="/about">
            <el-icon><InfoFilled /></el-icon>
            <span>关于</span>
          </el-menu-item>

          <el-menu-item v-if="isLoggedIn" index="/login" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>登出</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </header>

    <main class="page-container">
      <router-view v-slot="{ Component, route: currentRoute }">
        <transition :name="routeTransitionName" mode="out-in">
          <component :is="Component" :key="currentRoute.path.startsWith('/chat') ? currentRoute.path : currentRoute.fullPath" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  padding: 18px 20px 26px;
}

.app-header {
  position: relative;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 auto 20px;
  max-width: 1240px;
  padding: 12px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 32px rgba(22, 42, 62, 0.1);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  min-width: 184px;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  flex-shrink: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
  max-width: 360px;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  white-space: nowrap;
}

.theme-label {
  font-size: 12px;
  color: #66788f;
}

.theme-switch :deep(.el-radio-button__inner) {
  border-radius: 9px !important;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #0ea5a4, #f97316);
  box-shadow: 0 10px 20px rgba(14, 165, 164, 0.25);
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-text strong {
  font-size: 16px;
  line-height: 1.1;
}

.brand-text span {
  font-size: 12px;
  color: #6f7f94;
}

.top-nav {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  justify-content: flex-end;
  position: relative;
  border-bottom: 0;
  background: transparent;
}

:deep(.el-menu--horizontal) {
  display: flex;
  flex-wrap: nowrap;
  justify-content: flex-end;
  align-items: center;
  gap: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
  width: 100%;
}

:deep(.el-menu-item) {
  border-radius: 10px;
  margin-left: 4px;
  color: #44556d;
  min-width: 0;
  padding: 0 12px;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: 0;
  color: #0c8c8b;
  background: rgba(14, 165, 164, 0.12);
}

/* 让“...”子菜单与普通菜单项保持一致，避免默认下划线样式突兀。 */
:deep(.el-menu--horizontal > .el-sub-menu) {
  margin-left: 4px;
}

:deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
  border-bottom: 0 !important;
  border-radius: 10px;
  color: #44556d;
  padding: 0 12px;
}

:deep(.el-menu--horizontal > .el-sub-menu:hover .el-sub-menu__title),
:deep(.el-menu--horizontal > .el-sub-menu.is-opened .el-sub-menu__title),
:deep(.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title) {
  border-bottom: 0 !important;
  color: #0c8c8b;
  background: rgba(14, 165, 164, 0.12);
}

.message-menu-badge {
  display: inline-flex;
  align-items: center;
}

.message-menu-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.chat-menu-badge {
  display: inline-flex;
  align-items: center;
}

.chat-menu-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.more-title {
  font-weight: 700;
  letter-spacing: 1px;
}

:deep(.el-sub-menu .el-sub-menu__icon-arrow) {
  display: none;
}

.page-container {
  padding: 16px;
  max-width: 1240px;
  margin: 0 auto;
}

:deep(.fade-slide-enter-active),
:deep(.fade-slide-leave-active) {
  transition: opacity 0.26s ease, transform 0.28s ease;
}

:deep(.fade-slide-enter-from),
:deep(.fade-slide-leave-to) {
  opacity: 0;
  transform: translateY(10px);
}

:deep(.fade-slide-soft-enter-active),
:deep(.fade-slide-soft-leave-active) {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

:deep(.fade-slide-soft-enter-from),
:deep(.fade-slide-soft-leave-to) {
  opacity: 0;
  transform: translateY(5px);
}

@media (max-width: 820px) {
  .layout {
    padding: 12px 10px 20px;
  }

  .app-header {
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px;
  }

  .header-tools {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    flex-wrap: nowrap;
    overflow-x: auto;
    max-width: none;
  }

  .top-nav {
    order: 2;
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.el-menu--horizontal) {
    justify-content: flex-start;
  }

  :deep(.el-menu-item) {
    margin-left: 0;
    margin-right: 4px;
    padding: 0 12px;
  }

  :deep(.el-menu--horizontal > .el-sub-menu) {
    margin-left: 0;
    margin-right: 4px;
  }

  .page-container {
    padding: 8px 4px;
  }
}
</style>


