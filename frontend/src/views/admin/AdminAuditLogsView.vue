<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-5col">
        <div class="filter-item action-item">
          <el-input v-model="filters.action" clearable placeholder="动作码" @keyup.enter="loadLogs(1)" />
        </div>
        <div class="filter-item target-item">
          <el-input v-model="filters.targetType" clearable placeholder="目标类型" @keyup.enter="loadLogs(1)" />
        </div>
        <div class="filter-item user-item">
          <el-input-number v-model="filters.operatorUserId" :min="1" placeholder="操作者ID" />
        </div>
        <el-button type="primary" class="query-btn" @click="loadLogs(1)">查询</el-button>
        <el-button class="export-btn" @click="exportCsv">导出当前页</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorUserId" label="操作者ID" width="98" />
        <el-table-column label="操作者" width="160">
          <template #default="{ row }">{{ row.operatorNickname || row.operatorUsername || '-' }}</template>
        </el-table-column>
        <el-table-column prop="action" label="动作" min-width="170" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="105" />
        <el-table-column prop="targetId" label="目标ID" width="95" />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="快照" width="120" fixed="right">
          <template #default="{ row }">
            <el-button class="action-btn btn-primary-soft" @click="viewSnapshot(row)">查看</el-button>
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
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-dialog v-model="snapshotDialog.visible" title="审计快照" width="760px">
      <el-row :gutter="12">
        <el-col :span="12">
          <p class="snapshot-title">Before</p>
          <pre class="snapshot-pre">{{ snapshotDialog.before }}</pre>
        </el-col>
        <el-col :span="12">
          <p class="snapshot-title">After</p>
          <pre class="snapshot-pre">{{ snapshotDialog.after }}</pre>
        </el-col>
      </el-row>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminAuditLogs } from '../../api/admin'

/**
 * 管理端-操作审计页职责：
 * - 查询管理动作日志（操作者、动作、目标、时间、IP）；
 * - 查看 before/after 快照；
 * - 支持当前页 CSV 导出用于线下复核。
 */
const filters = reactive({
  action: '',
  targetType: '',
  operatorUserId: null
})

const pageData = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0
})

const snapshotDialog = reactive({
  visible: false,
  before: '',
  after: ''
})

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

const prettyJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (_) {
    return String(value)
  }
}

const viewSnapshot = (row) => {
  snapshotDialog.visible = true
  snapshotDialog.before = prettyJson(row.detailBefore)
  snapshotDialog.after = prettyJson(row.detailAfter)
}

const loadLogs = async (targetPage = 1) => {
  const response = await getAdminAuditLogs({
    page: targetPage,
    pageSize: pageData.pageSize,
    action: filters.action || undefined,
    targetType: filters.targetType || undefined,
    operatorUserId: filters.operatorUserId || undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载审计日志失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = data.list || []
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = data.total || 0
}

const escapeCsv = (value) => {
  const raw = value == null ? '' : String(value)
  if (raw.includes('"') || raw.includes(',') || raw.includes('\n')) {
    return `"${raw.replaceAll('"', '""')}"`
  }
  return raw
}

const exportCsv = () => {
  // 导出仅基于当前页数据，避免误以为是全量导出。
  const headers = ['id', 'operatorUserId', 'operatorUsername', 'operatorNickname', 'action', 'targetType', 'targetId', 'ip', 'createTime']
  const lines = [headers.join(',')]
  for (const item of pageData.list) {
    const row = [
      item.id,
      item.operatorUserId,
      item.operatorUsername || '',
      item.operatorNickname || '',
      item.action || '',
      item.targetType || '',
      item.targetId || '',
      item.ip || '',
      formatTime(item.createTime)
    ].map(escapeCsv)
    lines.push(row.join(','))
  }

  const csvContent = `\uFEFF${lines.join('\n')}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `admin-audit-logs-page-${pageData.page}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

loadLogs(1)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.table-card {
  border-radius: 16px;
}

.filters {
  display: grid;
  align-items: center;
  column-gap: 12px;
}

.filters-5col {
  grid-template-columns: 220px 170px 170px 96px 122px;
}

.filter-item :deep(.el-input),
.filter-item :deep(.el-input-number) {
  width: 100%;
}

.filter-item :deep(.el-input__wrapper),
.filter-item :deep(.el-input-number) {
  min-height: 44px;
  font-size: 15px;
}

.query-btn,
.export-btn {
  min-height: 42px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
}

.admin-table :deep(.el-table__header th) {
  height: 54px;
  font-size: 16px;
  font-weight: 700;
  color: #566678;
}

.admin-table :deep(.el-table__row td) {
  height: 54px;
  font-size: 16px;
}

.admin-table :deep(.el-table__header .cell),
.admin-table :deep(.el-table__body .cell) {
  text-align: center;
}

/* 操作列按钮统一收敛为紧凑尺寸，提升管理端表格信息密度 */
.action-btn {
  min-width: 66px;
  height: 30px;
  padding: 0 10px;
  border-radius: 9px !important;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.1;
}

.btn-primary-soft {
  border: 1px solid #91b7d5 !important;
  color: #2d618d !important;
  background: #e9f3fb !important;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.snapshot-title {
  font-weight: 700;
  margin-bottom: 8px;
  color: #44566a;
}

.snapshot-pre {
  margin: 0;
  min-height: 280px;
  max-height: 440px;
  overflow: auto;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(248, 252, 255, 0.9);
  font-size: 13px;
  line-height: 1.48;
}
</style>

