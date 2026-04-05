<template>
  <section class="insights-page">
    <el-card class="insights-filter-card motion-fade-soft">
      <div class="filters">
        <el-select v-model="filters.granularity" class="filter-item" @change="handleGranularityChange">
          <el-option label="按天" value="day" />
          <el-option label="按周" value="week" />
          <el-option label="按月" value="month" />
        </el-select>
        <el-input-number
          v-model="filters.days"
          class="filter-item"
          :min="1"
          :max="maxDays"
          :controls="false"
          @change="handleDaysChange"
        />
        <el-button type="primary" class="refresh-btn" :loading="loading" @click="loadAll">刷新分析</el-button>
      </div>
    </el-card>

    <el-card class="insights-card motion-fade-soft">
      <template #header>
        <span>治理健康提醒</span>
      </template>
      <div class="summary-grid governance-grid">
        <div class="summary-item warning"><span>待审核</span><strong>{{ governance.pendingContentCount || 0 }}</strong></div>
        <div class="summary-item danger"><span>已下架</span><strong>{{ governance.offShelfContentCount || 0 }}</strong></div>
        <div class="summary-item"><span>被举报总量</span><strong>{{ governance.reportTotalCount || 0 }}</strong></div>
        <div class="summary-item"><span>待处理举报</span><strong>{{ governance.reportPendingCount || 0 }}</strong></div>
      </div>
    </el-card>

    <el-card class="insights-card motion-fade-soft">
      <template #header>
        <span>作品表现 Top5</span>
      </template>
      <el-table :data="topContents" class="insights-table" size="small" empty-text="暂无作品数据">
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="100" align="center" />
        <el-table-column prop="viewCount" label="浏览" width="72" align="center" />
        <el-table-column prop="likeCount" label="点赞" width="72" align="center" />
        <el-table-column prop="collectionCount" label="收藏" width="72" align="center" />
        <el-table-column prop="commentCount" label="评论" width="72" align="center" />
        <el-table-column label="互动率" width="92" align="center">
          <template #default="{ row }">{{ formatRate(row.engagementRate) }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="150" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="insights-card motion-fade-soft">
      <template #header>
        <span>趋势图</span>
      </template>
      <div ref="trendChartRef" class="chart-box"></div>
    </el-card>

    <el-card class="insights-card motion-fade-soft">
      <template #header>
        <div class="card-head">
          <span>标签/分类效果</span>
          <el-tabs v-model="taxonomyTab" class="taxonomy-tabs" @tab-change="renderTaxonomyChart">
            <el-tab-pane label="分类" name="categories" />
            <el-tab-pane label="标签" name="tags" />
          </el-tabs>
        </div>
      </template>
      <div ref="taxonomyChartRef" class="chart-box"></div>
      <el-table :data="currentTaxonomyRows" class="insights-table taxonomy-table" size="small" empty-text="暂无数据">
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="contentCount" label="作品数" width="84" align="center" />
        <el-table-column prop="totalEngagement" label="总互动" width="84" align="center" />
        <el-table-column label="平均互动率" width="110" align="center">
          <template #default="{ row }">{{ formatRate(row.avgEngagementRate) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="insights-card motion-fade-soft">
      <template #header>
        <span>发布时间段分析</span>
      </template>
      <div ref="publishTimeChartRef" class="chart-box"></div>
      <div class="recommend-wrap">
        <span class="recommend-label">推荐时段：</span>
        <el-tag
          v-for="slot in recommendedSlots"
          :key="slot.label"
          class="recommend-tag"
          type="success"
          effect="light"
        >
          {{ slot.label }}（{{ slot.contentCount || 0 }}条，{{ formatRate(slot.engagementRate) }}）
        </el-tag>
        <span v-if="recommendedSlots.length === 0" class="empty-tip">暂无足够数据生成推荐时段</span>
      </div>
    </el-card>

  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getMyInsightsGovernance,
  getMyInsightsPublishTime,
  getMyInsightsTaxonomy,
  getMyInsightsTopContents,
  getMyInsightsTrend
} from '../api/insights'

/**
 * 用户数据分析页职责（/insights）：
 * 1. 聚合 5 组统计（治理提醒/Top5/趋势/标签分类/发布时间段）。
 * 2. 统一筛选粒度与时间窗，保证各图口径一致可对账。
 * 3. 在无数据时返回空态，不抛前端异常。
 */
const loading = ref(false)
const taxonomyTab = ref('categories')

const filters = reactive({
  granularity: 'day',
  days: 30
})

const topContents = ref([])
const trendPoints = ref([])
const taxonomy = reactive({
  categories: [],
  tags: []
})
const publishTimePoints = ref([])
const recommendedSlots = ref([])
const governance = reactive({
  pendingContentCount: 0,
  offShelfContentCount: 0,
  reportTotalCount: 0,
  reportPendingCount: 0
})

const trendChartRef = ref(null)
const taxonomyChartRef = ref(null)
const publishTimeChartRef = ref(null)

let trendChart = null
let taxonomyChart = null
let publishTimeChart = null

const maxDays = computed(() => {
  if (filters.granularity === 'week') return 52
  if (filters.granularity === 'month') return 24
  return 90
})

const currentTaxonomyRows = computed(() => (
  taxonomyTab.value === 'tags' ? taxonomy.tags : taxonomy.categories
))

// 互动率统一按 0~1 小数转百分比展示，分母为 0 的情况后端已兜底返回 0。
const formatRate = (value) => {
  const number = Number(value || 0)
  return `${(number * 100).toFixed(2)}%`
}

const formatTime = (timeString) => {
  if (!timeString) return '-'
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return '-'
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 除治理提醒外，其余图表默认跟随筛选条件（granularity + days）。
const commonParams = () => ({
  granularity: filters.granularity,
  days: filters.days
})

// Top5 采用后端排序结果，前端不再二次排序，避免口径偏移。
const loadTopContents = async () => {
  const response = await getMyInsightsTopContents({ limit: 5 })
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载 Top5 失败')
  }
  topContents.value = Array.isArray(response.data.data.list) ? response.data.data.list : []
}

const loadTrend = async () => {
  const response = await getMyInsightsTrend(commonParams())
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载趋势失败')
  }
  trendPoints.value = Array.isArray(response.data.data.points) ? response.data.data.points : []
}

const loadTaxonomy = async () => {
  const response = await getMyInsightsTaxonomy({ days: filters.days })
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载标签/分类统计失败')
  }
  taxonomy.categories = Array.isArray(response.data.data.categories) ? response.data.data.categories : []
  taxonomy.tags = Array.isArray(response.data.data.tags) ? response.data.data.tags : []
}

const loadPublishTime = async () => {
  const response = await getMyInsightsPublishTime({ days: filters.days })
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载发布时间段失败')
  }
  publishTimePoints.value = Array.isArray(response.data.data.points) ? response.data.data.points : []
  recommendedSlots.value = Array.isArray(response.data.data.recommendedSlots) ? response.data.data.recommendedSlots : []
}

const loadGovernance = async () => {
  const response = await getMyInsightsGovernance()
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || '加载治理提醒失败')
  }
  const data = response.data.data
  governance.pendingContentCount = Number(data.pendingContentCount || 0)
  governance.offShelfContentCount = Number(data.offShelfContentCount || 0)
  governance.reportTotalCount = Number(data.reportTotalCount || 0)
  governance.reportPendingCount = Number(data.reportPendingCount || 0)
}

// 图表实例懒初始化，避免重复 init 导致内存泄漏或 resize 异常。
const ensureCharts = () => {
  if (trendChartRef.value && !trendChart) trendChart = echarts.init(trendChartRef.value)
  if (taxonomyChartRef.value && !taxonomyChart) taxonomyChart = echarts.init(taxonomyChartRef.value)
  if (publishTimeChartRef.value && !publishTimeChart) publishTimeChart = echarts.init(publishTimeChartRef.value)
}

// 趋势图展示“浏览+互动+新增粉丝”联合序列，用于观察内容与增长联动。
const renderTrendChart = () => {
  const labels = trendPoints.value.map((item) => item.label)
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 30, right: 18, top: 38, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '浏览', type: 'bar', data: trendPoints.value.map((item) => Number(item.viewCount || 0)), barMaxWidth: 16 },
      { name: '点赞', type: 'line', smooth: true, data: trendPoints.value.map((item) => Number(item.likeCount || 0)) },
      { name: '收藏', type: 'line', smooth: true, data: trendPoints.value.map((item) => Number(item.collectionCount || 0)) },
      { name: '评论', type: 'line', smooth: true, data: trendPoints.value.map((item) => Number(item.commentCount || 0)) },
      { name: '新增粉丝', type: 'line', smooth: true, data: trendPoints.value.map((item) => Number(item.newFollowerCount || 0)) }
    ]
  })
}

// 标签/分类共用一张图，通过 tab 切换数据集，降低图表理解成本。
const renderTaxonomyChart = () => {
  const rows = currentTaxonomyRows.value.slice(0, 12)
  taxonomyChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 30, right: 30, top: 38, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: rows.map((item) => item.name) },
    yAxis: [
      { type: 'value', minInterval: 1, name: '总互动' },
      { type: 'value', min: 0, name: '互动率' }
    ],
    series: [
      { name: '总互动', type: 'bar', data: rows.map((item) => Number(item.totalEngagement || 0)), barMaxWidth: 16 },
      { name: '平均互动率', type: 'line', smooth: true, yAxisIndex: 1, data: rows.map((item) => Number(item.avgEngagementRate || 0)) }
    ]
  })
}

const renderPublishTimeChart = () => {
  const labels = publishTimePoints.value.map((item) => item.label)
  publishTimeChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 30, right: 24, top: 38, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: labels },
    yAxis: [
      { type: 'value', minInterval: 1, name: '作品数' },
      { type: 'value', min: 0, name: '互动率' }
    ],
    series: [
      { name: '作品数', type: 'bar', data: publishTimePoints.value.map((item) => Number(item.contentCount || 0)), barMaxWidth: 12 },
      { name: '互动率', type: 'line', smooth: true, yAxisIndex: 1, data: publishTimePoints.value.map((item) => Number(item.engagementRate || 0)) }
    ]
  })
}

const renderAllCharts = async () => {
  await nextTick()
  ensureCharts()
  renderTrendChart()
  renderTaxonomyChart()
  renderPublishTimeChart()
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      // 页面顶部优先展示治理风险态势，其余图表并行加载。
      loadGovernance(),
      loadTopContents(),
      loadTrend(),
      loadTaxonomy(),
      loadPublishTime()
    ])
    await renderAllCharts()
  } catch (error) {
    ElMessage.error(error?.message || '加载数据分析失败')
  } finally {
    loading.value = false
  }
}

// 粒度切换后自动裁剪 days 到合法范围，避免后端收到越界参数。
const handleGranularityChange = () => {
  if (filters.days > maxDays.value) {
    filters.days = maxDays.value
  }
  loadAll()
}

const handleDaysChange = () => {
  if (filters.days < 1) {
    filters.days = 1
  }
  if (filters.days > maxDays.value) {
    filters.days = maxDays.value
  }
  loadAll()
}

const handleResize = () => {
  trendChart?.resize()
  taxonomyChart?.resize()
  publishTimeChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  taxonomyChart?.dispose()
  publishTimeChart?.dispose()
  trendChart = null
  taxonomyChart = null
  publishTimeChart = null
})
</script>

<style scoped>
.insights-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insights-filter-card,
.insights-card {
  border-radius: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-item {
  width: 180px;
}

.refresh-btn {
  min-height: 40px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-head small {
  color: #6b7b91;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.summary-item {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.76);
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
  font-size: 22px;
}

.summary-item.warning {
  border-color: rgba(245, 158, 11, 0.36);
  background: rgba(255, 251, 235, 0.85);
}

.summary-item.danger {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(254, 242, 242, 0.85);
}

.governance-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.chart-box {
  width: 100%;
  height: 330px;
}

.insights-table {
  margin-top: 6px;
}

.taxonomy-tabs {
  margin-left: auto;
}

.taxonomy-table {
  margin-top: 10px;
}

.recommend-wrap {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.recommend-label {
  color: #516178;
  font-size: 13px;
}

.recommend-tag {
  border-radius: 999px;
}

.empty-tip {
  color: #8292a9;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .governance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .filter-item {
    width: 100%;
  }

  .refresh-btn {
    width: 100%;
  }

  .summary-grid,
  .governance-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .chart-box {
    height: 280px;
  }
}
</style>
