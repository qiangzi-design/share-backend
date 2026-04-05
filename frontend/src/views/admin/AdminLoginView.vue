<template>
  <section class="admin-login-page">
    <el-card class="admin-login-card motion-fade-up">
      <p class="kicker">ADMIN CONSOLE</p>
      <h1>管理端登录</h1>
      <p class="desc">使用你的平台账号登录，系统会自动校验管理权限。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <div class="actions">
            <el-button type="primary" :loading="loading" @click="handleLogin">进入管理端</el-button>
            <el-button @click="router.push('/')">返回首页</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'
import { getMyProfile } from '../../api/user'
import { clearAdminCache, refreshAdminMe } from '../../utils/adminAuth'

/**
 * 管理端独立登录入口职责：
 * - 复用普通登录接口；
 * - 登录后强制校验 admin/me；
 * - 无管理权限立即清理会话并提示。
 */
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const clearAuth = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('username')
  sessionStorage.removeItem('nickname')
  sessionStorage.removeItem('userId')
  clearAdminCache()
  window.dispatchEvent(new Event('auth-state-changed'))
  window.dispatchEvent(new Event('admin-state-changed'))
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const loginResponse = await login(form)
    if (loginResponse.data?.code !== 200 || !loginResponse.data?.data) {
      ElMessage.error(loginResponse.data?.message || '登录失败')
      return
    }

    const data = loginResponse.data.data
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('username', data.username)
    sessionStorage.setItem('nickname', data.nickname || data.username)

    try {
      const profileResponse = await getMyProfile()
      if (profileResponse.data?.data?.id) {
        sessionStorage.setItem('userId', String(profileResponse.data.data.id))
      }
      if (profileResponse.data?.data?.nickname) {
        sessionStorage.setItem('nickname', profileResponse.data.data.nickname)
      }
    } catch (_) {
      // ignore profile fallback
    }

    try {
      await refreshAdminMe()
    } catch (_) {
      clearAuth()
      ElMessage.error('当前账号没有管理端权限')
      return
    }

    window.dispatchEvent(new Event('auth-state-changed'))
    window.dispatchEvent(new Event('admin-state-changed'))
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (error) {
    clearAuth()
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: calc(100vh - 120px);
  display: grid;
  place-items: center;
}

.admin-login-card {
  width: min(520px, 100%);
  border-radius: 18px;
}

.kicker {
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 1.3px;
}

h1 {
  margin-top: 6px;
  margin-bottom: 8px;
  color: #0f172a;
}

.desc {
  color: #64748b;
  margin-bottom: 14px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
