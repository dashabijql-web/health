<template>
  <section class="sc-war-support">
    <div class="sc-panel sc-closure-panel panel-enter" style="--delay:.16s">
      <div class="sc-panel-head">
        <div>
          <h2>今日处置闭环</h2>
          <span class="sc-panel-subtitle">今日预警统一口径</span>
        </div>
        <strong>{{ warningHandledRate }}%</strong>
      </div>
      <div class="resp-body">
        <div class="resp-cards">
          <div class="rc rc-p">
            <div class="rc-n">{{ pendingCount }}</div>
            <div class="rc-l">待处置</div>
          </div>
          <div class="rc rc-d">
            <div class="rc-n">{{ handledCount }}</div>
            <div class="rc-l">已处置</div>
          </div>
        </div>
        <div class="closure-progress">
          <div class="bar-lbl">
            <span>处置率</span>
            <strong>{{ warningHandledRate }}%</strong>
          </div>
          <div class="bar-t">
            <div class="bar-d" :style="{ width: warningHandledRate + '%' }">
              <span>{{ warningHandledRate }}%</span>
            </div>
            <div class="bar-p" :style="{ left: warningHandledRate + '%', width: (100 - warningHandledRate) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="sc-panel sc-trend-panel panel-enter" style="--delay:.2s">
      <div class="sc-panel-head">
        <div>
          <h2>近7日预警趋势</h2>
          <span class="sc-panel-subtitle">按自然日汇总</span>
        </div>
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
          <circle
            v-for="(pt, i) in trendPath.pts" :key="i"
            :cx="pt.x" :cy="pt.y" r="4"
            fill="#ff3b3b" stroke="rgba(255,59,59,.25)" stroke-width="7"
            class="trend-dot" :style="{ '--td': (i * 0.08) + 's' }"
          />
          <text v-for="(d, i) in warningTrend" :key="i" :x="i / Math.max(warningTrend.length-1,1) * 376 + 2" y="188" fill="#3a5268" font-size="11">{{ (d.date||d.stat_date||'').slice(5) }}</text>
        </svg>
        <div v-else class="panel-empty">暂无趋势数据</div>
        <div class="trend-stats">
          <div class="ts-card">
            <div class="ts-v" style="color:#00c8ff">{{ trend7dayTotal }}</div>
            <div class="ts-l">近7日总量</div>
          </div>
          <div class="ts-card">
            <div class="ts-v" :style="{ color: trendChange > 0 ? '#ffd600' : '#00e676' }">{{ trendChange > 0 ? '▲' : trendChange < 0 ? '▼' : '—' }}{{ trendChange === 0 ? '' : Math.abs(trendChange) + '%' }}</div>
            <div class="ts-l">较昨日变化</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  handledCount: { type: Number, default: 0 },
  pendingCount: { type: Number, default: 0 },
  trend7dayTotal: { type: Number, default: 0 },
  trendChange: { type: Number, default: 0 },
  trendPath: { type: Object, default: null },
  warningHandledRate: { type: Number, default: 0 },
  warningTrend: { type: Array, default: () => [] }
})
</script>

<style scoped lang="scss">
@import '../safety-command.scss';
</style>
