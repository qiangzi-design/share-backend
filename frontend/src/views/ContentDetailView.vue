<template>
  <section class="detail-page">
    <el-card v-if="content" class="content-card motion-fade-up">
      <template #header>
        <div class="header-row">
          <h2>{{ content.title }}</h2>
          <span class="time-text">{{ formatTime(content.createTime) }}</span>
        </div>
      </template>

      <div class="author-row">
        <button class="author-main author-profile-link" type="button" @click="goToAuthorProfile">
          <UserAvatar :size="44" :src="normalizeFileUrl(author?.avatar || '')" :fallback-text="getAuthorDisplayName()" />
          <div class="author-meta">
            <strong>{{ getAuthorDisplayName() }}</strong>
            <span>粉丝 {{ followState.followerCount }}</span>
          </div>
        </button>
        <el-button
          v-if="canFollowAuthor"
          size="small"
          :type="followState.isFollowing ? 'default' : 'primary'"
          :loading="followState.loading"
          @click="handleToggleFollow"
        >
          {{ followState.isFollowing ? '已关注' : '关注作者' }}
        </el-button>
        <el-button
          v-if="canChatWithAuthor"
          size="small"
          @click="goChatWithAuthor"
        >
          私聊
        </el-button>
        <el-button
          v-if="canFollowAuthor"
          size="small"
          type="danger"
          plain
          @click="handleReportAuthor"
        >
          举报用户
        </el-button>
      </div>

      <div class="content-html" v-html="content.content"></div>

      <div v-if="content.categoryName || parseTags(content.tags).length" class="detail-meta">
        <el-tag v-if="content.categoryName" effect="dark" type="success">{{ content.categoryName }}</el-tag>
        <el-tag v-for="tag in parseTags(content.tags)" :key="`tag-${tag}`" type="info">{{ tag }}</el-tag>
      </div>

      <div v-if="parseMediaList(content.images).length" class="image-grid">
        <el-image
          v-for="(image, idx) in parseMediaList(content.images)"
          :key="idx"
          :src="image"
          fit="cover"
          class="detail-image"
          :preview-src-list="parseMediaList(content.images)"
          :initial-index="idx"
        />
      </div>

      <div v-if="parseMediaList(content.videos).length" class="video-grid">
        <video
          v-for="(video, idx) in parseMediaList(content.videos)"
          :key="`video-${idx}`"
          :src="normalizeFileUrl(video)"
          :poster="videoPosterMap[normalizeFileUrl(video)] || ''"
          class="detail-video"
          controls
          preload="metadata"
        />
      </div>

      <div class="action-row">
        <el-button :type="contentLike.isLiked ? 'danger' : 'default'" @click="handleToggleContentLike">
          {{ contentLike.likeCount ?? content.likeCount ?? 0 }} 点赞
        </el-button>
        <el-button :type="contentCollection.isCollected ? 'warning' : 'default'" @click="handleToggleCollection">
          {{ contentCollection.collectionCount ?? content.collectionCount ?? 0 }} 收藏
        </el-button>
        <el-button v-if="canReportContent" @click="handleReportContent">举报</el-button>
        <span class="meta-text">浏览 {{ content.viewCount || 0 }}</span>
        <span class="meta-text">评论 {{ content.commentCount || 0 }}</span>
        <span v-if="(content.images || '').trim() && Number(content.imageSize || 0) > 0" class="meta-text">
          图片大小{{ formatBytes(content.imageSize) }}
        </span>
      </div>
    </el-card>

    <el-card class="comment-card motion-fade-soft">
      <template #header>
        <div class="header-row">
          <span>评论区</span>
          <span class="time-text">共 {{ comments.total }} 条</span>
        </div>
      </template>

      <div class="comment-form">
        <el-input
          v-model="commentInput"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          :placeholder="replyToCommentId ? `回复 @${replyToName || '用户'}...` : '写下你的评论...'"
        />
        <div class="comment-actions">
          <EmojiPicker @select="appendCommentEmoji" />
          <el-button v-if="replyToCommentId" @click="cancelReply">取消回复</el-button>
          <el-button type="primary" @click="submitComment">提交</el-button>
        </div>
      </div>

      <el-empty v-if="comments.list.length === 0" description="暂无评论" />

      <div v-for="comment in comments.list" :key="comment.id" class="comment-item">
        <div class="comment-main">
          <div class="comment-user">
            <UserAvatar :size="34" :src="normalizeFileUrl(comment.avatar || '')" :fallback-text="getDisplayName(comment)" />
            <div class="user-meta">
              <strong>{{ getDisplayName(comment) }}</strong>
              <span>{{ formatTime(comment.createTime) }}</span>
            </div>
          </div>
          <p class="comment-content">{{ comment.commentContent }}</p>
          <div class="comment-meta">
            <el-button class="comment-action-btn reply" link @click="startReply(comment.id, comment)">
              回复
            </el-button>
            <el-button
              class="comment-action-btn"
              :class="{ liked: commentLikeStatus[comment.id]?.isLiked }"
              link
              @click="toggleLike(comment.id)"
            >
              点赞 {{ commentLikeStatus[comment.id]?.likeCount ?? comment.likeCount ?? 0 }}
            </el-button>
            <el-button
              v-if="canReportComment(comment)"
              class="comment-action-btn danger"
              link
              @click="handleReportComment(comment)"
            >
              举报
            </el-button>
          </div>
        </div>

        <div v-if="replies[comment.id]?.length" class="reply-list">
          <div v-for="reply in replies[comment.id]" :key="reply.id" class="reply-item">
            <div class="comment-user">
              <UserAvatar :size="30" :src="normalizeFileUrl(reply.avatar || '')" :fallback-text="getDisplayName(reply)" />
              <div class="user-meta">
                <strong>{{ getDisplayName(reply) }}</strong>
                <span>{{ formatTime(reply.createTime) }}</span>
              </div>
            </div>
            <p class="comment-content">{{ reply.commentContent }}</p>
            <div class="comment-meta">
              <el-button
                class="comment-action-btn"
                :class="{ liked: commentLikeStatus[reply.id]?.isLiked }"
                link
                @click="toggleLike(reply.id)"
              >
                点赞 {{ commentLikeStatus[reply.id]?.likeCount ?? reply.likeCount ?? 0 }}
              </el-button>
              <el-button
                v-if="canReportComment(reply)"
                class="comment-action-btn danger"
                link
                @click="handleReportComment(reply)"
              >
                举报
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, ->, total"
          :current-page="comments.page"
          :page-size="comments.pageSize"
          :total="comments.total"
          @current-change="loadComments"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="reportDialog.visible"
      :title="reportDialog.title"
      width="520px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="违规模板">
          <el-select
            v-model="reportDialog.templateCode"
            placeholder="请选择举报模板（可选）"
            clearable
            filterable
          >
            <el-option
              v-for="item in reportTemplates"
              :key="item.code"
              :label="item.label"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="补充描述">
          <el-input
            v-model="reportDialog.customReason"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 6 }"
            maxlength="500"
            show-word-limit
            placeholder="可补充具体情况；如不选模板，请至少填写这里"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reportDialog.submitting" @click="submitReport">
          提交举报
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmojiPicker from '../components/EmojiPicker.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { getContentDetail, reportContentView } from '../api/content'
import { createComment, getCommentLikeStatus, getCommentList, getCommentReplies, toggleCommentLike } from '../api/comment'
import { getContentCollectionStatus, toggleContentCollection } from '../api/collection'
import { getContentLikeStatus, toggleContentLike } from '../api/like'
import { getFollowStatus, getFollowerCount, toggleFollowUser } from '../api/follow'
import { createReport, getReportTemplates } from '../api/report'
import { getPublicUserProfile } from '../api/user'
import { extractVideoPoster } from '../utils/media'

/**
 * 内容详情页职责：
 * 1. 展示内容全文、媒体与作者信息，并承载点赞/收藏/关注主互动。
 * 2. 管理评论树（评论/回复/评论点赞）与举报入口。
 * 3. 执行浏览计数上报（延时 + 可见性判断），降低无效浏览噪声。
 */
const route = useRoute()
const router = useRouter()

const content = ref(null)
const author = ref(null)
const contentLike = reactive({
  isLiked: false,
  likeCount: 0
})
const contentCollection = reactive({
  isCollected: false,
  collectionCount: 0
})
const followState = reactive({
  isFollowing: false,
  followerCount: 0,
  loading: false
})

const commentInput = ref('')
const replyToCommentId = ref(null)
const replyToName = ref('')
const replies = reactive({})
const commentLikeStatus = reactive({})
const videoPosterMap = ref({})
let viewReportTimer = null
let hasReportedView = false

const comments = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  total: 0
})

const reportTemplates = ref([])
const reportDialog = reactive({
  visible: false,
  title: '举报',
  targetType: '',
  targetId: null,
  templateCode: '',
  customReason: '',
  submitting: false
})

const currentUserId = computed(() => Number(sessionStorage.getItem('userId') || 0))
const canFollowAuthor = computed(() => {
  if (!content.value?.userId) return false
  if (!sessionStorage.getItem('token')) return false
  return Number(content.value.userId) !== currentUserId.value
})
const canChatWithAuthor = computed(() => canFollowAuthor.value && followState.isFollowing)
const canReportContent = computed(() => {
  if (!sessionStorage.getItem('token')) return false
  if (!content.value?.id || !content.value?.userId) return false
  return Number(content.value.userId) !== currentUserId.value
})

const canReportComment = (comment) => {
  if (!sessionStorage.getItem('token')) return false
  if (!comment?.id || !comment?.userId) return false
  return Number(comment.userId) !== currentUserId.value
}

// 详情页主数据失败直接回首页，避免停留在无效路由状态。
const loadDetail = async () => {
  try {
    const response = await getContentDetail(route.params.id)
    if (response.data?.code !== 200) {
      throw new Error(response.data?.message || '加载内容详情失败')
    }
    content.value = response.data.data
  } catch (_) {
    ElMessage.error('加载内容详情失败')
    router.push('/')
  }
}

// 浏览上报采用“一次会话只计一次”规则，避免短时重复刷新叠加浏览。
const reportView = async () => {
  if (!content.value?.id || hasReportedView) return
  try {
    const response = await reportContentView(content.value.id)
    if (response.data?.code !== 200) return
    hasReportedView = true
    const nextCount = Number(response.data.data?.viewCount)
    if (!Number.isNaN(nextCount) && content.value) {
      content.value.viewCount = nextCount
    }
  } catch (_) {
    // ignore
  }
}

// 进入详情后延迟 3 秒再计数，过滤快速误触进入。
const scheduleViewReport = () => {
  if (viewReportTimer) {
    window.clearTimeout(viewReportTimer)
  }
  viewReportTimer = window.setTimeout(() => {
    if (document.visibilityState !== 'visible') {
      return
    }
    reportView()
  }, 3000)
}

const loadAuthorProfile = async () => {
  if (!content.value?.userId) return
  try {
    const response = await getPublicUserProfile(content.value.userId)
    if (response.data?.code !== 200) {
      return
    }
    author.value = response.data.data
    followState.followerCount = Number(response.data.data?.followerCount || 0)
  } catch (_) {
    // ignore
  }
}

const loadContentLike = async () => {
  if (!sessionStorage.getItem('token')) return
  try {
    const response = await getContentLikeStatus(route.params.id)
    if (response.data?.code !== 200) return
    contentLike.isLiked = response.data.data.isLiked
    contentLike.likeCount = response.data.data.likeCount
  } catch (_) {
    // ignore
  }
}

const loadContentCollection = async () => {
  if (!sessionStorage.getItem('token')) return
  try {
    const response = await getContentCollectionStatus(route.params.id)
    if (response.data?.code !== 200) return
    contentCollection.isCollected = response.data.data.isCollected
    contentCollection.collectionCount = response.data.data.collectionCount
  } catch (_) {
    // ignore
  }
}

const loadFollowState = async () => {
  if (!content.value?.userId) return

  try {
    const countResp = await getFollowerCount(content.value.userId)
    if (countResp.data?.code === 200) {
      followState.followerCount = Number(countResp.data.data || 0)
    }
  } catch (_) {
    // ignore
  }

  if (!sessionStorage.getItem('token') || !canFollowAuthor.value) {
    return
  }

  try {
    const response = await getFollowStatus(content.value.userId)
    if (response.data?.code !== 200) return
    followState.isFollowing = response.data.data.isFollowing
    followState.followerCount = Number(response.data.data.followerCount || followState.followerCount)
  } catch (_) {
    // ignore
  }
}

/**
 * 评论列表加载策略：
 * - 先拉主评论分页，再按评论拉回复；
 * - 评论点赞状态只在登录时补齐，避免匿名请求过多。
 */
const loadComments = async (targetPage = 1) => {
  try {
    const response = await getCommentList(route.params.id, targetPage, comments.pageSize)
    const data = response.data.data || {}
    comments.list = data.comments || []
    comments.page = data.page || targetPage
    comments.pageSize = data.pageSize || comments.pageSize
    comments.total = data.total || 0
    if (content.value) {
      content.value.commentCount = comments.total
    }

    for (const comment of comments.list) {
      await loadReplies(comment.id)
      await loadCommentLike(comment.id)
    }
  } catch (_) {
    ElMessage.error('加载评论失败')
  }
}

const loadReplies = async (commentId) => {
  try {
    const response = await getCommentReplies(commentId)
    replies[commentId] = response.data.data || []
    for (const reply of replies[commentId]) {
      await loadCommentLike(reply.id)
    }
  } catch (_) {
    replies[commentId] = []
  }
}

const loadCommentLike = async (commentId) => {
  if (!sessionStorage.getItem('token')) return
  try {
    const response = await getCommentLikeStatus(commentId)
    if (response.data?.code !== 200) return
    commentLikeStatus[commentId] = response.data.data
  } catch (_) {
    // ignore
  }
}

const handleToggleContentLike = async () => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再点赞')
    router.push('/login')
    return
  }
  try {
    const response = await toggleContentLike(route.params.id)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '点赞失败')
      return
    }
    contentLike.isLiked = response.data.data.isLiked
    contentLike.likeCount = response.data.data.likeCount
    if (content.value) {
      content.value.likeCount = response.data.data.likeCount
    }
  } catch (_) {
    ElMessage.error('点赞失败')
  }
}

const handleToggleCollection = async () => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再收藏')
    router.push('/login')
    return
  }
  try {
    const response = await toggleContentCollection(route.params.id)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '收藏失败')
      return
    }
    contentCollection.isCollected = response.data.data.isCollected
    contentCollection.collectionCount = response.data.data.collectionCount
    if (content.value) {
      content.value.collectionCount = response.data.data.collectionCount
    }
  } catch (_) {
    ElMessage.error('收藏失败')
  }
}

// 举报模板与自由输入二选一即可，满足“模板化 + 个性化补充”并存。
const loadReportTemplates = async () => {
  if (!sessionStorage.getItem('token')) {
    reportTemplates.value = []
    return
  }
  try {
    const response = await getReportTemplates()
    if (response.data?.code !== 200) return
    reportTemplates.value = Array.isArray(response.data?.data) ? response.data.data : []
  } catch (_) {
    reportTemplates.value = []
  }
}

const openReportDialog = (targetType, targetId, title = '举报') => {
  reportDialog.visible = true
  reportDialog.title = title
  reportDialog.targetType = targetType
  reportDialog.targetId = Number(targetId)
  reportDialog.templateCode = ''
  reportDialog.customReason = ''
  reportDialog.submitting = false
}

const submitReport = async () => {
  const customReason = (reportDialog.customReason || '').trim()
  if (!reportDialog.templateCode && !customReason) {
    ElMessage.warning('请至少选择一个违规模板或填写补充描述')
    return
  }
  reportDialog.submitting = true
  try {
    const template = reportTemplates.value.find((item) => item.code === reportDialog.templateCode)
    const response = await createReport({
      targetType: reportDialog.targetType,
      targetId: Number(reportDialog.targetId),
      templateCode: reportDialog.templateCode || undefined,
      templateLabel: template?.label || undefined,
      reason: customReason || undefined
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '举报提交失败')
      return
    }
    ElMessage.success('举报已提交，管理员会尽快处理')
    reportDialog.visible = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '举报提交失败')
  } finally {
    reportDialog.submitting = false
  }
}

const handleReportContent = async () => {
  if (!canReportContent.value || !content.value?.id) return
  openReportDialog('content', content.value.id, '举报内容')
}

const handleReportAuthor = async () => {
  if (!canFollowAuthor.value || !content.value?.userId) return
  openReportDialog('user', content.value.userId, '举报用户')
}

const handleReportComment = async (comment) => {
  if (!canReportComment(comment)) return
  openReportDialog('comment', comment.id, '举报评论')
}

const handleToggleFollow = async () => {
  if (!content.value?.userId || !canFollowAuthor.value) {
    return
  }
  followState.loading = true
  try {
    const response = await toggleFollowUser(content.value.userId)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '关注操作失败')
      return
    }
    followState.isFollowing = response.data.data.isFollowing
    followState.followerCount = Number(response.data.data.followerCount || 0)
    ElMessage.success(followState.isFollowing ? '关注成功' : '已取消关注')
  } catch (_) {
    ElMessage.error('关注操作失败')
  } finally {
    followState.loading = false
  }
}

const toggleLike = async (commentId) => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再点赞')
    router.push('/login')
    return
  }
  try {
    const response = await toggleCommentLike(commentId)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '评论点赞失败')
      return
    }
    commentLikeStatus[commentId] = response.data.data
  } catch (_) {
    ElMessage.error('评论点赞失败')
  }
}

const submitComment = async () => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再评论')
    router.push('/login')
    return
  }
  if (!commentInput.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  try {
    const payload = {
      contentId: Number(route.params.id),
      commentContent: commentInput.value.trim()
    }
    if (replyToCommentId.value) {
      payload.parentId = replyToCommentId.value
    }
    const response = await createComment(payload)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '评论失败')
      return
    }
    ElMessage.success(replyToCommentId.value ? '回复成功' : '评论成功')
    commentInput.value = ''
    replyToCommentId.value = null
    replyToName.value = ''
    await loadComments(1)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '评论失败')
  }
}

const startReply = (commentId, comment) => {
  replyToCommentId.value = commentId
  replyToName.value = getDisplayName(comment)
}

const cancelReply = () => {
  replyToCommentId.value = null
  replyToName.value = ''
}

const appendCommentEmoji = (emoji) => {
  if (!emoji) return
  commentInput.value = `${commentInput.value || ''}${emoji}`
}

const goToAuthorProfile = () => {
  if (!content.value?.userId) return
  router.push(`/users/${content.value.userId}`)
}

const goChatWithAuthor = () => {
  if (!content.value?.userId) return
  router.push({ path: '/chat', query: { targetUserId: String(content.value.userId) } })
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  return new Date(timeString).toLocaleString('zh-CN')
}

const formatBytes = (bytes) => {
  const size = Number(bytes || 0)
  if (size <= 0) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / (1024 * 1024)).toFixed(2)} MB`
  return `${(size / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

const parseTags = (tagText) => {
  if (!tagText) return []
  return String(tagText)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const parseMediaList = (text) => {
  if (!text) return []
  return String(text)
    .split(',')
    .map((item) => normalizeFileUrl(item.trim()))
    .filter(Boolean)
}

const ensureVideoPosters = async () => {
  if (!content.value?.videos) return
  const urls = parseMediaList(content.value.videos)
  if (!urls.length) return

  const tasks = urls.map(async (url) => {
    if (!url || videoPosterMap.value[url]) return
    try {
      const poster = await extractVideoPoster(url, {
        seekSeconds: 0.8,
        maxWidth: 900,
        quality: 0.82
      })
      if (!poster) return
      videoPosterMap.value = {
        ...videoPosterMap.value,
        [url]: poster
      }
    } catch (_) {
      // ignore poster fallback
    }
  })

  await Promise.all(tasks)
}

// 评论展示名兜底顺序：昵称 -> 用户名 -> 用户ID，避免空名导致 UI 断层。
const getDisplayName = (comment) => {
  if (!comment) return '匿名用户'
  return comment.nickname || comment.username || `用户${comment.userId || ''}`
}

const getAuthorDisplayName = () => {
  if (author.value?.nickname) return author.value.nickname
  if (author.value?.username) return author.value.username
  if (content.value?.userId) return `用户${content.value.userId}`
  return '作者'
}

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

onMounted(async () => {
  hasReportedView = false
  await loadDetail()
  await ensureVideoPosters()
  scheduleViewReport()
  await Promise.all([loadAuthorProfile(), loadContentLike(), loadContentCollection(), loadFollowState()])
  await Promise.all([loadComments(1), loadReportTemplates()])
})

onBeforeUnmount(() => {
  if (viewReportTimer) {
    window.clearTimeout(viewReportTimer)
    viewReportTimer = null
  }
})
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-card,
.comment-card {
  border-radius: 18px;
}

.comment-card {
  animation-delay: 0.08s;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-text {
  color: #8c96a8;
  font-size: 12px;
}

.author-row {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(31, 41, 55, 0.08);
  background: rgba(248, 252, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.author-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-profile-link {
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  cursor: pointer;
  padding: 4px;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.author-profile-link:hover,
.author-profile-link:focus-visible {
  border-color: rgba(14, 165, 164, 0.45);
  background: rgba(236, 253, 245, 0.75);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.author-meta strong {
  color: #21324a;
}

.author-meta span {
  color: #6c7d92;
  font-size: 12px;
}

.content-html {
  line-height: 1.8;
  color: #2f3a4f;
  font-size: 15px;
}

.detail-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.detail-image {
  width: 100%;
  height: 180px;
  border-radius: 10px;
}

.video-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}

.detail-video {
  width: 100%;
  max-height: 260px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.06);
}

.action-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.meta-text {
  color: #66768a;
  font-size: 13px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.comment-item {
  padding: 14px 16px;
  border: 1px solid rgba(31, 41, 55, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.comment-item + .comment-item {
  margin-top: 12px;
}

.comment-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-meta strong {
  font-size: 14px;
  color: #21324a;
}

.user-meta span {
  font-size: 12px;
  color: #8c96a8;
}

.comment-content {
  margin: 0;
  line-height: 1.7;
  color: #2f3f58;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-action-btn {
  border-radius: 8px;
  padding: 2px 8px;
  font-size: 12px;
  color: #0f766e !important;
  border: 1px solid transparent !important;
  background: transparent !important;
}

.comment-action-btn :deep(span) {
  color: inherit !important;
}

.comment-action-btn:hover,
.comment-action-btn:focus-visible {
  background: rgba(14, 165, 164, 0.12) !important;
  color: #0c6972 !important;
}

.comment-action-btn.liked {
  color: #dc2626 !important;
}

.comment-action-btn.liked:hover,
.comment-action-btn.liked:focus-visible {
  background: rgba(220, 38, 38, 0.12) !important;
  color: #b91c1c !important;
}

.comment-action-btn.danger {
  color: #dc2626 !important;
}

.comment-action-btn.danger:hover,
.comment-action-btn.danger:focus-visible {
  background: rgba(220, 38, 38, 0.12) !important;
  color: #b91c1c !important;
}

.reply-list {
  margin-top: 10px;
  margin-left: 42px;
  border-left: 2px solid rgba(14, 165, 164, 0.2);
  padding-left: 12px;
}

.reply-item {
  padding: 10px 12px;
  border: 1px solid rgba(31, 41, 55, 0.08);
  border-radius: 12px;
  background: rgba(246, 250, 255, 0.85);
}

.reply-item + .reply-item {
  margin-top: 10px;
}

.reply-item .comment-user :deep(.el-avatar) {
  border: 1px solid rgba(31, 41, 55, 0.08);
}

@media (max-width: 760px) {
  .author-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .comment-item {
    padding: 12px;
  }

  .reply-list {
    margin-left: 16px;
    padding-left: 10px;
  }
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}
</style>





