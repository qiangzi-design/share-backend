<template>
  <section class="auth-page">
    <div class="auth-shell">
      <aside class="auth-intro motion-fade-up">
        <p class="intro-kicker">CREATE YOUR SPACE</p>
        <h1>把你的生活方式，变成有温度的内容流</h1>
        <p>注册后即可发布图文、整理标签、管理个人主页，和更多人建立真实互动。</p>
      </aside>

      <el-card class="auth-card motion-fade-soft">
        <h2>创建账号</h2>
        <el-form ref="formRef" :model="registerForm" :rules="rules" label-width="88px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="registerForm.username" placeholder="3-20个字符" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" />
          </el-form-item>
          <el-form-item class="actions">
            <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
            <el-button @click="router.push('/login')">去登录</el-button>
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
import { register } from '../api/auth'

/**
 * 注册页职责：
 * - 完成账号创建基础校验；
 * - 注册成功后跳转登录，不在本页自动登录。
 */
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: ''
})

const validateConfirmPassword = (_, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在 3-20 字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await register({
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email,
      nickname: registerForm.nickname
    })
    if (response.data?.code !== 200) {
      ElMessage.error(response.data?.message || '注册失败')
      return
    }
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '注册失败')
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
  width: min(1040px, 100%);
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 18px;
  align-items: stretch;
}

.auth-intro {
  border-radius: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at 12% 16%, rgba(255, 255, 255, 0.26), transparent 42%),
    linear-gradient(145deg, #f97316, #ef5f1f 58%, #de4b1f);
  color: #fff7ed;
  box-shadow: 0 18px 34px rgba(239, 95, 31, 0.28);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.intro-kicker {
  font-size: 12px;
  letter-spacing: 1.6px;
  opacity: 0.9;
}

.auth-intro h1 {
  margin-top: 10px;
  line-height: 1.3;
  font-size: clamp(26px, 3vw, 34px);
}

.auth-intro p {
  margin-top: 12px;
  line-height: 1.75;
  color: rgba(255, 247, 237, 0.92);
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

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    padding: 20px;
  }
}
</style>
