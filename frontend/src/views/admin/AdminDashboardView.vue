<template>
  <section class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h2>管理驾驶舱</h2>
        <p>支持手动补跑每日 AI 快讯，便于抓取失败时快速恢复当天数据。</p>
      </div>
      <el-button type="primary" :loading="refreshingAiBrief" @click="handleRefreshAiBrief">
        手动刷新 AI 快讯
      </el-button>
    </div>

    <div class="metric-grid">
      <el-card v-for="item in metricCards" :key="item.key" class="metric-card">
        <p class="metric-label">{{ item.label }}</p>
        <strong class="metric-value">{{ item.value }}</strong>
      </el-card>
    </div>

    <el-card class="chart-card">
      <template #header>
        <div class="chart-head">
          <span>趋势统计</span>
          <div class="chart-tools">
            <el-radio-group v-model="granularity" size="small" @change="loadTrends">
              <el-radio-button label="day">按天</el-radio-button>
              <el-radio-button label="month">按月</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div ref="chartRef" class="chart-box"></div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAdminDashboardOverview, getAdminDashboardTrends, refreshAdminAiBrief } from '../../api/admin'

const granularity = ref('day')
const overview = ref({
  userCount: 0,
  activeUserCount: 0,
  contentCount: 0,
  commentCount: 0,
  chatMessageCount: 0,
  reportCount: 0,
  pendingReportCount: 0,
  dailyUv: 0,
  monthlyUv: 0
})
const trendPoints = ref([])
const refreshingAiBrief = ref(false)
const chartRef = ref(null)
let chart = null

const metricCards = computed(() => [
  { key: 'userCount', label: '用户总量', value: overview.value.userCount || 0 },
  { key: 'activeUserCount', label: '活跃用户', value: overview.value.activeUserCount || 0 },
  { key: 'contentCount', label: '可见内容', value: overview.value.contentCount || 0 },
  { key: 'commentCount', label: '可见评论', value: overview.value.commentCount || 0 },
  { key: 'chatMessageCount', label: '私聊消息', value: overview.value.chatMessageCount || 0 },
  { key: 'reportCount', label: '举报总量', value: overview.value.reportCount || 0 },
  { key: 'pendingReportCount', label: '待处理举报', value: overview.value.pendingReportCount || 0 },
  { key: 'dailyUv', label: '今日UV', value: overview.value.dailyUv || 0 },
  { key: 'monthlyUv', label: '本月UV', value: overview.value.monthlyUv || 0 }
])

const ensureChart = () => {
  if (!chartRef.value) return null
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  return chart
}

const renderChart = async () => {
  await nextTick()
  const instance = ensureChart()
  if (!instance) return

  const labels = trendPoints.value.map((item) => item.label)
  instance.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      top: 2,
      textStyle: { color: '#4b6078' }
    },
    grid: { left: 30, right: 16, top: 36, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: trendPoints.value.map((item) => item.userCount || 0),
        lineStyle: { color: '#0ea5a4', width: 3 },
        itemStyle: { color: '#0ea5a4' },
        areaStyle: { color: 'rgba(14, 165, 164, 0.12)' }
      },
      {
        name: '新增内容',
        type: 'line',
        smooth: true,
        data: trendPoints.value.map((item) => item.contentCount || 0),
        lineStyle: { color: '#f97316', width: 2 },
        itemStyle: { color: '#f97316' }
      },
      {
        name: '新增评论',
        type: 'line',
        smooth: true,
        data: trendPoints.value.map((item) => item.commentCount || 0),
        lineStyle: { color: '#3b82f6', width: 2 },
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '新增举报',
        type: 'bar',
        data: trendPoints.value.map((item) => item.reportCount || 0),
        itemStyle: { color: '#ef4444' },
        barMaxWidth: 16
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        data: trendPoints.value.map((item) => item.uvCount || 0),
        lineStyle: { color: '#8b5cf6', width: 2 },
        itemStyle: { color: '#8b5cf6' }
      }
    ]
  })
}

const loadOverview = async () => {
  const response = await getAdminDashboardOverview()
  if (response.data?.code === 200 && response.data?.data) {
    overview.value = {
      ...overview.value,
      ...response.data.data
    }
  }
}

const loadTrends = async () => {
  const days = granularity.value === 'day' ? 14 : 12
  const response = await getAdminDashboardTrends({
    granularity: granularity.value,
    days
  })
  if (response.data?.code === 200 && response.data?.data?.points) {
    trendPoints.value = response.data.data.points
  } else {
    trendPoints.value = []
  }
  await renderChart()
}

/**
 * 管理员可在仪表盘一键触发 AI 快讯补跑。
 * 触发成功后会立刻刷新仪表盘指标与趋势，确保可视化数据同步更新。
 */
const handleRefreshAiBrief = async () => {
  if (refreshingAiBrief.value) return
  refreshingAiBrief.value = true
  try {
    const response = await refreshAdminAiBrief()
    if (response.data?.code !== 200 || !response.data?.data) {
      ElMessage.error(response.data?.message || 'AI快讯刷新失败')
      return
    }

    const data = response.data.data
    const briefDate = data.date || '今日'
    const itemCount = Number(data.itemCount || 0)
    ElMessage.success(`AI快讯刷新成功：${briefDate}（共 ${itemCount} 条）`)

    await Promise.all([loadOverview(), loadTrends()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || 'AI快讯刷新失败，请稍后重试')
  } finally {
    refreshingAiBrief.value = false
  }
}

const onResize = () => {
  chart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await Promise.all([loadOverview(), loadTrends()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 0;
}

.head-text h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2d3d;
}

.head-text p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #67809b;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  border-radius: 12px;
}

.metric-label {
  font-size: 12px;
  color: #67809b;
}

.metric-value {
  font-size: 30px;
  color: #1f2d3d;
  line-height: 1.15;
}

.chart-card {
  border-radius: 12px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-head span {
  font-weight: 600;
}

.chart-box {
  width: 100%;
  height: 360px;
}

@media (max-width: 1400px) {
  .metric-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
