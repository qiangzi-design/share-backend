<template>
  <section class="my-works-page">
    <el-card class="works-card motion-fade-up">
      <template #header>
        <div class="works-header">
          <div>
            <h2>作品管理</h2>
            <p>在这里统一管理你发布的作品，可编辑、删除并查看互动数据。</p>
          </div>
          <div class="works-header-actions">
            <el-select v-model="sort" class="sort-select" @change="loadMyWorks(1)">
              <el-option label="最新发布" value="latest" />
              <el-option label="热门优先" value="popular" />
              <el-option label="浏览量优先" value="view_desc" />
            </el-select>
            <el-button class="publish-btn" type="primary" @click="router.push('/publish')">去发布</el-button>
          </div>
        </div>
      </template>

      <el-table class="works-table" size="small" table-layout="fixed" :data="myWorks.list" empty-text="暂无作品">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="审核" width="94" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="reviewTagType(row.reviewStatus)">
              {{ reviewText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="156" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
        <el-table-column prop="likeCount" label="点赞" width="70" align="center" />
        <el-table-column prop="collectionCount" label="收藏" width="70" align="center" />
        <el-table-column prop="commentCount" label="评论" width="70" align="center" />
        <el-table-column label="操作" width="166" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <el-button class="action-btn edit" @click="router.push(`/content/${row.id}/edit`)">编辑</el-button>
              <el-button class="action-btn delete" @click="confirmDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next, ->, total"
          :current-page="myWorks.page"
          :page-size="myWorks.pageSize"
          :total="myWorks.total"
          @current-change="loadMyWorks"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteContent } from '../api/content'
import { getMyProfile, getUserContents } from '../api/user'

/**
 * 我的作品页职责：
 * - 展示当前用户作品列表与互动数据；
 * - 支持编辑与删除；
 * - 排序支持最新/热门/浏览优先。
 */
const router = useRouter()
const sort = ref('latest')
const profileId = ref(null)

const myWorks = reactive({
  list: [],
  page: 1,
  pageSize: 10,
  total: 0
})

const loadProfile = async () => {
  const response = await getMyProfile()
  if (response.data?.code !== 200 || !response.data?.data?.id) {
    throw new Error(response.data?.message || '加载个人信息失败')
  }
  profileId.value = Number(response.data.data.id)
}

const loadMyWorks = async (targetPage = 1) => {
  if (!profileId.value) return
  try {
    const response = await getUserContents(profileId.value, {
      page: targetPage,
      pageSize: myWorks.pageSize,
      sort: sort.value
    })
    const data = response.data?.data || {}
    myWorks.list = data.list || []
    myWorks.page = data.page || targetPage
    myWorks.pageSize = data.pageSize || myWorks.pageSize
    myWorks.total = Number(data.total || 0)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载作品失败')
  }
}

// 删除采用二次确认，且在删除最后一条时自动回退到上一页，避免空页停留。
const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认删除吗？', '提示', { type: 'warning' })
    const response = await deleteContent(id)
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    const reloadPage = myWorks.list.length === 1 && myWorks.page > 1 ? myWorks.page - 1 : myWorks.page
    await loadMyWorks(reloadPage)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

const reviewText = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return '通过'
  if (status === 'rejected') return '驳回'
  return '待审核'
}

const reviewTagType = (reviewStatus) => {
  const status = String(reviewStatus || 'pending').toLowerCase()
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'warning'
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

onMounted(async () => {
  try {
    await loadProfile()
    await loadMyWorks(1)
  } catch (_) {
    ElMessage.error('加载作品管理失败')
  }
})
</script>

<style scoped>
.my-works-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.works-card {
  border-radius: 18px;
}

.works-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.works-header h2 {
  margin: 0;
  color: #1f2d3d;
  font-size: 24px;
}

.works-header p {
  margin: 6px 0 0;
  color: #5b6d83;
  font-size: 14px;
}

.works-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-select {
  width: 148px;
}

.publish-btn {
  height: 34px;
  border-radius: 999px;
  padding: 0 16px;
}

.works-table :deep(.el-table__header th) {
  height: 52px;
  color: #55667a;
  font-size: 14px;
}

.works-table :deep(.el-table__body td) {
  height: 52px;
}

.works-table :deep(.el-table__header .cell),
.works-table :deep(.el-table__body .cell) {
  text-align: center;
}

.action-group {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.action-btn {
  height: 28px;
  min-width: 56px;
  border-radius: 999px !important;
  padding: 0 12px;
  font-size: 12px;
}

.action-btn.edit {
  color: #0f766e !important;
  background: linear-gradient(180deg, rgba(14, 165, 164, 0.12), rgba(14, 165, 164, 0.05)) !important;
  border: 1px solid rgba(14, 165, 164, 0.35) !important;
}

.action-btn.delete {
  color: #b42323 !important;
  background: linear-gradient(180deg, rgba(220, 38, 38, 0.1), rgba(220, 38, 38, 0.04)) !important;
  border: 1px solid rgba(220, 38, 38, 0.33) !important;
}

.pager-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .works-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
