<template>
  <section class="ai-brief-page">
    <el-card class="brief-card">
      <template #header>
        <div class="brief-head">
          <div>
            <h2>每日AI快讯</h2>
            <p class="brief-sub">每天上午9点自动更新，聚合公开热点事件。</p>
          </div>
          <div class="head-tools">
            <el-tag v-if="todayBrief.isFallback" type="warning" effect="light">展示最近一期</el-tag>
            <el-button :loading="loadingToday" @click="loadTodayBrief">刷新</el-button>
          </div>
        </div>
      </template>

      <el-skeleton :loading="loadingToday" animated :rows="5">
        <template #default>
          <div class="today-summary">
            <h3>{{ todayBrief.title || '每日AI快讯' }}</h3>
            <p class="summary-text">{{ todayBrief.summary || '暂无快讯摘要' }}</p>
            <div class="meta-row">
              <span>日期：{{ formatDate(todayBrief.date) }}</span>
              <span>来源数：{{ todayBrief.sourceCount || 0 }}</span>
              <span>条目数：{{ todayBrief.itemCount || 0 }}</span>
              <span>生成时间：{{ formatDateTime(todayBrief.generatedAt) }}</span>
            </div>
          </div>

          <el-empty
            v-if="!todayBrief.items || todayBrief.items.length === 0"
            description="今日暂无可展示的AI热点"
          />

          <ul v-else class="item-list">
            <li v-for="item in todayBrief.items" :key="`${todayBrief.date}-${item.rank}-${item.title}`" class="item-row">
              <div class="rank">#{{ item.rank }}</div>
              <div class="item-main">
                <div class="item-title-row">
                  <h4>{{ item.title }}</h4>
                  <el-tag size="small" type="info">热度 {{ item.score || 0 }}</el-tag>
                </div>
                <p v-if="item.summary" class="item-summary">{{ item.summary }}</p>
                <div class="item-meta">
                  <span>{{ item.sourceName || '未知来源' }}</span>
                  <span v-if="item.eventTime">{{ formatDateTime(item.eventTime) }}</span>
                  <a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">查看来源</a>
                </div>
              </div>
            </li>
          </ul>
        </template>
      </el-skeleton>
    </el-card>

    <el-card class="history-card">
      <template #header>
        <div class="history-head">
          <span>历史快讯</span>
          <el-select v-model="historyDays" style="width: 140px" @change="loadHistoryBrief">
            <el-option :value="7" label="近7天" />
            <el-option :value="14" label="近14天" />
            <el-option :value="30" label="近30天" />
          </el-select>
        </div>
      </template>

      <el-skeleton :loading="loadingHistory" animated :rows="4">
        <template #default>
          <el-empty v-if="historyList.length === 0" description="暂无历史快讯" />
          <ul v-else class="history-list">
            <li v-for="row in historyList" :key="row.id || row.date" class="history-row">
              <div class="history-date">{{ formatDate(row.date) }}</div>
              <div class="history-main">
                <h4>{{ row.title }}</h4>
                <p>{{ row.summary || '暂无摘要' }}</p>
              </div>
              <div class="history-metrics">
                <span>来源 {{ row.sourceCount || 0 }}</span>
                <span>条目 {{ row.itemCount || 0 }}</span>
              </div>
            </li>
          </ul>
        </template>
      </el-skeleton>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiBriefHistory, getTodayAiBrief } from '../api/aiBrief'

/**
 * AI快讯页面职责：
 * 1. 展示“今日快讯”完整条目；
 * 2. 展示“历史快讯”摘要，便于追溯热点变化；
 * 3. 当今日未生成时，展示后端回退的最近一期快讯。
 */
const loadingToday = ref(false)
const loadingHistory = ref(false)
const historyDays = ref(7)

const todayBrief = ref({
  date: null,
  title: '',
  summary: '',
  sourceCount: 0,
  itemCount: 0,
  generatedAt: null,
  isFallback: false,
  items: []
})

const historyList = ref([])

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString('zh-CN')
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

// 加载当日快讯：后端会自动处理“今日无数据时回退最近一期”的兜底逻辑。
const loadTodayBrief = async () => {
  loadingToday.value = true
  try {
    const response = await getTodayAiBrief()
    if (response.data?.code === 200 && response.data?.data) {
      const data = response.data.data
      todayBrief.value = {
        ...todayBrief.value,
        ...data,
        items: Array.isArray(data.items) ? data.items : []
      }
      return
    }
    ElMessage.warning('AI快讯暂不可用，请稍后再试')
  } catch (_) {
    ElMessage.error('加载AI快讯失败')
  } finally {
    loadingToday.value = false
  }
}

// 加载历史快讯：仅展示摘要，不重复渲染完整条目，避免页面信息过载。
const loadHistoryBrief = async () => {
  loadingHistory.value = true
  try {
    const response = await getAiBriefHistory({ days: historyDays.value })
    if (response.data?.code === 200 && Array.isArray(response.data?.data)) {
      historyList.value = response.data.data
      return
    }
    historyList.value = []
  } catch (_) {
    historyList.value = []
    ElMessage.error('加载历史快讯失败')
  } finally {
    loadingHistory.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadTodayBrief(), loadHistoryBrief()])
})
</script>

<style scoped>
.ai-brief-page {
  display: grid;
  gap: 12px;
}

.brief-card,
.history-card {
  border-radius: 14px;
}

.brief-head,
.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.brief-head h2 {
  margin: 0;
  font-size: 28px;
  color: #1b2f4a;
}

.brief-sub {
  margin: 4px 0 0;
  color: #6b7d93;
}

.head-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.today-summary {
  margin-bottom: 12px;
}

.today-summary h3 {
  margin: 0;
  color: #0f2742;
}

.summary-text {
  margin: 8px 0;
  color: #4c647f;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #6b7d93;
}

.item-list,
.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.item-row,
.history-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: flex-start;
  border: 1px solid #dbe6f1;
  border-radius: 12px;
  padding: 12px;
  background: #f8fbff;
}

.rank {
  min-width: 42px;
  height: 30px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0ea5a4, #0284c7);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.item-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.item-title-row h4,
.history-main h4 {
  margin: 0;
  font-size: 18px;
  color: #162d4a;
}

.item-summary,
.history-main p {
  margin: 8px 0 0;
  color: #506985;
  line-height: 1.6;
}

.item-meta,
.history-metrics {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #6b7d93;
  align-items: center;
}

.item-meta a {
  color: #0c8c8b;
  text-decoration: none;
  font-weight: 600;
}

.item-meta a:hover {
  text-decoration: underline;
}

.history-date {
  min-width: 86px;
  font-size: 13px;
  color: #5f748d;
}

@media (max-width: 768px) {
  .item-row,
  .history-row {
    grid-template-columns: 1fr;
  }

  .history-date {
    min-width: auto;
  }
}
</style>
