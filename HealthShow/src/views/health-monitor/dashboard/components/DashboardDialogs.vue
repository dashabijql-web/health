<template>
  <el-drawer
    v-model="empDrawer.visible"
    title=""
    direction="rtl"
    size="520px"
    :append-to-body="true"
    :destroy-on-close="true"
    class="dm-emp-drawer"
    style="--el-bg-color:#080c20;--el-drawer-bg-color:#080c20"
    @closed="closeEmpDrawer"
  >
    <template #header>
      <div class="dm-drawer-hd">
        <div class="dm-drawer-avatar">{{ (empDrawer.data.empName || empDrawer.userName || '?').charAt(0) }}</div>
        <div>
          <div class="dm-drawer-name">{{ empDrawer.data.empName || empDrawer.userName || '--' }}</div>
          <div class="dm-drawer-meta">
            {{ empDrawer.data.deptName || '--' }} · {{ empDrawer.data.jobTypeName || '--' }}
          </div>
        </div>
        <div class="dm-drawer-warn-count">
          <span class="dm-dwc-num">{{ empDrawer.abnormalCount }}</span>
          <span class="dm-dwc-label">次异常</span>
        </div>
      </div>
    </template>

    <div v-loading="empDrawer.loading" class="dm-drawer-body">
      <div class="dm-drawer-vitals">
        <div class="dm-dv-card" v-for="v in empDrawer.vitals" :key="v.label">
          <div class="dm-dv-val" :style="{ color: v.color }">
            {{ v.val }}<span class="dm-dv-unit">{{ v.unit }}</span>
          </div>
          <div class="dm-dv-label">{{ v.label }}</div>
          <div class="dm-dv-status" :class="v.statusCls">{{ v.statusText }}</div>
        </div>
      </div>

      <div class="dm-drawer-section">
        <div class="dm-ds-title">{{ trendBlockTitle }}</div>
        <div id="empTrendChart" style="width:100%;height:200px;"></div>
      </div>

      <div class="dm-drawer-section">
        <div class="dm-ds-title">健康评分</div>
        <div id="empRadarChart" style="width:100%;height:180px;"></div>
      </div>

      <div class="dm-drawer-section">
        <div class="dm-ds-title">
          近期预警记录
          <span class="dm-ds-badge">{{ empDrawer.warnings.length }} 条</span>
        </div>
        <div class="dm-warn-table">
          <div class="dm-wt-row dm-wt-head">
            <span style="width:120px">时间</span>
            <span style="width:90px">类型</span>
            <span style="width:60px">数值</span>
            <span style="flex:1">描述</span>
            <span style="width:50px">级别</span>
          </div>
          <div class="dm-wt-row" v-for="(w, i) in empDrawer.warnings.slice(0, 15)" :key="w.id || w.createTime || i">
            <span style="width:120px;font-size:11px">{{ formatWarnTime(w.createTime) }}</span>
            <span style="width:90px">{{ alertTypeLabel(w.warningType) }}</span>
            <span style="width:60px;font-family:Consolas;color:#FFB84D">{{ w.warningValue }}</span>
            <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#8ba6c8">
              {{ w.warningMessage || '--' }}
            </span>
            <span style="width:50px">
              <span :class="['dm-wt-level', dashboardWarningLevelDrawerClass(normalizeWarningLevel(w.warningLevel))]">
                {{ dashboardWarningLevelLabel(normalizeWarningLevel(w.warningLevel)) }}
              </span>
            </span>
          </div>
          <div v-if="!empDrawer.warnings.length" class="dm-empty" style="padding:20px">暂无预警记录</div>
        </div>
      </div>
    </div>
  </el-drawer>

  <el-dialog
    v-model="handleDialog.visible"
    title="处理预警"
    width="400px"
    :append-to-body="true"
    class="dm-handle-dialog"
  >
    <div class="dm-hd-info">
      <div class="dm-hd-row"><span class="dm-hd-key">人员</span><span class="dm-hd-val">{{ handleDialog.event?.userName }}</span></div>
      <div class="dm-hd-row"><span class="dm-hd-key">类型</span><span class="dm-hd-val">{{ handleDialog.event?.type }}</span></div>
      <div class="dm-hd-row"><span class="dm-hd-key">数值</span><span class="dm-hd-val" style="color:#FFB84D">{{ handleDialog.event?.value }}</span></div>
    </div>
    <el-input
      v-model="handleDialog.remark"
      type="textarea"
      :rows="3"
      placeholder="请输入处理备注（可选）"
      style="margin-top:12px"
    />
    <template #footer>
      <el-button @click="handleDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="handleDialog.submitting" @click="submitHandle">确认处理</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="deptPersonModal.visible"
    width="960px"
    :append-to-body="true"
    :destroy-on-close="false"
    :show-close="false"
    class="dm-dept-person-dialog"
    style="background:#0a1628;border:1px solid rgba(0,212,255,0.18);border-radius:10px"
    :header-style="{ display:'none' }"
    :body-style="{ padding:0, background:'#0a1628' }"
    @closed="closeDeptPersonModal"
    @opened="handleDeptPersonOpened"
  >
    <div class="dm-dp-header">
      <span class="dm-dp-header-title">部门检测人次</span>
      <button class="dm-dp-header-close" @click="deptPersonModal.visible = false">×</button>
    </div>
    <div class="dm-dp-toolbar">
      <el-date-picker
        v-model="deptPersonModal.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width:300px;flex-shrink:0"
        @change="handleDeptPersonOpened"
      />
    </div>
    <div v-loading="deptPersonModal.loading" ref="deptPersonChartRef" style="width:100%;height:480px"></div>
  </el-dialog>

  <el-dialog
    v-model="deptDetailModal.visible"
    width="860px"
    :append-to-body="true"
    :destroy-on-close="false"
    :show-close="false"
    class="dm-dept-person-dialog"
    style="background:#0a1628;border:1px solid rgba(0,212,255,0.18);border-radius:10px"
    :header-style="{ display:'none' }"
    :body-style="{ padding:0, background:'#0a1628' }"
    @closed="closeDeptDetailModal"
    @opened="handleDeptDetailOpened"
  >
    <div class="dm-dp-header">
      <span class="dm-dp-header-title">{{ deptDetailModal.deptName }} — 检测 / 异常趋势</span>
      <button class="dm-dp-header-close" @click="deptDetailModal.visible = false">×</button>
    </div>
    <div class="dm-dp-toolbar">
      <el-date-picker
        v-model="deptDetailModal.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width:300px;flex-shrink:0"
        @change="handleDeptDetailOpened"
      />
    </div>
    <div v-loading="deptDetailModal.loading" ref="deptDetailChartRef" style="width:100%;height:420px"></div>
  </el-dialog>

  <el-dialog
    v-model="metricDetailModal.visible"
    width="860px"
    :append-to-body="true"
    :destroy-on-close="false"
    :show-close="false"
    class="dm-dept-person-dialog"
    style="background:#0a1628;border:1px solid rgba(0,212,255,0.18);border-radius:10px"
    :header-style="{ display:'none' }"
    :body-style="{ padding:0, background:'#0a1628' }"
    @closed="closeMetricDetailModal"
    @opened="handleMetricDetailOpened"
  >
    <div class="dm-dp-header">
      <span class="dm-dp-header-title">{{ metricDetailModal.metricLabel }} — 检测 / 异常趋势</span>
      <button class="dm-dp-header-close" @click="metricDetailModal.visible = false">×</button>
    </div>
    <div class="dm-dp-toolbar">
      <el-date-picker
        v-model="metricDetailModal.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width:300px;flex-shrink:0"
        @change="handleMetricDetailOpened"
      />
    </div>
    <div v-loading="metricDetailModal.loading" ref="metricDetailChartRef" style="width:100%;height:420px"></div>
  </el-dialog>

  <el-dialog
    v-model="warnCurveModal.visible"
    :title="warnCurveModal.title"
    width="780px"
    :append-to-body="true"
    :destroy-on-close="true"
    @opened="handleWarnCurveOpened"
    @closed="closeWarnCurveModal"
  >
    <div class="dm-wc-info" v-if="warnCurveModal.event">
      <el-tag :type="dashboardWarningLevelTagType(warnCurveModal.event.level)" size="small" effect="dark">{{ dashboardWarningLevelLabel(warnCurveModal.event.level) }}</el-tag>
      <span class="dm-wc-type">{{ warnCurveModal.event.type }}</span>
      <span class="dm-wc-sep">·</span>
      <span class="dm-wc-val" style="color:#FFB84D">{{ warnCurveModal.event.indicator }}: {{ warnCurveModal.event.value }}</span>
      <span class="dm-wc-sep">·</span>
      <span class="dm-wc-time">{{ warnCurveModal.event.time }}</span>
    </div>
    <div v-loading="warnCurveModal.loading" ref="warnCurveChartRef" style="width:100%;height:360px;margin-top:12px"></div>
    <div v-if="!warnCurveModal.loading && !warnCurveModal.hasData" style="text-align:center;color:#4a6080;padding:60px 0;font-size:13px">该时段暂无健康记录数据</div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import {
  dashboardWarningLevelDrawerClass,
  dashboardWarningLevelLabel,
  dashboardWarningLevelTagType
} from '../dashboard-warning-level'
import { normalizeWarningLevel } from '../../../alert-management/common/warning-lifecycle'

const props = defineProps({
  alertTypeLabel: { type: Function, required: true },
  deptDetailModal: { type: Object, required: true },
  deptPersonModal: { type: Object, required: true },
  formatWarnTime: { type: Function, required: true },
  handleDialog: { type: Object, required: true },
  initWarnCurveChart: { type: Function, required: true },
  loadDeptDetailChart: { type: Function, required: true },
  loadDeptPersonChart: { type: Function, required: true },
  loadMetricDetailChart: { type: Function, required: true },
  metricDetailModal: { type: Object, required: true },
  submitHandle: { type: Function, required: true },
  trendBlockTitle: { type: String, required: true },
  warnCurveModal: { type: Object, required: true },
  empDrawer: { type: Object, required: true }
})

const deptPersonChartRef = ref(null)
const deptDetailChartRef = ref(null)
const metricDetailChartRef = ref(null)
const warnCurveChartRef = ref(null)

function disposeChart(chart) {
  if (chart) chart.dispose()
}

function closeEmpDrawer() {
  disposeChart(props.empDrawer.trendChart)
  disposeChart(props.empDrawer.radarChart)
  props.empDrawer.trendChart = null
  props.empDrawer.radarChart = null
}

function closeDeptPersonModal() {
  disposeChart(props.deptPersonModal.chart)
  props.deptPersonModal.chart = null
}

function closeDeptDetailModal() {
  disposeChart(props.deptDetailModal.chart)
  props.deptDetailModal.chart = null
}

function closeMetricDetailModal() {
  disposeChart(props.metricDetailModal.chart)
  props.metricDetailModal.chart = null
}

function closeWarnCurveModal() {
  disposeChart(props.warnCurveModal.chart)
  props.warnCurveModal.chart = null
}

function handleDeptPersonOpened() {
  props.loadDeptPersonChart(deptPersonChartRef.value)
}

function handleDeptDetailOpened() {
  props.loadDeptDetailChart(deptDetailChartRef.value)
}

function handleMetricDetailOpened() {
  props.loadMetricDetailChart(metricDetailChartRef.value)
}

function handleWarnCurveOpened() {
  props.initWarnCurveChart(warnCurveChartRef.value)
}
</script>
