<template>
  <div class="rt-panel rt-table-panel">
    <div class="rt-ph">
      <div class="rt-ph-left">
        <span class="rt-ph-dot"></span>
        <span class="rt-ph-title">在线用户实时状态</span>
        <span class="rt-badge-online">{{ filteredCount }} 人在线</span>
        <span v-if="hrFilter" class="rt-badge-filter" @click="$emit('clearHrFilter')">
          心率: {{ hrFilter.label }} &times;
        </span>
      </div>
      <div class="rt-ph-right">
        <el-input
          v-model="searchName"
          placeholder="姓名/工号"
          clearable
          class="rt-inp"
          @clear="$emit('search')"
          @keyup.enter="$emit('search')"
        />
        <el-select
          v-model="searchDept"
          placeholder="全部部门"
          clearable
          class="rt-sel"
          @change="$emit('search')"
        >
          <el-option v-for="dept in deptList" :key="dept" :label="dept" :value="dept" />
        </el-select>
        <el-select
          v-model="searchStatus"
          placeholder="全部状态"
          clearable
          class="rt-sel-sm"
          @change="$emit('search')"
        >
          <el-option label="仅正常" value="normal" />
          <el-option label="仅预警" value="warning" />
        </el-select>
        <button class="rt-btn" @click="$emit('search')">
          <el-icon><Search /></el-icon> 查询
        </button>
        <button class="rt-btn rt-btn-g" title="重置" @click="$emit('reset')">
          <el-icon><RefreshLeft /></el-icon>
        </button>
        <button
          v-if="!isMobile"
          class="rt-btn rt-btn-g"
          :title="autoScrollEnabled ? '暂停滚动' : '开启滚动'"
          @click="$emit('toggleAutoScroll')"
        >
          <el-icon><component :is="autoScrollIcon" /></el-icon>
        </button>
      </div>
    </div>

    <div
      class="rt-tbl-wrap"
      @mouseenter="$emit('pauseAutoScroll')"
      @mouseleave="$emit('resumeAutoScroll')"
    >
      <div v-if="isMobile" class="rt-mobile-list">
        <div
          v-for="row in paginatedUserList"
          :key="row.imei || row.userCode || row.userName"
          role="button"
          tabindex="0"
          :class="['rt-mobile-card', row.status === 'normal' ? 'is-normal' : 'is-warning']"
          @click="$emit('showUserDetail', row)"
          @keydown.enter.prevent="$emit('showUserDetail', row)"
          @keydown.space.prevent="$emit('showUserDetail', row)"
        >
          <span class="rt-mobile-card-main">
            <span class="rt-mobile-name">{{ row.userName || '--' }}</span>
            <span class="rt-mobile-meta">{{ row.deptName || '未分组' }} · {{ row.userCode || '无工号' }}</span>
          </span>
          <span class="rt-mobile-vitals">
            <span :class="classifyHeartRate(row.heartRate)">心率 {{ row.heartRate || '--' }}</span>
            <span :class="classifyTemperature(row.temperature)">体温 {{ row.temperature ? `${row.temperature}°` : '--' }}</span>
            <span :class="['rt-status', row.status === 'normal' ? 'st-ok' : 'st-warn']">
              {{ row.status === 'normal' ? '正常' : '预警' }}
            </span>
          </span>
          <span class="rt-mobile-actions" v-if="row.imei">
            <button class="rt-msg-btn" title="文字消息" aria-label="发送文字消息" @click.stop="$emit('sendMessage', row)">
              <el-icon><ChatDotRound /></el-icon>
            </button>
            <button class="rt-msg-btn rt-voice-btn" title="语音广播" aria-label="语音广播" @click.stop="$emit('sendVoice', row)">
              <el-icon><Bell /></el-icon>
            </button>
          </span>
        </div>
      </div>
      <el-table
        v-else
        :data="paginatedUserList"
        v-loading="isLoading"
        element-loading-background="rgba(10,30,61,0.8)"
        element-loading-text="加载中..."
        :height="isMobile ? undefined : '100%'"
        style="width: 100%"
        :header-cell-style="tblHeadStyle"
        :cell-style="tblCellStyle"
        :row-class-name="getRealtimeRowClass"
        @row-click="$emit('showUserDetail', $event)"
      >
        <el-table-column label="#" width="52" align="center">
          <template #default="{ $index }">
            <span class="c-idx">{{ (currentPage - 1) * pageSize + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="姓名" min-width="70" align="center">
          <template #default="{ row }">
            <span class="c-name">{{ row.userName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="55" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.gender === 1 ? '#60a5fa' : '#f472b6' }">
              {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="55" align="center">
          <template #default="{ row }">
            <span class="c-code">{{ row.age != null ? row.age : '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userCode" label="工号" min-width="90" align="center">
          <template #default="{ row }">
            <span class="c-code">{{ row.userCode || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" min-width="80" align="center">
          <template #default="{ row }">
            <span class="c-dept">{{ row.deptName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="heartRate" label="心率" width="65" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="!row.heartRate" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifyHeartRate(row.heartRate)">{{ row.heartRate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bloodOxygen" label="血氧(%)" width="75" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="!row.bloodOxygen" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifyBloodOxygen(row.bloodOxygen)">{{ row.bloodOxygen }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="temperature" label="体温(°C)" width="80" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="!row.temperature" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifyTemperature(row.temperature)">{{ row.temperature }}°</span>
          </template>
        </el-table-column>
        <el-table-column prop="steps" label="步数" width="70" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.steps == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else class="c-steps">{{ row.steps }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="calories" label="卡路里(kcal)" width="105" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.calories == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else class="c-calories">{{ row.calories }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bloodPressureHigh" label="收缩压" width="70" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="!row.bloodPressureHigh" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifySystolic(row.bloodPressureHigh)">{{ row.bloodPressureHigh }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bloodPressureLow" label="舒张压" width="70" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="!row.bloodPressureLow" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifyDiastolic(row.bloodPressureLow)">{{ row.bloodPressureLow }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pressure" label="压力指数" width="80" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.pressure == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
              <span class="c-na">--</span>
            </el-tooltip>
            <span v-else :class="classifyPressure(row.pressure)">{{ row.pressure }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="60" align="center">
          <template #default="{ row }">
            <span :class="['rt-status', row.status === 'normal' ? 'st-ok' : 'st-warn']">
              {{ row.status === 'normal' ? '正常' : '预警' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdate" label="时间" min-width="120" align="center">
          <template #default="{ row }">
            <span class="c-time">{{ formatRealtimeTime(row.lastUpdate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <div v-if="row.imei" class="rt-action-buttons">
              <button class="rt-msg-btn" title="文字消息" aria-label="发送文字消息" @click.stop="$emit('sendMessage', row)">
                <el-icon><ChatDotRound /></el-icon>
              </button>
              <button class="rt-msg-btn rt-voice-btn" title="语音广播" aria-label="语音广播" @click.stop="$emit('sendVoice', row)">
                <el-icon><Bell /></el-icon>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Bell,
  ChatDotRound,
  RefreshLeft,
  Search,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  classifyBloodOxygen,
  classifyDiastolic,
  classifyHeartRate,
  classifyPressure,
  classifySystolic,
  classifyTemperature,
  formatRealtimeTime,
  getRealtimeRowClass
} from '../realtime-helpers'

const props = defineProps({
  filteredCount: { type: Number, default: 0 },
  hrFilter: { type: Object, default: null },
  deptList: { type: Array, default: () => [] },
  searchForm: { type: Object, required: true },
  autoScrollEnabled: { type: Boolean, default: true },
  isMobile: { type: Boolean, default: false },
  paginatedUserList: { type: Array, default: () => [] },
  isLoading: { type: Boolean, default: false },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 50 },
  tblHeadStyle: { type: Object, required: true },
  tblCellStyle: { type: Object, required: true }
})

const emit = defineEmits([
  'update:searchForm',
  'clearHrFilter',
  'search',
  'reset',
  'toggleAutoScroll',
  'pauseAutoScroll',
  'resumeAutoScroll',
  'showUserDetail',
  'sendMessage',
  'sendVoice'
])

function updateSearchForm(patch) {
  emit('update:searchForm', { ...props.searchForm, ...patch })
}

const searchName = computed({
  get: () => props.searchForm.name,
  set: (name) => updateSearchForm({ name })
})

const searchDept = computed({
  get: () => props.searchForm.dept,
  set: (dept) => updateSearchForm({ dept })
})

const searchStatus = computed({
  get: () => props.searchForm.status,
  set: (status) => updateSearchForm({ status })
})

const autoScrollIcon = computed(() => props.autoScrollEnabled ? VideoPause : VideoPlay)
</script>
