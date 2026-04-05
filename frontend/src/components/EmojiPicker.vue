<template>
  <el-popover placement="top-start" :width="320" trigger="click" popper-class="emoji-popper">
    <template #reference>
      <el-button class="emoji-btn" size="small" :disabled="disabled">
        <span class="emoji-btn-icon">😊</span>
        <span>表情</span>
      </el-button>
    </template>
    <div class="emoji-grid">
      <button
        v-for="emoji in emojis"
        :key="emoji"
        class="emoji-item"
        type="button"
        @click="emit('select', emoji)"
      >
        {{ emoji }}
      </button>
    </div>
  </el-popover>
</template>

<script setup>
defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select'])

const emojis = [
  '😀', '😄', '😁', '😆', '😉', '😊', '😍', '😘',
  '🤔', '😎', '🥳', '😭', '😡', '👏', '👍', '🙏',
  '🎉', '✨', '🔥', '💯', '❤️', '💙', '💚', '💛',
  '🌟', '🌈', '🎈', '🍀', '☕', '🍜', '🎵', '🚀'
]
</script>

<style scoped>
.emoji-btn {
  border-radius: 999px;
  border: 1px solid rgba(14, 165, 164, 0.35);
  color: #0f766e;
  background: rgba(236, 253, 245, 0.88);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.emoji-btn:hover,
.emoji-btn:focus-visible {
  border-color: rgba(14, 165, 164, 0.55);
  box-shadow: 0 8px 18px rgba(14, 165, 164, 0.2);
  transform: translateY(-1px);
}

.emoji-btn-icon {
  line-height: 1;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 7px;
}

.emoji-item {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(248, 252, 255, 0.9);
  border-radius: 9px;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 9px 0;
  transition: border-color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}

.emoji-item:hover,
.emoji-item:focus-visible {
  border-color: rgba(14, 165, 164, 0.5);
  background: rgba(236, 253, 245, 0.95);
  transform: translateY(-1px);
}
</style>
