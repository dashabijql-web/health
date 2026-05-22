<template>
  <div class="me-root">
    <!-- Header -->
    <div class="me-hd">
      <div class="me-hd-left">
        <span class="me-live-dot"></span>
        <h1 class="me-hd-title">入井健康准入系统</h1>
        <span class="me-hd-sub">今日 {{ currentDate }} 班前健康筛查</span>
        <button v-if="route.query.empCode" class="me-profile-btn" @click="backToProfile">
          返回画像
        </button>
      </div>
      <div class="me-hd-stats">
        <div class="me-stat-card me-stat-total">
          <div class="me-stat-val">{{ summary.totalToday }}</div>
          <div class="me-stat-label">今日检测</div>
        </div>
        <div class="me-stat-card me-stat-pass">
          <div class="me-stat-val">{{ summary.qualifiedCount }}</div>
          <div class="me-stat-label">准入通过</div>
        </div>
        <div class="me-stat-card me-stat-fail">
          <div class="me-stat-val">{{ summary.failedCount }}</div>
          <div class="me-stat-label">禁止入井</div>
        </div>
        <div class="me-stat-card me-stat-rate" :class="rateClass">
          <div class="me-stat-val">{{ summary.preShiftRate !== null ? summary.preShiftRate + '%' : '--' }}</div>
          <div class="me-stat-label">班前达标率</div>
        </div>
      </div>
      <div class="me-hd-right">
        <el-input
          v-model="searchText"
          placeholder="搜索姓名/部门"
          size="small"
          clearable
          style="width:200px"
          prefix-icon="Search"
        />
        <el-select v-model="filterDept" placeholder="全部部门" size="small" clearable style="width:140px;margin-left:8px">
          <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" size="small" style="width:120px;margin-left:8px">
          <el-option label="全部" value="" />
          <el-option label="准入" value="pass" />
          <el-option label="禁入" value="fail" />
        </el-select>
        <el-button size="small" type="primary" :loading="loading" @click="load" style="margin-left:8px">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- Criteria hint -->
    <div class="me-criteria">
      <span class="me-criteria-label">准入标准：</span>
      <span class="me-criteria-item ok">心率 60~100 bpm</span>
      <span class="me-criteria-sep">|</span>
      <span class="me-criteria-item ok">血氧 ≥ 95%</span>
      <span class="me-criteria-sep">|</span>
      <span class="me-criteria-item ok">血压高压 &lt; 140 mmHg</span>
      <span class="me-criteria-sep">|</span>
      <span class="me-criteria-item ok">血压低压 &lt; 90 mmHg</span>
      <span class="me-criteria-sep">|</span>
      <span class="me-criteria-item ok">体温 36.0~37.5 ℃</span>
      <span class="me-criteria-note">（任一超标即禁止入井）</span>
      <button class="me-export-btn" @click="exportList" style="margin-left:auto">导出名单</button>
    </div>

    <!-- Table -->
    <div class="me-table-wrap" v-loading="loading">
      <div v-if="!loading && filteredList.length === 0" class="me-empty">
        <PageEmptyState
          eyebrow="Access Queue"
          title="今日暂无检测数据"
          description="可以等待下一轮班前检测，或切换筛选条件查看其它状态记录。"
        />
      </div>

      <!-- 禁入人员 -->
      <div v-if="failList.length && (filterStatus === '' || filterStatus === 'fail')">
        <div class="me-group-hd fail-hd">
          <span class="me-fail-dot"></span>
          <span>禁止入井（{{ failList.length }} 人）</span>
          <span class="me-group-tip">以下人员存在健康异常，禁止下井作业</span>
        </div>
        <div class="me-cards fail-section">
          <div
            v-for="item in failList"
            :key="item.empCode"
            class="me-card me-card-fail"
            style="cursor:pointer"
            @click="goPortrait(item)"
          >
            <div class="me-card-avatar fail-avatar">{{ (item.empName || '?').charAt(0) }}</div>
            <div class="me-card-body">
              <div class="me-card-name">{{ item.empName }}</div>
              <div class="me-card-dept">{{ item.deptName }} · {{ item.jobTypeName }}</div>
              <div class="me-vitals-row">
                <span class="me-vital" :class="vClass(item.heartRate, 60, 100, true)">
                  <span class="me-vital-icon">HR</span>{{ item.heartRate ?? '--' }}bpm
                </span>
                <span class="me-vital" :class="vClass(item.bloodOxygen, 95, 100, false)">
                  <span class="me-vital-icon">SpO2</span>{{ item.bloodOxygen ?? '--' }}%
                </span>
                <span class="me-vital" :class="bpClass(item.systolic, item.diastolic)">
                  <span class="me-vital-icon">BP</span>{{ item.systolic ?? '--' }}/{{ item.diastolic ?? '--' }}mmHg
                </span>
              </div>
              <div class="me-fail-reasons">
                <span v-for="r in failReasons(item)" :key="r" class="me-reason-tag">{{ r }}</span>
              </div>
            </div>
            <div class="me-card-status fail-status">
              <el-icon :size="20"><CircleClose /></el-icon>
              <span>禁止入井</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 准入人员 -->
      <div v-if="passList.length && (filterStatus === '' || filterStatus === 'pass')">
        <div class="me-group-hd pass-hd">
          <span class="me-pass-dot"></span>
          <span>准入通过（{{ passList.length }} 人）</span>
          <span class="me-group-tip">以下人员体征正常，允许入井作业</span>
        </div>
        <el-table
          :data="paginatedPassList"
          style="width:100%;table-layout:fixed;cursor:pointer"
          @row-click="goPortrait"
          :header-cell-style="{ background:'#0d1830', color:'#5ea4c8', fontWeight:'600', fontSize:'12px' }"
          :row-style="{ background:'#0a1225', color:'#c0d4e8' }"
          :cell-style="{ padding:'7px 0', fontSize:'12px', color:'#c0d4e8' }"
          border
        >
          <el-table-column type="index" :index="(i) => (passPage-1)*passPageSize + i + 1" width="50" label="#" align="center" />
          <el-table-column label="姓名" prop="empName" min-width="80" />
          <el-table-column label="部门" prop="deptName" min-width="110" />
          <el-table-column label="工种" prop="jobTypeName" min-width="90" />
          <el-table-column label="心率" min-width="90" align="center">
            <template #default="{ row }">
              <span :class="['me-td-val', vClass(row.heartRate, 60, 100, true)]">{{ row.heartRate ?? '--' }} bpm</span>
            </template>
          </el-table-column>
          <el-table-column label="血氧" min-width="75" align="center">
            <template #default="{ row }">
              <span :class="['me-td-val', vClass(row.bloodOxygen, 95, 100, false)]">{{ row.bloodOxygen ?? '--' }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="血压" min-width="100" align="center">
            <template #default="{ row }">
              <span :class="['me-td-val', bpClass(row.systolic, row.diastolic)]">{{ row.systolic ?? '--' }}/{{ row.diastolic ?? '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="检测时间" min-width="100" align="center">
            <template #default="{ row }">
              <span style="font-size:11px;color:#5ea4c8">{{ fmtTime(row.recordTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="80" align="center">
            <template #default>
              <el-tag type="success" size="small" effect="dark">
                <el-icon><CircleCheck /></el-icon> 准入
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="me-pagination">
          <el-pagination
            v-model:current-page="passPage"
            :page-size="passPageSize"
            :total="passList.length"
            layout="total, prev, pager, next, jumper"
            background
            size="small"
          />
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="me-footer">
      <span>更新时间：{{ lastRefreshTime }}</span>
      <span style="margin-left:20px">数据每60秒自动刷新</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getMineEntryList, getPreShiftCompliance } from '@/api/health'
import dayjs from 'dayjs'
import { exportToExcel } from '@/utils/export-excel'
import { useIntervalTask } from '@/composables/useIntervalTask'
import PageEmptyState from '@/components/health-shell/PageEmptyState.vue'
import {
  bloodPressureClass,
  mineEntryFailReasons,
  mineEntryRateClass,
  vitalClass
} from './mine-entry-view-model.js'

const router = useRouter()
const route = useRoute()
function goPortrait(item) {
  if (!item?.empCode) return
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode: item.empCode || '',
      empName: item.empName || '',
      deptName: item.deptName || '',
      jobTypeName: item.jobTypeName || ''
    }
  })
}

function backToProfile() {
  if (!route.query.empCode) return
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode: route.query.empCode,
      empName: route.query.empName || ''
    }
  })
}

const loading = ref(false)
const searchText = ref('')
const filterDept = ref('')
const filterStatus = ref('')
const passPage = ref(1)
const passPageSize = 50
const currentDate = ref(dayjs().format('YYYY年MM月DD日'))
const lastRefreshTime = ref('')

const entryList = ref([])
const summary = ref({ totalToday: 0, qualifiedCount: 0, failedCount: 0, preShiftRate: null })

const rateClass = computed(() => {
  return mineEntryRateClass(summary.value.preShiftRate)
})

const deptOptions = computed(() => {
  const s = new Set(entryList.value.map(e => e.deptName).filter(Boolean))
  return [...s].sort()
})

const filteredList = computed(() => {
  passPage.value = 1
  return entryList.value.filter(item => {
    if (searchText.value && !item.empName?.includes(searchText.value) && !item.deptName?.includes(searchText.value)) return false
    if (filterDept.value && item.deptName !== filterDept.value) return false
    if (filterStatus.value === 'pass' && !item.qualified) return false
    if (filterStatus.value === 'fail' && item.qualified) return false
    return true
  })
})

const failList = computed(() => filteredList.value.filter(e => !e.qualified))
const passList = computed(() => filteredList.value.filter(e => e.qualified))
const paginatedPassList = computed(() => {
  const s = (passPage.value - 1) * passPageSize
  return passList.value.slice(s, s + passPageSize)
})

function vClass(val, min, max, isHeartRate) {
  return vitalClass(val, min, max, isHeartRate)
}
function bpClass(sys, dia) {
  return bloodPressureClass(sys, dia)
}
function failReasons(item) {
  return mineEntryFailReasons(item)
}
function fmtTime(t) {
  if (!t) return '--'
  return dayjs(t).format('HH:mm:ss')
}

async function load() {
  loading.value = true
  try {
    // 手机端请求 200 条（减少 WiFi 传输量），桌面端请求全量
    const isMobile = window.innerWidth < 992
    const fetchSize = isMobile ? 200 : 1000
    const [listRes, statsRes] = await Promise.allSettled([
      getMineEntryList(fetchSize),
      getPreShiftCompliance()
    ])
    if (listRes.status === 'fulfilled' && listRes.value.code === 200) {
      entryList.value = listRes.value.data || []
    } else if (listRes.status === 'rejected') {
      // 手机端网络较慢时列表可能加载失败，静默处理（统计数据仍正常显示）
      console.warn('[MineEntry] list load failed:', listRes.reason?.message)
    }
    if (statsRes.status === 'fulfilled' && statsRes.value.code === 200) {
      summary.value = statsRes.value.data || summary.value
    }
    lastRefreshTime.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function exportList() {
  const cols = [
    { label: '序号', key: '_idx' },
    { label: '姓名', key: 'empName' },
    { label: '工号', key: 'empCode' },
    { label: '部门', key: 'deptName' },
    { label: '工种', key: 'jobTypeName' },
    { label: '心率(bpm)', key: 'heartRate' },
    { label: '血氧(%)', key: 'bloodOxygen' },
    { label: '收缩压(mmHg)', key: 'systolic' },
    { label: '舒张压(mmHg)', key: 'diastolic' },
    { label: '状态', key: '_status' },
    { label: '检测时间', key: '_time' },
  ]
  const data = filteredList.value.map((row, i) => ({
    ...row,
    _idx: i + 1,
    _status: row.qualified ? '准入' : '禁止入井',
    _time: row.recordTime ? dayjs(row.recordTime).format('HH:mm:ss') : '--'
  }))
  exportToExcel(data, cols, `班前健康检查_${dayjs().format('YYYYMMDD')}`)
}

const { start: startMineEntryRefresh } = useIntervalTask(load, 60000)
onMounted(() => {
  load()
  startMineEntryRefresh()
})
</script>

<style scoped lang="scss">
@import './mine-entry.scss';
</style>
