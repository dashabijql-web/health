<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    class="rt-dialog"
    width="420px"
    :append-to-body="true"
    :close-on-click-modal="true"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="user" class="rt-detail-grid">
      <div
        v-for="item in items"
        :key="item.label"
        class="rt-detail-card"
      >
        <div class="rt-detail-label">{{ item.label }}</div>
        <div class="rt-detail-value" :style="{ color: item.color }">{{ item.value }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  user: { type: Object, default: null },
  items: { type: Array, default: () => [] }
})

defineEmits(['update:visible'])

const title = computed(() => `${props.user?.userName || ''} 实时体征`)
</script>

<style scoped>
.rt-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 8px 0;
}

.rt-detail-card {
  background: rgba(0,212,255,0.06);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 8px;
  padding: 12px 16px;
}

.rt-detail-label {
  font-size: 11px;
  color: #8ba6c8;
  margin-bottom: 4px;
}

.rt-detail-value {
  font-size: 22px;
  font-weight: 700;
  font-family: Consolas, monospace;
}
</style>
