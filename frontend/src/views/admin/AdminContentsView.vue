<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-4col">
        <div class="filter-item keyword-item">
          <el-input v-model="filters.keyword" clearable placeholder="搜索标题/正文" @keyup.enter="loadContents(1)" />
        </div>
        <div class="filter-item status-item">
          <el-select v-model="filters.reviewStatus" clearable placeholder="审核状态" class="review-select">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </div>
        <div class="filter-item user-item">
          <el-input-number v-model="filters.userId" :min="1" placeholder="作者ID" />
        </div>
        <el-button type="primary" class="query-btn" @click="loadContents(1)">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="作者" width="160">
          <template #default="{ row }">{{ row.authorNickname || row.authorUsername || row.userId }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reviewTagType(row.reviewStatus)">{{ reviewText(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewReason" label="审核备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button
                v-if="canRestore && row.reviewStatus !== 'approved'"
                class="action-btn btn-success-soft"
                @click="approveContent(row)"
              >
                通过
              </el-button>
              <el-button
                v-if="canOffShelf && row.reviewStatus !== 'rejected'"
                class="action-btn btn-danger-soft"
                @click="rejectContent(row)"
              >
                驳回
              </el-button>
              <span v-if="!canOffShelf && !canRestore" class="op-muted">只读</span>
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
          @current-change="loadContents"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminContents, offShelfAdminContent, restoreAdminContent } from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-内容审核页职责：
 * - 查询内容审核状态；
 * - 执行“通过/驳回（下架）”动作；
 * - 前端按钮按权限码与当前状态联动展示。
 */
const canOffShelf = hasAdminPermission('admin.content.off_shelf')
const canRestore = hasAdminPermission('admin.content.restore')

const filters = reactive({
  keyword: '',
  reviewStatus: null,
  userId: null
})

const pageData = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0
})

const reviewText = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return '已通过'
  if (status === 'rejected') return '已驳回'
  return '待审核'
}

const reviewTagType = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'warning'
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

const loadContents = async (targetPage = 1) => {
  const response = await getAdminContents({
    page: targetPage,
    pageSize: pageData.pageSize,
    keyword: filters.keyword || undefined,
    reviewStatus: filters.reviewStatus ?? undefined,
    userId: filters.userId || undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载内容失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = (data.list || []).map((row) => ({
    ...row,
    reviewStatus: String(row.reviewStatus || 'pending').toLowerCase()
  }))
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = data.total || 0
}

// 驳回会写 reviewReason，便于创作者在用户侧查看原因后修改重提。
const rejectContent = async (row) => {
  if (!canOffShelf) return
  await ElMessageBox.prompt('请输入驳回原因（可选）', '驳回内容', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消'
  })
    .then(async ({ value }) => {
      await offShelfAdminContent(row.id, { reason: value || '' })
      ElMessage.success('已驳回')
      await loadContents(pageData.page)
    })
    .catch(() => {})
}

// 通过动作会把内容恢复到可见状态，遵循“仅软治理不物删”策略。
const approveContent = async (row) => {
  if (!canRestore) return
  await ElMessageBox.prompt('请输入审核备注（可选）', '通过审核', {
    confirmButtonText: '通过',
    cancelButtonText: '取消'
  })
    .then(async ({ value }) => {
      await restoreAdminContent(row.id, { reason: value || '' })
      ElMessage.success('已通过')
      await loadContents(pageData.page)
    })
    .catch(() => {})
}

loadContents(1)
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
  column-gap: 14px;
}

.filters-4col {
  grid-template-columns: minmax(300px, 1fr) 220px 170px 108px;
}

.filter-item :deep(.el-input),
.filter-item :deep(.el-select),
.filter-item :deep(.el-input-number) {
  width: 100%;
}

.status-item {
  min-width: 220px;
}

.status-item :deep(.review-select) {
  width: 100%;
}

.filter-item :deep(.el-input__wrapper),
.filter-item :deep(.el-select__wrapper),
.filter-item :deep(.el-input-number) {
  min-height: 44px;
  font-size: 15px;
}

.status-item :deep(.el-select__placeholder) {
  color: #8b98aa;
}

.query-btn {
  min-height: 42px;
  border-radius: 14px;
  border: 1px solid #0b9897;
  background: linear-gradient(135deg, #11b3b2 0%, #0c9e9c 100%);
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 10px 18px rgba(14, 165, 164, 0.22);
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

.ops {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.ops :deep(.el-button + .el-button) {
  margin-left: 0;
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

.btn-danger-soft {
  border: 1px solid #eaa0ab !important;
  color: #b34350 !important;
  background: #ffeaed !important;
}

.btn-success-soft {
  border: 1px solid #86b9ad !important;
  color: #1f6c61 !important;
  background: #e2f3ef !important;
}

.op-muted {
  font-size: 13px;
  color: #8aa0b8;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>

