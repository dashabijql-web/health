<template>
  <nav class="mobile-bottom-nav" v-if="isMobile">
    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      :class="['mbn-item', isActive(item) ? 'mbn-active' : '']"
    >
      <span class="mbn-icon">{{ item.icon }}</span>
      <span class="mbn-label">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'MobileBottomNav',
  computed: {
    ...mapState({ device: s => s.app.device }),
    isMobile() { return this.device === 'mobile' },
    navItems() {
      return [
        {
          path: '/health-monitor/dashboard',
          icon: '📊',
          label: '指挥',
          matches: ['/health-monitor/dashboard', '/safety-command']
        },
        {
          path: '/health-monitor/real-time',
          icon: '❤️',
          label: '监测',
          matches: [
            '/health-monitor/real-time',
            '/health-monitor/heart-rate',
            '/health-monitor/pressure',
            '/health-monitor/blood-pressure',
            '/health-monitor/blood-oxygen',
            '/health-monitor/trend-warning'
          ]
        },
        {
          path: '/health-monitor/mine-entry',
          icon: '🪪',
          label: '准入',
          matches: ['/health-monitor/mine-entry']
        },
        {
          path: '/alert-management/notifications',
          icon: '🔔',
          label: '预警',
          matches: ['/alert-management', '/health-monitor/risk-warning']
        },
        {
          path: '/health-monitor/employee-archive',
          icon: '👤',
          label: '人员',
          matches: [
            '/health-monitor/employee-archive',
            '/health-monitor/employee-profile',
            '/health-monitor/health-portrait',
            '/health-monitor/workbench'
          ]
        },
      ]
    },
  },
  methods: {
    isActive(item) {
      const matchers = item.matches || [item.path]
      return matchers.some(prefix => this.$route.path.startsWith(prefix))
    }
  }
}
</script>

<style scoped>
.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  justify-content: space-around;
  align-items: stretch;
  background: #080f1e;
  border-top: 1px solid rgba(0,212,255,0.15);
  padding-bottom: env(safe-area-inset-bottom, 8px);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.5);
}
.mbn-item {
  flex: 1 1 0;
  width: 0; /* 让 flex-grow 均分起作用 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 2px 8px;
  gap: 3px;
  text-decoration: none;
  color: #4b5563;
  transition: color 0.2s;
  -webkit-tap-highlight-color: transparent;
  position: relative;
  min-height: 54px;
  &:active { background: rgba(255,255,255,0.04); }
}
/* active 状态：顶部蓝色指示线 + 亮色文字 */
.mbn-active {
  color: #00d4ff !important;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 20%;
    right: 20%;
    height: 2px;
    background: #00d4ff;
    border-radius: 0 0 2px 2px;
  }
}
.mbn-icon {
  font-size: 20px;
  line-height: 1;
}
.mbn-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3px;
}
</style>
