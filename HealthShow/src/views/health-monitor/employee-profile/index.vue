<template>
  <div class="ep-page hm-page-shell">
    <PageHeroHeader
      class="ep-hero"
      variant="cockpit"
      eyebrow="Employee Portrait"
      title="职工健康画像"
      :description="`聚焦 ${empInfo.empName || '--'} 的实时体征、预警闭环和 7 日趋势，给值班与画像分析一个统一视角。`"
    >
      <template #meta>
        <div class="ep-hero-meta">
          <span class="hm-status-chip">{{ empInfo.empName || '--' }}</span>
          <span :class="['hm-status-chip', isOnline ? 'hm-status-chip--success' : 'hm-status-chip--warning']">
            {{ isOnline ? '在线中' : '当前离线' }}
          </span>
          <span class="hm-status-chip">最近更新 {{ lastUpdate }}</span>
        </div>
      </template>
      <template #actions>
        <button type="button" class="hm-action-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <button
          type="button"
          class="hm-action-btn hm-action-btn--success"
          @click="openAiReport"
          :disabled="aiReportLoading"
        >
          <el-icon><Document /></el-icon>
          {{ aiReportLoading ? '生成中...' : 'AI 诊断报告' }}
        </button>
      </template>
    </PageHeroHeader>

    <div class="ep-quickbar">
      <button class="ep-qbtn" @click="goArchive">档案库</button>
      <button class="ep-qbtn" @click="goMineEntry">准入页</button>
      <button class="ep-qbtn" @click="goWorkbench">月度日历</button>
      <button class="ep-qbtn" @click="goReportCenter">报表中心</button>
    </div>

    <MetricStrip
      class="ep-summary-strip"
      :items="summaryMetricItems"
    />

    <!-- AI 报告 Dialog -->
    <el-dialog v-model="aiReportVisible" :title="'AI 健康诊断报告 — ' + empInfo.empName" width="820px" :close-on-click-modal="false">
      <div v-if="aiReportLoading" style="text-align:center;padding:40px 0">
        <div class="ep-report-dots"><span></span><span></span><span></span></div>
        <p style="color:#8ba6c8;margin-top:16px">正在生成健康诊断报告，请稍候（约15~30秒）...</p>
      </div>
      <div v-else-if="aiReportContent" v-html="aiReportHtml" class="ep-report-content"></div>
      <div v-else style="text-align:center;color:#666;padding:40px 0">生成失败，请重试</div>
      <template #footer>
        <el-button @click="aiReportVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!aiReportContent || aiReportLoading" @click="printAiReport">打印 / 导出 PDF</el-button>
      </template>
    </el-dialog>

    <!-- 主体三列 -->
    <div class="ep-body" v-loading="loading">

      <!-- ═══════ 左栏 ═══════ -->
      <aside class="ep-left">

        <!-- 基本信息 -->
        <div class="ep-panel ep-basic">
          <div class="ep-ph"><span class="ep-ph-bar"></span>职工基本信息</div>
          <div class="ep-basic-body">
            <div class="ep-basic-name">{{ empInfo.empName || '--' }}</div>
            <div class="ep-basic-grid">
              <div class="ep-bi"><span>性别</span><b>{{ empInfo.gender === 2 ? '女' : '男' }}</b></div>
              <div class="ep-bi"><span>年龄</span><b>{{ calcAge(empInfo.birthDate) }}</b></div>
              <div class="ep-bi"><span>部门</span><b>{{ empInfo.deptName || '--' }}</b></div>
              <div class="ep-bi"><span>岗位</span><b>{{ empInfo.jobTypeName || '--' }}</b></div>
              <div class="ep-bi full"><span>手机</span><b>{{ empInfo.phone || '--' }}</b></div>
              <div class="ep-bi full"><span>工号</span><b class="code">{{ empInfo.empCode || '--' }}</b></div>
            </div>
          </div>
        </div>

        <div class="ep-panel ep-ai-summary">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>AI 风险摘要
            <span class="ep-ph-link" @click="openAiReport">生成完整报告</span>
          </div>
          <div class="ep-ai-list">
            <div v-for="(item, idx) in profileInsightLines" :key="idx" class="ep-ai-item">
              <span class="ep-ai-dot"></span>
              <span>{{ item }}</span>
            </div>
          </div>
        </div>

        <!-- 7天体征趋势 -->
        <div class="ep-panel ep-trend">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>7天体征趋势
            <span class="ep-ph-link">趋势摘要</span>
          </div>
          <div class="ep-trend-stats">
            <div v-for="item in trendStats" :key="item.key" :class="['ep-trend-stat', `tone-${item.tone}`]">
              <span class="ep-trend-stat-label">{{ item.label }}</span>
              <span class="ep-trend-stat-value">{{ item.value }}<em v-if="item.unit">{{ item.unit }}</em></span>
            </div>
          </div>
          <div ref="trendRef" class="ep-trend-chart"></div>
        </div>

      </aside>

      <!-- ═══════ 中栏：左面板 | 矿工 | 右面板 三列网格 ═══════ -->
      <main class="ep-center">

        <!-- 左侧两个滚动面板 -->
        <div class="ep-side-panels ep-side-left">
          <!-- 心率数据 -->
          <div class="ep-scroll-panel sp-red">
            <div class="ep-sp-header">
              <span class="ep-sp-dot red"></span>
              <span class="ep-sp-name">心率数据</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-a">
                <div v-for="(item, idx) in hrItems.concat(hrItems)" :key="'hr'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 体征数据 -->
          <div class="ep-scroll-panel sp-yellow">
            <div class="ep-sp-header">
              <span class="ep-sp-dot yellow"></span>
              <span class="ep-sp-name">体征数据</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-b">
                <div v-for="(item, idx) in vitalItems.concat(vitalItems)" :key="'vt'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中央矿工舞台 -->
        <div class="ep-miner-stage">
          <div class="ep-ring ep-ring1"></div>
          <div class="ep-ring ep-ring2"></div>
          <div class="ep-scan-overlay"></div>
          <img class="ep-miner" src="/assets/miner-worker.png" />
          <!-- 身体部位异常高亮热区 -->
          <div class="ep-hotspot ep-hs-heart"
            :class="{ active: isHrAbnormal, danger: isHrDanger }"
            title="心率监测区域">
            <span class="ep-hs-pulse"></span>
            <span class="ep-hs-label">心率 {{ vitals.heartRate || '--' }} bpm</span>
          </div>
          <div class="ep-hotspot ep-hs-lung"
            :class="{ active: isSpo2Abnormal, danger: isSpo2Danger }"
            title="血氧监测区域">
            <span class="ep-hs-pulse"></span>
            <span class="ep-hs-label">血氧 {{ vitals.bloodOxygen || '--' }}%</span>
          </div>
          <div class="ep-hotspot ep-hs-head"
            :class="{ active: isTempAbnormal || isPressureHigh, danger: isTempDanger }"
            title="头部监测区域">
            <span class="ep-hs-pulse"></span>
            <span class="ep-hs-label">{{ vitals.temperature ? vitals.temperature + '°C' : '--' }}</span>
          </div>
          <div class="ep-hotspot ep-hs-arm"
            :class="{ active: isBpAbnormal, danger: isBpDanger }"
            title="血压监测区域">
            <span class="ep-hs-pulse"></span>
            <span class="ep-hs-label">{{ (vitals.systolic && vitals.diastolic) ? vitals.systolic + '/' + vitals.diastolic : '--' }}</span>
          </div>
          <div class="ep-glow-base"></div>
        </div>

        <!-- 右侧两个滚动面板 -->
        <div class="ep-side-panels ep-side-right">
          <!-- 血氧监测 -->
          <div class="ep-scroll-panel sp-blue">
            <div class="ep-sp-header">
              <span class="ep-sp-dot blue"></span>
              <span class="ep-sp-name">血氧监测</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-c">
                <div v-for="(item, idx) in spo2Items.concat(spo2Items)" :key="'sp'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 预警摘要 -->
          <div class="ep-scroll-panel sp-orange">
            <div class="ep-sp-header">
              <span class="ep-sp-dot orange"></span>
              <span class="ep-sp-name">预警摘要</span>
              <span class="ep-sp-tag">统计</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-d">
                <div v-for="(item, idx) in warnItems.concat(warnItems)" :key="'wn'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ═══════ 右栏 ═══════ -->
      <aside class="ep-right">

        <!-- 实时体征数据 -->
        <div class="ep-panel ep-vitals">
          <div class="ep-ph"><span class="ep-ph-bar"></span>实时体征数据<span class="ep-ph-link" @click="refresh">刷新</span></div>
          <div class="ep-vital-grid">
            <div class="ep-vital-card hr">
              <div class="ep-vc-label">心率</div>
              <div class="ep-vc-val"><span :class="hrClass(vitals.heartRate)">{{ vitals.heartRate || '--' }}</span> bpm</div>
              <div class="ep-vc-bar"><div :style="{ width: hrPct(vitals.heartRate) + '%' }" class="ep-vc-fill hr-fill"></div></div>
              <div class="ep-vc-range">正常 60-100 bpm</div>
            </div>
            <div class="ep-vital-card spo2">
              <div class="ep-vc-label">血氧</div>
              <div class="ep-vc-val"><span :class="spo2Class(vitals.bloodOxygen)">{{ vitals.bloodOxygen || '--' }}</span> %</div>
              <div class="ep-vc-bar"><div :style="{ width: spo2Pct(vitals.bloodOxygen) + '%' }" class="ep-vc-fill spo2-fill"></div></div>
              <div class="ep-vc-range">正常 ≥ 95%</div>
            </div>
            <div class="ep-vital-card temp">
              <div class="ep-vc-label">体温</div>
              <div class="ep-vc-val"><span :class="tempClass(vitals.temperature)">{{ fmtTemp(vitals.temperature) }}</span> °C</div>
              <div class="ep-vc-bar"><div :style="{ width: tempPct(vitals.temperature) + '%' }" class="ep-vc-fill temp-fill"></div></div>
              <div class="ep-vc-range">正常 36-37.3 °C</div>
            </div>
            <div class="ep-vital-card steps">
              <div class="ep-vc-label">今日步数</div>
              <div class="ep-vc-val"><span class="cyan">{{ exercise.todaySteps > 0 ? exercise.todaySteps : '--' }}</span> 步</div>
              <div class="ep-vc-bar"><div :style="{ width: Math.min(100, (exercise.todaySteps||0)/100) + '%' }" class="ep-vc-fill steps-fill"></div></div>
              <div class="ep-vc-range">目标 10,000 步</div>
            </div>
            <div class="ep-vital-card cals">
              <div class="ep-vc-label">今日卡路里</div>
              <div class="ep-vc-val"><span class="cyan">{{ exercise.todayCalories > 0 ? exercise.todayCalories : '--' }}</span> kcal</div>
              <div class="ep-vc-bar"><div :style="{ width: Math.min(100, (exercise.todayCalories||0)/20) + '%' }" class="ep-vc-fill cals-fill"></div></div>
              <div class="ep-vc-range">目标 2,000 kcal</div>
            </div>
          </div>
          <div class="ep-update-time">更新于：{{ lastUpdate }}</div>
        </div>

        <!-- 健康风险评估 -->
        <div class="ep-panel ep-risk">
          <div class="ep-ph"><span class="ep-ph-bar"></span>健康风险评估</div>
          <div class="ep-risk-grid">
            <div v-for="r in riskItems" :key="r.name" :class="['ep-risk-card', r.level]">
              <div class="ep-risk-icon">{{ r.icon }}</div>
              <div class="ep-risk-body">
                <div class="ep-risk-name">{{ r.name }}</div>
                <div class="ep-risk-bar-wrap">
                  <div class="ep-risk-bar-track">
                    <div class="ep-risk-bar-fill" :style="{ width: r.pct + '%', background: r.color }"></div>
                  </div>
                  <span :class="['ep-risk-label', r.level]">{{ r.levelText }}</span>
                  <span class="ep-risk-pct">{{ r.pct }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ECG实时波形 -->
        <div class="ep-panel ep-ecg">
          <div class="ep-ph"><span class="ep-ph-bar"></span>实时心电图</div>
          <HeartRateWave
            v-if="vitals.heartRate"
            :width="340" :height="90"
            :heartRate="vitals.heartRate"
            waveColor="#00ff88" :speed="2" :showWarning="false"
          />
          <div v-else class="ep-no-ecg">暂无心电数据</div>
        </div>

      </aside>
    </div>

    <section class="ep-panel ep-warning-band">
      <div class="ep-ph">
        <span class="ep-ph-bar"></span>近期预警轨迹
        <span class="ep-warn-count" v-if="warnings.length">({{ warnings.length }})</span>
      </div>
      <div class="ep-warning-band__body">
        <div class="ep-warning-band__meta">
          <div class="ep-warn-summary">
            <div v-for="item in recentWarningSummary" :key="item.key" :class="['ep-warn-pill', `tone-${item.tone}`]">
              <span class="ep-warn-pill-label">{{ item.label }}</span>
              <strong class="ep-warn-pill-value">{{ item.value }}</strong>
            </div>
          </div>
          <div class="ep-warn-footer">
            <span class="ep-warn-footnote">{{ recentWarningFootnote }}</span>
            <button type="button" class="ep-warn-more" @click="goWarningCenter">查看全部</button>
          </div>
        </div>
        <div class="ep-warning-band__list">
          <div v-if="warnings.length === 0" class="ep-empty-warn">暂无预警记录</div>
          <div v-else class="ep-warn-list">
            <div v-for="(w, idx) in recentWarnings" :key="(w.id || w.time) + '_' + idx" class="ep-warn-item">
              <span :class="['ep-wdot', w.handled ? 'done' : 'pend']"></span>
              <div class="ep-warn-copy">
                <span class="ep-wtype">{{ w.warningType || w.type || '--' }}</span>
                <span class="ep-wtime">{{ fmtTime(w.createTime || w.time) }}</span>
              </div>
              <span :class="['ep-wst', w.handled ? 'done' : 'pend']">{{ w.handled ? '已处理' : '未处理' }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import HeartRateWave from '@/components/HeartRateWave.vue'
import { useEmployeeProfilePage } from './use-employee-profile-page'

const {
  aiReportContent,
  aiReportHtml,
  aiReportLoading,
  aiReportVisible,
  calcAge,
  empInfo,
  exercise,
  fmtTime,
  fmtTemp,
  goArchive,
  goMineEntry,
  goReportCenter,
  goWorkbench,
  hrClass,
  hrPct,
  hrItems,
  isBpAbnormal,
  isBpDanger,
  isHrAbnormal,
  isHrDanger,
  isOnline,
  isPressureHigh,
  isSpo2Abnormal,
  isSpo2Danger,
  isTempAbnormal,
  isTempDanger,
  lastUpdate,
  loading,
  openAiReport,
  pendCount,
  printAiReport,
  profileInsightLines,
  profileSummaryCards,
  pressClass,
  recentWarningFootnote,
  recentWarnings,
  recentWarningSummary,
  refresh,
  riskItems,
  spo2Class,
  spo2Pct,
  spo2Items,
  tempClass,
  tempPct,
  trendStats,
  trendRef,
  vitalItems,
  vitals,
  warnings,
  warnItems,
  goWarningCenter
} = useEmployeeProfilePage()

const summaryMetricItems = computed(() => profileSummaryCards.value.map((card) => ({
  key: card.label,
  label: card.label,
  value: card.value,
  note: card.sub,
  tone: card.tone === 'danger'
    ? 'danger'
    : card.tone === 'warn'
      ? 'warning'
      : card.tone === 'safe'
        ? 'success'
        : 'primary'
})))
</script>

<style scoped>
@import './employee-profile.scss';
</style>

