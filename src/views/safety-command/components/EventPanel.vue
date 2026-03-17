<template>
  <div class="panel ev-panel">
    <div class="ph">
      <span class="pt">
        <span class="pt-bar red-bar"></span>
        紧急事件
        <span class="pt-cnt">{{ events.length }}</span>
      </span>
      <div class="tabs">
        <span v-for="f in filterTabs" :key="f.type" :class="['tb', currentFilter===f.type&&'on']" @click="currentFilter=f.type">
          {{ f.label }}({{ f.count }})
        </span>
      </div>
    </div>
    <div ref="trendRef" class="ev-trend" v-if="trendData.length > 0"></div>
    <div class="ev-list">
      <div v-if="filteredEvents.length === 0" class="ev-empty">✅ 暂无事件</div>
      <div
        v-for="ev in filteredEvents" :key="ev.id"
        :class="['ev', (ev.eventType==='sos'||ev.eventType==='fall') ? 'ev-crit' : 'ev-warn']"
        @click="$emit('showDetail', ev)"
      >
        <div :class="['ev-side', (ev.eventType==='sos'||ev.eventType==='fall') ? '' : 'side-w']"></div>
        <div class="ev-body">
          <div class="ev-top">
            <span class="ev-type">{{ ev.icon }} {{ ev.type }}</span>
            <span v-if="ev.durationMinutes" :class="['ev-dur', (ev.eventType==='sos'||ev.eventType==='fall')?'':'dur-w', ev.durationMinutes>5&&'dur-crit']">{{ ev.durationMinutes }} MIN</span>
          </div>
          <div class="ev-meta">
            <span @click.stop="$emit('showPerson', ev)" class="link-txt">{{ ev.user }}</span>
            <span> · </span>
            <span @click.stop="$emit('showDept', ev.dept)" class="link-txt">{{ ev.dept || ev.location }}</span>
          </div>
        </div>
        <div class="ev-right">
          <span class="ev-age">{{ ev.time }}</span>
          <span class="ev-act" @click.stop="$emit('handle', ev)">处理</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import * as echarts from '@/utils/echarts-setup'

const props = defineProps({
  events: { type: Array, default: () => [] },
  trendData: { type: Array, default: () => [] }
})
defineEmits(['showAll','showDetail','showPerson','showDept','handle'])

const currentFilter = ref('all')
const trendRef = ref(null)
let trendChart = null

const filterTabs = computed(() => {
  const e = props.events
  return [
    { type:'all',      label:'全部',  count: e.length },
    { type:'sos',      label:'SOS',   count: e.filter(x=>x.eventType==='sos').length },
    { type:'fall',     label:'跌倒',  count: e.filter(x=>x.eventType==='fall').length },
    { type:'static',   label:'静止',  count: e.filter(x=>x.eventType==='static').length },
    { type:'abnormal', label:'异常',  count: e.filter(x=>x.eventType==='abnormal').length }
  ]
})

const filteredEvents = computed(() =>
  currentFilter.value === 'all' ? props.events : props.events.filter(e => e.eventType === currentFilter.value)
)

const buildTrendChart = () => {
  if (!trendRef.value || props.trendData.length === 0) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    grid:{left:0,right:0,top:2,bottom:0},
    xAxis:{type:'category',data:props.trendData.map(d=>d.date||d.label||''),show:false},
    yAxis:{type:'value',show:false},
    series:[{type:'line',data:props.trendData.map(d=>d.count||d.value||0),smooth:true,symbol:'none',lineStyle:{color:'#ff4757',width:1.5},areaStyle:{color:'rgba(255,71,87,.12)'}}],
    tooltip:{trigger:'axis',backgroundColor:'rgba(10,22,42,.9)',borderColor:'rgba(255,71,87,.3)',textStyle:{color:'#fff',fontSize:10},formatter:(p)=>`${p[0].name}: ${p[0].value}条`}
  })
}

watch(() => props.trendData, async () => { await nextTick(); buildTrendChart() }, { deep:true })
onUnmounted(() => { if (trendChart) trendChart.dispose() })
</script>

<style scoped lang="scss">
$cyan:#00d4ff; $red:#ff4757; $orange:#ff6b35; $yellow:#ffd32a; $green:#2ed573;
$panel:rgba(10,22,42,.82); $border2:rgba(0,212,255,.07);
$dim:rgba(255,255,255,.45); $dim2:rgba(255,255,255,.22);

.ev-panel { flex:1.5; }

.panel { background:$panel; border:1px solid $border2; border-radius:5px; padding:10px 12px; display:flex; flex-direction:column; min-height:0; backdrop-filter:blur(6px); position:relative; overflow:hidden;
  &::before { content:''; position:absolute; top:0; left:14px; right:14px; height:1px; background:linear-gradient(90deg,transparent,rgba($cyan,.15),transparent); }
}
.ph { display:flex; justify-content:space-between; align-items:center; padding-bottom:8px; margin-bottom:8px; flex-shrink:0; border-bottom:1px solid rgba($cyan,.08); }
.pt { font-size:11px; font-weight:600; color:#fff; display:flex; align-items:center; gap:7px; letter-spacing:.5px; text-transform:uppercase; }
.pt-bar { width:2px; height:12px; border-radius:1px; flex-shrink:0; }
.red-bar { background:$red; box-shadow:0 0 6px $red; }
.pt-cnt { font-family:'JetBrains Mono','Courier New',monospace; color:$red; font-size:12px; }
.tabs { display:flex; gap:2px; }
.tb { font-size:9px; padding:2px 7px; border-radius:2px; border:1px solid rgba($cyan,.12); color:$dim; cursor:pointer; transition:all .15s;
  &.on, &:hover { background:rgba($cyan,.1); border-color:rgba($cyan,.35); color:$cyan; }
}

.ev-trend { height:32px; flex-shrink:0; margin-bottom:4px; }
.ev-list { flex:1; overflow-y:auto; min-height:0; display:flex; flex-direction:column; gap:4px; }
.ev-empty { text-align:center; padding:20px; color:rgba($green,.7); font-size:11px; }

.ev {
  display:flex; align-items:stretch; border-radius:3px; overflow:hidden;
  border:1px solid rgba($red,.12); background:rgba($red,.03);
  cursor:pointer; transition:background .15s; flex-shrink:0;
  &.ev-warn { border-color:rgba($orange,.12); background:rgba($orange,.03); }
  &:hover { background:rgba($red,.08); }
  &.ev-warn:hover { background:rgba($orange,.08); }
}
.ev-side {
  width:3px; flex-shrink:0;
  background:linear-gradient(180deg,$red,rgba($red,.3));
}
.side-w { background:linear-gradient(180deg,$orange,rgba($orange,.3)); }

.ev-body { flex:1; padding:7px 9px; display:flex; flex-direction:column; gap:3px; }
.ev-top  { display:flex; align-items:center; gap:6px; }
.ev-type { font-size:11px; font-weight:600; color:#fff; letter-spacing:.3px; }
.ev-dur  {
  font-size:8px; padding:1px 5px; border-radius:1px; font-family:'JetBrains Mono','Courier New',monospace; letter-spacing:.5px;
  background:rgba($red,.15); color:#ff8090; border:1px solid rgba($red,.2);
  &.dur-w  { background:rgba($orange,.15); color:#ffaa55; border-color:rgba($orange,.2); }
  &.dur-crit { color:$red; animation:blink 1s infinite; }
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:.3} }

.ev-meta { font-size:10px; color:$dim; display:flex; gap:3px; align-items:center; }
.link-txt { cursor:pointer; &:hover { color:$cyan; } }

.ev-right { padding:7px 9px; display:flex; flex-direction:column; align-items:flex-end; justify-content:space-between; flex-shrink:0; }
.ev-age { font-size:9px; color:$dim2; font-family:'JetBrains Mono','Courier New',monospace; }
.ev-act {
  font-size:9px; padding:2px 8px; border-radius:1px; cursor:pointer; letter-spacing:.5px;
  background:rgba($cyan,.07); border:1px solid rgba($cyan,.25); color:$cyan;
  transition:all .15s;
  &:hover { background:rgba($cyan,.18); }
}
</style>
