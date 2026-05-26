<template>
  <div :class="['hm-empty-state', compact ? 'hm-empty-state--compact' : '']">
    <div class="hm-empty-state__visual" aria-hidden="true">
      <div class="hm-empty-state__orbit"></div>
      <div class="hm-empty-state__core">
        <slot name="icon">
          <span class="hm-empty-state__core-mark">{{ compact ? '0' : 'DATA' }}</span>
        </slot>
      </div>
    </div>
    <span v-if="eyebrow" class="hm-empty-state__eyebrow">{{ eyebrow }}</span>
    <div class="hm-empty-state__title">{{ title }}</div>
    <div v-if="description" class="hm-empty-state__description">{{ description }}</div>
    <div v-if="$slots.actions" class="hm-empty-state__actions">
      <slot name="actions" />
    </div>
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
