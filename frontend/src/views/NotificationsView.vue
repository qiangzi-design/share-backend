<template>
  <section class="notifications-page">
    <el-card class="notifications-card motion-fade-up">
      <div class="card-head">
        <div>
          <h2>消息通知</h2>
          <p>点赞、评论、收藏、公告和私聊动态都会在这里展示。</p>
        </div>
        <el-button
          type="primary"
          :disabled="allUnreadCount <= 0"
          :loading="markingAllRead"
          @click="markAllReadOnce"
        >
          全部已读
        </el-button>
      </div>

      <div class="summary">
        <span>未读互动：{{ unreadCount }}</span>
        <span>系统未读：{{ systemUnreadCount }}</span>
        <span>私聊未读：{{ chatUnreadCount }}</span>
        <span>公告未读：{{ announcementUnreadCount }}</span>
        <span>总未读：{{ allUnreadCount }}</span>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="互动通知" name="interaction">
          <el-empty v-if="!loading && list.length === 0" description="暂无互动通知" />

          <div v-else class="notification-list">
            <article
              v-for="item in list"
              :key="item.id"
              class="notification-item"
              :class="{ unread: !item.isRead }"
            >
              <UserAvatar :size="42" :src="normalizeFileUrl(item.actorAvatar || '')" :fallback-text="getActorName(item)" />
              <div class="notification-main">
                <div class="title-row">
                  <h3>{{ item.title || '互动通知' }}</h3>
                  <div class="meta-right">
                    <span class="read-status" :class="{ unread: !item.isRead }">{{ item.isRead ? '已读' : '未读' }}</span>
                    <span class="time">{{ formatTime(item.createTime) }}</span>
                  </div>
                </div>
                <p class="body">{{ item.body }}</p>
                <p v-if="item.contentTitle" class="content-title">关联内容：{{ item.contentTitle }}</p>
              </div>
              <div class="item-actions">
                <el-button
                  v-if="!item.isRead"
                  text
                  type="primary"
                  @click="markInteractionItemRead(item)"
                >
                  标记已读
                </el-button>
                <el-button
                  v-if="item.contentId"
                  text
                  @click="openNotification(item)"
                >
                  查看详情
                </el-button>
              </div>
              <span v-if="!item.isRead" class="dot" />
            </article>
          </div>

          <div v-if="list.length > 0" class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :current-page="page"
              :page-size="pageSize"
              :total="total"
              @current-change="loadList"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统通知" name="system">
          <el-empty v-if="!systemLoading && systemList.length === 0" description="暂无系统通知" />

          <div v-else class="notification-list">
            <article
              v-for="item in systemList"
              :key="`sys-${item.id}`"
              class="notification-item"
              :class="{ unread: !item.isRead }"
            >
              <div class="notification-main">
                <div class="title-row">
                  <h3>{{ item.title || '系统通知' }}</h3>
                  <div class="meta-right">
                    <span class="read-status" :class="{ unread: !item.isRead }">{{ item.isRead ? '已读' : '未读' }}</span>
                    <span class="time">{{ formatTime(item.createTime) }}</span>
                  </div>
                </div>
                <p class="body">{{ item.body }}</p>
                <p v-if="item.contentTitle" class="content-title">关联内容：{{ item.contentTitle }}</p>
              </div>
              <div class="item-actions">
                <el-button
                  v-if="!item.isRead"
                  text
                  type="primary"
                  @click="markSystemItemRead(item)"
                >
                  标记已读
                </el-button>
                <el-button
                  v-if="item.contentId"
                  text
                  @click="openNotification(item)"
                >
                  查看详情
                </el-button>
              </div>
              <span v-if="!item.isRead" class="dot" />
            </article>
          </div>

          <div v-if="systemList.length > 0" class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :current-page="systemPage"
              :page-size="systemPageSize"
              :total="systemTotal"
              @current-change="loadSystemList"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="公告消息" name="announcement">
          <el-empty v-if="!announcementLoading && announcementList.length === 0" description="暂无公告" />

          <div v-else class="notification-list">
            <article
              v-for="item in announcementList"
              :key="`ann-${item.id}`"
              class="notification-item"
              :class="{ unread: !item.isRead }"
            >
              <div class="notification-main">
                <div class="title-row">
                  <h3>
                    <el-tag v-if="item.isPinned" size="small" type="danger" effect="light" class="pin-tag">置顶</el-tag>
                    {{ item.title || '公告' }}
                  </h3>
                  <div class="meta-right">
                    <span class="read-status" :class="{ unread: !item.isRead }">{{ item.isRead ? '已读' : '未读' }}</span>
                    <span class="time">{{ formatTime(item.publishTime || item.createTime) }}</span>
                  </div>
                </div>
                <p class="body">{{ item.body }}</p>
              </div>
              <el-button
                v-if="!item.isRead"
                text
                type="primary"
                class="ann-read-btn"
                @click="markAnnouncementItemRead(item)"
              >
                标记已读
              </el-button>
            </article>
          </div>

          <div v-if="announcementList.length > 0" class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :current-page="announcementPage"
              :page-size="announcementPageSize"
              :total="announcementTotal"
              @current-change="loadAnnouncementList"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '../components/UserAvatar.vue'
import { getNotificationList, getUnreadSummary, markAllNotificationsRead, markNotificationRead } from '../api/notification'
import {
  getAnnouncementList,
  markAnnouncementRead,
  markAllAnnouncementsRead
} from '../api/announcement'

/**
 * 消息中心职责：
 * 1. 聚合互动通知、公告通知与私聊未读数，提供统一已读入口。
 * 2. 通过自定义事件与顶部角标同步，避免不同页面统计口径漂移。
 * 3. 支持实时事件插入首屏列表，减少用户等待下一次手动刷新。
 */
const router = useRouter()

const activeTab = ref('interaction')
const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const unreadCount = ref(0)
const systemUnreadCount = ref(0)
const chatUnreadCount = ref(0)
const markingAllRead = ref(false)

const systemLoading = ref(false)
const systemList = ref([])
const systemPage = ref(1)
const systemPageSize = ref(20)
const systemTotal = ref(0)

const announcementLoading = ref(false)
const announcementList = ref([])
const announcementPage = ref(1)
const announcementPageSize = ref(10)
const announcementTotal = ref(0)
const announcementUnreadCount = ref(0)

const allUnreadCount = computed(() => unreadCount.value + systemUnreadCount.value + chatUnreadCount.value + announcementUnreadCount.value)

// 公告列表的置顶排序与发布时间排序由前端再次兜底，防止接口顺序变化影响展示。
const sortAnnouncementList = (rows) =>
  [...rows].sort((a, b) => {
    const pinnedDiff = Number(Boolean(b?.isPinned)) - Number(Boolean(a?.isPinned))
    if (pinnedDiff !== 0) return pinnedDiff
    const timeA = new Date(a?.publishTime || a?.createTime || 0).getTime() || 0
    const timeB = new Date(b?.publishTime || b?.createTime || 0).getTime() || 0
    return timeB - timeA
  })

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const getActorName = (item) => {
  if (!item) return '用户'
  return item.actorNickname || item.actorUsername || `用户${item.actorId || ''}`
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN')
}

const setUnreadLocal = (count) => {
  unreadCount.value = Number(count || 0)
}

const setSystemUnreadLocal = (count) => {
  systemUnreadCount.value = Number(count || 0)
}

const setAnnouncementUnreadLocal = (count) => {
  announcementUnreadCount.value = Number(count || 0)
}

const setChatUnreadLocal = (count) => {
  chatUnreadCount.value = Number(count || 0)
}

// 互动未读同步到全局角标。
const syncUnread = (count) => {
  setUnreadLocal(count)
  window.dispatchEvent(
    new CustomEvent('interaction-unread-sync', {
      detail: { unreadCount: unreadCount.value }
    })
  )
}

// 系统未读同步到全局角标，管理动作通知单独归档在“系统通知”页签。
const syncSystemUnread = (count) => {
  setSystemUnreadLocal(count)
  window.dispatchEvent(
    new CustomEvent('system-unread-sync', {
      detail: { unreadCount: systemUnreadCount.value }
    })
  )
}

// 公告未读同步到全局角标。
const syncAnnouncementUnread = (count) => {
  setAnnouncementUnreadLocal(count)
  window.dispatchEvent(
    new CustomEvent('announcement-unread-sync', {
      detail: { unreadCount: announcementUnreadCount.value }
    })
  )
}

// 私聊未读同步到全局角标（消息页只负责展示，不直接修改私聊业务数据）。
const syncChatUnread = (count) => {
  setChatUnreadLocal(count)
  window.dispatchEvent(
    new CustomEvent('chat-unread-sync', {
      detail: { unreadCount: chatUnreadCount.value }
    })
  )
}

const loadUnreadSummary = async () => {
  try {
    // 聚合接口一次返回三类未读，避免消息页初始化时额外发起多次请求。
    const response = await getUnreadSummary()
    if (response.data?.code === 200 && response.data?.data) {
      const summary = response.data.data
      syncUnread(summary.interactionUnreadCount)
      syncSystemUnread(summary.systemUnreadCount)
      syncChatUnread(summary.chatUnreadCount)
      syncAnnouncementUnread(summary.announcementUnreadCount)
    }
  } catch (_) {
    // ignore
  }
}

const loadAnnouncementList = async (targetPage = announcementPage.value) => {
  announcementLoading.value = true
  try {
    const response = await getAnnouncementList({
      page: targetPage,
      pageSize: announcementPageSize.value
    })
    const data = response.data?.data || {}
    announcementList.value = sortAnnouncementList(data.list || [])
    announcementPage.value = data.page || targetPage
    announcementPageSize.value = data.pageSize || announcementPageSize.value
    announcementTotal.value = Number(data.total || 0)
  } catch (_) {
    ElMessage.error('加载公告失败')
  } finally {
    announcementLoading.value = false
  }
}

const loadList = async (targetPage = page.value) => {
  loading.value = true
  try {
    const response = await getNotificationList({
      page: targetPage,
      pageSize: pageSize.value,
      category: 'interaction'
    })
    const data = response.data?.data || {}
    list.value = data.list || []
    page.value = data.page || targetPage
    pageSize.value = data.pageSize || pageSize.value
    total.value = Number(data.total || 0)
  } catch (_) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const loadSystemList = async (targetPage = systemPage.value) => {
  systemLoading.value = true
  try {
    const response = await getNotificationList({
      page: targetPage,
      pageSize: systemPageSize.value,
      category: 'system'
    })
    const data = response.data?.data || {}
    systemList.value = data.list || []
    systemPage.value = data.page || targetPage
    systemPageSize.value = data.pageSize || systemPageSize.value
    systemTotal.value = Number(data.total || 0)
  } catch (_) {
    ElMessage.error('加载系统通知失败')
  } finally {
    systemLoading.value = false
  }
}

const markReadIfNeeded = async (item) => {
  if (!item || item.isRead) return
  try {
    const response = await markNotificationRead(item.id)
    if (response.data?.code === 200 && response.data?.data) {
      item.isRead = true
      // 单条已读后统一回拉未读汇总，保证互动/系统两类计数同步准确。
      await loadUnreadSummary()
    }
  } catch (_) {
    ElMessage.error('标记已读失败')
  }
}

const markInteractionItemRead = async (item) => {
  await markReadIfNeeded(item)
}

const markSystemItemRead = async (item) => {
  await markReadIfNeeded(item)
}

const openNotification = async (item) => {
  await markReadIfNeeded(item)
  if (item.contentId) {
    router.push(`/content/${item.contentId}`)
  }
}

const markAnnouncementItemRead = async (item) => {
  if (!item || item.isRead) return
  try {
    const response = await markAnnouncementRead(item.id)
    item.isRead = true
    const nextUnread = response.data?.data?.unreadCount
    if (nextUnread !== undefined) {
      syncAnnouncementUnread(nextUnread)
    } else {
      syncAnnouncementUnread(Math.max(0, announcementUnreadCount.value - 1))
    }
  } catch (_) {
    ElMessage.error('标记已读失败')
  }
}

// 一键已读会分别调用互动和公告接口，再统一回填本地列表状态。
const markAllReadOnce = async () => {
  if (allUnreadCount.value <= 0) {
    ElMessage.info('当前没有未读消息')
    return
  }
  markingAllRead.value = true
  try {
    let interactionResult = null
    let announcementResult = null
    if (unreadCount.value > 0) {
      interactionResult = await markAllNotificationsRead()
    }
    if (announcementUnreadCount.value > 0) {
      announcementResult = await markAllAnnouncementsRead()
    }

    if (interactionResult?.data?.data?.unreadCount !== undefined) {
      // read-all 接口返回的是通知总未读，这里统一回拉汇总，避免互动/系统拆分后口径不准。
      await loadUnreadSummary()
    }
    if (announcementResult?.data?.data?.unreadCount !== undefined) {
      syncAnnouncementUnread(announcementResult.data.data.unreadCount)
    }

    list.value = list.value.map((item) => ({ ...item, isRead: true }))
    systemList.value = systemList.value.map((item) => ({ ...item, isRead: true }))
    announcementList.value = announcementList.value.map((item) => ({ ...item, isRead: true }))
    ElMessage.success('已全部标记为已读')
  } catch (_) {
    ElMessage.error('一键已读失败')
  } finally {
    markingAllRead.value = false
  }
}

// 实时互动通知到达后：先更新未读计数，再尝试把通知插入第一页列表。
const handleInteractionRealtimeEvent = (event) => {
  const payload = event?.detail || {}
  const isSystemNotice = String(payload.type || '').toLowerCase() === 'system_notice'
  if (isSystemNotice) {
    syncSystemUnread(systemUnreadCount.value + 1)
  } else if (Number.isFinite(Number(payload.unreadCount))) {
    syncUnread(payload.unreadCount)
  } else {
    syncUnread(unreadCount.value + 1)
  }

  if (!payload.notificationId) {
    if (isSystemNotice) {
      loadSystemList(1)
    } else {
      loadList(1)
    }
    return
  }

  if (isSystemNotice && systemPage.value !== 1) {
    return
  }
  if (!isSystemNotice && page.value !== 1) {
    return
  }

  const nextItem = {
    id: payload.notificationId,
    actorId: payload.actorId,
    actorUsername: payload.actorUsername,
    actorNickname: payload.actorNickname,
    actorAvatar: payload.actorAvatar,
    contentId: payload.contentId,
    contentTitle: payload.contentTitle,
    type: payload.type,
    title: payload.title,
    body: payload.body,
    createTime: payload.createTime,
    isRead: false
  }

  if (isSystemNotice) {
    systemList.value = [nextItem, ...systemList.value].slice(0, systemPageSize.value)
    systemTotal.value += 1
    return
  }

  list.value = [nextItem, ...list.value].slice(0, pageSize.value)
  total.value += 1
}

// 私聊未读更新仍以私聊接口为准，这里仅更新数字。
const handleChatRealtimeEvent = (event) => {
  const payload = event?.detail || {}
  const unread = Number(payload.unreadCount)
  if (Number.isFinite(unread) && unread >= 0) {
    syncChatUnread(unread)
    return
  }
  loadUnreadSummary()
}

const handleAnnouncementRealtimeEvent = async (event) => {
  const payload = event?.detail || {}
  if (Number.isFinite(Number(payload.unreadCount))) {
    syncAnnouncementUnread(payload.unreadCount)
  } else {
    await loadUnreadSummary()
  }
  if (activeTab.value === 'announcement') {
    await loadAnnouncementList(1)
  }
}

const handleInteractionUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (!Number.isFinite(incoming) || incoming < 0) return
  const previous = Number(unreadCount.value || 0)
  setUnreadLocal(incoming)
  if (incoming > previous) {
    loadList(1)
  }
}

const handleSystemUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (!Number.isFinite(incoming) || incoming < 0) return
  const previous = Number(systemUnreadCount.value || 0)
  setSystemUnreadLocal(incoming)
  if (incoming > previous) {
    loadSystemList(1)
  }
}

const handleAnnouncementUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (!Number.isFinite(incoming) || incoming < 0) return
  const previous = Number(announcementUnreadCount.value || 0)
  setAnnouncementUnreadLocal(incoming)
  if (incoming > previous) {
    loadAnnouncementList(1)
  }
}

const handleChatUnreadSync = (event) => {
  const incoming = Number(event?.detail?.unreadCount)
  if (!Number.isFinite(incoming) || incoming < 0) return
  setChatUnreadLocal(incoming)
}

watch(activeTab, async (tabName) => {
  // 切换页签时主动拉取对应列表，确保实时事件到达后的分页数据始终一致。
  if (tabName === 'system') {
    await loadSystemList(1)
    return
  }
  if (tabName === 'announcement') {
    await loadAnnouncementList(1)
  }
})

onMounted(async () => {
  window.addEventListener('interaction-realtime-event', handleInteractionRealtimeEvent)
  window.addEventListener('chat-realtime-event', handleChatRealtimeEvent)
  window.addEventListener('announcement-realtime-event', handleAnnouncementRealtimeEvent)
  window.addEventListener('interaction-unread-sync', handleInteractionUnreadSync)
  window.addEventListener('system-unread-sync', handleSystemUnreadSync)
  window.addEventListener('announcement-unread-sync', handleAnnouncementUnreadSync)
  window.addEventListener('chat-unread-sync', handleChatUnreadSync)
  await Promise.all([
    loadUnreadSummary(),
    loadList(1),
    loadSystemList(1),
    loadAnnouncementList(1)
  ])
})

onBeforeUnmount(() => {
  window.removeEventListener('interaction-realtime-event', handleInteractionRealtimeEvent)
  window.removeEventListener('chat-realtime-event', handleChatRealtimeEvent)
  window.removeEventListener('announcement-realtime-event', handleAnnouncementRealtimeEvent)
  window.removeEventListener('interaction-unread-sync', handleInteractionUnreadSync)
  window.removeEventListener('system-unread-sync', handleSystemUnreadSync)
  window.removeEventListener('announcement-unread-sync', handleAnnouncementUnreadSync)
  window.removeEventListener('chat-unread-sync', handleChatUnreadSync)
})
</script>

<style scoped>
.notifications-page {
  display: flex;
  flex-direction: column;
}

.notifications-card {
  border-radius: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.card-head h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.card-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  color: #0f766e;
  font-weight: 600;
  margin-bottom: 12px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  gap: 12px;
  align-items: flex-start;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.notification-item.unread {
  border-color: rgba(14, 165, 164, 0.55);
  background: rgba(240, 253, 250, 0.9);
}

.notification-main {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.meta-right {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.title-row h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pin-tag {
  flex-shrink: 0;
}

.time {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.read-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.5);
  color: #64748b;
  font-size: 12px;
  min-width: 44px;
  padding: 2px 8px;
  line-height: 1.3;
}

.read-status.unread {
  border-color: rgba(245, 108, 108, 0.6);
  color: #ef4444;
  background: rgba(254, 226, 226, 0.55);
}

.body {
  margin: 6px 0 0;
  color: #334155;
  line-height: 1.6;
}

.content-title {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #f43f5e;
  margin-top: 8px;
  flex-shrink: 0;
}

.item-actions {
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
  justify-content: center;
}

.ann-read-btn {
  align-self: center;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
