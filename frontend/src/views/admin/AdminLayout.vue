<template>
  <section class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <h2>管理端</h2>
        <p>{{ adminName }}</p>
      </div>
      <el-menu class="admin-menu" :default-active="activePath" router>
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="admin-content">
      <div class="admin-toolbar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item to="/">首页</el-breadcrumb-item>
          <el-breadcrumb-item>管理端</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <router-view />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ensureAdminMe, getAdminPermissionsFromCache } from '../../utils/adminAuth'

/**
 * 管理端布局职责：
 * - 根据权限动态渲染左侧菜单；
 * - 承载管理端二级路由容器；
 * - 启动时拉取 admin/me 同步管理员身份。
 */
const route = useRoute()
const adminName = ref('管理员')
const permissions = ref(getAdminPermissionsFromCache())

const allMenuItems = [
  { path: '/admin/dashboard', label: '仪表盘', permission: 'admin.dashboard.read' },
  { path: '/admin/users', label: '用户管理', permission: 'admin.user.read' },
  { path: '/admin/contents', label: '内容审核', permission: 'admin.content.read' },
  { path: '/admin/comments', label: '评论审核', permission: 'admin.comment.read' },
  { path: '/admin/reports', label: '举报中心', permission: 'admin.report.read' },
  { path: '/admin/report-templates', label: '举报模板', permission: 'admin.report.read' },
  { path: '/admin/categories', label: '分类管理', permission: 'admin.category.read' },
  { path: '/admin/tags', label: '标签运营', permission: 'admin.tag.read' },
  { path: '/admin/announcements', label: '公告管理', permission: 'admin.announcement.read' },
  { path: '/admin/templates', label: '消息模板', permission: 'admin.template.read' },
  { path: '/admin/analytics', label: '运营分析', permission: 'admin.analytics.read' },
  { path: '/admin/audit-logs', label: '操作审计', permission: 'admin.audit.read' }
]

const menuItems = computed(() =>
  allMenuItems.filter((item) => permissions.value.includes(item.permission))
)

const activePath = computed(() => {
  const matched = allMenuItems.find((item) => route.path.startsWith(item.path))
  return matched?.path || '/admin/dashboard'
})

onMounted(async () => {
  const me = await ensureAdminMe().catch(() => null)
  if (!me) return
  adminName.value = me.nickname || me.username || '管理员'
  permissions.value = Array.isArray(me.permissions) ? me.permissions : []
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
  min-height: calc(100vh - 220px);
}

.admin-sidebar {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: var(--shadow-soft);
  padding: 16px 12px;
}

.admin-brand {
  padding: 6px 10px 14px;
}

.admin-brand h2 {
  font-size: 56px;
  line-height: 1;
  letter-spacing: 0.4px;
  color: #123253;
}

.admin-brand p {
  margin-top: 10px;
  font-size: 18px;
  color: #5a7593;
  font-weight: 600;
}

.admin-menu {
  border-right: 0;
  background: transparent;
}

.admin-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  margin: 6px 0;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.78);
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: #0a7f7e;
  border-color: rgba(14, 165, 164, 0.34);
  background: linear-gradient(135deg, rgba(14, 165, 164, 0.16), rgba(14, 165, 164, 0.08));
}

.admin-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-toolbar {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.78);
  padding: 12px 16px;
  margin-bottom: 12px;
}

.admin-toolbar :deep(.el-breadcrumb__item) {
  font-size: 18px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .admin-brand h2 {
    font-size: 40px;
  }
}
</style>
