<template>
  <section class="home-page">
    <el-card class="intro-panel motion-fade-up">
      <div class="intro-head">
        <div>
          <p class="intro-kicker">LIFE · SHARE · COMMUNITY</p>
          <h1>把日常过成值得被看见的故事</h1>
          <p class="intro-text">
            这里是一个让用户分享生活点滴的平台。你可以发布经历、美食、旅行、心情，也能通过评论、点赞和收藏与他人建立连接。
          </p>
        </div>
        <router-link to="/about" class="about-link">了解更多</router-link>
      </div>

      <div class="hero-metrics">
        <div class="metric-item">
          <span>沉浸式浏览</span>
          <strong>搜索 + 筛选 + 排序</strong>
        </div>
        <div class="metric-item">
          <span>轻松创作</span>
          <strong>图文发布 + 多标签管理</strong>
        </div>
        <div class="metric-item">
          <span>高质量互动</span>
          <strong>点赞 + 评论 + 收藏</strong>
        </div>
      </div>

      <div class="feature-grid">
        <div class="feature-item">
          <h3>内容分享</h3>
          <p>支持图文发布、分类标签和个人内容管理。</p>
        </div>
        <div class="feature-item">
          <h3>互动交流</h3>
          <p>支持点赞、评论、回复与收藏，增强社区互动。</p>
        </div>
        <div class="feature-item">
          <h3>高效浏览</h3>
          <p>支持搜索、筛选、排序和分页，快速发现感兴趣内容。</p>
        </div>
      </div>
    </el-card>

    <el-card class="search-panel motion-fade-soft">
      <div class="search-row">
        <el-input v-model="query.keyword" placeholder="搜索标题或内容" clearable @keyup.enter="loadContents(1)" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 180px">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
        <el-select v-model="query.sort" style="width: 160px">
          <el-option label="最新发布" value="latest" />
          <el-option label="最早发布" value="oldest" />
          <el-option label="热门优先" value="popular" />
        </el-select>
        <el-button type="primary" @click="loadContents(1)">搜索</el-button>
      </div>
    </el-card>

    <el-card class="cta-panel motion-fade-up">
      <div class="cta-main">
        <div class="cta-copy">
          <p class="cta-kicker">START TODAY</p>
          <h2>一分钟开始你的内容主页</h2>
          <p>已经有很多人在这里分享学习、生活与兴趣。你也可以马上发布第一条内容，建立自己的表达阵地。</p>
          <div class="cta-actions">
            <el-button class="cta-btn cta-btn-primary" type="primary" size="large" @click="goPrimaryAction">{{ primaryActionText }}</el-button>
            <el-button class="cta-btn cta-btn-secondary" size="large" @click="goSecondaryAction">{{ secondaryActionText }}</el-button>
          </div>
        </div>
        <div class="cta-stats">
          <div class="cta-stat-item">
            <strong>{{ page.total }}</strong>
            <span>内容总量</span>
          </div>
          <div class="cta-stat-item">
            <strong>{{ categories.length }}</strong>
            <span>分类可浏览</span>
          </div>
          <div class="cta-stat-item">
            <strong>{{ page.pageSize }}</strong>
            <span>每页展示</span>
          </div>
        </div>
      </div>
    </el-card>

    <div class="content-list">
      <el-card
        v-for="(content, index) in page.list"
        :key="content.id"
        class="content-card"
        :style="{ '--stagger': `${Math.min(index, 10) * 0.06}s` }"
      >
        <template #header>
          <div class="card-header">
            <div class="title-wrap">
              <router-link :to="`/content/${content.id}`" class="title-link">{{ content.title }}</router-link>
              <el-tag v-if="content.categoryName" class="category-tag" effect="dark" type="success">{{ content.categoryName }}</el-tag>
            </div>
            <span class="post-time">{{ formatTime(content.createTime) }}</span>
          </div>
        </template>

        <div class="author-row">
          <button class="author-link" type="button" @click="goToAuthorProfile(content.userId)">
            <UserAvatar :size="28" :src="normalizeFileUrl(content.authorAvatar || '')" :fallback-text="content.authorName || '用户'" />
            <span class="author-name">{{ content.authorName || `用户${content.userId || ''}` }}</span>
          </button>
          <el-button
            v-if="canFollowUser(content.userId) && !authorFollowStatus[content.userId]?.isFollowing"
            class="follow-author-btn"
            size="small"
            type="primary"
            :loading="Boolean(authorFollowStatus[content.userId]?.loading)"
            @click="handleToggleAuthorFollow(content.userId)"
          >
            关注作者
          </el-button>
          <el-button
            v-if="canFollowUser(content.userId) && authorFollowStatus[content.userId]?.isFollowing"
            class="follow-author-btn chat-author-btn"
            size="small"
            @click="goChatWithUser(content.userId)"
          >
            私聊
          </el-button>
        </div>

        <p class="content-preview">
          {{ stripHtml(content.content).slice(0, 160) }}<span v-if="stripHtml(content.content).length > 160">...</span>
        </p>

        <div v-if="parseTags(content.tags).length" class="tag-row">
          <el-tag v-for="tag in parseTags(content.tags)" :key="`${content.id}-${tag}`" size="small" type="info">{{ tag }}</el-tag>
        </div>

        <div v-if="content._imageList?.length" class="image-row">
          <el-image
            v-for="(image, imageIndex) in content._imageList"
            :key="imageIndex"
            :src="image"
            fit="cover"
            class="preview-image"
            :preview-src-list="content._allImageList"
            :initial-index="imageIndex"
            preview-teleported
          />
        </div>

        <div v-if="content._videoList?.length" class="video-row">
          <video
            v-for="(video, videoIndex) in content._videoList"
            :key="`video-${videoIndex}`"
            :src="video"
            :poster="videoPosterMap[video] || ''"
            class="preview-video"
            controls
            preload="metadata"
          />
        </div>

        <div class="card-footer">
          <el-button size="small" :type="likeStatus[content.id] ? 'danger' : 'default'" @click="handleLike(content.id)">
            {{ likeCount[content.id] ?? content.likeCount ?? 0 }} 点赞
          </el-button>
          <el-button size="small" :type="collectionStatus[content.id] ? 'warning' : 'default'" @click="handleCollection(content.id)">
            {{ collectionCount[content.id] ?? content.collectionCount ?? 0 }} 收藏
          </el-button>
          <span class="meta-text">评论 {{ content.commentCount || 0 }}</span>
          <span class="meta-text">浏览 {{ content.viewCount || 0 }}</span>
          <router-link :to="`/content/${content.id}`">查看详情</router-link>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && page.list.length === 0" description="暂无内容" />

    <div class="pager-wrap">
      <el-pagination
        background
        layout="prev, pager, next, jumper, ->, total"
        :current-page="page.page"
        :page-size="page.pageSize"
        :total="page.total"
        @current-change="loadContents"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '../components/UserAvatar.vue'
import { getCategories, getContentList } from '../api/content'
import { getContentCollectionStatus, toggleContentCollection } from '../api/collection'
import { getContentLikeStatus, toggleContentLike } from '../api/like'
import { getFollowStatus, toggleFollowUser } from '../api/follow'
import { extractVideoPoster } from '../utils/media'

/**
 * 首页职责：
 * 1. 承载公开内容浏览主链路（搜索/筛选/分页/详情入口）。
 * 2. 在列表层做互动状态回显（点赞/收藏/关注），降低进入详情页前的操作成本。
 * 3. 对图片/视频做轻量预处理（缩略展示 + 视频封面提取），保证首屏性能与可读性平衡。
 */
const router = useRouter()
const loading = ref(false)
const categories = ref([])
const likeStatus = ref({})
const likeCount = ref({})
const collectionStatus = ref({})
const collectionCount = ref({})
const authorFollowStatus = ref({})
const videoPosterMap = ref({})
const isLoggedIn = computed(() => Boolean(sessionStorage.getItem('token')))
const primaryActionText = computed(() => (isLoggedIn.value ? '立即发布内容' : '免费注册账号'))
const secondaryActionText = computed(() => (isLoggedIn.value ? '进入我的主页' : '已有账号，去登录'))

const query = reactive({
  keyword: '',
  categoryId: null,
  sort: 'latest'
})

const page = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  total: 0
})

// 分类是列表筛选的前置条件，失败时只提示不阻塞页面继续浏览。
const loadCategories = async () => {
  try {
    const response = await getCategories()
    categories.value = response.data.data || []
  } catch (_) {
    ElMessage.error('加载分类失败')
  }
}

// 互动状态按内容维度单独拉取，避免列表接口承担用户态计算压力。
const loadLikeStatus = async (contentId) => {
  if (!sessionStorage.getItem('token')) return
  try {
    const response = await getContentLikeStatus(contentId)
    likeStatus.value[contentId] = response.data.data.isLiked
    likeCount.value[contentId] = response.data.data.likeCount
  } catch (_) {
    // ignore
  }
}

// 收藏状态与点赞状态拆分维护，便于后续独立扩展收藏业务规则。
const loadCollectionStatus = async (contentId) => {
  if (!sessionStorage.getItem('token')) return
  try {
    const response = await getContentCollectionStatus(contentId)
    collectionStatus.value[contentId] = response.data.data.isCollected
    collectionCount.value[contentId] = response.data.data.collectionCount
  } catch (_) {
    // ignore
  }
}

/**
 * 核心列表加载：
 * - 先取内容主数据，再并发补齐“用户态”字段（点赞/收藏/关注）。
 * - 图片仅首屏显示 3 张、视频仅显示 1 条，避免卡片高度与加载时间失控。
 */
const loadContents = async (targetPage = 1) => {
  loading.value = true
  try {
    const response = await getContentList({
      page: targetPage,
      pageSize: page.pageSize,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      sort: query.sort
    })
    const data = response.data.data || {}
    page.list = (data.list || []).map((item) => {
      const allImages = parseImages(item.images)
      const allVideos = parseVideos(item.videos)
      return {
        ...item,
        _allImageList: allImages,
        _imageList: allImages.slice(0, 3),
        _videoList: allVideos.slice(0, 1)
      }
    })
    page.page = data.page || targetPage
    page.pageSize = data.pageSize || page.pageSize
    page.total = data.total || 0

    likeStatus.value = {}
    likeCount.value = {}
    collectionStatus.value = {}
    collectionCount.value = {}
    authorFollowStatus.value = {}

    const contentStatusTasks = page.list.map((item) => Promise.all([loadLikeStatus(item.id), loadCollectionStatus(item.id)]))
    const uniqueAuthorIds = [...new Set(page.list.map((item) => Number(item.userId || 0)).filter((userId) => canFollowUser(userId)))]
    const followTasks = uniqueAuthorIds.map((userId) => loadAuthorFollowStatus(userId))
    await Promise.all([...contentStatusTasks, ...followTasks])
    await ensureVideoPosters(page.list)
  } catch (_) {
    ElMessage.error('加载内容失败')
  } finally {
    loading.value = false
  }
}

// 视频封面异步提取只做“体验增强”，失败时静默回退到默认播放器首帧。
const ensureVideoPosters = async (contentList) => {
  const videoUrls = [...new Set((contentList || []).flatMap((item) => item._videoList || []))]
  if (videoUrls.length === 0) return

  const tasks = videoUrls.map(async (url) => {
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

// 未登录用户执行互动时统一走登录拦截，保证行为口径一致。
const handleLike = async (contentId) => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再点赞')
    router.push('/login')
    return
  }
  try {
    const response = await toggleContentLike(contentId)
    likeStatus.value[contentId] = response.data.data.isLiked
    likeCount.value[contentId] = response.data.data.likeCount
  } catch (_) {
    ElMessage.error('点赞操作失败')
  }
}

// 收藏与点赞分离，便于后续收藏业务独立埋点和提醒策略。
const handleCollection = async (contentId) => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再收藏')
    router.push('/login')
    return
  }
  try {
    const response = await toggleContentCollection(contentId)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '收藏操作失败')
      return
    }
    collectionStatus.value[contentId] = response.data.data.isCollected
    collectionCount.value[contentId] = response.data.data.collectionCount
  } catch (_) {
    ElMessage.error('收藏操作失败')
  }
}

// CTA 按登录态分流，保证匿名用户与已登录用户的最短路径不同。
const goPrimaryAction = () => {
  if (isLoggedIn.value) {
    router.push('/publish')
    return
  }
  router.push('/register')
}

const goSecondaryAction = () => {
  if (isLoggedIn.value) {
    router.push('/profile')
    return
  }
  router.push('/login')
}

const getCurrentUserId = () => Number(sessionStorage.getItem('userId') || 0)

// 自己不能关注自己，且匿名用户不展示关注能力。
const canFollowUser = (userId) => {
  if (!sessionStorage.getItem('token')) return false
  const targetId = Number(userId || 0)
  if (!targetId) return false
  return targetId !== getCurrentUserId()
}

// 作者关注状态单独缓存，避免重复请求导致按钮频繁抖动。
const loadAuthorFollowStatus = async (userId) => {
  if (!canFollowUser(userId)) return
  try {
    const response = await getFollowStatus(userId)
    if (response.data?.code !== 200) return
    authorFollowStatus.value[userId] = {
      isFollowing: Boolean(response.data.data?.isFollowing),
      followerCount: Number(response.data.data?.followerCount || 0),
      loading: false
    }
  } catch (_) {
    // ignore
  }
}

/**
 * 关注按钮采用“乐观 loading + 失败回滚”策略：
 * - 先把按钮置为 loading 防重复点击；
 * - 接口失败回滚到旧状态，确保 UI 与后端一致。
 */
const handleToggleAuthorFollow = async (userId) => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再关注')
    router.push('/login')
    return
  }
  if (!canFollowUser(userId)) return

  const previous = authorFollowStatus.value[userId] || { isFollowing: false, followerCount: 0, loading: false }
  authorFollowStatus.value[userId] = {
    ...previous,
    loading: true
  }

  try {
    const response = await toggleFollowUser(userId)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '关注操作失败')
      authorFollowStatus.value[userId] = { ...previous, loading: false }
      return
    }
    authorFollowStatus.value[userId] = {
      isFollowing: Boolean(response.data.data?.isFollowing),
      followerCount: Number(response.data.data?.followerCount || 0),
      loading: false
    }
    ElMessage.success(authorFollowStatus.value[userId].isFollowing ? '关注成功' : '已取消关注')
  } catch (_) {
    authorFollowStatus.value[userId] = { ...previous, loading: false }
    ElMessage.error('关注操作失败')
  }
}

const goToAuthorProfile = (userId) => {
  if (!userId) return
  router.push(`/users/${userId}`)
}

// 从内容流直接发起私聊，减少用户跳转路径。
const goChatWithUser = (userId) => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录后再私聊')
    router.push('/login')
    return
  }
  if (!userId) return
  router.push({ path: '/chat', query: { targetUserId: String(userId) } })
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  return new Date(timeString).toLocaleString('zh-CN')
}

const stripHtml = (value) => {
  if (!value) return ''
  return value.replace(/<[^>]*>/g, '')
}

const parseTags = (tagText) => {
  if (!tagText) return []
  return String(tagText)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const parseImages = (imageText) => {
  if (!imageText) return []
  return String(imageText)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const parseVideos = (videoText) => {
  if (!videoText) return []
  return String(videoText)
    .split(',')
    .map((item) => normalizeFileUrl(item.trim()))
    .filter(Boolean)
}

// 统一补全相对路径，避免不同接口返回格式导致媒体无法显示。
const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

onMounted(async () => {
  await loadCategories()
  await loadContents(1)
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.intro-panel {
  border-radius: 20px;
  overflow: hidden;
  background:
    linear-gradient(130deg, rgba(14, 165, 164, 0.16), rgba(249, 115, 22, 0.16)),
    rgba(255, 255, 255, 0.84) !important;
}

.intro-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.intro-kicker {
  font-size: 12px;
  letter-spacing: 1.6px;
  color: #0c8c8b;
  margin-bottom: 8px;
}

.intro-head h1 {
  font-size: clamp(28px, 3.4vw, 38px);
  line-height: 1.2;
  margin-bottom: 10px;
}

.intro-text {
  color: #304156;
  line-height: 1.8;
  max-width: 780px;
  font-size: 15px;
}

.about-link {
  white-space: nowrap;
  align-self: flex-start;
  padding: 10px 14px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #0ea5a4, #0c8c8b);
  box-shadow: 0 10px 20px rgba(14, 165, 164, 0.28);
}

.hero-metrics {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.74);
  animation: fadeUp 0.48s ease both;
}

.metric-item span {
  font-size: 12px;
  color: #64748b;
}

.metric-item strong {
  display: block;
  margin-top: 4px;
  font-size: 14px;
  color: #203247;
}

.feature-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-item {
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 12px;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
  animation: fadeUp 0.52s ease both;
}

.feature-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(22, 42, 62, 0.12);
}

.feature-item h3 {
  font-size: 15px;
  margin-bottom: 6px;
}

.feature-item p {
  color: #52647d;
  font-size: 13px;
  line-height: 1.6;
}

.search-panel {
  border-radius: 16px;
}

.search-row {
  display: grid;
  grid-template-columns: 1fr 180px 160px 96px;
  gap: 12px;
}

.cta-panel {
  border-radius: 16px;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 16%, rgba(255, 255, 255, 0.3), transparent 40%),
    linear-gradient(145deg, rgba(14, 165, 164, 0.18), rgba(249, 115, 22, 0.2)),
    rgba(255, 255, 255, 0.82) !important;
}

.cta-main {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 14px;
  align-items: center;
}

.cta-kicker {
  font-size: 12px;
  letter-spacing: 1.4px;
  color: #0c8c8b;
}

.cta-copy h2 {
  margin-top: 6px;
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.3;
  color: #1f2d3d;
}

.cta-copy p {
  margin-top: 10px;
  line-height: 1.8;
  color: #44546b;
}

.cta-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cta-btn {
  min-width: 148px;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease, border-color 0.22s ease;
}

.cta-btn:hover,
.cta-btn:focus-visible {
  transform: translateY(-2px);
}

.cta-btn-primary:hover,
.cta-btn-primary:focus-visible {
  box-shadow: 0 14px 28px color-mix(in oklab, var(--primary-500) 40%, transparent) !important;
}

.cta-btn-secondary {
  background: rgba(255, 255, 255, 0.86) !important;
  border-color: rgba(14, 165, 164, 0.45) !important;
  color: #125f76 !important;
}

.cta-btn-secondary:hover,
.cta-btn-secondary:focus-visible {
  background: rgba(255, 255, 255, 0.98) !important;
  border-color: rgba(14, 165, 164, 0.68) !important;
  box-shadow: 0 10px 22px rgba(17, 110, 136, 0.16);
}

.cta-stats {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.cta-stat-item {
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.84);
}

.cta-stat-item strong {
  display: block;
  font-size: 24px;
  line-height: 1.2;
  color: #203247;
}

.cta-stat-item span {
  margin-top: 4px;
  display: inline-block;
  font-size: 12px;
  color: #64748b;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-card {
  border-radius: 16px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  animation: cardLiftIn 0.5s ease both;
  animation-delay: calc(var(--stagger, 0s) + 0.08s);
}

.content-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.title-link {
  color: #1f2d3d;
  font-weight: 600;
  font-size: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-link:hover {
  color: #0c8c8b;
}

.category-tag {
  border-radius: 999px;
}

.post-time {
  color: #7f8a9e;
  font-size: 12px;
  white-space: nowrap;
}

.author-row {
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.author-link {
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(248, 252, 255, 0.88);
  border-radius: 999px;
  padding: 4px 10px 4px 4px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.author-link:hover,
.author-link:focus-visible {
  border-color: rgba(14, 165, 164, 0.55);
  background: rgba(236, 253, 245, 0.92);
}

.author-name {
  font-size: 13px;
  color: #44556d;
}

.follow-author-btn {
  border-radius: 999px;
  padding: 0 12px;
}

.chat-author-btn {
  margin-left: -2px;
}

.content-preview {
  color: #314055;
  line-height: 1.7;
  margin-top: 4px;
}

.tag-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.image-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-image {
  width: 148px;
  height: 148px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.04);
}

.video-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-video {
  width: min(320px, 100%);
  max-height: 200px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.08);
}

.card-footer {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  color: #607188;
}

.meta-text {
  font-size: 13px;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 6px;
}

.metric-item:nth-child(2) {
  animation-delay: 0.05s;
}

.metric-item:nth-child(3) {
  animation-delay: 0.1s;
}

.feature-item:nth-child(2) {
  animation-delay: 0.08s;
}

.feature-item:nth-child(3) {
  animation-delay: 0.16s;
}

@keyframes cardLiftIn {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.995);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .intro-head {
    flex-direction: column;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .search-row {
    grid-template-columns: 1fr;
  }

  .cta-main {
    grid-template-columns: 1fr;
  }

  .cta-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .preview-image {
    width: 104px;
    height: 104px;
  }
}

@media (max-width: 560px) {
  .cta-stats {
    grid-template-columns: 1fr;
  }
}
</style>
