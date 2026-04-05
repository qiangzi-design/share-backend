<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-4col">
        <el-input v-model="filters.keyword" clearable placeholder="搜索标签名称" @keyup.enter="loadTags(1)" />
        <el-select v-model="filters.status" clearable placeholder="标签状态">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" class="query-btn" @click="loadTags(1)">查询</el-button>
        <el-button v-if="canWrite" class="create-btn" @click="openCreateDialog">新建标签</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="标签名称" min-width="180" />
        <el-table-column prop="useCount" label="使用次数" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button class="action-btn btn-primary-soft" @click="openContentsDialog(row)">关联内容</el-button>
              <el-button v-if="canWrite" class="action-btn btn-primary-soft" @click="openEditDialog(row)">重命名</el-button>
              <el-button
                v-if="canWrite && row.status === 1"
                class="action-btn btn-danger-soft"
                @click="toggleStatus(row, false)"
              >
                禁用
              </el-button>
              <el-button
                v-if="canWrite && row.status !== 1"
                class="action-btn btn-success-soft"
                @click="toggleStatus(row, true)"
              >
                启用
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
          @current-change="loadTags"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新建标签' : '标签重命名'" width="420px">
      <el-form ref="dialogFormRef" :model="dialog.form" :rules="dialogRules" label-width="70px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="dialog.form.name" maxlength="30" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitDialog">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contentsDialog.visible" :title="`标签「${contentsDialog.tagName}」关联内容`" width="760px">
      <el-table :data="contentsDialog.list" class="admin-table">
        <el-table-column prop="id" label="内容ID" width="90" />
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="userId" label="作者ID" width="100" />
        <el-table-column prop="tags" label="标签" min-width="180" show-overflow-tooltip />
      </el-table>
      <div class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, ->, total"
          :current-page="contentsDialog.page"
          :page-size="contentsDialog.pageSize"
          :total="contentsDialog.total"
          @current-change="loadTagContents"
        />
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAdminTag,
  disableAdminTag,
  enableAdminTag,
  getAdminTagContents,
  getAdminTags,
  updateAdminTag
} from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-标签运营页职责：
 * - 标签增改启停；
 * - 展示标签使用次数；
 * - 查看标签关联内容，辅助运营判断是否应禁用标签。
 */
const canWrite = hasAdminPermission('admin.tag.write')
const dialogFormRef = ref(null)

const filters = reactive({
  keyword: '',
  status: null
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
  tagId: null,
  form: {
    name: ''
  }
})

const dialogRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

const contentsDialog = reactive({
  visible: false,
  tagId: null,
  tagName: '',
  list: [],
  page: 1,
  pageSize: 10,
  total: 0
})

const loadTags = async (targetPage = 1) => {
  const response = await getAdminTags({
    page: targetPage,
    pageSize: pageData.pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status ?? undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载标签失败')
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
  dialog.tagId = null
  dialog.form.name = ''
  dialog.visible = true
}

const openEditDialog = (row) => {
  dialog.mode = 'edit'
  dialog.tagId = row.id
  dialog.form.name = row.name || ''
  dialog.visible = true
}

const submitDialog = async () => {
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (dialog.mode === 'create') {
      await createAdminTag(dialog.form)
      ElMessage.success('标签创建成功')
    } else {
      await updateAdminTag(dialog.tagId, dialog.form)
      ElMessage.success('标签更新成功')
    }
    dialog.visible = false
    await loadTags(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

const toggleStatus = async (row, enable) => {
  try {
    if (enable) {
      await enableAdminTag(row.id)
      ElMessage.success('标签已启用')
    } else {
      await disableAdminTag(row.id)
      ElMessage.success('标签已禁用')
    }
    await loadTags(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

const openContentsDialog = async (row) => {
  contentsDialog.visible = true
  contentsDialog.tagId = row.id
  contentsDialog.tagName = row.name || ''
  contentsDialog.page = 1
  contentsDialog.list = []
  contentsDialog.total = 0
  await loadTagContents(1)
}

const loadTagContents = async (targetPage = 1) => {
  if (!contentsDialog.tagId) return
  const response = await getAdminTagContents(contentsDialog.tagId, {
    page: targetPage,
    pageSize: contentsDialog.pageSize
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载关联内容失败')
    return
  }
  const data = response.data.data || {}
  contentsDialog.list = data.list || []
  contentsDialog.page = data.page || targetPage
  contentsDialog.pageSize = data.pageSize || contentsDialog.pageSize
  contentsDialog.total = Number(data.total || 0)
}

loadTags(1)
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

