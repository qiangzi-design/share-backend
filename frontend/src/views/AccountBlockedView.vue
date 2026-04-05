<template>
  <section class="blocked-page">
    <el-card class="blocked-card">
      <h1>账号已封禁</h1>
      <p class="tip">你的登录会话已失效，如有疑问可联系管理员申诉。</p>
      <div class="reason-box">
        <label>封禁原因</label>
        <p>{{ blockReason }}</p>
      </div>
      <div class="actions">
        <el-button @click="goHome">返回首页</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

// 封禁提示页职责：给被封禁用户明确反馈并提供回首页入口。
const router = useRouter()

const blockReason = computed(() => sessionStorage.getItem('blocked_reason') || '你的账号已被封禁，请联系管理员')

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.blocked-page {
  min-height: calc(100vh - 180px);
  display: grid;
  place-items: center;
}

.blocked-card {
  width: min(560px, 100%);
  border-radius: 16px;
  padding: 20px 16px;
}

.blocked-card h1 {
  font-size: 30px;
  color: #dc2626;
}

.tip {
  margin-top: 8px;
  color: #5f738a;
}

.reason-box {
  margin-top: 16px;
  border-radius: 12px;
  border: 1px solid rgba(220, 38, 38, 0.26);
  background: rgba(254, 242, 242, 0.8);
  padding: 12px;
}

.reason-box label {
  display: block;
  font-size: 12px;
  color: #991b1b;
}

.reason-box p {
  margin-top: 6px;
  color: #7f1d1d;
  line-height: 1.6;
  white-space: pre-wrap;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
