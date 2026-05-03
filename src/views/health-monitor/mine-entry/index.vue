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
      <button class="me-export-btn" @click="exportList" style="margin-left:auto">⬇ 导出名单</button>
    </div>

    <!-- Table -->
    <div class="me-table-wrap" v-loading="loading">
      <div v-if="!loading && filteredList.length === 0" class="me-empty">
        <el-icon size="50" color="#2d3561"><UserFilled /></el-icon>
        <p>今日暂无检测数据</p>
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
                  <span class="me-vital-icon">♥</span>{{ item.heartRate ?? '--' }}bpm
                </span>
                <span class="me-vital" :class="vClass(item.bloodOxygen, 95, 100, false)">
                  <span class="me-vital-icon">💨</span>{{ item.bloodOxygen ?? '--' }}%
                </span>
                <span class="me-vital" :class="bpClass(item.systolic, item.diastolic)">
                  <span class="me-vital-icon">🫀</span>{{ item.systolic ?? '--' }}/{{ item.diastolic ?? '--' }}mmHg
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, CircleCheck, CircleClose, UserFilled } from '@element-plus/icons-vue'
import { getMineEntryList, getPreShiftCompliance } from '@/api/health'
import dayjs from 'dayjs'
import { exportToExcel } from '@/utils/export-excel'

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
  const r = summary.value.preShiftRate
  if (r === null) return ''
  return r >= 90 ? 'me-stat-rate-ok' : r >= 70 ? 'me-stat-rate-warn' : 'me-stat-rate-bad'
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
  if (val === null || val === undefined) return ''
  if (isHeartRate) return (val < min || val > max) ? 'vital-bad' : 'vital-ok'
  return val < min ? 'vital-bad' : 'vital-ok'
}
function bpClass(sys, dia) {
  if ((sys !== null && sys >= 140) || (dia !== null && dia >= 90)) return 'vital-bad'
  return 'vital-ok'
}
function failReasons(item) {
  const reasons = []
  if (item.heartRate !== null && (item.heartRate < 60 || item.heartRate > 100)) reasons.push(`心率${item.heartRate}bpm`)
  if (item.bloodOxygen !== null && item.bloodOxygen < 95) reasons.push(`血氧${item.bloodOxygen}%`)
  if (item.systolic !== null && item.systolic >= 140) reasons.push(`高压${item.systolic}`)
  if (item.diastolic !== null && item.diastolic >= 90) reasons.push(`低压${item.diastolic}`)
  return reasons
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

let timer = null
onMounted(() => {
  load()
  timer = setInterval(load, 60000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.me-root {
  min-height: 100%;
  background: #060d1f;
  color: #c0d4e8;
  padding: 16px 20px;
  font-size: 13px;
}

/* ── Header ── */
.me-hd {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.me-hd-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.me-live-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #00ff88;
  box-shadow: 0 0 8px #00ff88;
  animation: pulse 1.5s infinite;
  flex-shrink: 0;
}
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.me-hd-title { margin: 0; font-size: 18px; font-weight: 700; color: #e8f4ff; }
.me-hd-sub { font-size: 12px; color: #5ea4c8; white-space: nowrap; }
.me-profile-btn {
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0,180,255,0.28);
  background: rgba(0,180,255,0.08);
  color: #b7e9ff;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s ease;
}
.me-profile-btn:hover { background: rgba(0,180,255,0.16); }

.me-hd-stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.me-stat-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(0,180,255,0.2);
  border-radius: 8px;
  padding: 8px 16px;
  text-align: center;
  min-width: 80px;
}
.me-stat-val { font-size: 22px; font-weight: 700; color: #00d4ff; }
.me-stat-label { font-size: 11px; color: #5ea4c8; margin-top: 2px; }
.me-stat-pass .me-stat-val { color: #38ef7d; }
.me-stat-fail .me-stat-val { color: #ff5252; }
.me-stat-rate-ok .me-stat-val { color: #38ef7d; }
.me-stat-rate-warn .me-stat-val { color: #ffd200; }
.me-stat-rate-bad .me-stat-val { color: #ff5252; }

.me-hd-right {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 4px;
}

/* ── Criteria ── */
.me-criteria {
  background: rgba(0,212,255,0.06);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 6px;
  padding: 8px 16px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
}
.me-criteria-label { color: #5ea4c8; font-weight: 600; }
.me-criteria-item { color: #38ef7d; }
.me-criteria-sep { color: #2d4060; }
.me-criteria-note { color: #5ea4c8; }
.me-export-btn {
  padding: 4px 12px; background: rgba(0,212,255,.08); border: 1px solid rgba(0,212,255,.25);
  border-radius: 4px; color: #00d4ff; font-size: 11px; cursor: pointer; white-space: nowrap; transition: background .2s;
  &:hover { background: rgba(0,212,255,.18); }
}

/* ── Table wrap ── */
.me-table-wrap { min-height: 300px; }
.me-empty { text-align: center; padding: 60px 0; color: #4a6080; }
.me-empty p { margin-top: 12px; font-size: 13px; }

/* ── Group headers ── */
.me-group-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px 6px 0 0;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 0;
}
.fail-hd { background: rgba(255,82,82,0.12); color: #ff5252; border-bottom: 1px solid rgba(255,82,82,0.3); }
.pass-hd { background: rgba(56,239,125,0.08); color: #38ef7d; border-bottom: 1px solid rgba(56,239,125,0.2); margin-top: 20px; }
.me-fail-dot { width:8px;height:8px;border-radius:50%;background:#ff5252;box-shadow:0 0 6px #ff5252; }
.me-pass-dot { width:8px;height:8px;border-radius:50%;background:#38ef7d;box-shadow:0 0 6px #38ef7d; }
.me-group-tip { font-size:11px;opacity:0.7;font-weight:400; }

/* ── Fail cards ── */
.me-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  background: rgba(255,82,82,0.04);
  border: 1px solid rgba(255,82,82,0.15);
  border-top: none;
  border-radius: 0 0 6px 6px;
}
.me-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: rgba(255,82,82,0.08);
  border: 1px solid rgba(255,82,82,0.25);
  border-radius: 8px;
  padding: 12px 14px;
  width: calc(33.33% - 8px);
  min-width: 280px;
}
@media (max-width: 1200px) { .me-card { width: calc(50% - 6px); } }
@media (max-width: 800px)  { .me-card { width: 100%; } }

.me-card-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700;
  flex-shrink: 0;
}
.fail-avatar { background: rgba(255,82,82,0.2); color: #ff8080; border: 1px solid rgba(255,82,82,0.4); }
.me-card-body { flex: 1; min-width: 0; }
.me-card-name { font-size: 15px; font-weight: 600; color: #e0d0d0; }
.me-card-dept { font-size: 11px; color: #8ba6c8; margin: 2px 0 6px; }
.me-vitals-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.me-vital {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255,255,255,0.05);
  display: flex; align-items: center; gap: 4px;
}
.me-vital-icon { font-size: 12px; }
.vital-bad { color: #ff5252; background: rgba(255,82,82,0.12); border: 1px solid rgba(255,82,82,0.3); }
.vital-ok  { color: #38ef7d; background: rgba(56,239,125,0.08); }

.me-fail-reasons { display: flex; flex-wrap: wrap; gap: 4px; }
.me-reason-tag {
  font-size: 11px;
  background: rgba(255,82,82,0.15);
  color: #ff7070;
  border: 1px solid rgba(255,82,82,0.4);
  border-radius: 4px;
  padding: 1px 7px;
}
.me-card-status {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; flex-shrink: 0;
}
.fail-status { color: #ff5252; font-size: 11px; }

/* ── Pass table cell ── */
.me-td-val { font-family: Consolas, monospace; }
.me-td-val.vital-ok { color: #38ef7d; }
.me-td-val.vital-bad { color: #ff5252; font-weight: 600; }

/* ── Pagination ── */
.me-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 10px 0 4px;
}
:deep(.el-pagination) {
  --el-pagination-bg-color: #0d1830;
  --el-pagination-text-color: #5ea4c8;
  --el-pagination-button-color: #5ea4c8;
}
:deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #00d4ff;
  color: #000;
}

/* ── Footer ── */
.me-footer {
  text-align: right;
  font-size: 11px;
  color: #3a5070;
  margin-top: 16px;
  padding-top: 8px;
  border-top: 1px solid rgba(0,180,255,0.08);
}
</style>
