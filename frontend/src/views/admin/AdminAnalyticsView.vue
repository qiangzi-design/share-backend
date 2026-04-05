<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-3col">
        <el-select v-model="filters.granularity" placeholder="粒度">
          <el-option label="按天" value="day" />
          <el-option label="按月" value="month" />
        </el-select>
        <el-input-number v-model="filters.days" :min="1" :max="filters.granularity === 'day' ? 90 : 36" />
        <el-button type="primary" class="query-btn" @click="loadAll">刷新分析</el-button>
      </div>
    </el-card>

    <el-card class="metric-card">
      <template #header><span>内容质量概览</span></template>
      <div class="summary-grid">
        <div class="summary-item"><span>浏览</span><strong>{{ contentSummary.totalViews || 0 }}</strong></div>
        <div class="summary-item"><span>点赞</span><strong>{{ contentSummary.totalLikes || 0 }}</strong></div>
        <div class="summary-item"><span>收藏</span><strong>{{ contentSummary.totalCollections || 0 }}</strong></div>
        <div class="summary-item"><span>评论</span><strong>{{ contentSummary.totalComments || 0 }}</strong></div>
        <div class="summary-item"><span>互动率</span><strong>{{ formatRate(contentSummary.overallEngagementRate) }}</strong></div>
      </div>
      <div ref="contentChartRef" class="chart-box"></div>
    </el-card>

    <el-card class="metric-card">
      <template #header><span>用户增长概览</span></template>
      <div class="summary-grid">
        <div class="summary-item"><span>新增用户</span><strong>{{ userSummary.totalNewUsers || 0 }}</strong></div>
        <div class="summary-item"><span>平均活跃</span><strong>{{ userSummary.avgActiveUsers || 0 }}</strong></div>
      </div>
      <div ref="userChartRef" class="chart-box"></div>
    </el-card>

    <el-card class="metric-card">
      <template #header><span>审核效率概览</span></template>
      <div class="summary-grid">
        <div class="summary-item"><span>处理量</span><strong>{{ modSummary.processedCount || 0 }}</strong></div>
        <div class="summary-item"><span>解决量</span><strong>{{ modSummary.resolvedCount || 0 }}</strong></div>
        <div class="summary-item"><span>解决率</span><strong>{{ formatRate(modSummary.resolveRate) }}</strong></div>
        <div class="summary-item"><span>平均耗时(分钟)</span><strong>{{ modSummary.avgHandleMinutes || 0 }}</strong></div>
      </div>
      <div ref="moderationChartRef" class="chart-box"></div>
    </el-card>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getAdminContentQualityAnalytics,
  getAdminModerationEfficiencyAnalytics,
  getAdminUserGrowthAnalytics
} from '../../api/admin'

/**
 * 管理端-运营分析页职责：
 * 1. 聚合内容质量、用户增长、审核效率三组管理指标。
 * 2. 统一按 granularity + days 过滤，保持各图时间窗一致。
 * 3. 仅做只读展示，不在本页承载治理动作。
 */
const filters = reactive({
  granularity: 'day',
  days: 14
})

const contentPoints = ref([])
const userPoints = ref([])
const modPoints = ref([])
const contentSummary = ref({})
const userSummary = ref({})
const modSummary = ref({})

const contentChartRef = ref(null)
const userChartRef = ref(null)
const moderationChartRef = ref(null)

let contentChart = null
let userChart = null
let moderationChart = null

const formatRate = (value) => {
  const number = Number(value || 0)
  return `${(number * 100).toFixed(2)}%`
}

// 所有统计接口共用同一参数结构，避免前后端字段漂移。
const commonParams = () => ({
  granularity: filters.granularity,
  days: filters.days
})

const loadContentQuality = async () => {
  const response = await getAdminContentQualityAnalytics(commonParams())
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载内容质量失败')
  }
  contentSummary.value = response.data.data.summary || {}
  contentPoints.value = response.data.data.points || []
}

const loadUserGrowth = async () => {
  const response = await getAdminUserGrowthAnalytics(commonParams())
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载用户增长失败')
  }
  userSummary.value = response.data.data.summary || {}
  userPoints.value = response.data.data.points || []
}

const loadModeration = async () => {
  const response = await getAdminModerationEfficiencyAnalytics(commonParams())
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载审核效率失败')
  }
  modSummary.value = response.data.data.summary || {}
  modPoints.value = response.data.data.points || []
}

// 图表实例按需初始化，防止重复 init 造成性能损耗。
const ensureCharts = () => {
  if (contentChartRef.value && !contentChart) contentChart = echarts.init(contentChartRef.value)
  if (userChartRef.value && !userChart) userChart = echarts.init(userChartRef.value)
  if (moderationChartRef.value && !moderationChart) moderationChart = echarts.init(moderationChartRef.value)
}

// 三张图分别对应“质量/增长/效率”，强调管理视角而非内容详情视角。
const renderCharts = async () => {
  await nextTick()
  ensureCharts()
  const labels = contentPoints.value.map((item) => item.label)
  contentChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 4 },
    grid: { left: 30, right: 20, top: 40, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '点赞', type: 'line', smooth: true, data: contentPoints.value.map((item) => item.likeCount || 0) },
      { name: '收藏', type: 'line', smooth: true, data: contentPoints.value.map((item) => item.collectionCount || 0) },
      { name: '评论', type: 'line', smooth: true, data: contentPoints.value.map((item) => item.commentCount || 0) },
      { name: '浏览', type: 'bar', data: contentPoints.value.map((item) => item.viewCount || 0), barMaxWidth: 18 }
    ]
  })

  const userLabels = userPoints.value.map((item) => item.label)
  userChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 4 },
    grid: { left: 30, right: 20, top: 40, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: userLabels },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增用户', type: 'bar', data: userPoints.value.map((item) => item.newUserCount || 0), barMaxWidth: 18 },
      { name: '活跃用户', type: 'line', smooth: true, data: userPoints.value.map((item) => item.activeUserCount || 0) }
    ]
  })

  const modLabels = modPoints.value.map((item) => item.label)
  moderationChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 4 },
    grid: { left: 30, right: 20, top: 40, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: modLabels },
    yAxis: [
      { type: 'value', minInterval: 1, name: '数量' },
      { type: 'value', min: 0, max: 1, name: '比率' }
    ],
    series: [
      { name: '处理量', type: 'bar', data: modPoints.value.map((item) => item.processedCount || 0), barMaxWidth: 18 },
      { name: '解决量', type: 'bar', data: modPoints.value.map((item) => item.resolvedCount || 0), barMaxWidth: 18 },
      { name: '解决率', type: 'line', yAxisIndex: 1, smooth: true, data: modPoints.value.map((item) => item.resolveRate || 0) }
    ]
  })
}

const loadAll = async () => {
  try {
    await Promise.all([loadContentQuality(), loadUserGrowth(), loadModeration()])
    await renderCharts()
  } catch (error) {
    ElMessage.error(error?.message || '加载分析失败')
  }
}

const handleResize = () => {
  contentChart?.resize()
  userChart?.resize()
  moderationChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  contentChart?.dispose()
  userChart?.dispose()
  moderationChart?.dispose()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.metric-card {
  border-radius: 16px;
}

.filters {
  display: grid;
  align-items: center;
  gap: 12px;
}

.filters-3col {
  grid-template-columns: 180px 180px 100px;
}

.query-btn {
  min-height: 42px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.summary-item {
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.74);
  border-radius: 12px;
  padding: 10px;
}

.summary-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 4px;
  color: #1f2937;
  font-size: 20px;
}

.chart-box {
  width: 100%;
  height: 320px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
