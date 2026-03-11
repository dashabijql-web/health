<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="event ? event.type + ' - 事件处理' : '事件处理'"
    width="560px"
    :close-on-click-modal="false"
    class="handle-dlg"
  >
    <div v-if="event" class="handle-body">
      <!-- 事件信息 -->
      <div class="evt-info">
        <div class="ei-row">
          <span class="ei-icon">{{ event.icon }}</span>
          <div class="ei-main">
            <div class="ei-r1">
              <span class="ei-type">{{ event.type }}</span>
              <span :class="['ei-level', 'lv-' + event.level]">{{ getLevelText(event.level) }}</span>
            </div>
            <div class="ei-r2">
              <span>{{ event.user }}</span>
              <span class="dim">{{ event.dept }}</span>
              <span class="dim">{{ event.location }}</span>
              <span class="dim">{{ event.time }}</span>
            </div>
          </div>
          <div v-if="event.durationMinutes" :class="['ei-dur', { crit: event.durationMinutes > 5 }]">
            {{ event.durationMinutes }}分钟
          </div>
        </div>
      </div>

      <!-- 处理表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="handle-form">
        <el-form-item label="处理方式" prop="handleType">
          <el-radio-group v-model="form.handleType">
            <el-radio value="confirmed">确认属实</el-radio>
            <el-radio value="false_alarm">误报</el-radio>
            <el-radio value="dispatched">已派遣救援</el-radio>
            <el-radio value="resolved">已解决</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理说明" prop="handleNote">
          <el-input
            v-model="form.handleNote"
            type="textarea"
            :rows="3"
            placeholder="请填写处理说明..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="通知领导">
          <el-switch v-model="form.notifyLeader" />
          <span class="switch-hint">{{ form.notifyLeader ? '将通知值班领导' : '' }}</span>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">确认处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { handleRiskWarning } from '@/api/risk-warning'

const props = defineProps({
  visible: { type: Boolean, default: false },
  event: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'handled'])

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  handleType: 'confirmed',
  handleNote: '',
  notifyLeader: false
})

const rules = {
  handleType: [{ required: true, message: '请选择处理方式', trigger: 'change' }],
  handleNote: [{ required: true, message: '请填写处理说明', trigger: 'blur' }]
}

const getLevelText = (l) => ({ critical: '特急', high: '紧急', medium: '一般', low: '轻微' }[l] || l)

const onSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    // 后端 RiskWarningController 提取 handleBy 和 handleRemark 两个字段
    await handleRiskWarning(props.event.id, {
      handleBy:     form.handleType,   // 处理方式（confirmed/false_alarm/dispatched/resolved）
      handleRemark: form.handleNote    // 处理说明文本
    })
    ElMessage.success('处理成功')
    emit('handled', props.event.id)
    emit('update:visible', false)
    // Reset form
    form.handleType = 'confirmed'
    form.handleNote = ''
    form.notifyLeader = false
  } catch (err) {
    ElMessage.error('处理失败: ' + (err.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
$blue: #1890ff;
$red: #ff5252;
$orange: #ff9800;
$yellow: #faad14;

.handle-body {
  .evt-info {
    background: rgba($blue, .06);
    border: 1px solid rgba($blue, .2);
    border-radius: 6px;
    padding: 10px 12px;
    margin-bottom: 16px;
  }
  .ei-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .ei-icon { font-size: 24px; }
  .ei-main { flex: 1; }
  .ei-r1 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .ei-type { font-size: 14px; font-weight: bold; }
  .ei-level {
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 3px;
    &.lv-critical { background: rgba($red, .2); color: $red; font-weight: bold; }
    &.lv-high { background: rgba($orange, .2); color: $orange; }
    &.lv-medium { background: rgba($yellow, .2); color: $yellow; }
    &.lv-low { background: rgba($blue, .15); color: $blue; }
  }
  .ei-r2 {
    font-size: 12px;
    color: #999;
    display: flex;
    gap: 8px;
    .dim { color: #bbb; }
  }
  .ei-dur {
    font-size: 14px;
    font-weight: bold;
    color: $orange;
    &.crit { color: $red; }
  }
}

.handle-form {
  margin-top: 8px;
}

.switch-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}
</style>
