<template>
  <section class="auth-page">
    <div class="auth-shell">
      <aside class="auth-intro motion-fade-up">
        <p class="intro-kicker">WELCOME BACK</p>
        <h1>每次登录，都是一次新的灵感回归</h1>
        <p>记录你的生活亮点，浏览他人的故事，与同频的人持续连接。</p>
      </aside>

      <el-card class="auth-card motion-fade-soft">
        <h2>登录账号</h2>
        <el-form ref="formRef" :model="loginForm" :rules="rules" label-width="72px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item class="actions">
            <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
            <el-button @click="router.push('/register')">去注册</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { getMyProfile } from '../api/user'

/**
 * 登录页职责：
 * - 完成用户名密码登录；
 * - 回写 token 与基础用户信息；
 * - 广播 auth-state-changed 触发全局导航同步。
 */
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await login(loginForm)
    if (response.data?.code !== 200 || !response.data?.data) {
      ElMessage.error(response.data?.message || '登录失败')
      return
    }
    const data = response.data.data
    sessionStorage.removeItem('blocked_reason')
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
      // 忽略附加资料请求失败
    }
    window.dispatchEvent(new Event('auth-state-changed'))
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-shell {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  align-items: stretch;
}

.auth-intro {
  border-radius: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at 80% 16%, rgba(255, 255, 255, 0.28), transparent 44%),
    linear-gradient(150deg, #0ea5a4, #0c8c8b 55%, #0f766e);
  color: #ecfffe;
  box-shadow: 0 18px 34px rgba(12, 140, 139, 0.26);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.intro-kicker {
  font-size: 12px;
  letter-spacing: 1.5px;
  opacity: 0.92;
}

.auth-intro h1 {
  margin-top: 10px;
  line-height: 1.3;
  font-size: clamp(26px, 3vw, 34px);
}

.auth-intro p {
  margin-top: 12px;
  line-height: 1.75;
  color: rgba(236, 255, 254, 0.9);
}

.auth-card {
  border-radius: 18px;
  padding: 8px;
  animation-delay: 0.1s;
}

.auth-card h2 {
  margin: 0 0 20px;
}

.actions :deep(.el-form-item__content) {
  gap: 10px;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    padding: 20px;
  }
}
</style>
