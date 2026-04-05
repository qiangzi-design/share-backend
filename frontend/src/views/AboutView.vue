<template>
  <section class="about-page">
    <div class="about-hero motion-fade-up">
      <p class="hero-kicker">ABOUT THIS SITE</p>
      <h1>一个为生活灵感而生的分享社区</h1>
      <p>我们希望把“发布内容”变成一件轻松又有成就感的事情，让记录与交流自然发生。</p>
    </div>

    <div class="about-grid">
      <el-card class="about-card motion-fade-soft">
        <h2>我们在做什么</h2>
        <p>提供图文发布、分类标签、搜索筛选与个人内容管理，让信息组织更清晰。</p>
      </el-card>
      <el-card class="about-card motion-fade-soft">
        <h2>为什么存在</h2>
        <p>日常值得被看见。无论是学习、旅行、美食还是心情，都可以沉淀成可回看的记录。</p>
      </el-card>
      <el-card class="about-card motion-fade-soft">
        <h2>你可以获得什么</h2>
        <p>更高效的内容浏览体验、真实可持续的互动反馈，以及不断完善的创作工具。</p>
      </el-card>
    </div>
  </section>
</template>

<script setup>
/**
 * 关于页职责：
 * - 向用户说明平台定位与核心价值；
 * - 不承载业务写操作，仅用于静态说明展示。
 */
</script>

<style scoped>
.about-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.about-hero {
  border-radius: 18px;
  padding: 24px;
  color: #f0fdfa;
  background:
    radial-gradient(circle at 88% 12%, rgba(255, 255, 255, 0.22), transparent 40%),
    linear-gradient(140deg, #0f766e, #0ea5a4 56%, #f97316);
  box-shadow: 0 18px 36px rgba(20, 88, 104, 0.24);
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 1.5px;
}

.about-hero h1 {
  margin-top: 8px;
  font-size: clamp(26px, 3.2vw, 36px);
  line-height: 1.3;
}

.about-hero p {
  margin-top: 10px;
  line-height: 1.7;
  color: rgba(240, 253, 250, 0.92);
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.about-card {
  border-radius: 16px;
}

.about-card:nth-child(2) {
  animation-delay: 0.08s;
}

.about-card:nth-child(3) {
  animation-delay: 0.16s;
}

.about-card h2 {
  margin-bottom: 8px;
  color: #1f2d3d;
}

.about-card p {
  line-height: 1.8;
  color: #4b5a70;
}

@media (max-width: 860px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
