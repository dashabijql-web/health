<template>
  <div class="ea-page">
    <!-- 顶部标题栏 -->
    <div class="ea-header">
      <button class="ea-back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <div class="ea-title">
        <el-icon class="ea-title-icon"><UserFilled /></el-icon>
        职工健康档案库
      </div>
      <div class="ea-header-right">
        <span class="ea-total">共 <b>{{ filtered.length }}</b> 名员工</span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="ea-search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入姓名或工号搜索"
        class="ea-input"
        clearable
        @clear="keyword = ''"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select
        v-model="selectedDept"
        placeholder="请选择部门"
        class="ea-dept-select"
        clearable
      >
        <el-option
          v-for="d in deptList"
          :key="d"
          :label="d"
          :value="d"
        />
      </el-select>
      <button class="ea-reset-btn" @click="keyword = ''; selectedDept = ''">重置</button>
    </div>

    <!-- 员工卡片网格 -->
    <div v-loading="loading" class="ea-grid">
      <div
        v-for="emp in paginated"
        :key="emp.empCode"
        class="ea-card"
      >
        <!-- 卡片顶部装饰线 -->
        <div class="ea-card-top-bar"></div>

        <div class="ea-card-body">
          <!-- 基本信息 -->
          <div class="ea-info">
            <div class="ea-name-row">
              <span class="ea-name">{{ emp.empName }}</span>
              <span :class="['ea-gender', emp.gender === 2 ? 'f' : 'm']">
                {{ emp.gender === 2 ? '女' : '男' }}
              </span>
            </div>
            <div class="ea-fields">
              <div class="ea-field"><span class="ef-label">部门</span><span class="ef-val">{{ emp.deptName || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">岗位</span><span class="ef-val">{{ emp.jobTypeName || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">工号</span><span class="ef-val code">{{ emp.empCode || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">年龄</span><span class="ef-val">{{ calcAge(emp.birthDate) }}</span></div>
              <div class="ea-field full"><span class="ef-label">手机</span><span class="ef-val">{{ emp.phone || '--' }}</span></div>
            </div>
          </div>
        </div>

        <!-- 健康状态条 -->
        <div class="ea-health-bar">
          <div class="ea-hb-label">健康状态</div>
          <div class="ea-hb-track">
            <div class="ea-hb-fill" :style="{ width: (emp._healthScore || 72) + '%', background: healthBarColor(emp._healthScore || 72) }"></div>
          </div>
          <div class="ea-hb-score" :style="{ color: healthBarColor(emp._healthScore || 72) }">{{ emp._healthScore || 72 }}</div>
        </div>

        <!-- 职工健康档案按钮 -->
        <button class="ea-detail-btn" @click="openDetail(emp)">
          <el-icon><DataAnalysis /></el-icon>
          职工健康档案
        </button>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && filtered.length === 0" class="ea-empty">
        <el-icon size="48" color="#4a6080"><Search /></el-icon>
        <p>未找到符合条件的员工</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="ea-pagination" v-if="filtered.length > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filtered.length"
        layout="prev, pager, next, total"
        background
        class="ea-pager"
      />
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, UserFilled, Search, DataAnalysis } from '@element-plus/icons-vue'
import { getEmployeeListDetail } from '@/api/employee'
import { getRealtimeOverview } from '@/api/realtime'

const router = useRouter()
const loading = ref(false)
const employees = ref([])
const keyword = ref('')
const selectedDept = ref('')
const currentPage = ref(1)
const pageSize = 20
const onlineSet = ref(new Set())

// 部门列表（从员工数据动态提取）
const deptList = computed(() => {
  const set = new Set()
  employees.value.forEach(e => { if (e.deptName) set.add(e.deptName) })
  return [...set].sort()
})

// 过滤后员工列表
const filtered = computed(() => {
  let list = employees.value
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(e =>
      (e.empName || '').toLowerCase().includes(kw) ||
      (e.empCode || '').toLowerCase().includes(kw)
    )
  }
  if (selectedDept.value) {
    list = list.filter(e => e.deptName === selectedDept.value)
  }
  return list
})

// 当前页数据
const paginated = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// 搜索条件变化时重置页码
watch([keyword, selectedDept], () => { currentPage.value = 1 })

function calcAge(birthDate) {
  if (!birthDate) return '--'
  const birth = new Date(birthDate)
  const now = new Date()
  const age = now.getFullYear() - birth.getFullYear()
  return age > 0 && age < 100 ? age + '岁' : '--'
}

function healthBarColor(score) {
  if (score >= 80) return '#00e676'
  if (score >= 60) return '#ffaa00'
  return '#ff3b3b'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getEmployeeListDetail()
    if (res.code === 200) {
      employees.value = (res.data || []).map(e => ({
        ...e,
        _healthScore: e.healthScore || 0
      }))
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

async function loadOnlineStatus() {
  try {
    const res = await getRealtimeOverview()
    if (res.code === 200 && Array.isArray(res.data)) {
      onlineSet.value = new Set(res.data.map(r => r.userCode || r.empCode))
    }
  } catch (e) { /* ignore */ }
}

function openDetail(emp) {
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode:     emp.empCode     || '',
      empName:     emp.empName     || '',
      gender:      emp.gender      ?? 1,
      birthDate:   emp.birthDate   || '',
      deptName:    emp.deptName    || '',
      jobTypeName: emp.jobTypeName || '',
      phone:       emp.phone       || '',
    }
  })
}

onMounted(() => {
  loadData()
  loadOnlineStatus()
})
</script>

<style scoped>
/* ═══════════════════════════════════ 整体布局 ═══════════════════════════════════ */
.ea-page {
  min-height: 100vh;
  background: #0b0f1e;
  color: #c8d8f0;
  display: flex;
  flex-direction: column;
  padding: 0 0 24px;
}

/* ═══════════════════════════════════ 头部 ═══════════════════════════════════ */
.ea-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 28px 14px;
  border-bottom: 1px solid #1a2545;
  background: linear-gradient(180deg, #0f1628 0%, #0b0f1e 100%);
}

.ea-back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 180, 255, 0.1);
  border: 1px solid rgba(0, 180, 255, 0.3);
  color: #00b4ff;
  padding: 7px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.ea-back-btn:hover { background: rgba(0, 180, 255, 0.2); }

.ea-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #e8f4ff;
  letter-spacing: 2px;
  flex: 1;
  justify-content: center;
  text-shadow: 0 0 20px rgba(0, 180, 255, 0.4);
}
.ea-title-icon { color: #00b4ff; font-size: 24px; }

.ea-header-right { min-width: 100px; text-align: right; }
.ea-total { font-size: 13px; color: #7a9abf; }
.ea-total b { color: #00d4ff; font-size: 15px; }

/* ═══════════════════════════════════ 搜索栏 ═══════════════════════════════════ */
.ea-search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 28px;
  background: rgba(15, 22, 40, 0.8);
  border-bottom: 1px solid #1a2545;
}

.ea-input { width: 300px; }
.ea-dept-select { width: 200px; }

:deep(.ea-input .el-input__wrapper),
:deep(.ea-dept-select .el-input__wrapper) {
  background: rgba(10, 20, 40, 0.8) !important;
  border: 1px solid #1e3060 !important;
  box-shadow: none !important;
  border-radius: 6px;
}
:deep(.ea-input .el-input__inner),
:deep(.ea-dept-select .el-input__inner) {
  color: #c8d8f0 !important;
  background: transparent !important;
}
:deep(.el-select-dropdown) {
  background: #0f1a35 !important;
  border: 1px solid #1e3060 !important;
}
:deep(.el-select-dropdown__item) { color: #a0b8d8 !important; }
:deep(.el-select-dropdown__item:hover) { background: #1a2c50 !important; }

.ea-reset-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid #2a3a5a;
  color: #7a9abf;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.ea-reset-btn:hover { background: rgba(255, 255, 255, 0.1); color: #c8d8f0; }

/* ═══════════════════════════════════ 卡片网格 ═══════════════════════════════════ */
.ea-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  padding: 20px 28px;
  flex: 1;
}

@media (max-width: 1400px) { .ea-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1000px) { .ea-grid { grid-template-columns: repeat(2, 1fr); } }

/* ═══════════════════════════════════ 单个卡片 ═══════════════════════════════════ */
.ea-card {
  background: linear-gradient(145deg, #0f1a35 0%, #0d1528 100%);
  border: 1px solid #1a2d50;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.ea-card:hover {
  transform: translateY(-3px);
  border-color: #00b4ff55;
  box-shadow: 0 8px 24px rgba(0, 100, 255, 0.15);
}

.ea-card-top-bar {
  height: 3px;
  background: linear-gradient(90deg, #00b4ff, #0050ff, transparent);
}

.ea-card-body {
  display: flex;
  gap: 14px;
  padding: 14px 14px 10px;
}

/* 信息区 */
.ea-info { flex: 1; overflow: hidden; }
.ea-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.ea-name { font-size: 16px; font-weight: 700; color: #e8f4ff; }
.ea-gender {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 600;
}
.ea-gender.m { background: rgba(0, 150, 255, 0.2); color: #4ea8ff; border: 1px solid #0064ff44; }
.ea-gender.f { background: rgba(255, 60, 130, 0.15); color: #ff7eb0; border: 1px solid #ff3c8244; }

.ea-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 3px 8px; }
.ea-field { display: flex; align-items: center; gap: 5px; }
.ea-field.full { grid-column: span 2; }
.ef-label { font-size: 11px; color: #4a7090; min-width: 26px; }
.ef-val { font-size: 12px; color: #9ab8d8; }
.ef-val.code { font-family: monospace; color: #00c8ff; font-size: 11px; }

/* 健康状态条 */
.ea-health-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 8px;
}
.ea-hb-label { font-size: 11px; color: #4a7090; min-width: 44px; }
.ea-hb-track { flex: 1; height: 4px; background: #1a2a45; border-radius: 2px; overflow: hidden; }
.ea-hb-fill { height: 100%; border-radius: 2px; transition: width 0.5s; }
.ea-hb-score { font-size: 12px; font-weight: 700; min-width: 24px; text-align: right; }

/* 详情按钮 */
.ea-detail-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0 14px 14px;
  padding: 7px 0;
  background: linear-gradient(90deg, rgba(0, 100, 255, 0.15), rgba(0, 180, 255, 0.1));
  border: 1px solid rgba(0, 150, 255, 0.3);
  border-radius: 6px;
  color: #00c8ff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  width: calc(100% - 28px);
}
.ea-detail-btn:hover {
  background: linear-gradient(90deg, rgba(0, 100, 255, 0.3), rgba(0, 180, 255, 0.2));
  border-color: #00b4ff;
  box-shadow: 0 0 12px rgba(0, 150, 255, 0.2);
}

/* ═══════════════════════════════════ 空状态 ═══════════════════════════════════ */
.ea-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #4a6080;
  gap: 12px;
  font-size: 14px;
}

/* ═══════════════════════════════════ 分页 ═══════════════════════════════════ */
.ea-pagination {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}
:deep(.ea-pager .el-pagination__total),
:deep(.ea-pager .el-pager li),
:deep(.ea-pager button) {
  background: rgba(15, 26, 53, 0.8) !important;
  color: #6a88b0 !important;
  border: 1px solid #1a2d50 !important;
}
:deep(.ea-pager .el-pager li.is-active) {
  background: rgba(0, 100, 255, 0.3) !important;
  color: #00c8ff !important;
  border-color: #0064ff55 !important;
}
</style>
