import { createRouter, createWebHistory } from 'vue-router'
import { clearAdminCache, ensureAdminMe } from '../utils/adminAuth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/ai-brief',
      name: 'AiBrief',
      component: () => import('../views/AiBriefView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/content/:id',
      name: 'ContentDetail',
      component: () => import('../views/ContentDetailView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/publish',
      name: 'Publish',
      component: () => import('../views/PublishView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/content/:id/edit',
      name: 'EditContent',
      component: () => import('../views/PublishView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/insights',
      name: 'DataInsights',
      component: () => import('../views/DataInsightsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-works',
      name: 'MyWorks',
      component: () => import('../views/MyWorksView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notifications',
      name: 'Notifications',
      component: () => import('../views/NotificationsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-reports',
      name: 'MyReports',
      component: () => import('../views/MyReportsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/users/:id',
      name: 'PublicProfile',
      component: () => import('../views/PublicProfileView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/about',
      name: 'About',
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('../views/AdminForbiddenView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/admin/login',
      name: 'AdminLogin',
      component: () => import('../views/admin/AdminLoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/account-blocked',
      name: 'AccountBlocked',
      component: () => import('../views/AccountBlockedView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('../views/admin/AdminDashboardView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.dashboard.read' }
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('../views/admin/AdminUsersView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.user.read' }
        },
        {
          path: 'contents',
          name: 'AdminContents',
          component: () => import('../views/admin/AdminContentsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.content.read' }
        },
        {
          path: 'comments',
          name: 'AdminComments',
          component: () => import('../views/admin/AdminCommentsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.comment.read' }
        },
        {
          path: 'reports',
          name: 'AdminReports',
          component: () => import('../views/admin/AdminReportsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.report.read' }
        },
        {
          path: 'report-templates',
          name: 'AdminReportTemplates',
          component: () => import('../views/admin/AdminReportTemplatesView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.report.read' }
        },
        {
          path: 'audit-logs',
          name: 'AdminAuditLogs',
          component: () => import('../views/admin/AdminAuditLogsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.audit.read' }
        },
        {
          path: 'categories',
          name: 'AdminCategories',
          component: () => import('../views/admin/AdminCategoriesView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.category.read' }
        },
        {
          path: 'tags',
          name: 'AdminTags',
          component: () => import('../views/admin/AdminTagsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.tag.read' }
        },
        {
          path: 'announcements',
          name: 'AdminAnnouncements',
          component: () => import('../views/admin/AdminAnnouncementsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.announcement.read' }
        },
        {
          path: 'templates',
          name: 'AdminTemplates',
          component: () => import('../views/admin/AdminTemplatesView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.template.read' }
        },
        {
          path: 'analytics',
          name: 'AdminAnalytics',
          component: () => import('../views/admin/AdminAnalyticsView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, adminPermission: 'admin.analytics.read' }
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const token = sessionStorage.getItem('token')
  const toAdminRoute = to.path.startsWith('/admin')

  if (to.path === '/admin/login') {
    if (!token) return true
    try {
      const me = await ensureAdminMe()
      const permissions = Array.isArray(me?.permissions) ? me.permissions : []
      if (permissions.length > 0) {
        return '/admin/dashboard'
      }
    } catch (_) {
      clearAdminCache()
    }
    return true
  }

  if (to.meta.requiresAdmin) {
    if (!token) {
      return '/admin/login'
    }
    try {
      const me = await ensureAdminMe()
      const permission = to.meta.adminPermission
      const permissions = Array.isArray(me?.permissions) ? me.permissions : []
      if (!permission || permissions.includes(permission)) {
        return true
      }
      return '/403'
    } catch (_) {
      clearAdminCache()
      return '/admin/login'
    }
  }

  if (to.meta.requiresAuth && !token) {
    return toAdminRoute ? '/admin/login' : '/login'
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return '/profile'
  }
  return true
})

export default router
