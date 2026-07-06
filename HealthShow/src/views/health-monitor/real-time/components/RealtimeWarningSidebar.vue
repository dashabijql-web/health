<template>
  <aside class="rt-aside">
    <div class="rt-aside-head">
      <span class="rt-aside-dot"></span>
      <span class="rt-aside-title">预警人员</span>
      <span class="rt-aside-count">{{ warningUsers.length }}</span>
    </div>

    <div v-if="warningUsers.length" class="rt-aside-list">
      <div
        v-for="user in warningUsers"
        :key="user.imei || user.userCode || user.userName"
        :class="['rt-warn-card', isDanger(user) ? 'rt-warn-card--danger' : '']"
        @click="$emit('showDetail', user)"
      >
        <div class="rt-warn-card-head">
          <span class="rt-warn-card-name">{{ user.userName }}</span>
          <span class="rt-warn-card-dept">{{ user.deptName || '未分组' }}</span>
        </div>
        <div class="rt-warn-card-vitals">
          <span v-if="isAbnormalHr(user.heartRate)" class="rt-vital-tag vt-danger">
            心率 {{ user.heartRate }}
          </span>
          <span v-if="isAbnormalSpo2(user.bloodOxygen)" class="rt-vital-tag vt-danger">
            血氧 {{ user.bloodOxygen }}%
          </span>
          <span v-if="isAbnormalTemp(user.temperature)" class="rt-vital-tag vt-warn">
            体温 {{ user.temperature }}°
          </span>
          <span v-if="isAbnormalBp(user.bloodPressureHigh)" class="rt-vital-tag vt-warn">
            血压 {{ user.bloodPressureHigh }}/{{ user.bloodPressureLow || '--' }}
          </span>
          <span v-if="isAbnormalPressure(user.pressure)" class="rt-vital-tag vt-warn">
            压力 {{ user.pressure }}
          </span>
        </div>
        <div v-if="user.imei" class="rt-warn-card-actions">
          <button class="rt-warn-action rt-warn-action--msg" @click.stop="$emit('sendMessage', user)">
            <el-icon><ChatDotRound /></el-icon> 消息
          </button>
          <button class="rt-warn-action rt-warn-action--voice" @click.stop="$emit('sendVoice', user)">
            <el-icon><Bell /></el-icon> 广播
          </button>
        </div>
      </div>
    </div>

    <div v-else class="rt-aside-empty">
      <div class="rt-aside-empty-icon">&#10003;</div>
      <div class="rt-aside-empty-text">当前全员状态正常</div>
      <div class="rt-aside-empty-sub">15 秒自动刷新</div>
    </div>
  </aside>
</template>

<script setup>
import { Bell, ChatDotRound } from '@element-plus/icons-vue'

defineProps({
  warningUsers: { type: Array, default: () => [] }
})

defineEmits(['showDetail', 'sendMessage', 'sendVoice'])

function isDanger(user) {
  return (user.heartRate && (user.heartRate < 45 || user.heartRate > 130))
    || (user.bloodOxygen && user.bloodOxygen < 88)
    || (user.temperature && (user.temperature < 34.5 || user.temperature > 39))
    || (user.bloodPressureHigh && user.bloodPressureHigh >= 180)
    || (user.pressure && user.pressure >= 90)
}

function isAbnormalHr(v) {
  return v && (v < 50 || v > 120)
}

function isAbnormalSpo2(v) {
  return v && v < 95
}

function isAbnormalTemp(v) {
  return v && (v < 35 || v > 37.5)
}

function isAbnormalBp(v) {
  return v && (v >= 140 || v < 90)
}

function isAbnormalPressure(v) {
  return v != null && v >= 70
}
</script>
