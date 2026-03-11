<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><Setting /></el-icon>
        <div>
          <h1 class="main-title">告警阈值配置</h1>
          <p class="sub-title">配置各项生理指标的正常、预警、危险范围</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <div class="risk-tabs">
      <button v-for="tab in riskTabs" :key="tab.value"
              class="risk-tab" :class="{ active: activeRisk === tab.value }"
              @click="activeRisk = tab.value">
        {{ tab.label }}
      </button>
    </div>

    <div v-loading="loading" class="config-grid">
      <div v-for="item in filteredConfigList" :key="item.id" class="config-card" :class="{ 'is-disabled': !item.enabled }">
        <div class="card-top">
          <div class="card-info">
            <span class="card-emoji">{{ getEmoji(item.configType) }}</span>
            <div>
              <div class="card-name">{{ item.configName }}</div>
              <div class="card-unit">单位: {{ item.unit }}</div>
            </div>
          </div>
          <el-switch :model-value="!!item.enabled" active-color="#38ef7d" inactive-color="#4a5578" @change="handleToggle(item)" />
        </div>

        <div class="range-bar-wrap">
          <div class="range-legend">
            <span class="legend-item"><span class="legend-dot critical"></span>危险</span>
            <span class="legend-item"><span class="legend-dot warn"></span>预警</span>
            <span class="legend-item"><span class="legend-dot normal"></span>正常</span>
          </div>
          <div class="range-bar">
            <div class="range-segment critical-low" :style="{ width: calcWidth(item, item.criticalLow, item.warnLow) }">
              <span class="range-label">{{ item.criticalLow }}</span>
            </div>
            <div class="range-segment warn-low" :style="{ width: calcWidth(item, item.warnLow, item.normalMin) }">
              <span class="range-label">{{ item.warnLow }}</span>
            </div>
            <div class="range-segment normal" :style="{ width: calcWidth(item, item.normalMin, item.normalMax) }">
              <span class="range-label">{{ item.normalMin }}-{{ item.normalMax }}</span>
            </div>
            <div class="range-segment warn-high" :style="{ width: calcWidth(item, item.normalMax, item.warnHigh) }">
              <span class="range-label">{{ item.warnHigh }}</span>
            </div>
            <div class="range-segment critical-high" :style="{ width: calcWidth(item, item.warnHigh, item.criticalHigh) }">
              <span class="range-label">{{ item.criticalHigh }}</span>
            </div>
          </div>
        </div>

        <div class="card-bottom">
          <el-button type="primary" size="small" @click="openEdit(item)"><el-icon><Edit /></el-icon> 编辑阈值</el-button>
        </div>
      </div>
      <div v-if="!loading && filteredConfigList.length === 0" class="config-empty">
        <el-icon size="36" color="#2d3561"><Setting /></el-icon>
        <p>该风险等级暂无告警配置</p>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="`编辑 ${editForm.configName} 阈值`" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="editForm" :rules="formRules" label-width="100px" class="form-body">
        <div class="range-section">
          <div class="section-header normal-header"><span class="section-dot normal"></span>正常范围</div>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="最小值" prop="normalMin"><el-input-number v-model="editForm.normalMin" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="最大值" prop="normalMax"><el-input-number v-model="editForm.normalMax" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          </el-row>
        </div>
        <div class="range-section">
          <div class="section-header warn-header"><span class="section-dot warn"></span>预警范围</div>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="低值" prop="warnLow"><el-input-number v-model="editForm.warnLow" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="高值" prop="warnHigh"><el-input-number v-model="editForm.warnHigh" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          </el-row>
        </div>
        <div class="range-section">
          <div class="section-header critical-header"><span class="section-dot critical"></span>危险范围</div>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="低值" prop="criticalLow"><el-input-number v-model="editForm.criticalLow" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="高值" prop="criticalHigh"><el-input-number v-model="editForm.criticalHigh" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { Timer, Edit, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlertConfigList, updateAlertConfig, toggleAlertConfig } from '@/api/alert-config'

const currentTime = ref('')
const updateTime = () => { currentTime.value = new Date().toLocaleString('zh-CN') }
updateTime()
const clockTimer = setInterval(updateTime, 1000)
onBeforeUnmount(() => clearInterval(clockTimer))

const riskTabs = [
  { label: '默认（无工种）', value: null },
  { label: '低危岗位', value: 1 },
  { label: '中危岗位', value: 2 },
  { label: '高危岗位', value: 3 }
]
const activeRisk = ref(null)

const loading = ref(false)
const configList = ref([])

const filteredConfigList = computed(() => {
  return configList.value.filter(item => {
    if (activeRisk.value === null) return item.riskLevel == null
    return item.riskLevel === activeRisk.value
  })
})

const loadConfigList = async () => {
  loading.value = true
  try {
    const res = await getAlertConfigList()
    if (res.code === 200) configList.value = res.data || []
  } catch (e) { ElMessage.error('加载告警配置失败') }
  finally { loading.value = false }
}

const emojiMap = { 1: '\u2764\uFE0F', 2: '\uD83D\uDCA8', 3: '\uD83C\uDF21\uFE0F', 4: '\uD83E\uDEC0', 5: '\uD83E\uDE78' }
const getEmoji = (type) => emojiMap[type] || '\u2764\uFE0F'

const calcWidth = (item, from, to) => {
  const total = item.criticalHigh - item.criticalLow
  if (total <= 0) return '20%'
  return Math.max((Math.abs(to - from) / total) * 100, 5) + '%'
}

const handleToggle = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要${item.enabled ? '禁用' : '启用'}「${item.configName}」告警吗？`, '状态确认', { type: 'warning' })
    const res = await toggleAlertConfig(item.id)
    if (res.code === 200) { ElMessage.success('操作成功'); await loadConfigList() }
    else ElMessage.error(res.message || '操作失败')
  } catch (e) { /* cancelled */ }
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editForm = reactive({ id: null, configName: '', normalMin: 0, normalMax: 0, warnLow: 0, warnHigh: 0, criticalLow: 0, criticalHigh: 0 })

const validateRange = (rule, value, callback) => {
  const { criticalLow, warnLow, normalMin, normalMax, warnHigh, criticalHigh } = editForm
  if (criticalLow > warnLow || warnLow > normalMin || normalMin > normalMax || normalMax > warnHigh || warnHigh > criticalHigh) {
    callback(new Error('范围值应满足: 危险低 <= 预警低 <= 正常低 <= 正常高 <= 预警高 <= 危险高'))
  } else callback()
}

const formRules = {
  normalMin: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }],
  normalMax: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }],
  warnLow: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }],
  warnHigh: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }],
  criticalLow: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }],
  criticalHigh: [{ required: true, message: '必填' }, { validator: validateRange, trigger: 'blur' }]
}

const openEdit = (item) => {
  Object.assign(editForm, { id: item.id, configName: item.configName, normalMin: item.normalMin, normalMax: item.normalMax, warnLow: item.warnLow, warnHigh: item.warnHigh, criticalLow: item.criticalLow, criticalHigh: item.criticalHigh })
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await updateAlertConfig({ id: editForm.id, normalMin: editForm.normalMin, normalMax: editForm.normalMax, warnLow: editForm.warnLow, warnHigh: editForm.warnHigh, criticalLow: editForm.criticalLow, criticalHigh: editForm.criticalHigh })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; loadConfigList() }
    else ElMessage.error(res.message || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
  finally { submitting.value = false }
}

onMounted(() => loadConfigList())
</script>

<style scoped lang="scss">
@import '@/styles/dark-admin.scss';

.page-container {
  @include da-container;
  height: calc(100vh - 50px);
  min-height: unset;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.page-header { @include da-page-header; flex-shrink: 0; }
.page-header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 36px; color: $da-accent; background: rgba(0,212,255,.1); border-radius: 10px; padding: 8px; }
.main-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 2px; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: $da-text-dim; margin: 0; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $da-text-dim; background: $da-accent-dim; padding: 6px 14px; border-radius: 20px; border: 1px solid $da-accent-hover; }

.risk-tabs { display: flex; gap: 8px; margin-top: 16px; flex-shrink: 0; }
.risk-tab { padding: 7px 18px; border-radius: 20px; border: 1px solid $da-border-light; background: $da-panel; color: $da-text-dim; font-size: 13px; cursor: pointer; transition: all .2s;
  &:hover { border-color: $da-accent; color: $da-accent; }
  &.active { background: $da-accent-dim; border-color: $da-accent; color: $da-accent; font-weight: 600; }
}
.config-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(440px, 1fr)); gap: 16px; margin-top: 16px; flex: 1; min-height: 0; overflow-y: auto; align-content: start; }
.config-empty { grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; padding: 50px 0; color: rgba(126,184,247,0.4); gap: 10px; p { font-size: 13px; margin: 0; } }
.config-card { background: $da-panel; border: 1px solid $da-border; border-left: 4px solid $da-success; border-radius: 10px; padding: 20px; transition: transform .25s, box-shadow .25s;
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,212,255,.15); }
  &.is-disabled { border-left-color: #4a5578; opacity: .7; }
}
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.card-info { display: flex; align-items: center; gap: 12px; }
.card-emoji { font-size: 28px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: $da-panel-alt; border: 1px solid $da-border-light; border-radius: 12px; }
.card-name { font-size: 16px; font-weight: 600; color: $da-text-bright; }
.card-unit { font-size: 12px; color: $da-text-dim; margin-top: 2px; }

.range-bar-wrap { margin-bottom: 16px; }
.range-legend { display: flex; gap: 16px; margin-bottom: 8px; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: $da-text-dim; }
.legend-dot { display: inline-block; width: 8px; height: 8px; border-radius: 2px; &.critical { background: $da-danger; } &.warn { background: $da-warning; } &.normal { background: $da-success; } }
.range-bar { display: flex; height: 32px; border-radius: 6px; overflow: hidden; border: 1px solid $da-border-light; }
.range-segment { display: flex; align-items: center; justify-content: center; min-width: 30px;
  &.critical-low, &.critical-high { background: rgba(255,82,82,.35); }
  &.warn-low, &.warn-high { background: rgba(255,210,0,.3); }
  &.normal { background: rgba(56,239,125,.3); }
}
.range-label { font-size: 11px; font-weight: 600; color: $da-text-bright; white-space: nowrap; text-shadow: 0 1px 3px rgba(0,0,0,.5); }
.card-bottom { display: flex; justify-content: flex-end; }

.form-body { padding: 8px 0; }
.range-section { margin-bottom: 16px; padding: 16px; background: $da-panel-alt; border: 1px solid $da-border; border-radius: 8px; }
.section-header { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid $da-border;
  &.normal-header { color: $da-success; } &.warn-header { color: $da-warning; } &.critical-header { color: $da-danger; }
}
.section-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%;
  &.normal { background: $da-success; box-shadow: 0 0 6px rgba(56,239,125,.5); }
  &.warn { background: $da-warning; box-shadow: 0 0 6px rgba(255,210,0,.5); }
  &.critical { background: $da-danger; box-shadow: 0 0 6px rgba(255,82,82,.5); }
}

@include da-el-overrides;
:deep(.el-input-number) { .el-input__wrapper { background: #0d1228; box-shadow: 0 0 0 1px $da-border-light inset; &:hover { box-shadow: 0 0 0 1px $da-accent inset; } } .el-input__inner { color: $da-text; } }
</style>
