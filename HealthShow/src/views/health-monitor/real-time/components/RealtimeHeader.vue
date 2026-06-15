<template>
  <header class="rt-hd">
    <PageHeroHeader
      class="rt-hd-hero"
      variant="cockpit"
      eyebrow="Realtime Watch"
      title="实时健康监控"
      description="在线人员状态、预警聚焦与消息广播统一在一处。"
    >
      <template #meta>
        <div class="rt-hd-meta">
          <span class="rt-live-dot"></span>
          <span class="rt-hd-meta-label">在线监测</span>
          <span class="rt-hd-time">{{ currentTime }}</span>
        </div>
      </template>
      <template #actions>
        <div class="rt-hd-right">
          <div class="rt-hd-stat">
            <span class="rt-hd-stat-val st-ok">{{ normalCount }}</span>
            <span class="rt-hd-stat-lbl">正常</span>
          </div>
          <div class="rt-hd-sep"></div>
          <div class="rt-hd-stat">
            <span class="rt-hd-stat-val st-warn" :class="{ 'val-blink': warningCount > 0 }">{{ warningCount }}</span>
            <span class="rt-hd-stat-lbl">预警中</span>
          </div>
        </div>
      </template>
    </PageHeroHeader>

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

    <MetricStrip
      class="rt-hd-kpis"
      :items="headerMetricItems"
      dense
    />
  </header>
</template>

<script setup>
import { computed } from 'vue'
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'
import { getRealtimeIndicator } from '../realtime-helpers'

const props = defineProps({
  warningUsers: { type: Array, default: () => [] },
  totalCount: { type: Number, default: 0 },
  normalCount: { type: Number, default: 0 },
  warningCount: { type: Number, default: 0 },
  currentTime: { type: String, default: '' }
})

const tickerUsers = computed(() => [...props.warningUsers, ...props.warningUsers])
const headerMetricItems = computed(() => [
  {
    label: '在线总数',
    value: `${props.totalCount} 人`,
    note: '15 秒自动刷新',
    tone: 'primary'
  },
  {
    label: '正常状态',
    value: `${props.normalCount} 人`,
    note: '实时同步',
    tone: 'success'
  },
  {
    label: '预警中',
    value: `${props.warningCount} 人`,
    note: props.warningCount > 0 ? '优先通知与广播' : '当前无预警',
    tone: props.warningCount > 0 ? 'danger' : 'success'
  }
])
</script>
