<template>
  <div :class="['hm-metric-strip', dense ? 'hm-metric-strip--dense' : '']">
    <button
      v-for="item in items"
      :key="item.key || item.label"
      type="button"
      :data-tone="item.tone || 'primary'"
      :class="['hm-metric-strip__item', isClickable(item) ? 'is-clickable' : '']"
      :disabled="!isClickable(item)"
      @click="handleSelect(item)"
    >
      <span class="hm-metric-strip__label">{{ item.label }}</span>
      <span class="hm-metric-strip__value">{{ item.value }}</span>
      <span v-if="item.note" class="hm-metric-strip__note">{{ item.note }}</span>
    </button>
  </div>
</template>

<script>
export default {
  name: 'MetricStrip',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    dense: {
      type: Boolean,
      default: false
    },
    clickable: {
      type: Boolean,
      default: false
    }
  },
  emits: ['select'],
  methods: {
    isClickable(item) {
      return this.clickable || Boolean(item?.clickable)
    },
    handleSelect(item) {
      if (!this.isClickable(item)) return
      this.$emit('select', item)
    }
  }
}
</script>
