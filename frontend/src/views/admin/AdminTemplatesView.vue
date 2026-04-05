<template>
  <section class="admin-page">
    <el-row :gutter="12" class="layout-row">
      <el-col :xs="24" :md="8">
        <el-card class="list-card">
          <template #header>
            <div class="card-title">消息模板</div>
          </template>

          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索模板编码/名称"
            class="search-input"
          />

          <el-scrollbar height="560px" class="template-scroll">
            <div class="template-list">
              <button
                v-for="item in filteredTemplates"
                :key="item.code"
                type="button"
                class="template-item"
                :class="{ active: item.code === activeCode }"
                @click="selectTemplate(item)"
              >
                <div class="template-head">
                  <span class="template-code">{{ item.code }}</span>
                  <el-tag size="small" :type="Number(item.status) === 1 ? 'success' : 'info'">
                    {{ Number(item.status) === 1 ? '启用' : '停用' }}
                  </el-tag>
                </div>
                <div class="template-name">{{ item.name || '-' }}</div>
              </button>
            </div>

            <el-empty v-if="filteredTemplates.length === 0" description="暂无模板" />
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card class="editor-card">
          <template #header>
            <div class="editor-head">
              <div>
                <div class="card-title">模板编辑</div>
                <p class="card-subtitle">选择左侧模板后可编辑并实时预览通知效果</p>
              </div>
              <div class="head-actions">
                <el-button class="action-btn" @click="loadTemplates">刷新</el-button>
                <el-button
                  type="primary"
                  class="action-btn"
                  :disabled="!canWrite || !activeCode"
                  :loading="saving"
                  @click="saveTemplate"
                >
                  保存
                </el-button>
              </div>
            </div>
          </template>

          <el-empty v-if="!activeCode" description="请选择模板" />

          <template v-else>
            <el-form label-width="100px" class="editor-form">
              <el-form-item label="模板编码">
                <el-input :model-value="form.code" disabled />
              </el-form-item>
              <el-form-item label="模板名称" required>
                <el-input v-model="form.name" maxlength="120" show-word-limit :disabled="!canWrite" />
              </el-form-item>
              <el-form-item label="标题模板" required>
                <el-input v-model="form.titleTemplate" maxlength="120" show-word-limit :disabled="!canWrite" />
              </el-form-item>
              <el-form-item label="正文模板" required>
                <el-input
                  v-model="form.bodyTemplate"
                  type="textarea"
                  :rows="5"
                  maxlength="800"
                  show-word-limit
                  :disabled="!canWrite"
                />
              </el-form-item>
              <el-form-item label="启用状态">
                <el-switch v-model="form.enabled" :disabled="!canWrite" />
              </el-form-item>
            </el-form>

            <el-divider content-position="left">变量预览输入</el-divider>
            <div class="vars-wrap">
              <el-empty v-if="placeholderKeys.length === 0" description="当前模板无变量" />
              <el-form v-else inline class="vars-form">
                <el-form-item v-for="key in placeholderKeys" :key="key" :label="key">
                  <el-input v-model="previewVars[key]" :placeholder="`输入 ${key}`" :disabled="!canWrite" />
                </el-form-item>
              </el-form>
            </div>
          </template>
        </el-card>

        <el-card class="preview-card" v-if="activeCode">
          <template #header>
            <div class="card-title">实时预览</div>
          </template>

          <div class="preview-box">
            <p class="preview-label">通知标题</p>
            <p class="preview-title">{{ previewTitle }}</p>

            <p class="preview-label">通知正文</p>
            <p class="preview-body">{{ previewBody }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminNotificationTemplates, updateAdminNotificationTemplate } from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-消息模板页职责：
 * - 维护治理通知模板（标题/正文/启停）；
 * - 支持变量占位符预览，减少上线后文案错误；
 * - 保存时按模板 code 更新，保证模板稳定标识。
 */
const canWrite = hasAdminPermission('admin.template.write')

const templates = ref([])
const keyword = ref('')
const activeCode = ref('')
const saving = ref(false)

const form = reactive({
  code: '',
  name: '',
  titleTemplate: '',
  bodyTemplate: '',
  enabled: true
})

const previewVars = reactive({})

const defaultVarExamples = {
  reason: '内容不符合社区规范',
  minutes: '60',
  contentTitle: '示例内容标题',
  username: '用户A',
  action: '系统处理',
  target: '某条评论'
}

const filteredTemplates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return templates.value
  return templates.value.filter((item) => {
    const code = String(item.code || '').toLowerCase()
    const name = String(item.name || '').toLowerCase()
    return code.includes(kw) || name.includes(kw)
  })
})

const placeholderKeys = computed(() => {
  const source = `${form.titleTemplate || ''}\n${form.bodyTemplate || ''}`
  const matches = source.match(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g) || []
  const keys = matches
    .map((item) => item.replace(/\{|\}|\s/g, ''))
    .filter(Boolean)
  return [...new Set(keys)]
})

// 左侧模板切换时把模板内容完整同步到编辑表单。
const applyTemplateToForm = (item) => {
  form.code = item?.code || ''
  form.name = item?.name || ''
  form.titleTemplate = item?.titleTemplate || ''
  form.bodyTemplate = item?.bodyTemplate || ''
  form.enabled = Number(item?.status || 0) === 1
}

const selectTemplate = (item) => {
  activeCode.value = item.code
  applyTemplateToForm(item)
}

watch(
  placeholderKeys,
  (keys) => {
    Object.keys(previewVars).forEach((key) => {
      if (!keys.includes(key)) {
        delete previewVars[key]
      }
    })
    keys.forEach((key) => {
      if (previewVars[key] == null) {
        previewVars[key] = defaultVarExamples[key] || ''
      }
    })
  },
  { immediate: true }
)

const renderTemplateText = (template) => {
  if (!template) return ''
  return template.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_, key) => {
    const value = previewVars[key]
    return value == null || value === '' ? `{{${key}}}` : String(value)
  })
}

const previewTitle = computed(() => renderTemplateText(form.titleTemplate) || '-')
const previewBody = computed(() => renderTemplateText(form.bodyTemplate) || '-')

const loadTemplates = async () => {
  const response = await getAdminNotificationTemplates()
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载模板失败')
    return
  }
  templates.value = response.data.data || []

  const stillExists = templates.value.some((item) => item.code === activeCode.value)
  if (!stillExists) {
    activeCode.value = templates.value[0]?.code || ''
  }
  const selected = templates.value.find((item) => item.code === activeCode.value)
  if (selected) {
    applyTemplateToForm(selected)
  }
}

const validateForm = () => {
  if (!form.code) {
    ElMessage.warning('请先选择一个模板')
    return false
  }
  if (!String(form.name || '').trim()) {
    ElMessage.warning('请输入模板名称')
    return false
  }
  if (!String(form.titleTemplate || '').trim()) {
    ElMessage.warning('请输入标题模板')
    return false
  }
  if (!String(form.bodyTemplate || '').trim()) {
    ElMessage.warning('请输入正文模板')
    return false
  }
  return true
}

const saveTemplate = async () => {
  if (!canWrite) return
  if (!validateForm()) return

  saving.value = true
  try {
    const response = await updateAdminNotificationTemplate(form.code, {
      name: form.name,
      titleTemplate: form.titleTemplate,
      bodyTemplate: form.bodyTemplate,
      status: form.enabled ? 1 : 0
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '模板更新失败')
      return
    }
    ElMessage.success('模板已更新')
    await loadTemplates()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '模板更新失败')
  } finally {
    saving.value = false
  }
}

loadTemplates()
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
}

.layout-row {
  width: 100%;
}

.list-card,
.editor-card,
.preview-card {
  border-radius: 16px;
}

.preview-card {
  margin-top: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #2a3d55;
}

.card-subtitle {
  margin: 4px 0 0;
  color: #6b7c93;
  font-size: 13px;
}

.search-input {
  margin-bottom: 10px;
}

.template-scroll {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 8px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.template-item {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 12px;
  padding: 10px;
  text-align: left;
  background: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: all 0.2s ease;
}

.template-item:hover {
  border-color: rgba(34, 197, 188, 0.5);
  background: rgba(236, 253, 245, 0.8);
}

.template-item.active {
  border-color: rgba(20, 184, 166, 0.7);
  box-shadow: 0 0 0 2px rgba(20, 184, 166, 0.16) inset;
  background: rgba(236, 253, 245, 0.9);
}

.template-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.template-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 13px;
  color: #365069;
}

.template-name {
  margin-top: 6px;
  color: #1f2f44;
  font-size: 15px;
  font-weight: 600;
}

.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.editor-form {
  margin-top: 4px;
}

.vars-wrap {
  min-height: 72px;
}

.vars-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 10px;
}

.preview-box {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
}

.preview-label {
  margin: 0;
  color: #6b7c93;
  font-size: 12px;
}

.preview-title {
  margin: 4px 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2f44;
}

.preview-body {
  margin: 4px 0 0;
  white-space: pre-wrap;
  line-height: 1.75;
  color: #2b3f56;
}

@media (max-width: 992px) {
  .editor-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

