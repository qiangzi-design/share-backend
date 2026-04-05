<template>
  <section class="admin-page">
    <el-card class="filter-card">
      <div class="filters filters-3col">
        <el-input v-model="filters.keyword" clearable placeholder="搜索用户名/昵称/邮箱" @keyup.enter="loadUsers(1)" />
        <el-select v-model="filters.status" clearable placeholder="用户状态">
          <el-option label="正常" :value="1" />
          <el-option label="禁言" :value="2" />
          <el-option label="封禁" :value="3" />
        </el-select>
        <el-button type="primary" class="query-btn" @click="loadUsers(1)">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="pageData.list" class="admin-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="240" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="禁言到期" width="170">
          <template #default="{ row }">{{ formatTime(row.muteUntil) || '-' }}</template>
        </el-table-column>
        <el-table-column label="封禁时间" width="170">
          <template #default="{ row }">{{ formatTime(row.banTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="风险标记" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" type="danger">{{ row.riskLevel }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button class="action-btn btn-primary-soft" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="canMuteUser && row.status !== 3"
                :class="['action-btn', row.status === 2 ? 'btn-unmute' : 'btn-mute']"
                @click="handleMuteToggle(row)"
              >
                {{ row.status === 2 ? '解除禁言' : '禁言' }}
              </el-button>
              <el-button
                v-if="canBanUser && row.status !== 3"
                class="action-btn btn-ban"
                @click="ban(row)"
              >
                封禁
              </el-button>
              <el-button
                v-if="canBanUser && row.status === 3"
                class="action-btn btn-unban"
                @click="unban(row)"
              >
                解封
              </el-button>
              <el-button
                v-if="canRiskMark"
                class="action-btn btn-risk"
                @click="openRiskDialog(row)"
              >
                {{ row.riskLevel ? '调整风险' : '风险标记' }}
              </el-button>
              <span v-if="!canBanUser && !canMuteUser && !canRiskMark" class="op-muted">只读</span>
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
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="muteDialog.visible" title="设置禁言" width="430px">
      <el-form label-width="90px">
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="muteDialog.minutes" :min="1" :max="525600" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="muteDialog.reason" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="muteDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmMute">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="riskDialog.visible" title="风险用户标记" width="520px">
      <el-form label-width="90px">
        <el-form-item label="风险等级">
          <el-select v-model="riskDialog.riskLevel" placeholder="选择等级">
            <el-option label="低" value="low" />
            <el-option label="中" value="medium" />
            <el-option label="高" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="riskDialog.riskNote" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="riskDialog.visible = false">取消</el-button>
        <el-button v-if="riskDialog.currentRiskLevel" class="danger-inline" @click="clearRisk">取消标记</el-button>
        <el-button type="primary" @click="confirmRisk">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer.visible" title="用户详情" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户ID">{{ detailDrawer.data.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailDrawer.data.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailDrawer.data.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailDrawer.data.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detailDrawer.data.status) }}</el-descriptions-item>
        <el-descriptions-item label="近30天内容">{{ detailDrawer.data.recentContentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="近30天评论">{{ detailDrawer.data.recentCommentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待审核内容">{{ detailDrawer.data.pendingReviewContentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待审核评论">{{ detailDrawer.data.pendingReviewCommentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="粉丝">{{ detailDrawer.data.followerCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="关注">{{ detailDrawer.data.followingCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="取关">{{ detailDrawer.data.unfollowCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ detailDrawer.data.riskLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险备注">{{ detailDrawer.data.riskNote || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  banAdminUser,
  getAdminUserDetail,
  getAdminUsers,
  markAdminUserRisk,
  muteAdminUser,
  unbanAdminUser,
  unmarkAdminUserRisk,
  unmuteAdminUser
} from '../../api/admin'
import { hasAdminPermission } from '../../utils/adminAuth'

/**
 * 管理端-用户管理页职责：
 * 1. 提供用户查询与治理动作（禁言/封禁/风险标记）。
 * 2. 严格按权限码做前端按钮门禁，后端再二次校验。
 * 3. 状态口径固定为：1正常、2禁言、3封禁（封禁优先级最高）。
 */
const canBanUser = hasAdminPermission('admin.user.ban')
const canMuteUser = hasAdminPermission('admin.user.mute')
const canRiskMark = hasAdminPermission('admin.user.risk_mark')

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

const muteDialog = reactive({
  visible: false,
  targetUserId: null,
  minutes: 60,
  reason: ''
})

const riskDialog = reactive({
  visible: false,
  targetUserId: null,
  riskLevel: 'low',
  riskNote: '',
  currentRiskLevel: ''
})

const detailDrawer = reactive({
  visible: false,
  data: {}
})

const currentUserId = Number(sessionStorage.getItem('userId') || 0)

// 兼容历史状态值 0（老封禁）并统一展示为“封禁”。
const statusLabel = (status) => {
  if (status === 1) return '正常'
  if (status === 2) return '禁言'
  if (status === 3 || status === 0) return '封禁'
  return '-'
}

const statusTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  if (status === 3 || status === 0) return 'danger'
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

const loadUsers = async (targetPage = 1) => {
  const response = await getAdminUsers({
    page: targetPage,
    pageSize: pageData.pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status ?? undefined
  })
  if (response.data?.code !== 200) {
    ElMessage.error(response.data?.message || '加载用户失败')
    return
  }
  const data = response.data.data || {}
  pageData.list = data.list || []
  pageData.page = data.page || targetPage
  pageData.pageSize = data.pageSize || pageData.pageSize
  pageData.total = data.total || 0
}

// 管理员不能封禁自己，避免误操作把当前会话直接踢出导致治理中断。
const ban = async (row) => {
  if (!canBanUser) return
  if (row.id === currentUserId) {
    ElMessage.warning('不能封禁当前登录账号')
    return
  }
  await ElMessageBox.prompt('请输入封禁原因（可选）', '封禁用户', {
    confirmButtonText: '封禁',
    cancelButtonText: '取消'
  })
    .then(async ({ value }) => {
      await banAdminUser(row.id, { reason: value || '' })
      ElMessage.success('已封禁')
      await loadUsers(pageData.page)
    })
    .catch((error) => {
      if (error === 'cancel' || error === 'close') return
      ElMessage.error(error?.response?.data?.message || '封禁失败')
    })
}

const unban = async (row) => {
  if (!canBanUser) return
  try {
    await unbanAdminUser(row.id)
    ElMessage.success('已解封')
    await loadUsers(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '解封失败')
  }
}

const openMuteDialog = (row) => {
  if (!canMuteUser) return
  // 封禁状态下不再允许禁言操作，遵循“封禁优先”。
  if (row.status === 3) {
    ElMessage.warning('该用户已封禁，封禁状态优先')
    return
  }
  muteDialog.visible = true
  muteDialog.targetUserId = row.id
  muteDialog.minutes = 60
  muteDialog.reason = ''
}

const confirmMute = async () => {
  if (!canMuteUser || !muteDialog.targetUserId) return
  if (muteDialog.targetUserId === currentUserId) {
    ElMessage.warning('不能禁言当前登录账号')
    return
  }
  try {
    await muteAdminUser(muteDialog.targetUserId, {
      minutes: muteDialog.minutes,
      reason: muteDialog.reason
    })
    muteDialog.visible = false
    ElMessage.success('禁言已生效')
    await loadUsers(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '禁言失败')
  }
}

const unmute = async (row) => {
  if (!canMuteUser) return
  try {
    await unmuteAdminUser(row.id)
    ElMessage.success('已解除禁言')
    await loadUsers(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '解除禁言失败')
  }
}

const handleMuteToggle = async (row) => {
  if (row.status === 2) {
    await unmute(row)
    return
  }
  openMuteDialog(row)
}

// 风险标记用于运营识别，不改变账号登录态和可用态。
const openRiskDialog = (row) => {
  if (!canRiskMark) return
  riskDialog.visible = true
  riskDialog.targetUserId = row.id
  riskDialog.riskLevel = row.riskLevel || 'low'
  riskDialog.riskNote = row.riskNote || ''
  riskDialog.currentRiskLevel = row.riskLevel || ''
}

const confirmRisk = async () => {
  if (!canRiskMark || !riskDialog.targetUserId) return
  try {
    await markAdminUserRisk(riskDialog.targetUserId, {
      riskLevel: riskDialog.riskLevel,
      riskNote: riskDialog.riskNote
    })
    ElMessage.success('风险标记已更新')
    riskDialog.visible = false
    await loadUsers(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '风险标记失败')
  }
}

const clearRisk = async () => {
  if (!canRiskMark || !riskDialog.targetUserId) return
  try {
    await unmarkAdminUserRisk(riskDialog.targetUserId)
    ElMessage.success('风险标记已取消')
    riskDialog.visible = false
    await loadUsers(pageData.page)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '取消标记失败')
  }
}

const openDetail = async (row) => {
  detailDrawer.visible = true
  detailDrawer.data = { ...row }
  try {
    const response = await getAdminUserDetail(row.id)
    if (response.data?.code === 200 && response.data?.data) {
      detailDrawer.data = response.data.data
    }
  } catch (_) {
    // keep list row data fallback
  }
}

loadUsers(1)
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

.table-card {
  min-height: 700px;
}

.filters {
  display: grid;
  align-items: center;
  gap: 12px;
}

.filters-3col {
  grid-template-columns: minmax(260px, 1fr) 180px 96px;
}

.query-btn {
  min-height: 42px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
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

.btn-mute {
  border: 1px solid #d8a14c !important;
  color: #1b2f4c !important;
  background: #fff8eb !important;
}

.btn-unmute {
  border: 1px solid #8fa1b8 !important;
  color: #1b2f4c !important;
  background: #f3f8ff !important;
}

.btn-ban {
  border: 1px solid #dc6e7c !important;
  color: #b34350 !important;
  background: #ffeaed !important;
}

.btn-unban {
  border: 1px solid #63a696 !important;
  color: #1f6c61 !important;
  background: #e2f3ef !important;
}

.btn-risk {
  border: 1px solid #8b5cf6 !important;
  color: #6d28d9 !important;
  background: #f3e8ff !important;
}

.danger-inline {
  border-color: #ef4444 !important;
  color: #ef4444 !important;
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

