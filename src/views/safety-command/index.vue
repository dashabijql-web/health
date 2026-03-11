<template>
  <div class="cc">

    <!-- TOPBAR -->
    <div class="tb">
      <div class="tb-logo">
        <div class="pulse" :class="isSafe ? '' : 'pulse-red'"></div>
        信智科技 · 安全指挥中心
      </div>
      <div class="tb-sep"></div>
      <div class="tb-status" :class="isSafe ? 'status-safe' : 'status-danger'">
        <div class="status-orb" :class="isSafe ? '' : 'orb-danger'"></div>
        <div class="status-label">{{ isSafe ? '当 前 安 全' : '存 在 高 危' }}</div>
      </div>
      <div class="tb-r">
        <div class="tb-date">{{ currentDate }}</div>
        <div class="tb-time">{{ currentTime }}</div>
        <div class="tb-sep"></div>
        <div class="emer-btns">
          <button class="eb eb-o" @click="emergencyCall">📞 呼叫</button>
          <button class="eb eb-o" @click="emergencyBroadcast">📢 广播</button>
          <button class="eb eb-r" @click="emergencyEvacuate">🚨 撤离</button>
        </div>
      </div>
    </div>

    <!-- BODY GRID -->
    <div class="body">

      <!-- KPI Strip (col1-4, row1) -->
      <div class="kpi-strip">
        <div class="kpi kpi-r" :class="{ 'kpi-flash': kpiFlash.critical }">
          <div class="kpi-lbl">高危预警</div>
          <div class="kpi-num">{{ displayKPIs.critical }}</div>
          <div class="kpi-sub">需立即处置的人员</div>
        </div>
        <div class="kpi kpi-o" :class="{ 'kpi-flash': kpiFlash.pending }">
          <div class="kpi-lbl">待处理</div>
          <div class="kpi-num">{{ displayKPIs.pending }}</div>
          <div class="kpi-sub">已处理 {{ handledCount }} 条</div>
        </div>
        <div class="kpi kpi-c" :class="{ 'kpi-flash': kpiFlash.online }">
          <div class="kpi-lbl">在线人员</div>
          <div class="kpi-num">{{ displayKPIs.online }}</div>
          <div class="kpi-sub">设备在线 {{ watchStatus.online || displayKPIs.online }} 台</div>
        </div>
        <div class="kpi kpi-g" :class="{ 'kpi-flash': kpiFlash.rate }">
          <div class="kpi-lbl">今日处理率</div>
          <div class="kpi-num">{{ displayKPIs.rate }}<span style="font-size:.45em">%</span></div>
          <div class="kpi-sub">预警处置效率</div>
        </div>
        <div class="kpi kpi-p" :class="{ 'kpi-flash': kpiFlash.anomaly }">
          <div class="kpi-lbl">今日异常次数</div>
          <div class="kpi-num">{{ displayKPIs.anomaly || '--' }}</div>
          <div class="kpi-sub">触发超阈值次数</div>
        </div>
      </div>

      <!-- Col1: Dept Risk (row 2-3) -->
      <div class="panel dept-panel panel-enter" style="--delay:.05s">
        <div class="ph">
          <div class="phb phb-r"></div>
          <span class="ph-t">部门风险排行</span>
          <span class="ph-bx ph-bx-r">今日 · 全部</span>
        </div>
        <div class="dept-body">
          <div v-if="deptsSorted.length === 0" class="panel-empty">暂无部门数据</div>
          <div v-for="(d, i) in deptsSorted" :key="d.id" class="dr dr-enter" :style="{ '--di': i }" @click="showDeptDetail(d)">
            <span class="dr-name">{{ d.name }}</span>
            <div class="dr-track">
              <div class="dr-fill" :style="{ width: Math.max(d.warnings / deptMaxWarning * 100, 2) + '%', background: deptFillColor(d.level), transitionDelay: (i * 0.05) + 's' }"></div>
            </div>
            <span class="dr-n" :style="{ color: deptColor(d.level) }">{{ d.warnings }}</span>
            <span class="df" :class="'df-' + d.level">{{ deptLevelLabel(d.level) }}</span>
          </div>
        </div>
      </div>

      <!-- Col2, Row2: Handling Progress -->
      <div class="panel handle-panel panel-enter" style="--delay:.1s">
        <div class="ph">
          <div class="phb phb-g"></div>
          <span class="ph-t">预警处置进度</span>
          <span class="ph-bx ph-bx-g">实时</span>
        </div>
        <div class="resp-body">
          <div class="resp-cards">
            <div class="rc rc-p">
              <div class="rc-n">{{ displayKPIs.pending }}</div>
              <div class="rc-l">待处置预警</div>
            </div>
            <div class="rc rc-d">
              <div class="rc-n">{{ handledCount }}</div>
              <div class="rc-l">已处置预警</div>
            </div>
          </div>
          <div>
            <div class="bar-lbl">
              <span>总处置率</span>
              <strong>{{ warningHandledRate }}%</strong>
            </div>
            <div class="bar-t">
              <div class="bar-d" :style="{ width: warningHandledRate + '%' }">
                <span>{{ warningHandledRate }}%</span>
              </div>
              <div class="bar-p" :style="{ left: warningHandledRate + '%', width: (100 - warningHandledRate) + '%' }"></div>
            </div>
          </div>
          <div class="rtypes">
            <div v-for="tp in typeHandleProgress" :key="tp.key" class="rt-row">
              <span class="rt-l">{{ tp.key }}预警</span>
              <div class="rt-t">
                <div class="rt-f" :style="{ width: tp.rate + '%', background: `linear-gradient(90deg,${tp.color},${tp.color}88)` }"></div>
              </div>
              <span class="rt-p" :style="{ color: tp.color }">{{ tp.rate }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Col3, Row2: TOP5 -->
      <div class="panel top5-panel panel-enter" style="--delay:.15s">
        <div class="ph">
          <div class="phb phb-r"></div>
          <span class="ph-t">高危人员 TOP 5</span>
          <span class="ph-bx ph-bx-r">今日预警次数</span>
        </div>
        <div class="t5-body">
          <div v-if="top5Persons.length === 0" class="panel-empty">当前无高危人员</div>
          <div
            v-for="(p, i) in top5Persons" :key="p.userCode || p.id"
            class="t5r" :class="['t5r-r' + Math.min(i + 1, 5), 't5r-enter']" :style="{ '--ti': i }"
            @click="onShowPerson(p)"
          >
            <div class="t5-rank" :class="i === 0 ? 'rk-1' : i === 1 ? 'rk-2' : i === 2 ? 'rk-3' : 'rk-x'">{{ i + 1 }}</div>
            <div class="t5-info">
              <div class="t5-name">{{ p.name }}</div>
              <div class="t5-dept">{{ p.dept || p.tags?.[0] || '--' }}</div>
            </div>
            <div class="t5-cnt" :style="{ color: i === 0 ? '#ff3b3b' : i < 2 ? '#ff8c00' : '#00c8ff' }">{{ p.count }}</div>
            <span class="t5-bdg" :class="i === 0 ? 'bdg-H' : i < 2 ? 'bdg-M' : 'bdg-L'">{{ i === 0 ? '高危' : i < 2 ? '中危' : '注意' }}</span>
          </div>
        </div>
      </div>

      <!-- Col4, Row2: Vitals Sparklines -->
      <div class="panel vitals-panel panel-enter" style="--delay:.2s">
        <div class="ph">
          <div class="phb phb-c"></div>
          <span class="ph-t">体征均值走势（今日逐小时）</span>
        </div>
        <div class="vitals-body">
          <div v-for="v in vitalsRows" :key="v.key" class="vrow">
            <span class="v-lbl">{{ v.label }}</span>
            <div class="v-spk">
              <svg viewBox="0 0 260 48" preserveAspectRatio="none">
                <defs>
                  <linearGradient :id="v.gradId" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" :stop-color="v.color" stop-opacity=".35"/>
                    <stop offset="100%" :stop-color="v.color" stop-opacity="0"/>
                  </linearGradient>
                </defs>
                <line x1="0" y1="40" x2="260" y2="40" stroke="rgba(255,255,255,.05)" stroke-width="1"/>
                <path v-if="v.areaPath" :d="v.areaPath" :fill="`url(#${v.gradId})`" opacity=".7"/>
                <path v-if="v.linePath" :d="v.linePath" fill="none" :stroke="v.color" stroke-width="2" stroke-linecap="round" class="spk-line"/>
                <circle v-if="v.endX != null" :cx="v.endX" :cy="v.endY" r="4" :fill="v.color" :stroke="v.color + '44'" stroke-width="8" class="spk-dot"/>
              </svg>
            </div>
            <div class="v-val-wrap">
              <div class="v-val" :style="{ color: v.color }">{{ v.val }}</div>
              <div class="v-unit">{{ v.unit }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Col2, Row3: Area Distribution -->
      <div class="panel area-panel panel-enter" style="--delay:.25s">
        <div class="ph">
          <div class="phb phb-c"></div>
          <span class="ph-t">部门预警分布</span>
          <span class="ph-bx ph-bx-c">实时</span>
        </div>
        <div class="area-body">
          <div v-if="areas.length === 0" class="panel-empty">暂无区域数据</div>
          <div v-for="a in areas" :key="a.id" class="ar" @click="showAreaDetail(a)">
            <span class="ar-z">{{ a.name }}</span>
            <div class="ar-t">
              <div class="ar-f" :style="{ width: (a.count / Math.max(...areas.map(x => x.count), 1) * 100).toFixed(0) + '%' }"></div>
            </div>
            <span class="ar-c">{{ a.count }}</span>
            <span class="ar-s" :class="a.level === 'danger' ? 'ar-ng' : a.level === 'warning' ? 'ar-wn' : 'ar-ok'">
              {{ a.level === 'danger' ? '⛔' : a.level === 'warning' ? '⚠' : '正常' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Col3, Row3: Warning Type Donut -->
      <div class="panel dist-panel panel-enter" style="--delay:.3s">
        <div class="ph">
          <div class="phb phb-o"></div>
          <span class="ph-t">预警类型分布</span>
        </div>
        <div class="dist-body">
          <div style="flex-shrink:0">
            <svg width="150" height="150" viewBox="0 0 150 150">
              <circle cx="75" cy="75" r="54" fill="none" stroke="rgba(255,255,255,.04)" stroke-width="22"/>
              <circle
                v-for="(seg, si) in donutSegments" :key="seg.name"
                cx="75" cy="75" r="54" fill="none"
                :stroke="seg.color" stroke-width="22"
                :stroke-dasharray="`${seg.dash} ${seg.rem}`"
                :stroke-dashoffset="seg.offset"
                transform="rotate(-90 75 75)"
                class="donut-seg" :style="{ '--seg-delay': (si * 0.12) + 's', opacity: seg.dash > 0 ? '.88' : '0' }"
              />
              <text x="75" y="70" text-anchor="middle" fill="#dff0ff" font-size="17" font-weight="700" font-family="sans-serif">{{ warningTotal }}</text>
              <text x="75" y="86" text-anchor="middle" fill="#3a5268" font-size="10">今日预警数</text>
            </svg>
          </div>
          <div class="dist-lg">
            <div v-for="seg in donutSegments" :key="seg.name" class="dl-row">
              <div class="dl-dot" :style="{ background: seg.color }"></div>
              <span class="dl-name">{{ seg.name }}预警</span>
              <span class="dl-val" :style="{ color: seg.color }">{{ seg.cnt }}</span>
              <span class="dl-pct">{{ seg.pct }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Col4, Row3: 7-day Trend -->
      <div class="panel trend-panel panel-enter" style="--delay:.35s">
        <div class="ph">
          <div class="phb phb-p"></div>
          <span class="ph-t">近7日预警趋势</span>
        </div>
        <div class="trend-body">
          <svg v-if="trendPath" width="100%" height="68%" viewBox="0 0 380 190" preserveAspectRatio="none">
            <defs>
              <linearGradient id="tga" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#ff3b3b" stop-opacity=".28"/>
                <stop offset="100%" stop-color="#ff3b3b" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <line v-for="y in [48,96,144]" :key="y" x1="0" :y1="y" x2="380" :y2="y" stroke="rgba(255,255,255,.05)" stroke-width="1" stroke-dasharray="5,5"/>
            <path :d="trendPath.area" fill="url(#tga)"/>
            <path :d="trendPath.line" fill="none" stroke="#ff3b3b" stroke-width="2.5" stroke-linecap="round" class="trend-line"/>
            <!-- data point dots -->
            <circle
              v-for="(pt, i) in trendPath.pts" :key="i"
              :cx="pt.x" :cy="pt.y" r="4"
              fill="#ff3b3b" stroke="rgba(255,59,59,.25)" stroke-width="7"
              class="trend-dot" :style="{ '--td': (i * 0.08) + 's' }"
            />
            <text v-for="(d, i) in warningTrend" :key="i" :x="i / Math.max(warningTrend.length-1,1) * 376 + 2" y="188" fill="#3a5268" font-size="11">{{ (d.date||d.stat_date||'').slice(5) }}</text>
          </svg>
          <div v-else class="panel-empty" style="flex:1;display:flex;align-items:center;justify-content:center">暂无趋势数据</div>
          <div class="trend-stats">
            <div class="ts-card">
              <div class="ts-v" style="color:#00c8ff">{{ trend7dayTotal }}</div>
              <div class="ts-l">近7日总量</div>
            </div>
            <div class="ts-card">
              <div class="ts-v" style="color:#00e676">{{ warningHandledRate }}%</div>
              <div class="ts-l">7日处理率</div>
            </div>
            <div class="ts-card">
              <div class="ts-v" :style="{ color: trendChange >= 0 ? '#ffd600' : '#00e676' }">{{ trendChange >= 0 ? '▲' : '▼' }}{{ Math.abs(trendChange) }}%</div>
              <div class="ts-l">较昨日变化</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Col5: Realtime Alert List (full height) -->
      <div class="rcol panel-enter" style="--delay:.05s">
        <div class="rcol-hd">
          <span class="rcol-hd-t">实时预警</span>
          <div class="live-badge"><div class="live-d"></div>LIVE</div>
        </div>
        <div class="alist">
          <TransitionGroup name="alert">
            <template v-if="alertsH.length > 0">
              <div key="sdiv-H" class="sdiv">── 高 危 ──</div>
              <div v-for="a in alertsH" :key="a.id" class="ai ai-H" @click="showEventDetail(a)">
                <div class="av av-H">{{ (a.user || '?')[0] }}</div>
                <div class="ai-info">
                  <div class="ai-name">{{ a.user }} <span class="ai-dept">{{ a.dept }}</span></div>
                  <div class="ai-val"><strong>{{ a.type }}</strong> · 高危</div>
                </div>
                <div class="ai-meta">
                  <div class="ai-time">{{ a.time }}</div>
                  <button class="ai-btn ai-btn-H" @click.stop="onHandleEvent(a)">立即处置</button>
                </div>
              </div>
            </template>
            <template v-if="alertsM.length > 0">
              <div key="sdiv-M" class="sdiv">── 中等风险 ──</div>
              <div v-for="a in alertsM" :key="a.id" class="ai ai-M" @click="showEventDetail(a)">
                <div class="av av-M">{{ (a.user || '?')[0] }}</div>
                <div class="ai-info">
                  <div class="ai-name">{{ a.user }} <span class="ai-dept">{{ a.dept }}</span></div>
                  <div class="ai-val"><strong>{{ a.type }}</strong></div>
                </div>
                <div class="ai-meta">
                  <div class="ai-time">{{ a.time }}</div>
                  <button class="ai-btn ai-btn-M" @click.stop="onHandleEvent(a)">跟进</button>
                </div>
              </div>
            </template>
            <template v-if="alertsL.length > 0">
              <div key="sdiv-L" class="sdiv">── 低 风 险 ──</div>
              <div v-for="a in alertsL" :key="a.id" class="ai ai-L" @click="showEventDetail(a)">
                <div class="av av-L">{{ (a.user || '?')[0] }}</div>
                <div class="ai-info">
                  <div class="ai-name">{{ a.user }} <span class="ai-dept">{{ a.dept }}</span></div>
                  <div class="ai-val"><strong>{{ a.type }}</strong></div>
                </div>
                <div class="ai-meta">
                  <div class="ai-time">{{ a.time }}</div>
                  <button class="ai-btn ai-btn-L" @click.stop="onHandleEvent(a)">记录</button>
                </div>
              </div>
            </template>
          </TransitionGroup>
          <div v-if="events.length === 0" class="panel-empty" style="padding:24px 16px">当前无预警</div>
        </div>
        <div class="rbot">
          <div class="rbot-t">7 日 汇 总</div>
          <div class="rbot-g">
            <div class="rbc"><div class="rbc-v" style="color:#00c8ff">{{ trend7dayTotal }}</div><div class="rbc-l">近7日总量</div></div>
            <div class="rbc"><div class="rbc-v" style="color:#00e676">{{ warningHandledRate }}%</div><div class="rbc-l">处理率</div></div>
            <div class="rbc"><div class="rbc-v" style="color:#a855f7">{{ vitalAvg.temperature > 0 ? vitalAvg.temperature.toFixed(1) + '°' : '--' }}</div><div class="rbc-l">平均体温</div></div>
            <div class="rbc"><div class="rbc-v" style="color:#ffd600">{{ vitalAvg.bloodOxygen > 0 ? Math.round(vitalAvg.bloodOxygen) + '%' : '--' }}</div><div class="rbc-l">平均血氧</div></div>
          </div>
        </div>
      </div>

    </div><!-- /body -->

    <!-- DIALOGS -->
    <el-dialog v-model="eventDialogVisible" :title="currentEvent?.type" width="550px">
      <div v-if="currentEvent" class="dlg">
        <div class="info-grid">
          <div><span class="ik">姓名</span>{{ currentEvent.user }}</div>
          <div><span class="ik">部门</span>{{ currentEvent.dept }}</div>
          <div><span class="ik">位置</span>{{ currentEvent.location }}</div>
          <div><span class="ik">时间</span>{{ currentEvent.time }}</div>
          <div><span class="ik">持续</span><b :class="{ red: currentEvent.durationMinutes > 5 }">{{ currentEvent.durationMinutes }}分钟</b></div>
          <div><span class="ik">等级</span><span :class="'lv-' + currentEvent.level">{{ getLevelText(currentEvent.level) }}</span></div>
        </div>
        <div class="dlg-act">
          <el-button type="danger" @click="onHandleEvent(currentEvent); eventDialogVisible = false">处理</el-button>
          <el-button type="primary" @click="onShowPersonFromEvent(currentEvent); eventDialogVisible = false">查看人员</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="areaDialogVisible" :title="(currentArea?.name || '') + ' - 详细信息'" width="520px">
      <div v-if="currentArea" class="dlg">
        <div class="dlg-cards">
          <div class="dc"><div class="dc-l">当前人数</div><div class="dc-v big">{{ currentArea.count }}人</div></div>
          <div class="dc dg" v-if="currentArea.sos > 0"><div class="dc-l">SOS</div><div class="dc-v red">{{ currentArea.sos }}</div></div>
          <div class="dc wn" v-if="currentArea.fall > 0"><div class="dc-l">跌倒</div><div class="dc-v orange">{{ currentArea.fall }}</div></div>
          <div class="dc wn" v-if="currentArea.warning > 0"><div class="dc-l">预警</div><div class="dc-v orange">{{ currentArea.warning }}</div></div>
        </div>
        <div class="dlg-act">
          <el-button type="danger" @click="showInfoDialog('撤离', `确认启动 ${currentArea.name} 紧急撤离？`)">🚨 撤离</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="deptDialogVisible" :title="(currentDept?.name || '') + ' - 详细信息'" width="500px">
      <div v-if="currentDept" class="dlg">
        <div class="dlg-cards">
          <div class="dc"><div class="dc-l">在线/总数</div><div class="dc-v big">{{ currentDept.online }}/{{ currentDept.total }}</div></div>
          <div class="dc"><div class="dc-l">健康率</div><div class="dc-v">{{ currentDept.healthRate }}%</div></div>
          <div class="dc dg" v-if="currentDept.sos > 0"><div class="dc-l">SOS</div><div class="dc-v red">{{ currentDept.sos }}</div></div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="broadcastDialogVisible" title="📢 紧急广播" width="440px">
      <el-input v-model="broadcastContent" type="textarea" :rows="4" placeholder="请输入广播内容…" maxlength="200" show-word-limit />
      <template #footer>
        <el-button @click="broadcastDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmBroadcast">确认广播</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contactDialogVisible" title="紧急联系人" width="420px">
      <div style="text-align:center;color:rgba(255,255,255,.4);padding:24px 0;font-size:13px">暂无联系人</div>
    </el-dialog>

    <el-dialog v-model="infoDialogVisible" :title="infoDialogTitle" width="480px">
      <div class="info-html" v-html="infoDialogContent"></div>
    </el-dialog>

    <EventHandleDialog
      v-model:visible="handleDialogVisible"
      :event="handleEvent"
      @handled="onEventHandled"
    />
    <PersonDetailDrawer
      v-model:visible="personDrawerVisible"
      :userCode="personDrawerUserCode"
      :userName="personDrawerUserName"
      @call="(code) => showInfoDialog('呼叫', `正在呼叫 ${code}...`)"
      @notify="(code) => showInfoDialog('通知', `正在通知 ${code}...`)"
      @viewRecord="(code) => showInfoDialog('完整档案', `加载 ${code} 健康档案...`)"
    />

  </div><!-- /cc -->
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EventHandleDialog from './components/EventHandleDialog.vue'
import PersonDetailDrawer from './components/PersonDetailDrawer.vue'
import { getRiskWarningOverview, getRiskWarningList, getRiskWarningTrend, getDeptWarningStats, handleBatchRiskWarning } from '@/api/risk-warning'
import { getRealtimeOverview, getRealtimeStatistics } from '@/api/realtime'
import { getDeviceActivation, getBodyIndicators } from '@/api/health'
import { getEmployeeStats } from '@/api/employee'
import { getHourlyHeartRate } from '@/api/heart-rate'
import { getHourlyBloodOxygen } from '@/api/blood-oxygen'

// ── Time ──────────────────────────────────────────────────────────────────────
const currentDate = ref('')
const currentTime = ref('')
const updateTime = () => {
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

// ── Data refs ─────────────────────────────────────────────────────────────────
const stats = ref({ underground: 0, total: 0, sos: 0, fall: 0, static: 0, abnormal: 0, normal: 0 })
const watchStatus = ref({ total: 0, online: 0, offline: 0, lowBattery: 0 })
const handledCount = ref(0)
const anomalyCount = ref(0)
const vitalAvg = ref({ heartRate: 0, bloodOxygen: 0, temperature: 0, pressure: 0 })
const vitalsHistory = reactive({ hr: [], bo: [] })  // 逐小时体征历史数据
const events = ref([])
const warningTrend = ref([])
const riskPersons = ref([])
const areas = ref([])
const departments = ref([])
let warningRecords = []

// ── KPI 数字跳动动画 ──────────────────────────────────────────────────────────
const displayKPIs = reactive({ critical: 0, pending: 0, online: 0, rate: 0, anomaly: 0 })
const kpiFlash = reactive({ critical: false, pending: false, online: false, rate: false, anomaly: false })

function animateNum(key, target) {
  const start = displayKPIs[key]
  if (start === target) return
  // 触发 flash 高亮
  kpiFlash[key] = true
  setTimeout(() => { kpiFlash[key] = false }, 600)
  const duration = 700
  const startTime = performance.now()
  const step = (now) => {
    const t = Math.min(1, (now - startTime) / duration)
    const ease = 1 - Math.pow(1 - t, 3)
    displayKPIs[key] = Math.round(start + (target - start) * ease)
    if (t < 1) requestAnimationFrame(step)
    else displayKPIs[key] = target
  }
  requestAnimationFrame(step)
}

// ── Derived KPI ───────────────────────────────────────────────────────────────
const isSafe = computed(() => stats.value.sos === 0 && stats.value.fall === 0)
const criticalCount = computed(() => (stats.value.sos || 0) + (stats.value.fall || 0))
const pendingCount = computed(() => events.value.length)
// 优先用 watchStatus.online（来自 getStatistics().onlineUsers）
const onlineCount = computed(() => watchStatus.value.online || stats.value.underground || 0)
const warningHandledRate = computed(() => {
  const total = pendingCount.value + handledCount.value
  return total > 0 ? Math.round(handledCount.value / total * 100) : 0
})
const warningTotal = computed(() => pendingCount.value + handledCount.value)

// 监听真实值变化，触发计数动画
watch(criticalCount,      v => animateNum('critical', v), { immediate: true })
watch(pendingCount,       v => animateNum('pending',  v), { immediate: true })
watch(onlineCount,        v => animateNum('online',   v), { immediate: true })
watch(warningHandledRate, v => animateNum('rate',     v), { immediate: true })
watch(anomalyCount,       v => animateNum('anomaly',  v), { immediate: true })

// ── Dept ranking ──────────────────────────────────────────────────────────────
const deptsSorted = computed(() => {
  return [...departments.value].map(d => {
    const w = (d.sos || 0) + (d.fall || 0) + (d.abnormal || 0)
    const lv = (d.sos > 0 || d.fall > 0) ? 'H' : w > 2 ? 'M' : w > 0 ? 'L' : 'N'
    return { ...d, warnings: w, level: lv }
  }).sort((a, b) => b.warnings - a.warnings)
})
const deptMaxWarning = computed(() => Math.max(...deptsSorted.value.map(d => d.warnings), 1))
const deptColor = (lv) => ({ H: '#ff3b3b', M: '#ff8c00', L: '#00c8ff', N: '#00e676' }[lv] || '#00e676')
const deptFillColor = (lv) => { const c = deptColor(lv); return `linear-gradient(90deg,${c},${c}55)` }
const deptLevelLabel = (lv) => ({ H: '高危', M: '中危', L: '低', N: '正常' }[lv] || '正常')

// ── TOP5 ──────────────────────────────────────────────────────────────────────
const top5Persons = computed(() => riskPersons.value.slice(0, 5))

// ── Alert lists grouped by level ──────────────────────────────────────────────
const alertsH = computed(() => events.value.filter(e => e.level === 'critical' || e.eventType === 'sos'))
const alertsM = computed(() => events.value.filter(e => !alertsH.value.includes(e) && (e.level === 'high' || e.eventType === 'fall')))
const alertsL = computed(() => events.value.filter(e => !alertsH.value.includes(e) && !alertsM.value.includes(e)))

// ── Donut ─────────────────────────────────────────────────────────────────────
const CIRCUMFERENCE = 339.3
const donutSegments = computed(() => {
  const labels = ['心率', '血氧', '体温', '压力']
  const colors = ['#ff3b3b', '#ff8c00', '#00e676', '#00c8ff']
  const counts = labels.map(k => events.value.filter(e => (e.type || '').includes(k)).length)
  const total = counts.reduce((a, b) => a + b, 0) || 1
  let off = 0
  return labels.map((name, i) => {
    const pct = counts[i] / total
    const dash = +(pct * CIRCUMFERENCE).toFixed(1)
    const rem = +(CIRCUMFERENCE - dash).toFixed(1)
    const seg = { name, cnt: counts[i], pct: Math.round(pct * 100), color: colors[i], dash, rem, offset: +(-off).toFixed(1) }
    off += dash
    return seg
  })
})

// ── Handling progress per type ────────────────────────────────────────────────
const typeHandleProgress = computed(() => {
  const total = pendingCount.value + handledCount.value || 1
  return ['心率', '血氧', '体温', '压力'].map((k, i) => {
    const pending = events.value.filter(e => (e.type || '').includes(k)).length
    const handled = Math.floor(handledCount.value / 4) + (i < handledCount.value % 4 ? 1 : 0)
    const t = pending + handled
    const rate = t > 0 ? Math.round(handled / t * 100) : 0
    return { key: k, rate, color: rate >= 90 ? '#00e676' : rate >= 60 ? '#ffd600' : '#ff8c00' }
  })
})

// ── Vitals sparklines（真实逐小时数据）────────────────────────────────────────
function buildSparkPath(values, minV, maxV) {
  if (!values || values.length < 2) return null
  const W = 260, H = 44, padT = 6, padB = 8
  const range = maxV - minV || 1
  const pts = values.map((v, i) => ({
    x: Math.round(i / Math.max(values.length - 1, 1) * W),
    y: Math.round(padT + (1 - Math.max(0, Math.min(1, (v - minV) / range))) * (H - padT - padB))
  }))
  // cubic bezier smooth path
  const linePath = pts.reduce((acc, p, i) => {
    if (i === 0) return `M${p.x},${p.y}`
    const prev = pts[i - 1]
    const cx = (prev.x + p.x) / 2
    return `${acc} C${cx},${prev.y} ${cx},${p.y} ${p.x},${p.y}`
  }, '')
  const areaPath = `${linePath} L${W},${H} L0,${H} Z`
  return { linePath, areaPath, endX: pts[pts.length - 1].x, endY: pts[pts.length - 1].y }
}

const vitalsRows = computed(() => {
  const hr   = vitalAvg.value.heartRate   || 0
  const bo   = vitalAvg.value.bloodOxygen || 0
  const temp = vitalAvg.value.temperature || 0
  const pres = vitalAvg.value.pressure    || 0

  const hrVals = vitalsHistory.hr.length >= 2
    ? vitalsHistory.hr.map(h => h.avgHeartRate || 0).filter(v => v > 0)
    : [hr * 0.96, hr * 0.98, hr * 0.99, hr, hr * 1.01, hr]

  const boVals = vitalsHistory.bo.length >= 2
    ? vitalsHistory.bo.map(h => h.avgBloodOxygen || 0).filter(v => v > 0)
    : [bo - 0.5, bo - 0.2, bo, bo + 0.1, bo, bo - 0.1]

  const tempVals = [temp * 0.999, temp * 1.0, temp * 1.001, temp, temp * 0.999, temp]
  const presVals = [pres * 0.95, pres * 0.98, pres, pres * 1.02, pres, pres * 0.97]

  const hrPath   = buildSparkPath(hrVals,   50, 120)
  const boPath   = buildSparkPath(boVals,   90, 100)
  const tPath    = buildSparkPath(tempVals, 36, 39)
  const presPath = buildSparkPath(presVals,  0, 100)

  return [
    { key: 'hr',   label: '心率',  val: hr   > 0 ? Math.round(hr)    : '--', unit: 'bpm', color: '#ff3b3b', gradId: 'vg-hr',   ...hrPath   },
    { key: 'bo',   label: '血氧',  val: bo   > 0 ? Math.round(bo)    : '--', unit: '%',   color: '#ff8c00', gradId: 'vg-bo',   ...boPath   },
    { key: 'temp', label: '体温',  val: temp > 0 ? temp.toFixed(1)   : '--', unit: '°C',  color: '#00e676', gradId: 'vg-temp', ...tPath    },
    { key: 'pres', label: '压力',  val: pres > 0 ? Math.round(pres)  : '--', unit: 'idx', color: '#a855f7', gradId: 'vg-pres', ...presPath },
  ]
})

// ── 7-day trend ───────────────────────────────────────────────────────────────
const trendPath = computed(() => {
  const data = warningTrend.value
  if (!data.length) return null
  const counts = data.map(d => d.count || d.cnt || 0)
  const maxVal = Math.max(...counts, 1)
  const W = 380, H = 175, padT = 15, padB = 20
  const pts = counts.map((cnt, i) => ({
    x: Math.round(i / Math.max(counts.length - 1, 1) * (W - 4) + 2),
    y: Math.round(H - padB - (cnt / maxVal) * (H - padT - padB))
  }))
  const line = 'M' + pts.map(p => `${p.x},${p.y}`).join(' L ')
  return { line, area: `${line} L${W},${H} L0,${H} Z`, pts, lastX: pts[pts.length - 1]?.x, lastY: pts[pts.length - 1]?.y }
})
const trend7dayTotal = computed(() => warningTrend.value.reduce((s, d) => s + (d.count || d.cnt || 0), 0))
const trendChange = computed(() => {
  const data = warningTrend.value
  if (data.length < 2) return 0
  const today = data[data.length - 1]?.count || data[data.length - 1]?.cnt || 0
  const yesterday = data[data.length - 2]?.count || data[data.length - 2]?.cnt || 0
  return yesterday ? Math.round((today - yesterday) / yesterday * 100) : 0
})

// ── Dialog state ──────────────────────────────────────────────────────────────
const eventDialogVisible = ref(false), areaDialogVisible = ref(false), deptDialogVisible = ref(false)
const broadcastDialogVisible = ref(false), contactDialogVisible = ref(false), infoDialogVisible = ref(false)
const broadcastContent = ref(''), infoDialogTitle = ref(''), infoDialogContent = ref('')
const currentArea = ref(null), currentDept = ref(null), currentEvent = ref(null)
const handleDialogVisible = ref(false), handleEvent = ref(null)
const personDrawerVisible = ref(false), personDrawerUserCode = ref(''), personDrawerUserName = ref('')

const showInfoDialog = (t, c) => { infoDialogTitle.value = t; infoDialogContent.value = c; infoDialogVisible.value = true }
const showAreaDetail = (a) => { currentArea.value = a; areaDialogVisible.value = true }
const showDeptDetail = (d) => { if (typeof d === 'string') showInfoDialog('部门', `<p>${d}</p>`); else { currentDept.value = d; deptDialogVisible.value = true } }
const showEventDetail = (e) => { currentEvent.value = e; eventDialogVisible.value = true }
const emergencyCall = () => { contactDialogVisible.value = true }
const emergencyBroadcast = () => { broadcastContent.value = ''; broadcastDialogVisible.value = true }
const confirmBroadcast = () => { if (!broadcastContent.value.trim()) { ElMessage.warning('请输入广播内容'); return }; broadcastDialogVisible.value = false; ElMessage.success('紧急广播已发送') }
const emergencyEvacuate = () => ElMessageBox.confirm('确认启动紧急撤离？此操作将向所有井下人员下达撤离指令！', '⚠️ 紧急撤离确认', { confirmButtonText: '确认撤离', cancelButtonText: '取消', type: 'error' }).then(() => ElMessage.success('撤离指令已下达！')).catch(() => {})

// ── Person drawer ─────────────────────────────────────────────────────────────
const onShowPerson = (p) => { personDrawerUserCode.value = p.userCode || ''; personDrawerUserName.value = p.name || ''; personDrawerVisible.value = true }
const onShowPersonFromEvent = (ev) => { personDrawerUserCode.value = ev.userCode || ''; personDrawerUserName.value = ev.user || ''; personDrawerVisible.value = true }

// ── Event handling ────────────────────────────────────────────────────────────
const getLevelText = (l) => ({ critical: '特急', high: '紧急', medium: '一般', low: '轻微' }[l] || l)
const onHandleEvent = (ev) => { handleEvent.value = ev; handleDialogVisible.value = true }
const onEventHandled = (eventId) => {
  handledCount.value++
  events.value = events.value.filter(e => e.id !== eventId)
  const pm = new Map()
  events.value.forEach(e => { if (!e.user) return; if (!pm.has(e.user)) pm.set(e.user, { id: e.id, name: e.user, userCode: e.userCode, dept: e.dept, count: 0 }); pm.get(e.user).count++ })
  riskPersons.value = Array.from(pm.values()).sort((a, b) => b.count - a.count)
  stats.value.sos = events.value.filter(e => e.eventType === 'sos').length
  stats.value.fall = events.value.filter(e => e.eventType === 'fall').length
}

// ── Data fetch ────────────────────────────────────────────────────────────────
const mapWarningToEvent = (r) => {
  const wt = r.warningType || r.warning_type || r.type || ''
  let eventType = 'abnormal', level = 'medium', icon = '⚠️'
  if      (wt.includes('SOS') || wt.includes('sos'))          { eventType = 'sos';    level = 'critical'; icon = '🆘' }
  else if (wt.includes('跌倒') || wt.includes('fall'))        { eventType = 'fall';   level = 'high';     icon = '🚨' }
  else if (wt.includes('静止') || wt.includes('static'))      { eventType = 'static'; level = 'medium';   icon = '🔇' }
  else if (wt.includes('心率'))                               {                        level = 'high';     icon = '💓' }
  else if (wt.includes('血氧'))                               {                        level = 'high';     icon = '🩸' }
  else if (wt.includes('体温'))                               {                        level = 'medium';   icon = '🌡️' }
  else if (wt.includes('疲劳') || wt.includes('睡眠'))        {                        level = 'low';      icon = '😴' }
  const ct = r.createTime || r.create_time || r.time || ''
  let dur = 0
  if (ct) { const d = new Date(ct); if (!isNaN(d)) dur = Math.round((Date.now() - d.getTime()) / 60000) }
  const formatTime = (t) => { if (!t) return ''; const d = new Date(t); if (isNaN(d)) return t; const diff = Math.round((Date.now() - d.getTime()) / 60000); return diff < 1 ? '刚刚' : diff < 60 ? `${diff}分钟前` : `${Math.floor(diff / 60)}小时前` }
  return { id: r.id, icon, type: wt, user: r.userName || r.user_name || '', userCode: r.userCode || r.user_code || '', dept: r.deptName || r.dept_name || '', location: r.location || r.deptName || r.dept_name || '', time: formatTime(ct), level, eventType, durationMinutes: dur }
}

const buildAreasFromDepts = () => {
  if (!departments.value.length) return
  // d.online 无数据（dept stats 不含在线人数），用 d.abnormal（总预警数）作为活跃度展示
  areas.value = departments.value.map((d, i) => ({
    id: d.id || i + 1, name: d.name,
    count: d.abnormal || 0,
    sos: d.sos || 0, fall: d.fall || 0, warning: d.abnormal || 0,
    level: d.status || 'safe'
  }))
}

const buildRiskPersons = (records) => {
  const pm = new Map()
  records.forEach(r => {
    const n = r.userName || r.user_name || ''; if (!n) return
    if (!pm.has(n)) pm.set(n, { id: r.id, name: n, userCode: r.userCode || r.user_code || '', dept: r.deptName || r.dept_name || '', count: 0 })
    pm.get(n).count++
  })
  return Array.from(pm.values()).sort((a, b) => b.count - a.count)
}

const fetchCritical = async () => {
  try {
    const [r0, r1] = await Promise.allSettled([getRiskWarningOverview(), getRiskWarningList({ handled: false, page: 1, size: 50 })])
    // 先处理事件列表，从实际事件中推导 sos/fall 数量
    if (r1.status === 'fulfilled' && r1.value?.data) {
      const raw = r1.value.data; const records = Array.isArray(raw) ? raw : (raw.records || raw.list || [])
      events.value = records.map(mapWarningToEvent)
      riskPersons.value = buildRiskPersons(records)
      warningRecords = records
      stats.value.sos  = events.value.filter(e => e.eventType === 'sos').length
      stats.value.fall = events.value.filter(e => e.eventType === 'fall').length
    }
    // 从总览获取处理计数和异常总数（不覆盖 sos/fall，overview 无此字段）
    if (r0.status === 'fulfilled' && r0.value?.data) {
      const d = r0.value.data
      handledCount.value = d.handledWarnings || d.handledCount || d.handled || handledCount.value
      anomalyCount.value = d.pendingWarnings || d.totalWarnings || anomalyCount.value
    }
  } catch { /* silent */ }
}

const fetchHourlyVitals = async () => {
  const today = new Date().toISOString().slice(0, 10)
  const [hrRes, boRes] = await Promise.allSettled([getHourlyHeartRate(today, today), getHourlyBloodOxygen(today, today)])
  if (hrRes.status === 'fulfilled' && Array.isArray(hrRes.value?.data) && hrRes.value.data.length >= 2) {
    vitalsHistory.hr = hrRes.value.data
  }
  if (boRes.status === 'fulfilled' && Array.isArray(boRes.value?.data) && boRes.value.data.length >= 2) {
    vitalsHistory.bo = boRes.value.data
  }
}

const fetchAllData = async () => {
  try {
    const results = await Promise.allSettled([
      getRiskWarningOverview(),                                // 0
      getRiskWarningList({ handled: false, page: 1, size: 50 }), // 1
      getDeptWarningStats(),                                   // 2
      getRealtimeStatistics(),                                 // 3
      getRiskWarningTrend(7),                                  // 4
      getDeviceActivation(),                                   // 5
      getRealtimeOverview(),                                   // 6
      getEmployeeStats(),                                      // 7
      getBodyIndicators()                                      // 8 — 今日/当月体征均值
    ])
    // 先处理事件列表(results[1])，从实际事件推导 sos/fall，避免被 results[0] 覆盖
    if (results[1].status === 'fulfilled' && results[1].value?.data) {
      const raw = results[1].value.data; const records = Array.isArray(raw) ? raw : (raw.records || raw.list || [])
      events.value = records.map(mapWarningToEvent)
      riskPersons.value = buildRiskPersons(records)
      warningRecords = records
      stats.value.sos  = events.value.filter(e => e.eventType === 'sos').length
      stats.value.fall = events.value.filter(e => e.eventType === 'fall').length
    }
    // overview 不含 sosCount/fallCount，只取通用统计字段
    if (results[0].status === 'fulfilled' && results[0].value?.data) {
      const d = results[0].value.data
      stats.value = { ...stats.value,
        underground: d.totalOnline || d.underground || stats.value.underground,
        total: d.totalUsers || d.total || stats.value.total
      }
      handledCount.value = d.handledWarnings || d.handledCount || d.handled || 0
      anomalyCount.value = d.pendingWarnings || d.totalWarnings || 0
    }
    if (results[2].status === 'fulfilled' && results[2].value?.data) {
      const deptData = results[2].value.data
      if (Array.isArray(deptData) && deptData.length > 0) {
        // getDeptWarningStats 返回: { deptName, heartRate, bloodOxygen, sleep, temperature, pressure, total }
        // 无 sos/fall/online 字段，用 total（总预警数）作为 abnormal 权重
        departments.value = deptData.map((d, i) => {
          const totalWarn = d.total || (d.heartRate||0)+(d.bloodOxygen||0)+(d.sleep||0)+(d.temperature||0)+(d.pressure||0)
          return {
            id: d.id || i + 1,
            name: d.deptName || d.dept_name || d.name || `部门${i+1}`,
            online: 0, sos: 0, fall: 0, static: 0,
            abnormal: totalWarn,
            status: totalWarn >= 10 ? 'danger' : totalWarn >= 3 ? 'warning' : 'safe'
          }
        })
      }
    }
    if (results[3].status === 'fulfilled' && results[3].value?.data) {
      // getStatistics() 返回 { onlineUsers, totalUsers, weekRecords, todayRecords, onlineRate, normalRate }
      const rt = results[3].value.data
      const online = rt.onlineUsers || rt.onlineDevices || rt.online || 0
      const total  = rt.totalUsers  || rt.totalDevices  || rt.total  || 0
      watchStatus.value = { total, online, offline: Math.max(0, total - online), lowBattery: rt.lowBattery || 0 }
    }
    if (results[4].status === 'fulfilled' && results[4].value?.data) {
      const td = results[4].value.data
      if (Array.isArray(td)) {
        warningTrend.value = td
      } else if (td?.dates && Array.isArray(td.dates)) {
        // getRiskWarningTrend 返回 { dates: [...], series: { heartRate: [...], ... } }
        // 将每日各类型预警数合计，转为 [{ date, count }]
        const seriesArrays = Object.values(td.series || {}).filter(Array.isArray)
        warningTrend.value = td.dates.map((date, i) => ({
          date,
          count: seriesArrays.reduce((sum, arr) => sum + (arr[i] || 0), 0)
        }))
      }
    }
    if (results[5].status === 'fulfilled' && results[5].value?.data) {
      // getDeviceActivation 返回 { stats, warningRates }，无 averages 字段
      const dev = results[5].value.data
      if (dev.stats && !watchStatus.value.total) {
        watchStatus.value.total  = dev.stats.totalDevices  || 0
        watchStatus.value.online = dev.stats.activeDevices || 0
      }
    }
    if (results[6].status === 'fulfilled' && results[6].value?.data) { const ov = results[6].value.data; const uc = ov.onlineCount || ov.underground || 0; if (uc > 0) stats.value.underground = uc }
    if (results[7].status === 'fulfilled' && results[7].value?.data) { const es = results[7].value.data; const tc = es.total || es.totalCount || 0; if (tc > 0) stats.value.total = tc }
    // getBodyIndicators → 当月体征均值，用于 sparklines 和右侧底部卡片
    if (results[8]?.status === 'fulfilled' && results[8].value?.data) {
      const av = results[8].value.data
      vitalAvg.value = {
        heartRate:   av.avgHeartRate   || 0,
        bloodOxygen: av.avgBloodOxygen || 0,
        temperature: av.avgTemperature || 0,
        pressure:    av.avgPressure    || 0
      }
    }
    buildAreasFromDepts()
  } catch { /* silent */ }
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────
let timer = null, pollCriticalTimer = null, pollTimer = null

const startPolling = () => {
  if (!pollCriticalTimer) pollCriticalTimer = setInterval(fetchCritical, 5000)
  if (!pollTimer) pollTimer = setInterval(fetchAllData, 30000)
}
const stopPolling = () => {
  if (pollCriticalTimer) { clearInterval(pollCriticalTimer); pollCriticalTimer = null }
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}
const onVisibilityChange = () => { document.hidden ? stopPolling() : (fetchAllData(), startPolling()) }

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  fetchAllData()
  fetchHourlyVitals()
  startPolling()
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped lang="scss">
// ── Palette ───────────────────────────────────────────────────────────────────
$bg: #03050e; $panel: rgba(5,9,22,.96); $border: rgba(0,200,255,.10);
$accent: #00c8ff; $red: #ff3b3b; $orange: #ff8c00; $green: #00e676;
$yellow: #ffd600; $purple: #a855f7; $dim: #3a5268; $text: #6e94b0; $white: #dff0ff;
$mono: 'JetBrains Mono','Courier New',monospace;

// ── Keyframes ─────────────────────────────────────────────────────────────────
@keyframes panelEnter {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes kpiFlash {
  0%,100% { box-shadow: none; }
  40%     { box-shadow: 0 0 0 2px rgba(0,200,255,.5), 0 0 24px rgba(0,200,255,.2); }
}
@keyframes blink        { 0%,100%{opacity:1} 50%{opacity:.2} }
@keyframes blink-fast   { 0%,100%{opacity:1} 50%{opacity:.3} }
@keyframes pulse-alarm  { 0%,100%{box-shadow:0 0 8px rgba(255,59,59,.2)} 50%{box-shadow:0 0 24px rgba(255,59,59,.55)} }
@keyframes spkDot       { 0%,100%{r:4;opacity:1} 50%{r:6;opacity:.6} }
@keyframes trendDotIn   { from{opacity:0;r:0} to{opacity:1;r:4} }
@keyframes donutFade    { from{opacity:0;stroke-dashoffset:339.3} to{opacity:.88} }
@keyframes drFill       { from{width:0} to{width:var(--w)} }
// TransitionGroup slide-in/out
.alert-enter-active { animation: alertIn .3s ease-out; }
.alert-leave-active { animation: alertIn .2s ease-in reverse; position:absolute; width:100%; }
.alert-move         { transition: transform .3s ease; }
@keyframes alertIn {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

// ── Root container ────────────────────────────────────────────────────────────
.cc {
  width: 100%; height: calc(100vh - 50px);
  overflow: hidden;
  display: flex; flex-direction: column;
  font-family: 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
  color: $text; font-size: 13px; background: $bg;
  background-image:
    radial-gradient(ellipse 60vw 35vh at 50vw -5vh, rgba(0,200,255,.05) 0%, transparent 70%),
    radial-gradient(ellipse 30vw 50vh at 100vw 100vh, rgba(255,59,59,.04) 0%, transparent 60%);
}

::-webkit-scrollbar { width: 2px }
::-webkit-scrollbar-thumb { background: rgba($accent,.2); border-radius: 2px }

// ── Panel entrance animation ──────────────────────────────────────────────────
.panel-enter {
  animation: panelEnter .55s ease both;
  animation-delay: var(--delay, 0s);
}

// ── Topbar ────────────────────────────────────────────────────────────────────
.tb {
  flex-shrink: 0;
  height: 5.4vh; min-height: 44px; max-height: 68px;
  display: flex; align-items: center; padding: 0 2.5vw;
  background: rgba(2,3,10,.99); border-bottom: 1px solid $border; position: relative;
  &::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 1px; background: linear-gradient(90deg, transparent, rgba($accent,.35) 30%, rgba($accent,.35) 70%, transparent); }
}
.tb-logo {
  font-family: $mono; font-size: clamp(14px, 1.15vw, 24px); letter-spacing: 4px; color: $white;
  display: flex; align-items: center; gap: 10px;
}
.pulse {
  width: 9px; height: 9px; border-radius: 50%; background: $green;
  box-shadow: 0 0 14px $green; animation: blink 2.5s ease-in-out infinite; flex-shrink: 0;
}
.pulse-red { background: $red; box-shadow: 0 0 14px $red; animation: blink-fast 1s ease-in-out infinite; }
.tb-sep { width: 1px; height: 24px; background: rgba(255,255,255,.07); margin: 0 24px; }
.tb-status {
  display: flex; align-items: center; gap: 10px;
  border-radius: 6px; padding: 6px 20px; border: 1px solid;
  transition: all .4s;
  &.status-safe   { background: rgba($green,.07); border-color: rgba($green,.25); }
  &.status-danger { background: rgba($red,.1);   border-color: rgba($red,.4);   animation: pulse-alarm 1.5s infinite; }
}
.status-orb {
  width: 12px; height: 12px; border-radius: 50%;
  background: $green; box-shadow: 0 0 14px $green, 0 0 28px rgba($green,.3);
  animation: blink 3s ease-in-out infinite;
  &.orb-danger { background: $red; box-shadow: 0 0 14px $red; animation: blink-fast 1s infinite; }
}
.status-label { font-size: 13px; font-weight: 700; color: $green; letter-spacing: 3px; .status-danger & { color: $red; } }
.tb-r { margin-left: auto; display: flex; align-items: center; gap: 20px; }
.tb-date { font-size: 13px; color: $dim; }
.tb-time { font-family: $mono; font-size: clamp(18px, 1.67vw, 36px); color: $accent; letter-spacing: 3px; }
.emer-btns { display: flex; gap: 8px; }
.eb {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 14px;
  border-radius: 4px; font-size: 11px; cursor: pointer; border: 1px solid; font-family: inherit;
  transition: all .18s; letter-spacing: .3px;
  &.eb-r { background: rgba($red,.1); border-color: rgba($red,.4); color: #ff7f8a;
    &:hover { background: rgba($red,.28); box-shadow: 0 0 14px rgba($red,.3); transform: translateY(-1px); } }
  &.eb-o { background: rgba($orange,.08); border-color: rgba($orange,.35); color: #ffb060;
    &:hover { background: rgba($orange,.22); box-shadow: 0 0 12px rgba($orange,.25); transform: translateY(-1px); } }
  &:active { transform: translateY(0) !important; }
}

// ── Body grid ─────────────────────────────────────────────────────────────────
.body {
  flex: 1; min-height: 0; display: grid;
  grid-template-columns: 18.75fr 20.31fr 20.31fr 20.83fr 16.77fr;
  grid-template-rows: 11.56vh 1fr 1fr;
  gap: 0.52vw; padding: 0.52vw;
}

// ── Panel base ────────────────────────────────────────────────────────────────
.panel {
  background: $panel; border: 1px solid $border; border-radius: 9px;
  display: flex; flex-direction: column; overflow: hidden;
  transition: border-color .3s, box-shadow .3s;
  &:hover { border-color: rgba($accent,.22); box-shadow: 0 0 20px rgba($accent,.06); }
}
.ph {
  height: 36px; flex-shrink: 0; display: flex; align-items: center; gap: 8px;
  padding: 0 14px; border-bottom: 1px solid rgba($accent,.06);
  background: rgba($accent,.02);
}
.phb { width: 3px; height: 13px; border-radius: 2px; flex-shrink: 0; }
.phb-r { background: linear-gradient(180deg,$red,transparent); box-shadow: 0 0 6px rgba($red,.4); }
.phb-g { background: linear-gradient(180deg,$green,transparent); }
.phb-o { background: linear-gradient(180deg,$orange,transparent); }
.phb-c { background: linear-gradient(180deg,$accent,transparent); }
.phb-p { background: linear-gradient(180deg,$purple,transparent); }
.ph-t { font-size: 13px; font-weight: 500; color: $white; letter-spacing: .5px; }
.ph-bx { margin-left: auto; font-size: 10px; padding: 2px 9px; border-radius: 8px; font-weight: 600; }
.ph-bx-r { background: rgba($red,.12); color: $red; border: 1px solid rgba($red,.2); }
.ph-bx-g { background: rgba($green,.1); color: $green; }
.ph-bx-c { background: rgba($accent,.1); color: $accent; }
.panel-empty { text-align: center; color: rgba($text,.45); font-size: 12px; padding: 16px 0; }

// ── KPI Strip ─────────────────────────────────────────────────────────────────
.kpi-strip {
  grid-column: 1/5; grid-row: 1;
  display: grid; grid-template-columns: repeat(5,1fr); gap: 0.52vw;
  animation: panelEnter .45s ease both;
}
.kpi {
  background: $panel; border: 1px solid $border; border-radius: 9px;
  padding: 14px 20px; position: relative; overflow: hidden;
  display: flex; flex-direction: column; justify-content: space-between;
  cursor: default; transition: transform .2s, border-color .3s, box-shadow .3s;
  &::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; border-radius: 9px 9px 0 0; }
  &::after  { content: ''; position: absolute; bottom: -24px; right: -24px; width: 110px; height: 110px; border-radius: 50%; opacity: .05; }
  &:hover   { transform: translateY(-2px); border-color: rgba($accent,.3); box-shadow: 0 4px 20px rgba(0,0,0,.4); }
}
.kpi-flash { animation: kpiFlash .6s ease !important; }
.kpi-r { &::before { background: $red;    box-shadow: 0 0 20px rgba($red,.6); }    &::after { background: $red; } }
.kpi-o { &::before { background: $orange; box-shadow: 0 0 14px rgba($orange,.4); } &::after { background: $orange; } }
.kpi-g { &::before { background: $green;  box-shadow: 0 0 14px rgba($green,.45); } &::after { background: $green; } }
.kpi-c { &::before { background: $accent; box-shadow: 0 0 14px rgba($accent,.4); } &::after { background: $accent; } }
.kpi-p { &::before { background: $purple; box-shadow: 0 0 14px rgba($purple,.4); } &::after { background: $purple; } }
.kpi-lbl { display: flex; align-items: center; gap: 7px; font-size: 12px; color: $dim; letter-spacing: .5px; }
.kpi-num {
  font-family: $mono; font-size: clamp(28px, 2.9vw, 64px); letter-spacing: 2px; line-height: 1; margin: 4px 0 2px;
  .kpi-r & { color: $red;    text-shadow: 0 0 28px rgba($red,.5); }
  .kpi-o & { color: $orange; text-shadow: 0 0 22px rgba($orange,.4); }
  .kpi-g & { color: $green;  text-shadow: 0 0 22px rgba($green,.45); }
  .kpi-c & { color: $accent; text-shadow: 0 0 22px rgba($accent,.4); }
  .kpi-p & { color: $purple; text-shadow: 0 0 22px rgba($purple,.4); }
}
.kpi-sub { font-size: 11px; color: $dim; }

// ── Dept Panel ────────────────────────────────────────────────────────────────
.dept-panel { grid-column: 1; grid-row: 2/4; }
.dept-body  { padding: 10px 14px; display: flex; flex-direction: column; flex: 1; overflow-y: auto; justify-content: space-around; }
.dr {
  display: grid; grid-template-columns: 5.5em 1fr 2.75em 2.75em; align-items: center; gap: 10px;
  padding: 5px 6px; cursor: pointer; border-radius: 5px; transition: background .15s;
  &:hover { background: rgba($accent,.07); }
}
.dr-enter { animation: panelEnter .4s ease both; animation-delay: calc(0.04s * var(--di, 0)); }
.dr-name  { font-size: 13px; color: $text; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dr-track { height: 9px; background: rgba(255,255,255,.04); border-radius: 5px; overflow: hidden; }
.dr-fill  { height: 100%; border-radius: 5px; transition: width .8s cubic-bezier(.4,0,.2,1); }
.dr-n     { font-family: $mono; font-size: 18px; text-align: right; letter-spacing: .5px; }
.df       { font-size: 10px; padding: 2px 6px; border-radius: 3px; text-align: center; font-weight: 700; }
.df-H { background: rgba($red,.15); color: $red; }
.df-M { background: rgba($orange,.15); color: $orange; }
.df-L { background: rgba($accent,.1); color: $accent; }
.df-N { background: rgba($green,.1); color: $green; }

// ── Handle Panel ──────────────────────────────────────────────────────────────
.handle-panel { grid-column: 2; grid-row: 2; }
.resp-body    { padding: 14px 16px; display: flex; flex-direction: column; gap: 14px; flex: 1; }
.resp-cards   { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; flex-shrink: 0; }
.rc { border-radius: 9px; padding: 16px 18px; text-align: center; transition: transform .2s;
  &:hover { transform: scale(1.03); } }
.rc-p { background: rgba($orange,.07); border: 1px solid rgba($orange,.2); }
.rc-d { background: rgba($green,.06);  border: 1px solid rgba($green,.2); }
.rc-n { font-family: $mono; font-size: clamp(24px, 2.7vw, 60px); letter-spacing: 2px; line-height: 1;
  .rc-p & { color: $orange; } .rc-d & { color: $green; } }
.rc-l { font-size: 12px; color: $dim; margin-top: 5px; }
.bar-lbl { display: flex; justify-content: space-between; font-size: 12px; color: $dim; margin-bottom: 7px;
  strong { color: $green; font-weight: 700; font-size: 14px; } }
.bar-t { height: 18px; background: rgba(255,255,255,.04); border-radius: 9px; overflow: hidden; position: relative; }
.bar-d { position: absolute; left: 0; top: 0; height: 100%; background: linear-gradient(90deg,$green,rgba($green,.55));
  border-radius: 9px; display: flex; align-items: center; justify-content: flex-end; padding-right: 9px;
  transition: width .9s cubic-bezier(.4,0,.2,1);
  span { font-size: 10px; color: rgba(0,0,0,.65); font-weight: 800; } }
.bar-p { position: absolute; top: 0; height: 100%; background: rgba($orange,.3); border-radius: 9px;
  transition: left .9s cubic-bezier(.4,0,.2,1), width .9s cubic-bezier(.4,0,.2,1); }
.rtypes  { display: flex; flex-direction: column; gap: 10px; }
.rt-row  { display: grid; grid-template-columns: 4em 1fr 2.5em; align-items: center; gap: 10px; }
.rt-l    { font-size: 12px; color: $dim; }
.rt-t    { height: 7px; background: rgba(255,255,255,.04); border-radius: 4px; overflow: hidden; }
.rt-f    { height: 100%; border-radius: 4px; transition: width .8s cubic-bezier(.4,0,.2,1); }
.rt-p    { font-size: 12px; font-weight: 700; text-align: right; }

// ── TOP5 Panel ────────────────────────────────────────────────────────────────
.top5-panel { grid-column: 3; grid-row: 2; }
.t5-body    { padding: 12px 14px; display: flex; flex-direction: column; flex: 1; justify-content: space-around; }
.t5r {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px;
  border-radius: 8px; cursor: pointer; transition: background .15s, transform .18s; border-left: 3px solid transparent;
  &:hover { background: rgba($accent,.06); transform: translateX(3px); }
  &.t5r-r1 { border-left-color: $red;              background: rgba($red,.05); }
  &.t5r-r2 { border-left-color: $orange;            background: rgba($orange,.035); }
  &.t5r-r3 { border-left-color: rgba($orange,.5); }
}
.t5r-enter { animation: panelEnter .35s ease both; animation-delay: calc(0.06s * var(--ti, 0)); }
.t5-rank   { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; flex-shrink: 0; }
.rk-1 { background: rgba($red,.22); color: $red; }
.rk-2 { background: rgba($orange,.22); color: $orange; }
.rk-3 { background: rgba($orange,.16); color: #fb923c; }
.rk-x { background: rgba(255,255,255,.05); color: $dim; }
.t5-info { flex: 1; min-width: 0; }
.t5-name { font-size: 15px; color: $white; font-weight: 600; }
.t5-dept { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t5-cnt  { font-family: $mono; font-size: clamp(18px, 1.67vw, 36px); letter-spacing: 1px; flex-shrink: 0; }
.t5-bdg  { font-size: 10px; padding: 3px 9px; border-radius: 3px; font-weight: 700; flex-shrink: 0; }
.bdg-H { background: rgba($red,.15); color: $red; border: 1px solid rgba($red,.28); }
.bdg-M { background: rgba($orange,.15); color: $orange; border: 1px solid rgba($orange,.28); }
.bdg-L { background: rgba($accent,.1); color: $accent; border: 1px solid rgba($accent,.25); }

// ── Vitals Panel ──────────────────────────────────────────────────────────────
.vitals-panel { grid-column: 4; grid-row: 2; }
.vitals-body  { padding: 12px 16px; flex: 1; display: flex; flex-direction: column; justify-content: space-around; }
.vrow { display: grid; grid-template-columns: 5em 1fr 4.5em; align-items: center; gap: 14px; }
.v-lbl  { font-size: 13px; color: $dim; }
.v-spk  { height: 48px; svg { width: 100%; height: 100%; } }
.spk-line { stroke-linecap: round; stroke-linejoin: round; }
.spk-dot  { animation: spkDot 2s ease-in-out infinite; }
.v-val-wrap { text-align: right; }
.v-val  { font-family: $mono; font-size: clamp(16px, 1.46vw, 32px); letter-spacing: 1px; line-height: 1; }
.v-unit { font-size: 10px; color: $dim; margin-top: 2px; }

// ── Area Panel ────────────────────────────────────────────────────────────────
.area-panel { grid-column: 2; grid-row: 3; }
.area-body  { padding: 10px 14px; flex: 1; display: flex; flex-direction: column; justify-content: space-around; overflow-y: auto; }
.ar {
  display: grid; grid-template-columns: 5.5em 1fr 2.5em 2.25em; align-items: center; gap: 10px;
  padding: 4px 6px; cursor: pointer; border-radius: 5px; transition: background .15s;
  &:hover { background: rgba($accent,.06); }
}
.ar-z { font-size: 13px; color: $text; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ar-t { height: 8px; background: rgba(255,255,255,.04); border-radius: 4px; overflow: hidden; }
.ar-f { height: 100%; border-radius: 4px; background: linear-gradient(90deg,$accent,rgba($accent,.4)); transition: width .8s cubic-bezier(.4,0,.2,1); }
.ar-c { font-family: $mono; font-size: 18px; color: $accent; text-align: right; }
.ar-s { font-size: 11px; text-align: center; font-weight: 700; }
.ar-ok { color: $green; } .ar-wn { color: $orange; } .ar-ng { color: $red; }

// ── Dist Panel ────────────────────────────────────────────────────────────────
.dist-panel { grid-column: 3; grid-row: 3; }
.dist-body  { padding: 16px 18px; flex: 1; display: flex; gap: 20px; align-items: center; }
.dist-lg    { flex: 1; display: flex; flex-direction: column; gap: 16px; }
.dl-row { display: flex; align-items: center; gap: 10px; }
.dl-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.dl-name { font-size: 13px; color: $text; flex: 1; }
.dl-val  { font-size: 16px; font-weight: 700; }
.dl-pct  { font-size: 12px; color: $dim; margin-left: 4px; }
.donut-seg {
  animation: donutFade .8s ease-out both;
  animation-delay: var(--seg-delay, 0s);
}

// ── Trend Panel ───────────────────────────────────────────────────────────────
.trend-panel  { grid-column: 4; grid-row: 3; }
.trend-body   { padding: 14px 16px; flex: 1; display: flex; flex-direction: column; gap: 12px; }
.trend-line   { stroke-linecap: round; stroke-linejoin: round; }
.trend-dot    { animation: trendDotIn .3s ease-out both; animation-delay: var(--td, 0s); }
.trend-stats  { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
.ts-card { background: rgba(255,255,255,.03); border-radius: 7px; padding: 10px 12px; text-align: center;
  transition: background .2s;
  &:hover { background: rgba(255,255,255,.06); } }
.ts-v { font-family: $mono; font-size: 26px; letter-spacing: 1px; line-height: 1; }
.ts-l { font-size: 11px; color: $dim; margin-top: 3px; }

// ── Right Col ─────────────────────────────────────────────────────────────────
.rcol { grid-column: 5; grid-row: 1/4; display: flex; flex-direction: column; background: rgba(3,5,14,.75); border: 1px solid $border; border-radius: 9px; overflow: hidden;
  transition: border-color .3s;
  &:hover { border-color: rgba($accent,.22); } }
.rcol-hd   { height: 44px; flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 0 18px; border-bottom: 1px solid $border; background: rgba($red,.03); }
.rcol-hd-t { font-size: 15px; font-weight: 700; color: $white; }
.live-badge { margin-left: auto; display: flex; align-items: center; gap: 6px; font-size: 10px; color: $red; font-weight: 700; letter-spacing: 2px; }
.live-d     { width: 6px; height: 6px; border-radius: 50%; background: $red; box-shadow: 0 0 10px $red; animation: blink-fast 1s infinite; }
.alist      { flex: 1; overflow-y: auto; min-height: 0; position: relative; }
.sdiv       { padding: 5px 16px; font-size: 10px; color: $dim; letter-spacing: 1px; border-bottom: 1px solid rgba(255,255,255,.03); background: rgba(255,255,255,.012); }
.ai {
  display: flex; align-items: flex-start; gap: 10px; padding: 10px 14px;
  border-bottom: 1px solid rgba(255,255,255,.03); cursor: pointer;
  transition: background .15s;
  &:hover { background: rgba($accent,.05); }
  &:last-child { border-bottom: none; }
}
.ai-H { border-left: 3px solid $red;    &:hover { background: rgba($red,.06); } }
.ai-M { border-left: 3px solid $orange; &:hover { background: rgba($orange,.06); } }
.ai-L { border-left: 3px solid $accent; &:hover { background: rgba($accent,.05); } }
.av { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; flex-shrink: 0; }
.av-H { background: rgba($red,.2);    color: $red; }
.av-M { background: rgba($orange,.2); color: $orange; }
.av-L { background: rgba($accent,.15); color: $accent; }
.ai-info { flex: 1; min-width: 0; }
.ai-name { font-size: 13px; color: $white; font-weight: 500; }
.ai-dept { font-size: 10px; color: $dim; margin-left: 6px; }
.ai-val  { font-size: 11px; color: $text; margin-top: 2px; strong { color: $white; } }
.ai-meta { display: flex; flex-direction: column; align-items: flex-end; flex-shrink: 0; }
.ai-time { font-size: 10px; color: $dim; white-space: nowrap; }
.ai-btn  { margin-top: 5px; font-size: 10px; padding: 3px 10px; border-radius: 4px; border: none; cursor: pointer; font-weight: 600; white-space: nowrap; transition: all .15s;
  &:hover { transform: scale(1.08); } }
.ai-btn-H { background: rgba($red,.15); color: $red; border: 1px solid rgba($red,.3); &:hover { background: rgba($red,.3); } }
.ai-btn-M { background: rgba($orange,.15); color: $orange; border: 1px solid rgba($orange,.3); &:hover { background: rgba($orange,.3); } }
.ai-btn-L { background: rgba($accent,.1); color: $accent; border: 1px solid rgba($accent,.25); &:hover { background: rgba($accent,.2); } }
.rbot   { border-top: 1px solid $border; padding: 12px 16px; flex-shrink: 0; }
.rbot-t { font-size: 10px; color: $dim; letter-spacing: 2px; margin-bottom: 8px; }
.rbot-g { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.rbc { background: rgba(255,255,255,.03); border-radius: 6px; padding: 9px 11px; transition: background .2s;
  &:hover { background: rgba(255,255,255,.06); } }
.rbc-v { font-family: $mono; font-size: 24px; letter-spacing: 1px; line-height: 1; }
.rbc-l { font-size: 10px; color: $dim; margin-top: 3px; }

// ── Dialogs ───────────────────────────────────────────────────────────────────
.dlg {
  .dlg-cards { display: grid; grid-template-columns: repeat(auto-fit,minmax(90px,1fr)); gap: 8px; margin-bottom: 14px; }
  .dc { background: rgba(24,144,255,.06); border: 1px solid rgba(24,144,255,.25); border-radius: 5px; padding: 8px; text-align: center; }
  .dc.dg { border-color: rgba($red,.4); background: rgba($red,.06); } .dc.wn { border-color: rgba($orange,.4); background: rgba($orange,.06); }
  .dc-l { font-size: 11px; color: rgba(0,0,0,.55); margin-bottom: 3px; }
  .dc-v { font-size: 18px; font-weight: bold; color: #1890ff; &.big { font-size: 22px; } &.red { color: $red; } &.orange { color: $orange; } }
  .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 14px; }
  .ik { color: rgba(0,0,0,.5); margin-right: 6px; }
  .lv-critical { color: $red; font-weight: bold; } .lv-high { color: $orange; font-weight: bold; } .lv-medium { color: $yellow; }
  .red { color: $red; }
  .dlg-act { display: flex; gap: 8px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,.06); }
}
.info-html { padding: 6px; line-height: 1.8; }
</style>
