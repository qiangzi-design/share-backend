<template>
  <span
    class="user-avatar-trigger"
    :class="{ disabled: !canPreview }"
    :title="canPreview ? '点击查看头像大图' : ''"
    @click.stop="openPreview"
  >
    <el-avatar :size="size" :src="resolvedSrc">
      {{ fallbackInitial }}
    </el-avatar>
  </span>

  <el-dialog
    v-model="previewVisible"
    append-to-body
    align-center
    class="avatar-preview-dialog"
    width="auto"
    :show-close="true"
    :close-on-click-modal="true"
  >
    <div class="avatar-preview-content">
      <img class="avatar-preview-image" :src="resolvedSrc" :alt="alt || fallbackText" />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  size: {
    type: [Number, String],
    default: 40
  },
  fallbackText: {
    type: String,
    default: '用户'
  },
  alt: {
    type: String,
    default: ''
  },
  previewable: {
    type: Boolean,
    default: true
  }
})

const previewVisible = ref(false)

const resolvedSrc = computed(() => {
  const value = String(props.src || '').trim()
  if (!value) return ''
  if (
    value.startsWith('http://') ||
    value.startsWith('https://') ||
    value.startsWith('/') ||
    value.startsWith('data:') ||
    value.startsWith('blob:')
  ) {
    return value
  }
  return `/${value}`
})

const fallbackInitial = computed(() => String(props.fallbackText || '用户').slice(0, 1))

const canPreview = computed(() => props.previewable && Boolean(resolvedSrc.value))

const openPreview = () => {
  if (!canPreview.value) return
  previewVisible.value = true
}
</script>

<style scoped>
.user-avatar-trigger {
  display: inline-flex;
  border-radius: 999px;
  line-height: 0;
  cursor: zoom-in;
}

.user-avatar-trigger.disabled {
  cursor: default;
}

.avatar-preview-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview-image {
  max-width: min(90vw, 560px);
  max-height: 72vh;
  border-radius: 14px;
  object-fit: contain;
  background: #fff;
}

:deep(.avatar-preview-dialog .el-dialog) {
  width: auto;
  margin: 0;
  max-width: 92vw;
  border-radius: 18px;
  overflow: hidden;
}

:deep(.avatar-preview-dialog .el-dialog__header) {
  padding: 10px 12px 0;
}

:deep(.avatar-preview-dialog .el-dialog__body) {
  padding: 10px 12px 14px;
}
</style>
