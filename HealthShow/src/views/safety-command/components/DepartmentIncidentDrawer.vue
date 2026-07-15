<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    direction="rtl"
    size="min(620px, 100vw)"
    class="department-incident-drawer"
    :title="`${departmentName} · 部门事件`"
  >
    <div class="dept-incident-body">
      <div class="dept-incident-scope">部门预警来自今日统计；其余数量与列表基于当前已加载开放事件</div>
      <div class="dept-incident-summary">
        <div><span>今日部门预警</span><strong>{{ departmentTotal }}</strong></div>
        <div><span>已加载事件</span><strong>{{ departmentEvents.length }}</strong></div>
        <div class="tone-danger"><span>高危</span><strong>{{ criticalCount }}</strong></div>
        <div class="tone-warning"><span>未分派</span><strong>{{ unassignedCount }}</strong></div>
        <div class="tone-danger"><span>已超时</span><strong>{{ overdueCount }}</strong></div>
      </div>

      <div v-if="departmentEvents.length" class="dept-incident-list">
        <article v-for="event in departmentEvents" :key="`${event.id}-${event.occurredAt}`" class="dept-incident-item">
          <button type="button" class="dept-incident-main" @click="$emit('show-event', event)">
            <span class="dept-incident-title">{{ event.user || '未知人员' }} · {{ event.type || '预警' }}</span>
            <span>{{ event.time || '刚刚' }} · {{ event.owner || '未分派' }}</span>
            <span>{{ event.location || '未接入定位' }} · {{ event.slaStatus === 'OVERDUE' ? '已超时' : '处置中' }}</span>
          </button>
          <button type="button" class="dept-incident-person" @click="$emit('show-person', event)">人员</button>
          <button type="button" class="dept-incident-handle" @click="$emit('show-event', event)">处置</button>
        </article>
      </div>
      <el-empty v-else :image-size="64" description="当前已加载范围内无该部门开放事件" />
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  department: { type: [Object, String], default: null },
  events: { type: Array, default: () => [] }
})

defineEmits(['update:visible', 'show-event', 'show-person'])

const departmentName = computed(() => typeof props.department === 'string'
  ? props.department
  : (props.department?.name || '未分组'))
const departmentTotal = computed(() => typeof props.department === 'object'
  ? Number(props.department?.warnings || props.department?.abnormal || 0)
  : departmentEvents.value.length)
const departmentEvents = computed(() => props.events.filter((event) => (event.dept || '未分组') === departmentName.value))
const criticalCount = computed(() => departmentEvents.value.filter((event) => ['critical', 'high'].includes(event.level)).length)
const unassignedCount = computed(() => departmentEvents.value.filter((event) => !event.owner || event.owner === '未分派').length)
const overdueCount = computed(() => departmentEvents.value.filter((event) => event.slaStatus === 'OVERDUE').length)
</script>

<style scoped lang="scss">
.dept-incident-body { display: grid; gap: 14px; }
.dept-incident-scope { color: #6e94b0; font-size: 12px; }
.dept-incident-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.dept-incident-summary > div { min-height: 82px; display: grid; align-content: center; gap: 6px; padding: 12px; border: 1px solid rgba(0,200,255,.16); border-radius: 8px; background: rgba(0,200,255,.045); }
.dept-incident-summary span { color: #6e94b0; font-size: 12px; }
.dept-incident-summary strong { color: #dff0ff; font: 700 24px/1 'JetBrains Mono','Courier New',monospace; }
.dept-incident-summary .tone-danger { border-color: rgba(255,59,59,.28); strong { color: #ff3b3b; } }
.dept-incident-summary .tone-warning { border-color: rgba(255,140,0,.26); strong { color: #ff8c00; } }
.dept-incident-list { display: grid; gap: 8px; }
.dept-incident-item { display: grid; grid-template-columns: minmax(0, 1fr) auto auto; align-items: center; gap: 8px; padding: 12px; border: 1px solid rgba(255,140,0,.2); border-radius: 8px; background: rgba(255,255,255,.025); }
.dept-incident-main { min-width: 0; display: grid; gap: 5px; text-align: left; border: 0; background: transparent; color: #6e94b0; cursor: pointer; }
.dept-incident-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #dff0ff; font-weight: 700; }
.dept-incident-person, .dept-incident-handle { min-height: 32px; padding: 0 10px; border-radius: 5px; border: 1px solid rgba(0,200,255,.28); background: rgba(0,200,255,.07); color: #00c8ff; cursor: pointer; }
.dept-incident-handle { border-color: rgba(255,140,0,.3); color: #ff8c00; background: rgba(255,140,0,.08); }
@media (max-width: 520px) {
  .dept-incident-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dept-incident-item { grid-template-columns: minmax(0, 1fr) auto; }
  .dept-incident-handle { grid-column: 2; }
}
</style>
