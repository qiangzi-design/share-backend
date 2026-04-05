<template>
  <section class="profile-page">
    <div class="profile-hero motion-fade-up">
      <p class="hero-kicker">MY SPACE</p>
      <h1>{{ profile.nickname || profile.username || '我的主页' }}</h1>
      <p>在这里维护资料、管理发布内容，并持续沉淀你的个人表达。</p>
    </div>

    <el-row class="profile-layout-row" :gutter="16">
      <el-col class="layout-col side-col" :xs="24" :sm="24" :md="24" :lg="9" :xl="8">
        <el-card class="profile-card motion-fade-soft">
          <template #header>
            <div class="card-title">
              <span>个人资料</span>
              <small>可修改昵称、简介与头像</small>
            </div>
          </template>

          <div class="avatar-wrap">
            <UserAvatar :size="96" :src="profile.avatar || ''" :fallback-text="profile.nickname || '我'" />
            <el-upload
              action="/api/users/me/avatar"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeUploadAvatar"
              :on-success="handleAvatarSuccess"
            >
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>

          <el-form :model="profile" label-width="72px">
            <el-form-item label="用户名">
              <el-input v-model="profile.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profile.email" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profile.nickname" maxlength="50" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="profile.bio" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col class="layout-col main-col" :xs="24" :sm="24" :md="24" :lg="15" :xl="16">
        <el-card class="content-card motion-fade-soft">
          <template #header>
            <div class="content-header">
              <span>我的作品概览（{{ myContents.total }}）</span>
              <div class="content-header-actions">
                <el-button class="works-entry-btn" @click="router.push('/my-works')">作品管理</el-button>
                <el-button class="insights-entry-btn" @click="router.push('/insights')">数据分析</el-button>
                <el-button class="publish-entry-btn" type="primary" @click="router.push('/publish')">去发布</el-button>
              </div>
            </div>
          </template>

          <div class="follow-summary">
            <div class="summary-item">
              <span>粉丝总数</span>
              <strong>{{ followSummary.followerCount }}</strong>
            </div>
            <div class="summary-item">
              <span>我关注的人</span>
              <strong>{{ followSummary.followingCount }}</strong>
            </div>
            <div class="summary-item">
              <span>本期新增粉丝</span>
              <strong>{{ currentPeriodGrowthTotal }}</strong>
            </div>
            <div class="summary-item">
              <span>本期取关粉丝</span>
              <strong>{{ currentPeriodUnfollowTotal }}</strong>
            </div>
          </div>

          <div class="chart-panel">
            <div class="chart-head">
              <span>关注/取关趋势</span>
              <el-radio-group v-model="growthPeriod" size="small" @change="loadFollowerGrowth">
                <el-radio-button label="day">按天</el-radio-button>
                <el-radio-button label="month">按月</el-radio-button>
                <el-radio-button label="year">按年</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="followerGrowthChartRef" class="chart-box"></div>
          </div>

          <el-table class="compact-table" size="small" table-layout="fixed" :data="topViewedContents" empty-text="暂无内容">
            <el-table-column prop="title" label="标题" min-width="132" show-overflow-tooltip />
            <el-table-column label="审核" width="88" align="center">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="reviewTagType(row.reviewStatus)"
                >
                  {{ reviewText(row.reviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布时间" width="148" align="center">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="64" align="center" />
            <el-table-column prop="likeCount" label="点赞" width="64" align="center" />
            <el-table-column prop="collectionCount" label="收藏" width="64" align="center" />
            <el-table-column prop="commentCount" label="评论" width="64" align="center" />
            <el-table-column label="详情" width="92" align="center">
              <template #default="{ row }">
                <el-button class="table-action-btn view" @click="router.push(`/content/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="chart-panel compact">
            <div class="chart-head">
              <span>内容互动概览</span>
            </div>
            <div ref="contentMetricsChartRef" class="chart-box"></div>
          </div>

          <div class="top-hint">仅展示浏览量最高的 5 条作品，更多操作请进入“作品管理”。</div>

          <div class="list-panel">
            <div class="panel-title">我的互动清单</div>
            <el-tabs v-model="interactionTab" @tab-change="handleInteractionTabChange">
              <el-tab-pane label="我的收藏" name="collections" />
              <el-tab-pane label="我的点赞" name="likes" />
            </el-tabs>

            <div v-if="currentInteractionData.list.length" class="history-list">
              <div v-for="item in currentInteractionData.list" :key="`${interactionTab}-${item.contentId}`" class="history-item">
                <el-image class="history-cover" :src="parseFirstImage(item.images)" fit="cover">
                  <template #error>
                    <div class="history-cover-fallback">无图</div>
                  </template>
                </el-image>
                <div class="history-body">
                  <router-link class="history-title" :to="`/content/${item.contentId}`">{{ item.title || '未命名内容' }}</router-link>
                  <div class="history-meta">
                    <span>作者 {{ item.authorName || `用户${item.authorId || ''}` }}</span>
                    <span>点赞 {{ item.likeCount || 0 }}</span>
                    <span>收藏 {{ item.collectionCount || 0 }}</span>
                    <span>评论 {{ item.commentCount || 0 }}</span>
                  </div>
                  <div class="history-time">{{ formatTime(item.actionTime || item.contentCreateTime) }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无记录" />

            <div class="pager-wrap">
              <el-pagination
                background
                layout="prev, pager, next, ->, total"
                :current-page="currentInteractionData.page"
                :page-size="currentInteractionData.pageSize"
                :total="currentInteractionData.total"
                @current-change="handleInteractionPageChange"
              />
            </div>
          </div>

          <div class="list-panel">
            <div class="panel-title">粉丝 / 关注列表</div>
            <el-tabs v-model="socialTab" @tab-change="handleSocialTabChange">
              <el-tab-pane label="粉丝" name="followers" />
              <el-tab-pane label="关注" name="following" />
            </el-tabs>

            <div v-if="currentSocialData.list.length" class="user-list">
              <div v-for="user in currentSocialData.list" :key="`${socialTab}-${user.userId}`" class="user-item">
                <UserAvatar :size="42" :src="normalizeFileUrl(user.avatar || '')" :fallback-text="getUserDisplayName(user)" />
                <div class="user-body">
                  <div class="user-name-row">
                    <strong>{{ getUserDisplayName(user) }}</strong>
                    <div class="user-actions">
                      <span class="user-time">{{ formatTime(user.followTime) }}</span>
                      <el-button
                        v-if="socialTab === 'following'"
                        size="small"
                        class="chat-btn"
                        @click="goChatWithUser(user.userId)"
                      >
                        私聊
                      </el-button>
                    </div>
                  </div>
                  <p class="user-bio">{{ user.bio || '这个用户很低调，还没有填写简介。' }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无用户" />

            <div class="pager-wrap">
              <el-pagination
                background
                layout="prev, pager, next, ->, total"
                :current-page="currentSocialData.page"
                :page-size="currentSocialData.pageSize"
                :total="currentSocialData.total"
                @current-change="handleSocialPageChange"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import UserAvatar from '../components/UserAvatar.vue'
import { getMyFollowSummary, getMyFollowerGrowth, getMyFollowers, getMyFollowing } from '../api/follow'
import { getMyCollections, getMyLikes, getMyProfile, getUserContents, updateMyProfile } from '../api/user'

/**
 * 个人主页职责：
 * 1. 展示和维护个人资料（头像/昵称/简介）。
 * 2. 展示作品概览、互动清单、社交关系，并提供二级入口（作品管理/数据分析）。
 * 3. 提供“关注趋势 + 内容互动”两张轻量图，帮助用户快速感知账号状态。
 */
const router = useRouter()
const saving = ref(false)
const growthPeriod = ref('day')
const followerGrowthChartRef = ref(null)
const contentMetricsChartRef = ref(null)
const followerGrowth = ref([])
const topViewedContents = ref([])
const interactionTab = ref('collections')
const socialTab = ref('followers')

let followerChart = null
let contentChart = null

const profile = reactive({
  id: null,
  username: '',
  email: '',
  nickname: '',
  avatar: '',
  bio: ''
})

const followSummary = reactive({
  followerCount: 0,
  followingCount: 0,
  unfollowCount: 0
})

const myContents = reactive({
  list: [],
  pageSize: 30,
  total: 0
})

const myCollections = reactive({
  list: [],
  page: 1,
  pageSize: 6,
  total: 0
})

const myLikes = reactive({
  list: [],
  page: 1,
  pageSize: 6,
  total: 0
})

const myFollowers = reactive({
  list: [],
  page: 1,
  pageSize: 6,
  total: 0
})

const myFollowing = reactive({
  list: [],
  page: 1,
  pageSize: 6,
  total: 0
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${sessionStorage.getItem('token') || ''}`
}))

const currentPeriodGrowthTotal = computed(() =>
  followerGrowth.value.reduce((sum, item) => sum + Number(item?.followCount ?? item?.count ?? 0), 0)
)

const currentPeriodUnfollowTotal = computed(() =>
  followerGrowth.value.reduce((sum, item) => sum + Number(item?.unfollowCount || 0), 0)
)

const currentInteractionData = computed(() => (interactionTab.value === 'collections' ? myCollections : myLikes))
const currentSocialData = computed(() => (socialTab.value === 'followers' ? myFollowers : myFollowing))

// 登录态资料作为页面主数据源，同时回写 session 以驱动顶部导航昵称刷新。
const loadProfile = async () => {
  try {
    const response = await getMyProfile()
    if (response.data?.code !== 200) {
      throw new Error(response.data?.message || '加载个人资料失败')
    }

    const data = response.data.data
    profile.id = data.id
    profile.username = data.username
    profile.email = data.email
    profile.nickname = data.nickname || ''
    profile.avatar = data.avatar || ''
    profile.bio = data.bio || ''

    followSummary.followerCount = Number(data.followerCount || 0)
    followSummary.followingCount = Number(data.followingCount || 0)

    sessionStorage.setItem('userId', String(data.id))
    sessionStorage.setItem('nickname', profile.nickname || profile.username)
    window.dispatchEvent(new Event('auth-state-changed'))
  } catch (_) {
    ElMessage.error('加载个人资料失败')
  }
}

// 关注汇总和趋势拆开加载，避免其中一个接口异常拖垮另一个模块。
const loadFollowSummary = async () => {
  try {
    const response = await getMyFollowSummary()
    if (response.data?.code !== 200) {
      return
    }
    followSummary.followerCount = Number(response.data.data?.followerCount || 0)
    followSummary.followingCount = Number(response.data.data?.followingCount || 0)
    followSummary.unfollowCount = Number(response.data.data?.unfollowCount || 0)
  } catch (_) {
    // ignore
  }
}

// 趋势周期切换只改变采样窗口，不改变总量口径。
const loadFollowerGrowth = async () => {
  try {
    const size = growthPeriod.value === 'day' ? 14 : growthPeriod.value === 'month' ? 12 : 5
    const response = await getMyFollowerGrowth({
      period: growthPeriod.value,
      size
    })
    if (response.data?.code !== 200) {
      followerGrowth.value = []
      await renderFollowerGrowthChart()
      return
    }
    followerGrowth.value = Array.isArray(response.data.data) ? response.data.data : []
    await renderFollowerGrowthChart()
  } catch (_) {
    followerGrowth.value = []
    await renderFollowerGrowthChart()
  }
}

// 资料保存后主动广播登录态变更，保证全局昵称实时更新。
const saveProfile = async () => {
  saving.value = true
  try {
    const response = await updateMyProfile({
      nickname: profile.nickname,
      bio: profile.bio
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '资料更新失败')
      return
    }
    sessionStorage.setItem('nickname', profile.nickname || profile.username)
    window.dispatchEvent(new Event('auth-state-changed'))
    ElMessage.success('资料更新成功')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '资料更新失败')
  } finally {
    saving.value = false
  }
}

const beforeUploadAvatar = (file) => {
  const validType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!validType) {
    ElMessage.error('仅支持 JPG、PNG、GIF、WEBP')
    return false
  }
  const validSize = file.size / 1024 / 1024 <= 2
  if (!validSize) {
    ElMessage.error('头像不能超过 2MB')
    return false
  }
  return true
}

const handleAvatarSuccess = (response) => {
  if (response.code !== 200) {
    ElMessage.error(response.message || '头像上传失败')
    return
  }
  profile.avatar = response.data
  ElMessage.success('头像更新成功')
}

// “我的作品概览”用于图表统计源，分页大小放大以提升图表代表性。
const loadMyContents = async (targetPage = 1) => {
  if (!profile.id) return
  try {
    const response = await getUserContents(profile.id, {
      page: targetPage,
      pageSize: myContents.pageSize
    })
    const data = response.data.data || {}
    myContents.list = data.list || []
    myContents.pageSize = data.pageSize || myContents.pageSize
    myContents.total = data.total || 0
    await renderContentMetricsChart()
  } catch (_) {
    ElMessage.error('加载我的内容失败')
  }
}

// 顶部表格固定展示“浏览量 Top5”，与作品管理全量列表做职责分离。
const loadTopViewedContents = async () => {
  if (!profile.id) return
  try {
    const response = await getUserContents(profile.id, {
      page: 1,
      pageSize: 5,
      sort: 'view_desc'
    })
    const data = response.data?.data || {}
    topViewedContents.value = data.list || []
  } catch (_) {
    topViewedContents.value = []
    ElMessage.error('加载热门作品失败')
  }
}

const loadMyCollections = async (targetPage = 1) => {
  try {
    const response = await getMyCollections({
      page: targetPage,
      pageSize: myCollections.pageSize
    })
    const data = response.data?.data || {}
    myCollections.list = data.list || []
    myCollections.page = data.page || targetPage
    myCollections.pageSize = data.pageSize || myCollections.pageSize
    myCollections.total = data.total || 0
  } catch (_) {
    ElMessage.error('加载收藏列表失败')
  }
}

const loadMyLikes = async (targetPage = 1) => {
  try {
    const response = await getMyLikes({
      page: targetPage,
      pageSize: myLikes.pageSize
    })
    const data = response.data?.data || {}
    myLikes.list = data.list || []
    myLikes.page = data.page || targetPage
    myLikes.pageSize = data.pageSize || myLikes.pageSize
    myLikes.total = data.total || 0
  } catch (_) {
    ElMessage.error('加载点赞列表失败')
  }
}

const loadMyFollowers = async (targetPage = 1) => {
  try {
    const response = await getMyFollowers({
      page: targetPage,
      pageSize: myFollowers.pageSize
    })
    const data = response.data?.data || {}
    myFollowers.list = data.list || []
    myFollowers.page = data.page || targetPage
    myFollowers.pageSize = data.pageSize || myFollowers.pageSize
    myFollowers.total = data.total || 0
  } catch (_) {
    ElMessage.error('加载粉丝列表失败')
  }
}

const loadMyFollowing = async (targetPage = 1) => {
  try {
    const response = await getMyFollowing({
      page: targetPage,
      pageSize: myFollowing.pageSize
    })
    const data = response.data?.data || {}
    myFollowing.list = data.list || []
    myFollowing.page = data.page || targetPage
    myFollowing.pageSize = data.pageSize || myFollowing.pageSize
    myFollowing.total = data.total || 0
  } catch (_) {
    ElMessage.error('加载关注列表失败')
  }
}

const handleInteractionTabChange = async (tabName) => {
  if (tabName === 'likes') {
    await loadMyLikes(1)
    return
  }
  await loadMyCollections(1)
}

const handleInteractionPageChange = async (targetPage) => {
  if (interactionTab.value === 'likes') {
    await loadMyLikes(targetPage)
    return
  }
  await loadMyCollections(targetPage)
}

const handleSocialTabChange = async (tabName) => {
  if (tabName === 'following') {
    await loadMyFollowing(1)
    return
  }
  await loadMyFollowers(1)
}

const handleSocialPageChange = async (targetPage) => {
  if (socialTab.value === 'following') {
    await loadMyFollowing(targetPage)
    return
  }
  await loadMyFollowers(targetPage)
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const reviewText = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return '通过'
  if (status === 'rejected') return '驳回'
  return '待审核'
}

const reviewTagType = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'warning'
}

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const parseFirstImage = (images) => {
  if (!images) return ''
  const list = String(images).split(',').map((item) => item.trim()).filter(Boolean)
  return list[0] || ''
}

const getUserDisplayName = (user) => {
  if (!user) return '用户'
  return user.nickname || user.username || `用户${user.userId || ''}`
}

const goChatWithUser = (userId) => {
  if (!userId) return
  router.push({ path: '/chat', query: { targetUserId: String(userId) } })
}

const ensureFollowerChart = () => {
  if (!followerGrowthChartRef.value) return null
  if (!followerChart) {
    followerChart = echarts.init(followerGrowthChartRef.value)
  }
  return followerChart
}

const ensureContentChart = () => {
  if (!contentMetricsChartRef.value) return null
  if (!contentChart) {
    contentChart = echarts.init(contentMetricsChartRef.value)
  }
  return contentChart
}

const renderFollowerGrowthChart = async () => {
  await nextTick()
  const chart = ensureFollowerChart()
  if (!chart) return

  const labels = followerGrowth.value.map((item) => item.label)
  const followValues = followerGrowth.value.map((item) => Number(item.followCount ?? item.count ?? 0))
  const unfollowValues = followerGrowth.value.map((item) => Number(item.unfollowCount || 0))
  const netValues = followerGrowth.value.map((item) => Number(item.netCount ?? (Number(item.followCount ?? item.count ?? 0) - Number(item.unfollowCount || 0))))

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 28, right: 18, top: 18, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#8aa0b7' } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } }
    },
    series: [
      {
        name: '新增粉丝',
        type: 'line',
        smooth: true,
        data: followValues,
        symbolSize: 8,
        areaStyle: {
          color: 'rgba(14, 165, 164, 0.16)'
        },
        lineStyle: {
          width: 3,
          color: '#0ea5a4'
        },
        itemStyle: {
          color: '#0ea5a4'
        }
      },
      {
        name: '取关粉丝',
        type: 'line',
        smooth: true,
        data: unfollowValues,
        symbolSize: 7,
        lineStyle: {
          width: 2,
          color: '#ef4444'
        },
        itemStyle: {
          color: '#ef4444'
        }
      },
      {
        name: '净增长',
        type: 'bar',
        data: netValues,
        itemStyle: {
          color: '#3b82f6'
        },
        barMaxWidth: 14
      }
    ]
  })
}

// 内容互动图基于当前 myContents 列表聚合（赞/藏/评），用于用户自我复盘。
const renderContentMetricsChart = async () => {
  await nextTick()
  const chart = ensureContentChart()
  if (!chart) return

  const list = myContents.list || []
  const labels = list.map((item) => {
    const title = item.title || '未命名内容'
    return title.length > 10 ? `${title.slice(0, 10)}...` : title
  })
  const likeData = list.map((item) => Number(item.likeCount || 0))
  const collectionData = list.map((item) => Number(item.collectionCount || 0))
  const commentData = list.map((item) => Number(item.commentCount || 0))

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {
      top: 0,
      textStyle: { color: '#486078' }
    },
    grid: { left: 28, right: 12, top: 38, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#8aa0b7' } },
      axisLabel: { interval: 0 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.2)' } }
    },
    series: [
      {
        name: '点赞',
        type: 'bar',
        data: likeData,
        itemStyle: { color: '#f97316' },
        barMaxWidth: 20
      },
      {
        name: '收藏',
        type: 'bar',
        data: collectionData,
        itemStyle: { color: '#0ea5a4' },
        barMaxWidth: 20
      },
      {
        name: '评论',
        type: 'bar',
        data: commentData,
        itemStyle: { color: '#3b82f6' },
        barMaxWidth: 20
      }
    ]
  })
}

const handleResize = () => {
  followerChart?.resize()
  contentChart?.resize()
}

watch(
  () => myContents.list,
  () => {
    renderContentMetricsChart()
  },
  { deep: true }
)

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadProfile()
  await Promise.all([
    loadMyContents(1),
    loadTopViewedContents(),
    loadFollowSummary(),
    loadFollowerGrowth(),
    loadMyCollections(1),
    loadMyFollowers(1)
  ])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  followerChart?.dispose()
  contentChart?.dispose()
  followerChart = null
  contentChart = null
})
</script>

<style scoped>
.profile-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-layout-row {
  align-items: flex-start;
}

.layout-col {
  display: flex;
}

.side-col .profile-card,
.main-col .content-card {
  width: 100%;
}

.profile-hero {
  border-radius: 18px;
  padding: 18px 20px;
  background:
    radial-gradient(circle at 15% 18%, rgba(255, 255, 255, 0.26), transparent 46%),
    linear-gradient(135deg, #f97316, #fb923c 52%, #0ea5a4);
  box-shadow: 0 16px 30px rgba(35, 79, 99, 0.2);
  color: #fff7ed;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 1.4px;
  opacity: 0.9;
}

.profile-hero h1 {
  margin-top: 8px;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.3;
}

.profile-hero p {
  margin-top: 8px;
  line-height: 1.7;
  color: rgba(255, 247, 237, 0.9);
}

.profile-card {
  margin-bottom: 16px;
  border-radius: 18px;
}

.content-card {
  border-radius: 18px;
  animation-delay: 0.1s;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.card-title small {
  color: #7a889d;
}

.avatar-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.content-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.works-entry-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border-color: rgba(14, 165, 164, 0.35) !important;
  color: #0f766e !important;
  background: rgba(240, 253, 250, 0.85) !important;
  transition: transform 0.2s ease, box-shadow 0.22s ease, background-color 0.22s ease;
}

.works-entry-btn:hover,
.works-entry-btn:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(14, 165, 164, 0.18);
  background: rgba(217, 249, 244, 0.95) !important;
}

.insights-entry-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border-color: rgba(51, 65, 85, 0.24) !important;
  color: #334155 !important;
  background: rgba(248, 250, 252, 0.92) !important;
  transition: transform 0.2s ease, box-shadow 0.22s ease, background-color 0.22s ease;
}

.insights-entry-btn:hover,
.insights-entry-btn:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(51, 65, 85, 0.15);
  background: rgba(241, 245, 249, 0.98) !important;
}

.follow-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.summary-item {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(248, 252, 255, 0.85);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item span {
  font-size: 12px;
  color: #6f7f94;
}

.summary-item strong {
  font-size: 22px;
  color: #1f2d3d;
  line-height: 1.15;
}

.chart-panel {
  margin-bottom: 14px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.72);
  padding: 10px 10px 6px;
}

.chart-panel.compact {
  margin-top: 14px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.chart-head span {
  color: #2d3f57;
  font-weight: 600;
}

.chart-box {
  width: 100%;
  height: 236px;
}

.publish-entry-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 0 !important;
  background: linear-gradient(135deg, #0ea5a4, #0f8ea2) !important;
  color: #ffffff !important;
  box-shadow: 0 10px 20px rgba(14, 165, 164, 0.24);
  transition: transform 0.2s ease, box-shadow 0.22s ease, filter 0.22s ease;
}

.publish-entry-btn :deep(span) {
  color: #ffffff !important;
}

.publish-entry-btn:hover,
.publish-entry-btn:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px rgba(14, 165, 164, 0.3);
  filter: saturate(1.05);
}

.table-action-btn {
  height: 26px;
  min-width: 52px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent !important;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, filter 0.2s ease;
}

.compact-table :deep(.el-table__cell) {
  padding-top: 8px;
  padding-bottom: 8px;
}

.table-action-btn:hover,
.table-action-btn:focus-visible {
  transform: translateY(0) !important;
}

.table-action-btn :deep(span) {
  color: inherit !important;
}

.table-action-btn.view {
  color: #0f766e !important;
  background: linear-gradient(180deg, rgba(14, 165, 164, 0.11), rgba(14, 165, 164, 0.05)) !important;
  border-color: rgba(14, 165, 164, 0.34) !important;
}

.table-action-btn.view:hover,
.table-action-btn.view:focus-visible {
  background: linear-gradient(180deg, rgba(14, 165, 164, 0.2), rgba(14, 165, 164, 0.1)) !important;
  border-color: rgba(14, 165, 164, 0.48) !important;
  box-shadow: 0 6px 12px rgba(14, 165, 164, 0.14);
  filter: saturate(1.06);
}

.top-hint {
  margin-top: 10px;
  color: #5b6d83;
  font-size: 13px;
  line-height: 1.6;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.list-panel {
  margin-top: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.74);
  padding: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 6px;
}

.history-list,
.user-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 6px;
}

.history-item,
.user-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(248, 252, 255, 0.82);
}

.history-cover {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.history-cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: #6b7d92;
  background: rgba(226, 232, 240, 0.8);
}

.history-body,
.user-body {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-title {
  font-size: 15px;
  color: #1f2d3d;
  font-weight: 600;
}

.history-title:hover {
  color: #0c8c8b;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #5b6d83;
}

.history-time,
.user-time {
  font-size: 12px;
  color: #7a889d;
}

.user-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name-row strong {
  color: #203247;
  font-size: 14px;
}

.user-bio {
  margin: 0;
  color: #5b6d83;
  font-size: 13px;
  line-height: 1.6;
}

.chat-btn {
  border-radius: 999px;
  padding: 0 10px;
}

@media (min-width: 1200px) {
  .side-col .profile-card {
    position: sticky;
    top: 92px;
  }
}

@media (max-width: 1199px) {
  .profile-card {
    margin-bottom: 0;
  }
}

@media (max-width: 900px) {
  .follow-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .content-header {
    gap: 10px;
    flex-wrap: wrap;
  }

  .content-header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .works-entry-btn,
  .insights-entry-btn,
  .publish-entry-btn {
    height: 32px;
    padding: 0 14px;
  }

  .table-action-btn {
    min-width: 44px;
    padding: 0 8px;
    font-size: 11px;
  }

  .chart-box {
    height: 230px;
  }

  .history-item,
  .user-item {
    padding: 8px;
  }

  .history-cover {
    width: 56px;
    height: 56px;
  }
}

@media (max-width: 560px) {
  .follow-summary {
    grid-template-columns: 1fr;
  }
}
</style>
