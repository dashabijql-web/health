<template>
  <div class="hm-page-shell rt-root">
    <RealtimeHeader
      :warning-users="warningUsers"
      :total-count="allUsers.length"
      :normal-count="normalCount"
      :warning-count="warningCount"
      :current-time="currentTime"
    />

    <section class="rt-bd">
      <main class="rt-main">
        <RealtimeUserTable
          v-model:search-form="searchForm"
          :filtered-count="filteredUserList.length"
          :hr-filter="hrFilter"
          :dept-list="deptList"
          :auto-scroll-enabled="autoScrollEnabled"
          :is-mobile="isMobile"
          :paginated-user-list="paginatedUserList"
          :is-loading="isLoading"
          :current-page="currentPage"
          :page-size="pageSize"
          :tbl-head-style="tblHeadStyle"
          :tbl-cell-style="tblCellStyle"
          @clear-hr-filter="clearHrFilter"
          @search="handleSearch"
          @reset="handleReset"
          @toggle-auto-scroll="toggleAutoScroll"
          @pause-auto-scroll="pauseAutoScroll"
          @resume-auto-scroll="resumeAutoScroll"
          @show-user-detail="showUserDetail"
          @send-message="handleSendMessage"
          @send-voice="handleSendVoice"
        />
      </main>
    </section>

    <el-dialog
      v-model="messageDialogVisible"
      title="发送消息到手表"
      class="rt-dialog"
      width="420px"
      :append-to-body="true"
      :close-on-click-modal="false"
    >
      <div v-if="messageTarget" class="rt-msg-meta">
        <span>{{ messageTarget.userName }}</span>
        <span class="rt-msg-dept">{{ messageTarget.deptName }}</span>
        <span class="rt-msg-imei">{{ messageTarget.imei }}</span>
      </div>
      <el-input
        v-model="messageText"
        type="textarea"
        :rows="4"
        placeholder="请输入要推送到手表的消息内容（最多 50 个字符）"
        :maxlength="50"
        show-word-limit
        resize="none"
      />
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!messageText.trim()" @click="confirmSendMessage">发送</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="voiceDialogVisible"
      title="语音广播到手表"
      class="rt-dialog"
      width="400px"
      :append-to-body="true"
      :close-on-click-modal="false"
    >
      <div v-if="voiceTarget" class="rt-msg-meta">
        <span>{{ voiceTarget.userName }}</span>
        <span class="rt-msg-dept">{{ voiceTarget.deptName }}</span>
        <span class="rt-msg-imei">{{ voiceTarget.imei }}</span>
      </div>
      <div class="rt-voice-template-list">
        <div
          v-for="template in voiceTemplates"
          :key="template.id"
          :class="['rt-voice-tpl', voiceTemplateId === template.id ? 'rt-voice-tpl--active' : '']"
          @click="voiceTemplateId = template.id"
        >
          {{ template.name }}
        </div>
      </div>
      <template #footer>
        <el-button @click="voiceDialogVisible = false">取消</el-button>
        <el-button type="warning" :disabled="!voiceTemplateId" @click="confirmSendVoice">
          立即播报
        </el-button>
      </template>
    </el-dialog>

    <RealtimeDetailDialog
      v-model:visible="detailVisible"
      :user="detailUser"
      :items="detailItems"
    />
  </div>
</template>

<script>
import { getCurrentInstance } from 'vue'
import { useClock } from '@/composables/useClock'
import { useIntervalTask } from '@/composables/useIntervalTask'
import { createEventBinding } from '@/utils/task-timer'
import { realtimeRuntimeMethods } from './realtime-runtime'
import { createRealtimePageState, realtimeComputed } from './realtime-view-model'
import RealtimeDetailDialog from './components/RealtimeDetailDialog.vue'
import RealtimeHeader from './components/RealtimeHeader.vue'
import RealtimeUserTable from './components/RealtimeUserTable.vue'

export default {
  name: 'RealtimeMonitor',
  components: { RealtimeDetailDialog, RealtimeHeader, RealtimeUserTable },
  setup() {
    const instance = getCurrentInstance()
    const { currentTime, startClock, stopClock } = useClock('HH:mm:ss')
    const { start: startRefreshTask, stop: stopRefreshTask } = useIntervalTask(() => {
      instance?.proxy?.fetchOnlineUsers?.()
    }, 15000)
    return { currentTime, startClock, stopClock, startRefreshTask, stopRefreshTask }
  },
  data() {
    return createRealtimePageState()
  },
  computed: {
    ...realtimeComputed
  },
  mounted() {
    this.fetchOnlineUsers()
    this.startRefreshTask()
    this.startClock()
    if (!this.isMobile) {
      this.startAutoScroll()
    }
    this._visibilityBinding = createEventBinding(() => document, 'visibilitychange', this.onVisibilityChange)
    this._visibilityBinding.start()
  },
  beforeUnmount() {
    this.stopRefreshTask()
    this.stopClock()
    this._autoScrollLoop?.stop()
    this._visibilityBinding?.stop()
  },
  methods: {
    ...realtimeRuntimeMethods
  }
}
</script>

<style lang="scss" src="./realtime.scss"></style>
