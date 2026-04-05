<template>
  <section class="my-reports-page">
    <el-card class="filter-card motion-fade-up">
      <div class="filters">
        <el-select v-model="filters.status" clearable placeholder="工单状态" style="width: 180px">
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="assigned" />
          <el-option label="已处理" value="resolved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-select v-model="filters.targetType" clearable placeholder="目标类型" style="width: 160px">
          <el-option label="内容" value="content" />
          <el-option label="评论" value="comment" />
          <el-option label="用户" value="user" />
        </el-select>
        <el-button type="primary" @click="loadReports(1)">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card motion-fade-soft">
      <template #header>
        <div class="table-head">
          <h2>我的举报记录</h2>
          <span>共 {{ pageData.total }} 条</span>
        </div>
      </template>

      <el-table :data="pageData.list">
        <el-table-column prop="id" label="ID" width="88" />
        <el-table-column label="举报目标" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.targetSummary || `${targetTypeLabel(row.targetType)} #${row.targetId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="处理结论" width="120">
          <template #default="{ row }">{{ resolveActionLabel(row.resolveAction) }}</template>
        </el-table-column>
        <el-table-column label="处理备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handleNote || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170">
          <template #default="{ row }">{{ formatTime(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button size="small" @click="openPreview(row)">详情</el-button>
              <el-button size="small" type="primary" plain @click="jumpToTarget(row)">定位</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, ->, total"
          :current-page="pageData.page"
          :page-size="pageData.pageSize"
          :total="pageData.total"
          @current-change="loadReports"
        />
      </div>
    </el-card>

    <el-dialog v-model="preview.visible" title="举报详情" width="720px">
      <div class="preview-meta">
        <div><strong>ID：</strong>{{ preview.row?.id || '-' }}</div>
        <div><strong>状态：</strong>{{ statusLabel(preview.row?.status) }}</div>
        <div><strong>目标：</strong>{{ preview.row?.targetSummary || '-' }}</div>
      </div>
      <div class="preview-layout">
        <div class="preview-block">
          <h4>当前目标数据</h4>
          <pre>{{ toPrettyJson(preview.current) }}</pre>
        </div>
        <div class="preview-block">
          <h4>举报时快照</h4>
          <pre>{{ toPrettyJson(preview.snapshot) }}</pre>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyReports } from '../api/report'

/**
 * 我的举报记录页职责：
 * - 展示举报工单状态流转；
 * - 支持查看目标快照；
 * - 支持按目标类型一键定位。
 */
const router = useRouter()

const filters = reactive({
  status: '',
  targetType: ''
})

const pageData = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0
})

const preview = reactive({
  visible: false,
  row: null,
  current: null,
  snapshot: null
})

const statusLabel = (status) => {
  if (status === 'pending') return '待处理'
  if (status === 'assigned') return '处理中'
  if (status === 'resolved') return '已处理'
  if (status === 'rejected') return '已驳回'
  return status || '-'
}

const statusTagType = (status) => {
  if (status === 'pending') return 'warning'
  if (status === 'assigned') return 'primary'
  if (status === 'resolved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'info'
}

const targetTypeLabel = (targetType) => {
  if (targetType === 'content') return '内容'
  if (targetType === 'comment') return '评论'
  if (targetType === 'user') return '用户'
  return targetType || '目标'
}

const resolveActionLabel = (action) => {
  if (!action) return '-'
  if (action === 'resolved') return '已处理'
  if (action === 'rejected') return '已驳回'
  return action
}

const formatTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const toPrettyJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(value, null, 2)
  } catch (_) {
    return String(value)
  }
}

const loadReports = async (targetPage = 1) => {
  try {
    const response = await getMyReports({
      page: targetPage,
      pageSize: pageData.pageSize,
      status: filters.status || undefined,
      targetType: filters.targetType || undefined
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '加载举报记录失败')
      return
    }
    const data = response.data.data || {}
    pageData.list = data.list || []
    pageData.page = data.page || targetPage
    pageData.pageSize = data.pageSize || pageData.pageSize
    pageData.total = Number(data.total || 0)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载举报记录失败')
  }
}

const openPreview = (row) => {
  preview.row = row
  preview.current = row.current || null
  preview.snapshot = row.targetSnapshot || null
  preview.visible = true
}

const jumpToTarget = (row) => {
  if (row.targetType === 'content') {
    router.push(`/content/${row.targetId}`)
    return
  }
  if (row.targetType === 'user') {
    router.push(`/users/${row.targetId}`)
    return
  }
  // 评论定位优先依赖当前目标或快照中的 contentId，避免评论被删后无法跳转。
  if (row.targetType === 'comment') {
    const current = row.current || {}
    const snapshot = row.targetSnapshot || {}
    const contentId = current.contentId || snapshot.contentId || snapshot.content_id
    if (contentId) {
      router.push(`/content/${contentId}`)
      return
    }
  }
  ElMessage.info('当前目标暂无法定位，请查看详情')
}

loadReports(1)
</script>

<style scoped>
.my-reports-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.table-card {
  border-radius: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.table-head h2 {
  margin: 0;
  font-size: 22px;
}

.table-head span {
  color: #64748b;
}

.ops {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.preview-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
  color: #334155;
}

.preview-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.preview-block h4 {
  margin: 0 0 6px;
}

.preview-block pre {
  margin: 0;
  min-height: 220px;
  max-height: 420px;
  overflow: auto;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(248, 252, 255, 0.88);
}

@media (max-width: 900px) {
  .preview-layout,
  .preview-meta {
    grid-template-columns: 1fr;
  }
}
</style>
