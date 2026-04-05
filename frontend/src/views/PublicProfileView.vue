<template>
  <section class="public-profile-page">
    <el-card class="hero-card motion-fade-up">
      <div class="hero-main">
        <div class="author-block">
          <UserAvatar :size="88" :src="normalizeFileUrl(profile.avatar || '')" :fallback-text="displayName" />
          <div class="author-meta">
            <h1>{{ displayName }}</h1>
            <p class="username">@{{ profile.username || `user-${profile.id || ''}` }}</p>
            <p class="bio">{{ profile.bio || '这个作者还没有填写个人简介。' }}</p>
          </div>
        </div>

        <div class="hero-side">
          <div class="stat-grid">
            <div class="stat-item">
              <span>粉丝</span>
              <strong>{{ profile.followerCount }}</strong>
            </div>
            <div class="stat-item">
              <span>关注</span>
              <strong>{{ profile.followingCount }}</strong>
            </div>
          </div>
          <el-button
            v-if="canFollow"
            class="follow-btn"
            :type="followState.isFollowing ? 'default' : 'primary'"
            :loading="followState.loading"
            @click="handleToggleFollow"
          >
            {{ followState.isFollowing ? '已关注' : '关注作者' }}
          </el-button>
          <el-button
            v-if="canFollow && followState.isFollowing"
            class="follow-btn"
            @click="goChatWithAuthor"
          >
            私聊
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="works-card motion-fade-soft">
      <template #header>
        <div class="works-head">
          <div>
            <span class="title">作者作品（{{ works.total }}）</span>
            <p class="tip">排序规则：点赞数降序，同点赞按发布时间降序</p>
          </div>
        </div>
      </template>

      <div v-if="works.list.length" class="works-grid">
        <el-card v-for="item in works.list" :key="item.id" class="work-item" shadow="never">
          <router-link class="work-title" :to="`/content/${item.id}`">{{ item.title || '未命名内容' }}</router-link>
          <p class="work-preview">
            {{ stripHtml(item.content).slice(0, 120) }}<span v-if="stripHtml(item.content).length > 120">...</span>
          </p>

          <div v-if="item._imageList?.length" class="image-row">
            <el-image
              v-for="(image, index) in item._imageList"
              :key="`${item.id}-${index}`"
              :src="image"
              fit="cover"
              class="preview-image"
              :preview-src-list="item._allImageList"
              :initial-index="index"
              preview-teleported
            />
          </div>

          <div class="work-meta">
            <span>点赞 {{ item.likeCount || 0 }}</span>
            <span>收藏 {{ item.collectionCount || 0 }}</span>
            <span>评论 {{ item.commentCount || 0 }}</span>
            <span>发布于 {{ formatTime(item.createTime) }}</span>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="该作者还没有公开发布内容" />

      <div class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, ->, total"
          :current-page="works.page"
          :page-size="works.pageSize"
          :total="works.total"
          @current-change="loadWorks"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '../components/UserAvatar.vue'
import { getFollowerCount, getFollowStatus, toggleFollowUser } from '../api/follow'
import { getPublicUserProfile, getUserContents } from '../api/user'

/**
 * 公开个人主页职责：
 * - 展示作者公开资料与作品列表；
 * - 支持关注/私聊入口；
 * - 作品排序固定为“点赞降序 + 时间降序”。
 */
const route = useRoute()
const router = useRouter()

const profile = reactive({
  id: null,
  username: '',
  nickname: '',
  avatar: '',
  bio: '',
  followerCount: 0,
  followingCount: 0
})

const works = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  total: 0
})

const followState = reactive({
  isFollowing: false,
  loading: false
})

const isLoggedIn = computed(() => Boolean(sessionStorage.getItem('token')))
const currentUserId = computed(() => Number(sessionStorage.getItem('userId') || 0))
const canFollow = computed(() => isLoggedIn.value && Number(profile.id || 0) > 0 && Number(profile.id) !== currentUserId.value)
const displayName = computed(() => profile.nickname || profile.username || `用户${profile.id || ''}`)

const parseImages = (images) => {
  if (!images) return []
  return String(images)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const stripHtml = (value) => {
  if (!value) return ''
  return String(value).replace(/<[^>]*>/g, '')
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  return new Date(timeString).toLocaleString('zh-CN')
}

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const getTargetUserId = () => Number(route.params.id || 0)

const loadProfile = async () => {
  const userId = getTargetUserId()
  if (!userId) {
    router.push('/')
    return false
  }

  try {
    const response = await getPublicUserProfile(userId)
    if (response.data?.code !== 200 || !response.data?.data) {
      ElMessage.error(response.data?.message || '该用户不存在或不可访问')
      router.push('/')
      return false
    }

    const data = response.data.data
    profile.id = data.id
    profile.username = data.username || ''
    profile.nickname = data.nickname || ''
    profile.avatar = data.avatar || ''
    profile.bio = data.bio || ''
    profile.followerCount = Number(data.followerCount || 0)
    profile.followingCount = Number(data.followingCount || 0)
    return true
  } catch (_) {
    ElMessage.error('加载作者信息失败')
    router.push('/')
    return false
  }
}

const loadWorks = async (targetPage = 1) => {
  const userId = getTargetUserId()
  if (!userId) return

  try {
    const response = await getUserContents(userId, {
      page: targetPage,
      pageSize: works.pageSize,
      sort: 'like_desc'
    })
    const data = response.data?.data || {}
    works.list = (data.list || []).map((item) => {
      const allImages = parseImages(item.images)
      return {
        ...item,
        _allImageList: allImages,
        _imageList: allImages.slice(0, 3)
      }
    })
    works.page = data.page || targetPage
    works.pageSize = data.pageSize || works.pageSize
    works.total = Number(data.total || 0)
  } catch (_) {
    ElMessage.error('加载作者作品失败')
  }
}

// 关注状态读取与粉丝数读取分离，避免任一接口失败导致整块不可用。
const loadFollowState = async () => {
  if (!profile.id) return
  try {
    const countResp = await getFollowerCount(profile.id)
    if (countResp.data?.code === 200) {
      profile.followerCount = Number(countResp.data.data || profile.followerCount)
    }
  } catch (_) {
    // ignore
  }

  if (!canFollow.value) {
    followState.isFollowing = false
    return
  }

  try {
    const response = await getFollowStatus(profile.id)
    if (response.data?.code !== 200) return
    followState.isFollowing = Boolean(response.data.data?.isFollowing)
    profile.followerCount = Number(response.data.data?.followerCount || profile.followerCount)
  } catch (_) {
    // ignore
  }
}

const handleToggleFollow = async () => {
  if (!canFollow.value) return
  followState.loading = true
  try {
    const response = await toggleFollowUser(profile.id)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '关注操作失败')
      return
    }
    followState.isFollowing = Boolean(response.data.data?.isFollowing)
    profile.followerCount = Number(response.data.data?.followerCount || profile.followerCount)
    ElMessage.success(followState.isFollowing ? '关注成功' : '已取消关注')
  } catch (_) {
    ElMessage.error('关注操作失败')
  } finally {
    followState.loading = false
  }
}

const goChatWithAuthor = () => {
  if (!profile.id) return
  router.push({ path: '/chat', query: { targetUserId: String(profile.id) } })
}

const initPage = async () => {
  const ok = await loadProfile()
  if (!ok) return
  await Promise.all([loadWorks(1), loadFollowState()])
}

watch(
  () => route.params.id,
  () => {
    initPage()
  }
)

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.public-profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card,
.works-card {
  border-radius: 18px;
}

.hero-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.author-block {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.author-meta h1 {
  margin: 0;
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.2;
  color: #1f2d3d;
}

.username {
  margin-top: 4px;
  color: #5a6f87;
}

.bio {
  margin-top: 8px;
  color: #34465c;
  line-height: 1.8;
  max-width: 720px;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  min-width: 96px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(248, 252, 255, 0.82);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-item span {
  font-size: 12px;
  color: #6b7e95;
}

.stat-item strong {
  font-size: 22px;
  color: #1f2d3d;
}

.follow-btn {
  border-radius: 999px;
  padding: 0 16px;
}

.works-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.works-head .title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.tip {
  margin-top: 4px;
  font-size: 12px;
  color: #5f738b;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.work-item {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.8);
}

.work-title {
  font-size: 18px;
  line-height: 1.4;
  color: #1f2d3d;
  font-weight: 600;
}

.work-title:hover {
  color: #0c8c8b;
}

.work-preview {
  margin-top: 8px;
  color: #33465f;
  line-height: 1.7;
}

.image-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-image {
  width: 112px;
  height: 112px;
  border-radius: 10px;
}

.work-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #5f7188;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .hero-main {
    flex-direction: column;
  }

  .hero-side {
    align-items: flex-start;
  }

  .works-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .author-block {
    flex-direction: column;
    align-items: flex-start;
  }

  .preview-image {
    width: 96px;
    height: 96px;
  }
}
</style>
