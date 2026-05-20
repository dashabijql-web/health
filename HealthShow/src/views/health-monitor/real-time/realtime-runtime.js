import { ElMessage } from 'element-plus'
import { getOnlineUsers } from '@/api/realtime'
import { sendWatchMessage, sendVoiceMessage, getVoiceTemplates } from '@/api/device'
import { createScrollLoop } from '@/composables/useScrollLoop'
import {
  classifyBloodOxygen,
  classifyDiastolic,
  classifyHeartRate,
  classifyPressure,
  classifySystolic,
  classifyTemperature,
  formatRealtimeTime,
  getRealtimeIndicator,
  getRealtimeRowClass,
  normalizeRealtimeUsersResponse,
  resolveRealtimeFetchSize
} from './realtime-helpers'

export const realtimeRuntimeMethods = {
  async fetchOnlineUsers() {
    if (this._fetching) return
    this._fetching = true
    const isFirst = !this._loaded
    if (isFirst) this.isLoading = true
    try {
      const fetchSize = resolveRealtimeFetchSize(window.innerWidth)
      const res = await getOnlineUsers(1, fetchSize, 20000)
      if (res && res.code === 200) {
        const { list, total } = normalizeRealtimeUsersResponse(res.data)
        this.onlineUsers = { list, total }
        const depts = new Set(list.map(user => user.deptName).filter(Boolean))
        this.deptList = [...depts].sort()
      }
    } catch {
      // keep silent; the page will retry on the next tick
    } finally {
      this._loaded = true
      this._fetching = false
      if (isFirst) this.isLoading = false
    }
  },

  onVisibilityChange() {
    if (document.hidden) {
      this.stopRefreshTask()
      this.stopClock()
      this._autoScrollLoop?.stop()
    } else {
      this.fetchOnlineUsers()
      this.startRefreshTask()
      this.startClock()
      this.startAutoScroll()
    }
  },

  handleSearch() {
    this.currentPage = 1
  },

  handleReset() {
    this.searchForm = { name: '', dept: '', status: '' }
    this.hrFilter = null
    this.currentPage = 1
  },

  clearHrFilter() {
    this.hrFilter = null
    this.currentPage = 1
  },

  initAutoScrollLoop() {
    if (this._autoScrollLoop) return this._autoScrollLoop
    this._autoScrollLoop = createScrollLoop({
      getElement: () => this.$el?.querySelector('.el-table__body-wrapper .el-scrollbar__wrap, .el-scrollbar__wrap'),
      intervalMs: 80,
      endPauseMs: 1500,
      shouldScroll: () => this.autoScrollEnabled && !this.scrollPaused && !this.isMobile,
      onReachEnd: () => {
        this.currentPage = this.currentPage < this.totalPages ? this.currentPage + 1 : 1
        this.$nextTick(() => {
          const el = this.$el?.querySelector('.el-table__body-wrapper .el-scrollbar__wrap, .el-scrollbar__wrap')
          if (el) el.scrollTop = 0
        })
        this.scrollPaused = false
      }
    })
    return this._autoScrollLoop
  },

  startAutoScroll() {
    if (this.isMobile) return
    this.initAutoScrollLoop().start()
  },

  stopAutoScroll() {
    this._autoScrollLoop?.stop()
  },

  toggleAutoScroll() {
    this.autoScrollEnabled = !this.autoScrollEnabled
  },

  pauseAutoScroll() {
    this.scrollPaused = true
  },

  resumeAutoScroll() {
    this.scrollPaused = false
  },

  rowClass({ row }) {
    return getRealtimeRowClass(row)
  },

  getUserIndicator(user) {
    return getRealtimeIndicator(user)
  },

  hrCls(value) {
    return classifyHeartRate(value)
  },

  spo2Cls(value) {
    return classifyBloodOxygen(value)
  },

  tempCls(value) {
    return classifyTemperature(value)
  },

  bpCls(value) {
    return classifySystolic(value)
  },

  bpLowCls(value) {
    return classifyDiastolic(value)
  },

  pressureCls(value) {
    return classifyPressure(value)
  },

  fmtTime(value) {
    return formatRealtimeTime(value)
  },

  showUserDetail(row) {
    this.detailUser = row
    this.detailVisible = true
  },

  handleSendMessage(row) {
    this.messageTarget = row
    this.messageText = ''
    this.messageDialogVisible = true
  },

  async handleSendVoice(row) {
    this.voiceTarget = row
    this.voiceTemplateId = ''
    if (this.voiceTemplates.length === 0) {
      try {
        const res = await getVoiceTemplates()
        if (res.code === 200) this.voiceTemplates = res.data || []
      } catch {}
    }
    this.voiceDialogVisible = true
  },

  async confirmSendVoice() {
    if (!this.voiceTemplateId) return
    try {
      const res = await sendVoiceMessage(this.voiceTarget.imei, this.voiceTemplateId)
      if (res.code === 200) {
        ElMessage.success('语音广播已推送，手表将在数秒内播放')
        this.voiceDialogVisible = false
      } else {
        ElMessage.error(res.message || '推送失败')
      }
    } catch {
      ElMessage.error('推送失败')
    }
  },

  async confirmSendMessage() {
    const text = this.messageText.trim()
    if (!text) return
    try {
      const res = await sendWatchMessage(this.messageTarget.imei, text)
      if (res.code === 200) {
        ElMessage.success('消息已推送到手表')
        this.messageDialogVisible = false
      } else {
        ElMessage.error(res.message || '推送失败')
      }
    } catch {
      ElMessage.error('推送失败')
    }
  }
}
