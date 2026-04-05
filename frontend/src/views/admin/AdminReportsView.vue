<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-3col">
        <div class="filter-item status-item">
          <el-select v-model="filters.status" clearable placeholder="工单状态">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="assigned" />
            <el-option label="已处理" value="resolved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </div>
        <div class="filter-item target-item">
          <el-select v-model="filters.targetType" clearable placeholder="目标类型">
            <el-option label="内容" value="content" />
            <el-option label="评论" value="comment" />
            <el-option label="用户" value="user" />
          </el-select>
        </div>
        <el-button type="primary" class="query-btn" @click="loadReports(1)">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标类型" width="100">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column label="举报人" width="170">
          <template #default="{ row }">{{ row.reporterNickname || row.reporterUsername || row.reporterId }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="处理时长(分钟)" width="132">
          <template #default="{ row }">
            <span>{{ row.handleDurationMinutes || 0 }}</span>
            <el-tag v-if="row.overtime" type="danger" size="small" class="overtime-tag">超时</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="170">
          <template #default="{ row }">{{ row.assigneeNickname || row.assigneeUsername || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handleNote || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button class="action-btn btn-primary-soft" @click="previewTarget(row)">查看目标</el-button>
              <el-button class="action-btn btn-primary-soft" @click="jumpToTarget(row)">一键定位</el-button>
              <template v-if="canHandleReport">
                <el-button class="action-btn btn-primary-soft" @click="assignToMe(row)">指派给我</el-button>
                <el-button class="action-btn btn-success-soft" @click="openHandleDialog(row)">处理</el-button>
              </template>
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

    <el-dialog v-model="previewDialog.visible" title="举报目标预览" width="760px">
      <div class="preview-layout">
        <div class="preview-block">
          <h4>当前目标</h4>
          <pre>{{ toPrettyJson(previewDialog.current) }}</pre>
        </div>
        <div class="preview-block">
          <h4>举报创建时快照</h4>
          <pre>{{ toPrettyJson(previewDialog.snapshot) }}</pre>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="handleDialog.visible" title="处理举报" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="处理结论">
          <el-select v-model="handleDialog.decision" @change="syncHandleAction">
            <el-option label="有效举报" value="valid" />
            <el-option label="无效举报（驳回）" value="invalid" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理动作">
          <el-select v-model="handleDialog.action" :disabled="handleDialog.decision === 'invalid'">
            <el-option
              v-for="item in currentActionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="违规模板">
          <el-select v-model="handleDialog.templateCode" placeholder="可选" clearable filterable>
            <el-option
              v-for="item in reportTemplates"
              :key="item.code"
              :label="item.label"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="违规补充描述">
          <el-input
            v-model="handleDialog.violationReason"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            maxlength="500"
            show-word-limit
            placeholder="可补充具体违规细节"
          />
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleDialog.handleNote"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="handleDialog.submitting" @click="submitHandle">确定处理</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  assignAdminReport,
  getAdminReportTargetPreview,
  getAdminReports,
  handleAdminReport
} from '../../api/admin'
import { getReportTemplates } from '../../api/report'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-举报中心职责：
 * 1. 展示举报工单并支持“查看目标/一键定位/指派/处理”闭环。
 * 2. 将“处理完成 + 驳回”统一到“处理”动作中，用 decision 区分有效与无效。
 * 3. 内容违规处理时强制要求模板或补充描述，保障通知文案可落地。
 */
const router = useRouter()
const canHandleReport = hasAdminPermission('admin.report.handle')

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

const previewDialog = reactive({
  visible: false,
  current: null,
  snapshot: null
})

const reportTemplates = ref([])

const handleDialog = reactive({
  visible: false,
  row: null,
  decision: 'valid',
  action: 'off_shelf_content',
  templateCode: '',
  violationReason: '',
  handleNote: '',
  submitting: false
})

const statusLabel = (status) => {
  if (status === 'pending') return '待处理'
  if (status === 'assigned') return '处理中'
  if (status === 'resolved') return '已处理'
  if (status === 'rejected') return '已驳回'
  return status || '-'
}

const targetTypeLabel = (targetType) => {
  if (targetType === 'content') return '内容'
  if (targetType === 'comment') return '评论'
  if (targetType === 'user') return '用户'
  return targetType || '-'
}

const statusTagType = (status) => {
  if (status === 'pending') return 'warning'
  if (status === 'assigned') return 'primary'
  if (status === 'resolved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'info'
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

const actionOptions = {
  content: [{ label: '下架内容', value: 'off_shelf_content' }],
  comment: [
    { label: '隐藏评论', value: 'hide_comment' },
    { label: '删除评论', value: 'delete_comment' }
  ],
  user: [{ label: '有效（仅记录）', value: 'valid_no_action' }],
  default: [{ label: '驳回举报', value: 'reject_report' }]
}

// 决策为“无效举报”时动作固定为 reject，避免前端误传治理动作。
const currentActionOptions = computed(() => {
  if (handleDialog.decision === 'invalid') {
    return actionOptions.default
  }
  const targetType = handleDialog.row?.targetType || 'content'
  return actionOptions[targetType] || actionOptions.default
})

const syncHandleAction = () => {
  if (handleDialog.decision === 'invalid') {
    handleDialog.action = 'reject_report'
    return
  }
  const options = currentActionOptions.value
  if (!options.some((item) => item.value === handleDialog.action)) {
    handleDialog.action = options[0]?.value || 'valid_no_action'
  }
}

const loadReportTemplates = async () => {
  try {
    const response = await getReportTemplates()
    if (response.data?.code !== 200) return
    reportTemplates.value = Array.isArray(response.data?.data) ? response.data.data : []
  } catch (_) {
    reportTemplates.value = []
  }
}

const loadReports = async (targetPage = 1) => {
  const response = await getAdminReports({
    page: targetPage,
    pageSize: pageData.pageSize,
    status: filters.status || undefined,
    targetType: filters.targetType || undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载举报失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = data.list || []
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = Number(data.total || 0)
}

const getCurrentUserId = () => Number(sessionStorage.getItem('userId') || 0)

const assignToMe = async (row) => {
  if (!canHandleReport) return
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.error('当前登录信息异常，请重新登录')
    return
  }
  try {
    const response = await assignAdminReport(row.id, { assigneeUserId: userId, handleNote: '接单处理中' })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '指派失败')
      return
    }
    ElMessage.success('已指派给自己')
    await loadReports(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '指派失败')
  }
}

// 进入处理弹窗时按目标类型预置动作，减少管理员重复选择成本。
const openHandleDialog = (row) => {
  handleDialog.visible = true
  handleDialog.row = row
  handleDialog.decision = 'valid'
  handleDialog.templateCode = ''
  handleDialog.violationReason = ''
  handleDialog.handleNote = ''
  handleDialog.submitting = false
  const options = actionOptions[row?.targetType] || actionOptions.default
  handleDialog.action = options[0]?.value || 'valid_no_action'
}

/**
 * 提交处理规则：
 * - decision=invalid => action 强制 reject_report；
 * - content + off_shelf_content 场景要求“模板或补充描述”至少一项。
 */
const submitHandle = async () => {
  if (!handleDialog.row?.id) return

  const customReason = (handleDialog.violationReason || '').trim()
  if (handleDialog.decision === 'valid' && handleDialog.row.targetType === 'content') {
    const action = handleDialog.action || 'off_shelf_content'
    if (action === 'off_shelf_content' && !handleDialog.templateCode && !customReason) {
      ElMessage.warning('内容违规处理需要选择违规模板或填写补充描述')
      return
    }
  }

  handleDialog.submitting = true
  try {
    const template = reportTemplates.value.find((item) => item.code === handleDialog.templateCode)
    const response = await handleAdminReport(handleDialog.row.id, {
      decision: handleDialog.decision,
      action: handleDialog.decision === 'invalid' ? 'reject_report' : handleDialog.action,
      violationTemplateCode: handleDialog.templateCode || undefined,
      violationTemplateLabel: template?.label || undefined,
      violationReason: customReason || undefined,
      handleNote: (handleDialog.handleNote || '').trim() || undefined
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '处理失败')
      return
    }
    ElMessage.success('处理成功')
    handleDialog.visible = false
    await loadReports(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '处理失败')
  } finally {
    handleDialog.submitting = false
  }
}

const previewTarget = async (row) => {
  try {
    const response = await getAdminReportTargetPreview(row.id)
    if (response.data?.code !== 200 || !response.data?.data) {
      ElMessage.error(response.data?.message || '加载预览失败')
      return
    }
    previewDialog.current = response.data.data.current || null
    previewDialog.snapshot = response.data.data.snapshot || null
    previewDialog.visible = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载预览失败')
  }
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
  if (row.targetType === 'comment') {
    const snapshot = row.targetSnapshot || {}
    const contentId = snapshot.contentId || snapshot.content_id
    if (contentId) {
      router.push(`/content/${contentId}`)
      return
    }
  }
  ElMessage.info('当前目标无法直接定位，请先点击“查看目标”')
}

loadReports(1)
loadReportTemplates()
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

.filters-3col {
  grid-template-columns: 180px 180px 96px;
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
  gap: 10px;
  flex-wrap: wrap;
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

.btn-primary-soft {
  border: 1px solid #91b7d5 !important;
  color: #2d618d !important;
  background: #e9f3fb !important;
}

.btn-success-soft {
  border: 1px solid #86b9ad !important;
  color: #1f6c61 !important;
  background: #e2f3ef !important;
}

.overtime-tag {
  margin-left: 6px;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.preview-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.preview-block h4 {
  margin: 0 0 6px;
  color: #334155;
}

.preview-block pre {
  margin: 0;
  min-height: 260px;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(248, 252, 255, 0.88);
}
</style>

