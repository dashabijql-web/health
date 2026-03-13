<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><Bell /></el-icon>
        <div>
          <h1 class="main-title">告警记录</h1>
          <p class="sub-title">查看和处理所有健康告警事件</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card stat-card-clickable" @click="filterByCard('all')">
          <div class="stat-icon-wrap primary"><el-icon size="26"><Bell /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ overview.todayTotal || 0 }}</div><div class="stat-label">今日告警</div></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-clickable" @click="filterByCard('unhandled')">
          <div class="stat-icon-wrap danger"><el-icon size="26"><WarningFilled /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ overview.pending || 0 }}</div><div class="stat-label">待处理</div></div>
          <div class="stat-badge" v-if="overview.pending > 0">urgent</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-clickable" @click="filterByCard('critical')">
          <div class="stat-icon-wrap warning"><el-icon size="26"><WarnTriangleFilled /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ overview.critical || 0 }}</div><div class="stat-label">危急告警</div></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-clickable" @click="filterByCard('handled')">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ handleRate }}<span class="unit">%</span></div><div class="stat-label">处理率</div></div>
        </div>
      </el-col>
    </el-row>

    <!-- Search Panel -->
    <div class="panel mb-16">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item><el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" /></el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.warningType" placeholder="告警类型" clearable style="width:140px">
            <el-option label="SOS求助" value="SOS" /><el-option label="跌倒" value="fall" /><el-option label="心率异常" value="heartRate" />
            <el-option label="血氧异常" value="bloodOxygen" /><el-option label="体温异常" value="temperature" /><el-option label="静态报警" value="staticAlert" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.warningLevel" placeholder="告警级别" clearable style="width:120px">
            <el-option label="危急" value="critical" /><el-option label="预警" value="warning" /><el-option label="提示" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.handleStatus" placeholder="处理状态" clearable style="width:120px">
            <el-option label="全部" value="" /><el-option label="已处理" value="handled" /><el-option label="未处理" value="unhandled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索姓名/设备号" clearable style="width:200px" @keyup.enter="handleSearch"><template #prefix><el-icon><Search /></el-icon></template></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Data Table -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>告警列表</div>
        <div class="panel-header-right">
          <el-button
            v-if="selectedRows.length > 0"
            type="warning"
            size="small"
            @click="batchHandle"
          >批量处理 ({{ selectedRows.length }})</el-button>
          <span class="total-badge">共 {{ pagination.total }} 条</span>
        </div>
      </div>
      <div class="table-body">
      <el-table :data="tableData" v-loading="loading" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="{ background:'#1a1f3a', cursor:'pointer' }"
        @row-click="openDetail"
        @selection-change="rows => selectedRows = rows">
        <el-table-column type="selection" width="46" align="center" @click.stop />
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="createTime" label="告警时间" width="170">
          <template #default="{row}">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="姓名" width="110" />
        <el-table-column label="告警类型" width="120" align="center">
          <template #default="{row}"><el-tag :type="typeTag(row.warningType)" size="small" effect="dark">{{ typeLabel(row.warningType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="warningValue" label="告警值" width="100" align="center" />
        <el-table-column label="告警级别" width="100" align="center">
          <template #default="{row}"><el-tag :type="levelTag(row.warningLevel)" size="small" effect="dark">{{ levelLabel(row.warningLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处理状态" width="100" align="center">
          <template #default="{row}"><el-tag :type="row.handleStatus===1?'success':'danger'" size="small" effect="dark">{{ row.handleStatus===1?'已处理':'未处理' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="handleBy" label="处理人" min-width="110">
          <template #default="{row}">{{ row.handleBy||'-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{row}">
            <el-button v-if="row.handleStatus!==1" type="warning" link size="small" @click.stop="openHandle(row)"><el-icon><Edit /></el-icon> 处理</el-button>
            <span v-else class="handled-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination background layout="total,sizes,prev,pager,next,jumper" :current-page="pagination.page" :page-sizes="[10,20,50,100]" :page-size="pagination.size" :total="pagination.total" @size-change="s=>{pagination.size=s;loadData()}" @current-change="p=>{pagination.page=p;loadData()}" />
      </div>
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailVisible" title="告警详情" width="480px" direction="rtl" :destroy-on-close="true">
      <div v-if="detailRow" class="detail-body">
        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">告警时间</span>
            <span class="detail-value">{{ formatDate(detailRow.createTime) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">姓名</span>
            <span class="detail-value">{{ detailRow.userName || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警类型</span>
            <el-tag :type="typeTag(detailRow.warningType)" size="small" effect="dark">{{ typeLabel(detailRow.warningType) }}</el-tag>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警级别</span>
            <el-tag :type="levelTag(detailRow.warningLevel)" size="small" effect="dark">{{ levelLabel(detailRow.warningLevel) }}</el-tag>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警值</span>
            <span class="detail-value">{{ detailRow.warningValue || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">处理状态</span>
            <el-tag :type="detailRow.handleStatus===1?'success':'danger'" size="small" effect="dark">{{ detailRow.handleStatus===1?'已处理':'未处理' }}</el-tag>
          </div>
        </div>

        <template v-if="detailRow.handleStatus === 1">
          <div class="detail-divider"></div>
          <div class="detail-section">
            <div class="detail-section-title">处理信息</div>
            <div class="detail-row">
              <span class="detail-label">处理人</span>
              <span class="detail-value">{{ detailRow.handleBy || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">处理时间</span>
              <span class="detail-value">{{ formatDate(detailRow.handleTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">处理备注</span>
              <span class="detail-value detail-note">{{ detailRow.handleNote || '-' }}</span>
            </div>
          </div>
        </template>

        <div class="detail-footer" v-if="detailRow.handleStatus !== 1">
          <el-button type="warning" style="width:100%" @click="() => { openHandle(detailRow); detailVisible = false }">
            <el-icon><Edit /></el-icon> 处理此告警
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Handle Dialog -->
    <el-dialog v-model="handleDialogVisible" title="处理告警" width="520px" :close-on-click-modal="false">
      <div class="handle-summary">
        <div class="summary-row"><span class="summary-label">告警人员</span><span class="summary-value">{{ currentRow?.userName||'-' }}</span></div>
        <div class="summary-row"><span class="summary-label">告警类型</span><el-tag :type="typeTag(currentRow?.warningType)" size="small" effect="dark">{{ typeLabel(currentRow?.warningType) }}</el-tag></div>
        <div class="summary-row"><span class="summary-label">告警时间</span><span class="summary-value">{{ formatDate(currentRow?.createTime) }}</span></div>
      </div>
      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="90px" class="form-body">
        <el-form-item label="处理方式" prop="handleType">
          <el-radio-group v-model="handleForm.handleType">
            <el-radio :value="1">立即响应</el-radio><el-radio :value="2">远程指导</el-radio><el-radio :value="3">误报处理</el-radio><el-radio :value="4">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注" prop="handleNote"><el-input v-model="handleForm.handleNote" type="textarea" :rows="3" placeholder="请输入处理说明" /></el-form-item>
        <el-form-item label="通知主管"><el-switch v-model="handleForm.notify" active-color="#38ef7d" inactive-color="#4a5578" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { Timer, Search, Refresh, Edit, Bell, WarningFilled, WarnTriangleFilled, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils'
import { useClock } from '@/composables/useClock'
import { getRiskWarningList, getRiskWarningOverview, handleRiskWarning, handleBatchRiskWarning } from '@/api/risk-warning'

const { currentTime } = useClock()

const overview = reactive({ todayTotal: 0, pending: 0, critical: 0, handled: 0 })
const handleRate = computed(() => { const t = overview.todayTotal||0; return t===0?0:((overview.handled/t)*100).toFixed(1) })

const loadOverview = async () => { try { const r = await getRiskWarningOverview(); if(r.code===200) Object.assign(overview, r.data||{}) } catch(e){} }

const searchForm = reactive({ dateRange: null, warningType: '', warningLevel: '', handleStatus: '', keyword: '' })
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const p = { page: pagination.page, size: pagination.size }
    if (searchForm.dateRange?.length===2) { p.startDate=searchForm.dateRange[0]; p.endDate=searchForm.dateRange[1] }
    if (searchForm.warningType) p.warningType = searchForm.warningType
    if (searchForm.warningLevel) p.warningLevel = searchForm.warningLevel
    if (searchForm.handleStatus==='handled') p.handled=true; else if(searchForm.handleStatus==='unhandled') p.handled=false
    if (searchForm.keyword) p.keyword = searchForm.keyword
    const res = await getRiskWarningList(p)
    if (res.code===200) { tableData.value = res.data?.list||[]; pagination.total = res.data?.total||0 }
  } catch(e) { ElMessage.error('加载告警列表失败') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.page=1; loadData() }
const handleReset = () => { Object.assign(searchForm, { dateRange:null, warningType:'', warningLevel:'', handleStatus:'', keyword:'' }); handleSearch() }

// KPI card click filter
const filterByCard = (type) => {
  Object.assign(searchForm, { dateRange: null, warningType: '', warningLevel: '', handleStatus: '', keyword: '' })
  if (type === 'unhandled') {
    searchForm.handleStatus = 'unhandled'
  } else if (type === 'critical') {
    searchForm.warningLevel = 'critical'
  } else if (type === 'handled') {
    searchForm.handleStatus = 'handled'
  }
  // type === 'all': no filter, just reset and reload
  handleSearch()
}

const typeMap = { SOS:{l:'SOS求助',t:'danger'}, fall:{l:'跌倒',t:'warning'}, heartRate:{l:'心率异常',t:''}, bloodOxygen:{l:'血氧异常',t:'info'}, temperature:{l:'体温异常',t:'warning'}, staticAlert:{l:'静态报警',t:'info'} }
const typeLabel = t => typeMap[t]?.l||t||'-'
const typeTag = t => typeMap[t]?.t||''
const levelMap = { critical:{l:'危急',t:'danger'}, warning:{l:'预警',t:'warning'}, info:{l:'提示',t:'info'} }
const levelLabel = l => levelMap[l]?.l||l||'-'
const levelTag = l => levelMap[l]?.t||'info'

// Detail drawer
const detailVisible = ref(false)
const detailRow = ref(null)
const openDetail = (row) => { detailRow.value = row; detailVisible.value = true }

// Batch selection
const selectedRows = ref([])
const batchHandle = async () => {
  if (!selectedRows.value.length) return
  try {
    const ids = selectedRows.value.map(r => r.id)
    const res = await handleBatchRiskWarning(ids)
    if (res.code === 200) {
      ElMessage.success(`已批量处理 ${ids.length} 条告警`)
      selectedRows.value = []
      loadData()
      loadOverview()
    } else {
      ElMessage.error(res.message || '批量处理失败')
    }
  } catch(e) { ElMessage.error('批量处理失败') }
}

const handleDialogVisible = ref(false)
const handleSubmitting = ref(false)
const handleFormRef = ref(null)
const currentRow = ref(null)
const handleForm = reactive({ handleType: 1, handleNote: '', notify: false })
const handleRules = { handleType:[{required:true,message:'请选择处理方式'}], handleNote:[{required:true,message:'请输入处理备注',trigger:'blur'}] }

const openHandle = (row) => { currentRow.value=row; handleForm.handleType=1; handleForm.handleNote=''; handleForm.notify=false; handleDialogVisible.value=true }
const submitHandle = async () => {
  const valid = await handleFormRef.value.validate().catch(()=>false)
  if(!valid) return
  handleSubmitting.value = true
  try {
    const res = await handleRiskWarning(currentRow.value.id, { handleType:handleForm.handleType, handleNote:handleForm.handleNote, notify:handleForm.notify })
    if(res.code===200) { ElMessage.success('处理成功'); handleDialogVisible.value=false; loadData(); loadOverview() }
    else ElMessage.error(res.message||'处理失败')
  } catch(e) { ElMessage.error('处理失败') }
  finally { handleSubmitting.value=false }
}

onMounted(() => { loadOverview(); loadData() })
</script>

<style scoped lang="scss">
@import '@/styles/dark-admin.scss';

.page-container {
  height: calc(100vh - 50px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: $da-bg;
  color: $da-text;
}
.page-header { @include da-page-header; flex-shrink: 0; }
.page-header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 36px; color: $da-accent; background: rgba(0,212,255,.1); border-radius: 10px; padding: 8px; }
.main-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 2px; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: $da-text-dim; margin: 0; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $da-text-dim; background: $da-accent-dim; padding: 6px 14px; border-radius: 20px; border: 1px solid $da-accent-hover; }
.mb-16 { margin-bottom: 16px; flex-shrink: 0; }
.stat-card { @include da-stat-card; }
.stat-card-clickable {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(0, 212, 255, 0.18);
    border-color: $da-accent-hover;
  }
  &:active {
    transform: translateY(0);
  }
}
.stat-icon-wrap { @include da-icon-wrap; &.primary { background: $da-grad-primary; } &.success { background: $da-grad-success; } &.warning { background: $da-grad-warning; } &.danger { background: $da-grad-danger; } &.info { background: $da-grad-info; } }
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: $da-text-bright; line-height: 1.1; .unit { font-size: 14px; font-weight: 400; color: $da-text-dim; margin-left: 2px; } }
.stat-label { font-size: 12px; color: $da-text-dim; margin-top: 4px; }
.stat-badge { font-size: 11px; color: $da-danger; background: rgba(255,82,82,.12); border: 1px solid rgba(255,82,82,.3); padding: 2px 8px; border-radius: 10px; }
.panel { @include da-panel; }
.panel.table-panel { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-body { flex: 1; min-height: 0; overflow: hidden; }
.search-form { flex-shrink: 0; padding: 16px 20px; }
.panel-header { flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid $da-border; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: $da-text; }
.title-bar { display: inline-block; width: 3px; height: 16px; background: $da-accent; border-radius: 2px; }
.panel-header-right { display: flex; align-items: center; gap: 10px; }
.total-badge { font-size: 12px; color: $da-text-dim; background: $da-accent-dim; border: 1px solid $da-accent-border; padding: 3px 12px; border-radius: 12px; }
.handled-text { font-size: 12px; color: $da-text-muted; }
.pagination-wrap { flex-shrink: 0; display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid $da-border; }
.handle-summary { padding: 14px 16px; margin-bottom: 16px; background: $da-panel-alt; border: 1px solid $da-border; border-radius: 8px; }
.summary-row { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; &:not(:last-child) { border-bottom: 1px solid $da-border; } }
.summary-label { font-size: 13px; color: $da-text-dim; }
.summary-value { font-size: 13px; color: $da-text-bright; font-weight: 500; }
.form-body { padding: 8px 0; }

// Detail drawer styles
.detail-body { padding: 8px 0; }
.detail-section { padding: 0 4px; }
.detail-section-title { font-size: 13px; font-weight: 600; color: $da-accent; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid $da-border; }
.detail-row { display: flex; align-items: flex-start; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.05); &:last-child { border-bottom: none; } }
.detail-label { font-size: 13px; color: $da-text-dim; flex-shrink: 0; width: 80px; }
.detail-value { font-size: 13px; color: $da-text-bright; font-weight: 500; text-align: right; flex: 1; }
.detail-note { font-weight: 400; color: $da-text; word-break: break-all; }
.detail-divider { height: 1px; background: $da-border; margin: 16px 0; }
.detail-footer { margin-top: 24px; padding-top: 16px; border-top: 1px solid $da-border; }

@include da-el-overrides;
:deep(.el-date-editor) { --el-input-bg-color: #{$da-panel-alt}; --el-input-border-color: #{$da-border-light}; --el-input-text-color: #{$da-text}; }
:deep(.el-drawer) { background: $da-panel; }
:deep(.el-drawer__header) { color: $da-text-bright; border-bottom: 1px solid $da-border; margin-bottom: 0; padding: 16px 20px; }
:deep(.el-drawer__body) { padding: 20px; color: $da-text; }
</style>
