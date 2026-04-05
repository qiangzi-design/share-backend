<template>
  <section class="forbidden-page">
    <el-card class="forbidden-card">
      <h1>403</h1>
      <p>你没有访问管理端页面的权限。</p>
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
    </el-card>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

// 管理端 403 页：拦截无权限访问并引导返回首页。
const router = useRouter()
</script>

<style scoped>
.forbidden-page {
  min-height: calc(100vh - 200px);
  display: grid;
  place-items: center;
}

.forbidden-card {
  width: min(420px, 100%);
  text-align: center;
  border-radius: 16px;
  padding: 20px 10px;
}

.forbidden-card h1 {
  font-size: 48px;
  color: #ef4444;
}

.forbidden-card p {
  margin: 8px 0 18px;
  color: #5f738a;
}
</style>
