<template>
  <section class="sc-intelligence-grid">
    <article class="sc-panel sc-coverage-panel panel-enter" style="--delay:.14s">
      <div class="sc-panel-head">
        <div>
          <h2>设备监测覆盖</h2>
          <span class="sc-panel-subtitle">指挥摘要实时口径</span>
        </div>
        <strong>{{ deviceCoverage.onlineRate ?? '--' }}{{ deviceCoverage.onlineRate == null ? '' : '%' }}</strong>
      </div>
      <div class="sc-coverage-body">
        <div class="sc-coverage-metrics">
          <div v-for="item in deviceCoverage.cards" :key="item.key" :class="['sc-coverage-metric', `tone-${item.tone}`]">
            <strong>{{ item.display }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
        <div class="sc-capability-list">
          <div v-for="item in deviceCoverage.capabilities" :key="item.key" :class="['sc-capability-row', `tone-${item.tone}`]">
            <span>{{ item.label }}</span>
            <strong>{{ item.display }}</strong>
            <em>{{ item.message }}</em>
          </div>
        </div>
      </div>
    </article>

    <article class="sc-panel sc-risk-people-panel panel-enter" style="--delay:.18s">
      <div class="sc-panel-head">
        <div>
          <h2>重点风险人员</h2>
          <span class="sc-panel-subtitle">开放事件按人员去重</span>
        </div>
        <strong>{{ riskPersons.length }}</strong>
      </div>
      <div class="sc-risk-people-list">
        <button
          v-for="(person, index) in visibleRiskPersons"
          :key="person.userCode || person.name"
          type="button"
          class="sc-risk-person-row"
          @click="emit('show-person', person)"
        >
          <span class="sc-risk-person-rank">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="sc-risk-person-main">
            <strong>{{ person.name }}</strong>
            <em>{{ person.dept || '未分配部门' }} · {{ formatTypes(person.types) }}</em>
          </span>
          <span class="sc-risk-person-count">{{ person.count }}<small>条</small></span>
        </button>
        <div v-if="visibleRiskPersons.length === 0" class="panel-empty">当前已加载事件中无风险人员</div>
      </div>
      <div class="sc-panel-scope">基于已加载 {{ loadedCount }} 条开放事件，不代表全库历史风险排名</div>
    </article>

    <article class="sc-panel sc-event-signal-panel panel-enter" style="--delay:.22s">
      <div class="sc-panel-head">
        <div>
          <h2>事件结构与处置信号</h2>
          <span class="sc-panel-subtitle">当前已加载 {{ loadedCount }} 条开放事件</span>
        </div>
      </div>
      <div class="sc-event-signal-body">
        <div class="sc-event-type-list">
          <div v-for="item in eventTypeItems" :key="item.key" class="sc-event-type-row">
            <span>{{ item.label }}</span>
            <div class="sc-event-type-track"><i :class="`tone-${item.tone}`" :style="{ width: item.percent + '%' }"></i></div>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
        <div class="sc-workflow-signals">
          <div v-for="item in workflowSignals" :key="item.key" :class="['sc-workflow-signal', `tone-${item.tone}`]">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>
      <div class="sc-panel-scope">类型和状态只描述当前队列样本；权威待办总数以页头为准</div>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  deviceCoverage: { type: Object, required: true },
  eventTypeItems: { type: Array, default: () => [] },
  loadedCount: { type: Number, default: 0 },
  riskPersons: { type: Array, default: () => [] },
  workflowSignals: { type: Array, default: () => [] }
})

const emit = defineEmits(['show-person'])
const visibleRiskPersons = computed(() => props.riskPersons.slice(0, 6))
const formatTypes = (types) => (types || []).slice(0, 2).join('、') || '风险预警'
</script>

<style scoped lang="scss">
@import '../safety-command.scss';
</style>
