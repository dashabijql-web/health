<template>
  <div :class="['hm-empty-state', compact ? 'hm-empty-state--compact' : '']">
    <span v-if="eyebrow" class="hm-empty-state__eyebrow">{{ eyebrow }}</span>
    <div class="hm-empty-state__title">{{ title }}</div>
    <div v-if="description" class="hm-empty-state__description">{{ description }}</div>
  </div>
</template>

<script>
export default {
  name: 'PageEmptyState',
  props: {
    eyebrow: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: '暂无数据'
    },
    description: {
      type: String,
      default: ''
    },
    compact: {
      type: Boolean,
      default: false
    }
  }
}
</script>
