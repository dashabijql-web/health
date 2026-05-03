<template>
  <div class="wcn-wrap">
    <div class="wcn-head">
      <div>
        <div class="wcn-title">预警中心</div>
        <div class="wcn-sub">总览、待处理、处置、阈值与紧急事件统一入口</div>
      </div>
    </div>
    <div class="wcn-tabs">
      <router-link
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        :class="['wcn-tab', isActive(item) ? 'is-active' : '']"
      >
        <span class="wcn-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const items = [
  { path: '/health-monitor/risk-warning', label: '总览', icon: '📈', matches: ['/health-monitor/risk-warning'] },
  { path: '/alert-management/notifications', label: '待处理', icon: '🔔', matches: ['/alert-management/notifications'] },
  { path: '/alert-management/records', label: '处置记录', icon: '🧾', matches: ['/alert-management/records'] },
  { path: '/alert-management/config', label: '阈值配置', icon: '⚙️', matches: ['/alert-management/config'] },
  { path: '/alert-management/sos', label: '紧急事件', icon: '🚨', matches: ['/alert-management/sos'] }
]

function isActive(item) {
  return item.matches.some(prefix => route.path.startsWith(prefix))
}
</script>

<style scoped lang="scss">
.wcn-wrap {
  margin-bottom: 12px;
}

.wcn-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.wcn-title {
  font-size: 16px;
  font-weight: 700;
  color: #dfefff;
  letter-spacing: 1px;
}

.wcn-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #7c94b2;
}

.wcn-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.wcn-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(0, 212, 255, 0.18);
  background: rgba(10, 22, 40, 0.72);
  color: #8fb5d3;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s ease;
}

.wcn-tab:hover {
  color: #dfefff;
  border-color: rgba(0, 212, 255, 0.38);
  background: rgba(0, 212, 255, 0.08);
}

.wcn-tab.is-active {
  color: #08131f;
  background: linear-gradient(135deg, #00d4ff, #7cf2ff);
  border-color: transparent;
  box-shadow: 0 8px 24px rgba(0, 212, 255, 0.18);
}

.wcn-icon {
  font-size: 14px;
  line-height: 1;
}
</style>
