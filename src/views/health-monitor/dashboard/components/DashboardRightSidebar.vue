<template>
  <aside class="dm-right">
    <div class="dm-panel dm-right-rank">
      <div class="dm-ph">
        <span class="dm-ph-bar"></span>
        <span class="dm-ph-title">异常人员排行</span>
        <span class="dm-ph-sub">{{ periodLabel }}累计</span>
      </div>
      <div class="dm-top5-list">
        <div
          v-for="(item, i) in top5DisplayData"
          :key="item.userCode || item.userName || i"
          class="dm-top5-row"
          style="cursor:pointer"
          @click="$emit('open-employee', item)"
        >
          <span class="dm-top5-rank" :class="'rk-' + (i + 1)">{{ i + 1 }}</span>
          <span class="dm-top5-name">{{ item.userName || item.name }}</span>
          <div class="dm-top5-bar-wrap">
            <div class="dm-top5-bar" :style="{ width: (item.count / top5Max * 100) + '%' }"></div>
          </div>
          <span class="dm-top5-val">{{ item.count }}</span>
        </div>
        <div v-if="!top5DisplayData.length" class="dm-empty">暂无数据</div>
      </div>
    </div>

    <health-tips :count="15" class="dm-right-tips" />

    <div class="dm-panel dm-right-warnrate">
      <div class="dm-ph">
        <span class="dm-ph-bar"></span>
        <span class="dm-ph-title">指标预警率分析</span>
        <span class="dm-ph-sub">{{ periodLabel }}触发预警人员占比</span>
      </div>
      <div class="dm-warn-stats">
        <div v-if="!warningRateList.length" class="dm-empty" style="padding:40px 20px;text-align:center;color:#4a6080;font-size:12px">
          暂无数据
        </div>
        <div
          v-for="item in warningRateList"
          :key="item.name"
          class="dm-warn-item"
          style="cursor:pointer"
          @click="goToWarningRecords(item)"
        >
          <div class="dm-warn-icon-wrap">
            <el-icon :size="15"><component :is="getWarningIcon(item.name)" /></el-icon>
          </div>
          <div class="dm-warn-body">
            <div class="dm-warn-top">
              <span class="dm-warn-name">{{ item.name }}</span>
              <span class="dm-warn-pct" :style="{ color: getWarnColor(item.rate) }">{{ item.rate }}%</span>
            </div>
            <div class="dm-warn-bar-bg">
              <div class="dm-warn-bar-fill" :style="{ width: item.rate + '%', background: getWarnGradient(item.rate) }"></div>
            </div>
          </div>
          <div class="dm-warn-tag" :class="getWarnTagClass(item.rate)">{{ getWarnTagLabel(item.rate) }}</div>
        </div>
      </div>
    </div>

    <div class="dm-panel dm-right-preshift" style="cursor:pointer" @click="goMineEntry">
      <div class="dm-ph">
        <span class="dm-ph-bar" style="background:#38ef7d"></span>
        <span class="dm-ph-title">班前健康准入</span>
        <span class="dm-ph-sub">点击查看准入名单</span>
      </div>
      <div class="dm-preshift-body">
        <div class="dm-ps-ring-wrap">
          <svg viewBox="0 0 80 80" class="dm-ps-ring">
            <circle cx="40" cy="40" r="32" fill="none" stroke="#1a2a4d" stroke-width="8"/>
            <circle
              cx="40"
              cy="40"
              r="32"
              fill="none"
              :stroke="preShiftData.preShiftRate >= 90 ? '#38ef7d' : preShiftData.preShiftRate >= 70 ? '#ffd200' : '#ff5252'"
              stroke-width="8"
              stroke-linecap="round"
              :stroke-dasharray="`${(preShiftData.preShiftRate || 0) * 2.01} 201`"
              stroke-dashoffset="50"
            />
          </svg>
          <div class="dm-ps-ring-inner">
            <div class="dm-ps-rate" :style="{ color: preShiftData.preShiftRate >= 90 ? '#38ef7d' : preShiftData.preShiftRate >= 70 ? '#ffd200' : '#ff5252' }">
              {{ preShiftData.preShiftRate !== null ? preShiftData.preShiftRate + '%' : '--' }}
            </div>
            <div class="dm-ps-rate-label">达标率</div>
          </div>
        </div>
        <div class="dm-ps-stats">
          <div class="dm-ps-stat">
            <span class="dm-ps-stat-val">{{ preShiftData.totalToday }}</span>
            <span class="dm-ps-stat-label">今日检测</span>
          </div>
          <div class="dm-ps-stat dm-ps-ok">
            <span class="dm-ps-stat-val">{{ preShiftData.qualifiedCount }}</span>
            <span class="dm-ps-stat-label">准入通过</span>
          </div>
          <div class="dm-ps-stat dm-ps-fail">
            <span class="dm-ps-stat-val">{{ preShiftData.failedCount }}</span>
            <span class="dm-ps-stat-label">禁止入井</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dm-panel dm-right-ai">
      <div class="dm-ph">
        <span class="dm-ph-bar dm-ph-bar-ai"></span>
        <span class="dm-ph-title">AI 全矿健康分析</span>
        <button class="dm-ai-btn" :disabled="mineAiLoading" @click="$emit('toggle-ai', false)">
          {{ mineAiLoading ? '分析中…' : (mineAiReport ? '刷新' : '生成分析') }}
        </button>
      </div>
      <div v-if="mineAiLoading" class="dm-ai-loading">🤖 DeepSeek 分析中，请稍候…</div>
      <div v-else-if="mineAiReport" class="dm-ai-preview" @click="$emit('show-ai')">
        {{ mineAiReport.replace(/#+\s*/g, '').slice(0, 120) }}…
        <span class="dm-ai-more">展开全文 ›</span>
      </div>
      <div v-else class="dm-ai-empty">点击「生成分析」获取全矿 AI 健康报告</div>
    </div>
  </aside>
</template>

<script>
import { defineComponent } from 'vue'
import { useRouter } from 'vue-router'
import HealthTips from '@/components/HealthTips.vue'

export default defineComponent({
  name: 'DashboardRightSidebar',
  components: { HealthTips },
  props: {
    periodLabel: { type: String, required: true },
    top5DisplayData: { type: Array, default: () => [] },
    top5Max: { type: Number, default: 1 },
    warningRateList: { type: Array, default: () => [] },
    preShiftData: { type: Object, default: () => ({ totalToday: 0, qualifiedCount: 0, failedCount: 0, preShiftRate: null }) },
    mineAiReport: { type: String, default: '' },
    mineAiLoading: { type: Boolean, default: false }
  },
  emits: ['open-employee', 'toggle-ai', 'show-ai'],
  setup() {
    const router = useRouter()

    const getWarningIcon = (name) => {
      const map = { '压力预警率': 'MagicStick', '体温预警率': 'Sunny', '心率预警率': 'Monitor', '血氧预警率': 'FirstAidKit' }
      return map[name] || 'Warning'
    }
    const getWarnColor = (rate) => (rate >= 20 ? '#ff5252' : rate >= 10 ? '#ffd200' : '#38ef7d')
    const getWarnGradient = (rate) => {
      return rate >= 20
        ? 'linear-gradient(90deg,#ff5252,#ff1744)'
        : rate >= 10
          ? 'linear-gradient(90deg,#ffd200,#ff9800)'
          : 'linear-gradient(90deg,#00d4ff,#38ef7d)'
    }
    const getWarnTagClass = (rate) => (rate >= 20 ? 'tag-danger' : rate >= 10 ? 'tag-warn' : 'tag-ok')
    const getWarnTagLabel = (rate) => (rate >= 20 ? '偏高' : rate >= 10 ? '注意' : '正常')
    const goToWarningRecords = (item) => {
      const map = { '压力预警率': '压力', '体温预警率': '体温', '心率预警率': '心率', '血氧预警率': '血氧' }
      const type = map[item.name]
      router.push({ path: '/alert-management/records', query: type ? { warningType: type } : {} })
    }
    const goMineEntry = () => router.push('/health-monitor/mine-entry')

    return {
      getWarningIcon,
      getWarnColor,
      getWarnGradient,
      getWarnTagClass,
      getWarnTagLabel,
      goMineEntry,
      goToWarningRecords
    }
  }
})
</script>

<style scoped lang="scss">
@import '../dashboard.scss';
</style>

