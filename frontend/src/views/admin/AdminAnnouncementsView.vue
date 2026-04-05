<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-4col">
        <el-input v-model="filters.keyword" clearable placeholder="搜索公告标题或正文" @keyup.enter="loadAnnouncements(1)" />
        <el-select v-model="filters.status" clearable placeholder="公告状态">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已下线" value="offline" />
        </el-select>
        <el-button type="primary" class="query-btn" @click="loadAnnouncements(1)">查询</el-button>
        <el-button v-if="canWrite" class="create-btn" @click="openCreateDialog">新建公告</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="90">
          <template #default="{ row }">{{ row.isPinned ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="240">
          <template #default="{ row }">
            {{ formatTime(row.startTime) || '不限' }} ~ {{ formatTime(row.endTime) || '不限' }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button v-if="canWrite" class="action-btn btn-primary-soft" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="canWrite && row.status !== 'published'"
                class="action-btn btn-success-soft"
                @click="publish(row)"
              >
                发布
              </el-button>
              <el-button
                v-if="canWrite && row.status === 'published'"
                class="action-btn btn-danger-soft"
                @click="offline(row)"
              >
                下线
              </el-button>
              <span v-if="!canWrite" class="op-muted">只读</span>
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
          @current-change="loadAnnouncements"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新建公告' : '编辑公告'" width="700px">
      <el-form ref="dialogFormRef" :model="dialog.form" :rules="dialogRules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="dialog.form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="body">
          <el-input v-model="dialog.form.body" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="dialog.form.isPinned" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="dialog.form.startTime" type="datetime" placeholder="可留空" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="dialog.form.endTime" type="datetime" placeholder="可留空" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitDialog">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAdminAnnouncement,
  getAdminAnnouncements,
  offlineAdminAnnouncement,
  publishAdminAnnouncement,
  updateAdminAnnouncement
} from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-公告管理页职责：
 * - 公告查询、创建、编辑、发布、下线；
 * - 有效期时间窗校验与格式规范；
 * - 置顶标记由后端排序生效，前端仅配置。
 */
const canWrite = hasAdminPermission('admin.announcement.write')
const dialogFormRef = ref(null)

const filters = reactive({
  keyword: '',
  status: ''
})

const pageData = reactive({
  list: [],
  page: 1,
  pageSize: 20,
  total: 0
})

const dialog = reactive({
  visible: false,
  mode: 'create',
  announcementId: null,
  form: {
    title: '',
    body: '',
    isPinned: false,
    startTime: '',
    endTime: ''
  }
})

const dialogRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  body: [{ required: true, message: '请输入公告正文', trigger: 'blur' }]
}

const statusLabel = (status) => {
  if (status === 'published') return '已发布'
  if (status === 'offline') return '已下线'
  return '草稿'
}

const statusTagType = (status) => {
  if (status === 'published') return 'success'
  if (status === 'offline') return 'warning'
  return 'info'
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const normalizeToIsoLocalDateTime = (value) => {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 19)
}

const normalizeEndOfDayIfMidnight = (isoValue) => {
  if (!isoValue) return isoValue
  if (String(isoValue).endsWith('T00:00:00')) {
    return `${String(isoValue).slice(0, 10)}T23:59:59`
  }
  return isoValue
}

// 公告列表查询支持标题/正文关键字与状态筛选。
const loadAnnouncements = async (targetPage = 1) => {
  const response = await getAdminAnnouncements({
    page: targetPage,
    pageSize: pageData.pageSize,
    status: filters.status || undefined,
    keyword: filters.keyword || undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载公告失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = data.list || []
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = Number(data.total || 0)
}

const openCreateDialog = () => {
  dialog.mode = 'create'
  dialog.announcementId = null
  dialog.form.title = ''
  dialog.form.body = ''
  dialog.form.isPinned = false
  dialog.form.startTime = ''
  dialog.form.endTime = ''
  dialog.visible = true
}

const openEditDialog = (row) => {
  dialog.mode = 'edit'
  dialog.announcementId = row.id
  dialog.form.title = row.title || ''
  dialog.form.body = row.body || ''
  dialog.form.isPinned = Boolean(row.isPinned)
  dialog.form.startTime = normalizeToIsoLocalDateTime(row.startTime)
  dialog.form.endTime = normalizeToIsoLocalDateTime(row.endTime)
  dialog.visible = true
}

const submitDialog = async () => {
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (dialog.form.startTime && dialog.form.endTime && dialog.form.endTime < dialog.form.startTime) {
    ElMessage.warning('结束时间不能小于开始时间')
    return
  }

  const payload = {
    title: dialog.form.title,
    body: dialog.form.body,
    isPinned: dialog.form.isPinned,
    startTime: dialog.form.startTime || null,
    endTime: normalizeEndOfDayIfMidnight(dialog.form.endTime || null)
  }

  try {
    if (dialog.mode === 'create') {
      await createAdminAnnouncement(payload)
      ElMessage.success('公告创建成功')
    } else {
      await updateAdminAnnouncement(dialog.announcementId, payload)
      ElMessage.success('公告更新成功')
    }
    dialog.visible = false
    await loadAnnouncements(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

const publish = async (row) => {
  try {
    await publishAdminAnnouncement(row.id)
    ElMessage.success('公告已发布')
    await loadAnnouncements(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发布失败')
  }
}

const offline = async (row) => {
  try {
    await offlineAdminAnnouncement(row.id)
    ElMessage.success('公告已下线')
    await loadAnnouncements(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '下线失败')
  }
}

loadAnnouncements(1)
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
  gap: 12px;
}

.filters-4col {
  grid-template-columns: minmax(260px, 1fr) 180px 96px 110px;
}

.query-btn,
.create-btn {
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

.btn-danger-soft {
  border: 1px solid #eaa0ab !important;
  color: #b34350 !important;
  background: #ffeaed !important;
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

