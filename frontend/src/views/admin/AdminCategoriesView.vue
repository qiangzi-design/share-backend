<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-4col">
        <el-input v-model="filters.keyword" clearable placeholder="搜索分类名称" @keyup.enter="loadCategories(1)" />
        <el-select v-model="filters.status" clearable placeholder="分类状态">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" class="query-btn" @click="loadCategories(1)">查询</el-button>
        <el-button v-if="canWrite" class="create-btn" @click="openCreateDialog">新建分类</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button v-if="canWrite" class="action-btn btn-primary-soft" @click="openEditDialog(row)">编辑</el-button>
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
          @current-change="loadCategories"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新建分类' : '编辑分类'" width="520px">
      <el-form ref="dialogFormRef" :model="dialog.form" :rules="dialogRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="dialog.form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="dialog.form.description" maxlength="500" show-word-limit type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="dialog.form.sortOrder" :min="-9999" :max="9999" />
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
  createAdminCategory,
  disableAdminCategory,
  enableAdminCategory,
  getAdminCategories,
  updateAdminCategory
} from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-分类管理页职责：
 * - 分类增改查与启停；
 * - 排序字段用于前台分类展示顺序；
 * - 禁用分类只影响新发/编辑可选项，不改历史内容。
 */
const canWrite = hasAdminPermission('admin.category.write')
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
  categoryId: null,
  form: {
    name: '',
    description: '',
    sortOrder: 0
  }
})

const dialogRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const loadCategories = async (targetPage = 1) => {
  const response = await getAdminCategories({
    page: targetPage,
    pageSize: pageData.pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status ?? undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载分类失败')
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
  dialog.categoryId = null
  dialog.form.name = ''
  dialog.form.description = ''
  dialog.form.sortOrder = 0
  dialog.visible = true
}

const openEditDialog = (row) => {
  dialog.mode = 'edit'
  dialog.categoryId = row.id
  dialog.form.name = row.name || ''
  dialog.form.description = row.description || ''
  dialog.form.sortOrder = Number(row.sortOrder || 0)
  dialog.visible = true
}

const submitDialog = async () => {
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (dialog.mode === 'create') {
      await createAdminCategory(dialog.form)
      ElMessage.success('分类创建成功')
    } else {
      await updateAdminCategory(dialog.categoryId, dialog.form)
      ElMessage.success('分类更新成功')
    }
    dialog.visible = false
    await loadCategories(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

const toggleStatus = async (row, enable) => {
  try {
    if (enable) {
      await enableAdminCategory(row.id)
      ElMessage.success('分类已启用')
    } else {
      await disableAdminCategory(row.id)
      ElMessage.success('分类已禁用')
    }
    await loadCategories(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

loadCategories(1)
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

