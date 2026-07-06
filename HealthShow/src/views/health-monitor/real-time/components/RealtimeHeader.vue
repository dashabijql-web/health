<template>
  <header class="rt-hd">
    <div class="rt-hd-bar">
      <div class="rt-hd-bar-left">
        <span class="rt-live-dot"></span>
        <span class="rt-hd-bar-title">实时健康监控</span>
        <span class="rt-hd-time">{{ currentTime }}</span>
      </div>
      <div class="rt-hd-bar-kpis">
        <div class="rt-hd-kpi">
          <span class="rt-hd-kpi-val kpi-primary">{{ totalCount }}</span>
          <span class="rt-hd-kpi-label">在线</span>
        </div>
        <div class="rt-hd-kpi-sep"></div>
        <div class="rt-hd-kpi">
          <span class="rt-hd-kpi-val kpi-success">{{ normalCount }}</span>
          <span class="rt-hd-kpi-label">正常</span>
        </div>
        <div class="rt-hd-kpi-sep"></div>
        <div class="rt-hd-kpi">
          <span class="rt-hd-kpi-val kpi-danger" :class="{ 'val-blink': warningCount > 0 }">{{ warningCount }}</span>
          <span class="rt-hd-kpi-label">预警</span>
        </div>
      </div>
    </div>

    <div class="rt-ticker-wrap">
      <span class="rt-ticker-label">实时预警</span>
      <div class="rt-ticker-scroll">
        <template v-if="warningUsers.length">
          <div class="rt-ticker-inner">
            <span
              v-for="(user, index) in tickerUsers"
              :key="`${user.userCode || user.imei || 'warning'}_${index}`"
              class="rt-ticker-tag"
            >
              {{ user.userName }} <em>{{ getRealtimeIndicator(user) }}</em>
            </span>
          </div>
        </template>
        <span v-else class="rt-ticker-empty">暂无预警人员</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { getRealtimeIndicator } from '../realtime-helpers'

const props = defineProps({
  warningUsers: { type: Array, default: () => [] },
  totalCount: { type: Number, default: 0 },
  normalCount: { type: Number, default: 0 },
  warningCount: { type: Number, default: 0 },
  currentTime: { type: String, default: '' }
})

const tickerUsers = computed(() => [...props.warningUsers, ...props.warningUsers])
</script>
