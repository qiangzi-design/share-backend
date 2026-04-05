<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-4col">
        <div class="filter-item id-item">
          <el-input-number v-model="filters.contentId" :min="1" placeholder="内容ID" />
        </div>
        <div class="filter-item id-item">
          <el-input-number v-model="filters.userId" :min="1" placeholder="作者ID" />
        </div>
        <div class="filter-item status-item">
          <el-select v-model="filters.reviewStatus" clearable placeholder="审核状态">
            <el-option label="通过" value="approved" />
            <el-option label="隐藏" value="rejected" />
          </el-select>
        </div>
        <el-button type="primary" class="query-btn" @click="loadComments(1)">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="contentId" label="内容ID" width="95" />
        <el-table-column label="作者" width="160">
          <template #default="{ row }">{{ row.authorNickname || row.authorUsername || row.userId }}</template>
        </el-table-column>
        <el-table-column prop="commentContent" label="评论内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.reviewStatus === 'approved' ? 'success' : 'danger'">
              {{ row.reviewStatus === 'approved' ? '通过' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewReason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button
                v-if="canHideComment && row.reviewStatus === 'approved'"
                class="action-btn btn-danger-soft"
                @click="hide(row)"
              >
                隐藏
              </el-button>
              <el-button
                v-if="canHideComment && row.reviewStatus !== 'approved'"
                class="action-btn btn-success-soft"
                @click="restore(row)"
              >
                恢复
              </el-button>
              <span v-if="!canHideComment" class="op-muted">只读</span>
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
          @current-change="loadComments"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminComments, hideAdminComment, restoreAdminComment } from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-评论审核页职责：
 * - 查询评论审核状态；
 * - 执行隐藏/恢复；
 * - 通过 reviewStatus 区分可见与隐藏。
 */
const canHideComment = hasAdminPermission('admin.comment.hide')

const filters = reactive({
  contentId: null,
  userId: null,
  reviewStatus: ''
})

const pageData = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0
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

const loadComments = async (targetPage = 1) => {
  const response = await getAdminComments({
    page: targetPage,
    pageSize: pageData.pageSize,
    contentId: filters.contentId || undefined,
    userId: filters.userId || undefined,
    reviewStatus: filters.reviewStatus || undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载评论失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = data.list || []
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = data.total || 0
}

// 隐藏评论会保留数据实体，只改变审核可见性。
const hide = async (row) => {
  if (!canHideComment) return
  await ElMessageBox.prompt('请输入隐藏原因（可选）', '隐藏评论', {
    confirmButtonText: '隐藏',
    cancelButtonText: '取消'
  })
    .then(async ({ value }) => {
      await hideAdminComment(row.id, { reason: value || '' })
      ElMessage.success('已隐藏')
      await loadComments(pageData.page)
    })
    .catch(() => {})
}

// 恢复评论用于误判纠正与申诉回滚场景。
const restore = async (row) => {
  if (!canHideComment) return
  await ElMessageBox.prompt('请输入恢复说明（可选）', '恢复评论', {
    confirmButtonText: '恢复',
    cancelButtonText: '取消'
  })
    .then(async ({ value }) => {
      await restoreAdminComment(row.id, { reason: value || '' })
      ElMessage.success('已恢复')
      await loadComments(pageData.page)
    })
    .catch(() => {})
}

loadComments(1)
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

.filters-4col {
  grid-template-columns: 150px 150px 180px 96px;
}

.filter-item :deep(.el-input-number),
.filter-item :deep(.el-select) {
  width: 100%;
}

.filter-item :deep(.el-input-number),
.filter-item :deep(.el-select__wrapper) {
  min-height: 44px;
}

.query-btn {
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

