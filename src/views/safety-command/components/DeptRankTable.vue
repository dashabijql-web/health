<template>
  <div class="panel rank-panel">
    <div class="ph">
      <span class="pt">
        <span class="pt-bar green-bar"></span>
        部门安全排行榜
      </span>
      <div class="rank-ctrl">
        <span class="lg lg-g">● 正常</span>
        <span class="lg lg-y">● 预警</span>
        <span class="lg lg-r">● 危险</span>
        <button :class="['scroll-btn', autoScroll&&'active']" @click="toggleAutoScroll">{{ autoScroll ? '⏸' : '▶' }}</button>
      </div>
    </div>
    <div class="rank-scroll" ref="rankingTableRef">
      <table>
        <thead>
          <tr>
            <th>#</th><th>部门</th><th>在线/总</th>
            <th>健康率</th><th>SOS</th><th>跌倒</th><th>预警</th><th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(dept, i) in departments" :key="dept.id" :class="getDeptRowClass(dept)" @click="$emit('showDept', dept)">
            <td><span :class="['rn', i===0?'rn-1':i===1?'rn-2':i===2?'rn-3':'rn-n']">{{ String(i+1).padStart(2,'0') }}</span></td>
            <td class="td-name">{{ dept.name }}</td>
            <td class="mono dim">{{ dept.online }}/{{ dept.total }}</td>
            <td>
              <div class="hb">
                <div class="hb-tr">
                  <div class="hb-fl" :style="{ width:(dept.healthRate||0)+'%', background:healthColor(dept.healthRate) }"></div>
                </div>
                <span class="hb-v" :style="{ color:healthColor(dept.healthRate) }">{{ dept.healthRate }}%</span>
              </div>
            </td>
            <td><span v-if="dept.sos>0" class="an an-r">{{ dept.sos }}</span><span v-else class="dim2">—</span></td>
            <td><span v-if="dept.fall>0" class="an an-o">{{ dept.fall }}</span><span v-else class="dim2">—</span></td>
            <td><span v-if="(dept.static+dept.abnormal)>0" class="an an-y">{{ dept.static+dept.abnormal }}</span><span v-else class="dim2">—</span></td>
            <td><span :class="['st', dept.status]">{{ dept.statusText }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

defineProps({ departments: { type: Array, default: () => [] } })
defineEmits(['showDetail','showDept'])

const autoScroll = ref(false)
const rankingTableRef = ref(null)
let scrollTimer = null

const toggleAutoScroll = () => {
  autoScroll.value = !autoScroll.value
  autoScroll.value ? startScroll() : stopScroll()
}
const startScroll = () => {
  if (scrollTimer) return
  scrollTimer = setInterval(() => {
    const el = rankingTableRef.value
    if (!el) return
    el.scrollTop = el.scrollTop + el.clientHeight >= el.scrollHeight - 5 ? 0 : el.scrollTop + 1
  }, 50)
}
const stopScroll = () => { if (scrollTimer) { clearInterval(scrollTimer); scrollTimer = null } }

const getDeptRowClass = (d) => d.sos>0||d.fall>0 ? 'tr-danger' : d.static>0||d.abnormal>1 ? 'tr-warn' : ''
const healthColor = (r) => r >= 90 ? '#2ed573' : r >= 75 ? '#ffd32a' : '#ff4757'

onUnmounted(() => stopScroll())
</script>

<style scoped lang="scss">
$cyan:#00d4ff; $red:#ff4757; $orange:#ff6b35; $yellow:#ffd32a; $green:#2ed573;
$panel:rgba(10,22,42,.82); $border2:rgba(0,212,255,.07);
$dim:rgba(255,255,255,.42); $dim2:rgba(255,255,255,.2);
$mono:'JetBrains Mono','Courier New',monospace;

.rank-panel { overflow:hidden; }

.panel { background:$panel; border:1px solid $border2; border-radius:5px; padding:10px 12px; display:flex; flex-direction:column; min-height:0; backdrop-filter:blur(6px); position:relative; overflow:hidden;
  &::before { content:''; position:absolute; top:0; left:14px; right:14px; height:1px; background:linear-gradient(90deg,transparent,rgba($cyan,.15),transparent); }
}
.ph { display:flex; justify-content:space-between; align-items:center; padding-bottom:8px; margin-bottom:8px; flex-shrink:0; border-bottom:1px solid rgba($cyan,.08); }
.pt { font-size:11px; font-weight:600; color:#fff; display:flex; align-items:center; gap:7px; letter-spacing:.5px; text-transform:uppercase; }
.pt-bar { width:2px; height:12px; border-radius:1px; flex-shrink:0; }
.green-bar { background:$green; box-shadow:0 0 6px $green; }

.rank-ctrl { display:flex; align-items:center; gap:8px; }
.lg { font-size:8px; color:rgba(255,255,255,.4); letter-spacing:.2px; }
.lg-g { color:rgba($green,.7); } .lg-y { color:rgba($yellow,.7); } .lg-r { color:rgba($red,.7); }
.scroll-btn { font-size:9px; padding:1px 6px; border-radius:2px; background:rgba($cyan,.07); border:1px solid rgba($cyan,.2); color:$dim; cursor:pointer; font-family:inherit; transition:all .15s;
  &.active { background:rgba($cyan,.18); border-color:rgba($cyan,.45); color:$cyan; }
}

.rank-scroll { flex:1; overflow-y:auto; overflow-x:auto; min-height:0; }
table { width:100%; min-width:480px; border-collapse:collapse; }
thead { position:sticky; top:0; background:rgba(10,22,42,.98); z-index:1; }
th { padding:4px 6px; font-size:8px; font-weight:700; color:rgba(255,255,255,.45); text-align:center; border-bottom:1px solid rgba($cyan,.07); letter-spacing:.8px; text-transform:uppercase; font-family:$mono; }
td { padding:5px 6px; font-size:10px; text-align:center; color:rgba(255,255,255,.88); border-bottom:1px solid rgba(255,255,255,.025); }
tbody tr { cursor:pointer; transition:background .1s; &:hover { background:rgba($cyan,.04); } }
.tr-danger { background:rgba($red,.05); }
.tr-warn   { background:rgba($yellow,.04); }
.td-name   { text-align:left; font-weight:600; font-size:10px; }
.mono { font-family:$mono; }
.dim  { color:$dim; }
.dim2 { color:$dim2; }

.rn { display:inline-block; width:20px; height:20px; line-height:20px; border-radius:50%; font-size:9px; font-weight:700; text-align:center; font-family:$mono; background:rgba($cyan,.15); color:$cyan; }
.rn-1 { background:linear-gradient(135deg,#FFD700,#FFA500); color:#000; box-shadow:0 0 8px rgba(255,215,0,.4); }
.rn-2 { background:linear-gradient(135deg,#C0C0C0,#A0A0A0); color:#000; }
.rn-3 { background:linear-gradient(135deg,#CD7F32,#B87333); color:#fff; }
.rn-n { background:rgba(255,255,255,.08); color:$dim; }

.hb { display:flex; align-items:center; gap:4px; }
.hb-tr { flex:1; height:3px; border-radius:2px; background:rgba(255,255,255,.06); overflow:hidden; min-width:30px; }
.hb-fl { height:100%; border-radius:2px; transition:width .4s; }
.hb-v  { font-size:9px; font-family:$mono; min-width:28px; text-align:right; font-weight:600; }

.an { font-weight:700; } .an-r { color:$red; } .an-o { color:$orange; } .an-y { color:$yellow; }
.st { padding:1px 5px; border-radius:2px; font-size:9px;
  &.safe    { background:rgba($green,.18); color:$green; }
  &.warning { background:rgba($yellow,.18); color:$yellow; }
  &.danger  { background:rgba($red,.18); color:$red; }
}
</style>
