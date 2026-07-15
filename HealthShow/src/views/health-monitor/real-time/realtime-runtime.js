import { ElMessage } from 'element-plus'
import { getOnlineUsers } from '@/api/realtime'
import { sendWatchMessage, sendVoiceMessage, getVoiceTemplates } from '@/api/device'
import { createScrollLoop } from '@/composables/useScrollLoop'
import { normalizeRealtimeUsersResponse } from './realtime-helpers'

export const realtimeRuntimeMethods = {
  async fetchOnlineUsers() {
    if (this._fetching) {
      this._pendingFetch = true
      return
    }
    this._fetching = true
    this.isRefreshing = true
    const isFirst = !this._loaded
    if (isFirst) this.isLoading = true
    try {
      const res = await getOnlineUsers({
        page: this.currentPage,
        size: this.pageSize,
        name: this.searchForm.name.trim(),
        dept: this.searchForm.dept,
        status: this.searchForm.status
      }, 20000)
      if (!res || res.code !== 200) throw new Error(res?.message || 'request failed')

      const data = normalizeRealtimeUsersResponse(res.data)
      this.onlineUsers = data
      this.currentPage = data.page
      this.isStale = data.stale
      this.lastSuccessfulRefresh = data.refreshedAt || new Date().toISOString()
      this.refreshError = data.stale ? '数据服务异常，当前显示缓存' : ''
    } catch {
      this.refreshError = this.onlineUsers.list.length ? '刷新失败，当前显示上次数据' : '实时数据加载失败'
    } finally {
      this._loaded = true
      this._fetching = false
      this.isRefreshing = false
      if (isFirst) this.isLoading = false
      if (this._pendingFetch) {
        this._pendingFetch = false
        this.$nextTick(() => this.fetchOnlineUsers())
      }
    }
  },

  manualRefresh() {
    this.fetchOnlineUsers()
  },

  onVisibilityChange() {
    if (document.hidden) {
      this.stopRefreshTask()
      this._autoScrollLoop?.stop()
    } else {
      this.fetchOnlineUsers()
      this.startRefreshTask()
      this.startAutoScroll()
    }
  },

  onViewportResize() {
    const wasMobile = this.isMobile
    this.viewportWidth = window.innerWidth
    const nextSize = this.isMobile ? 20 : 50
    if (wasMobile !== this.isMobile || this.pageSize !== nextSize) {
      this.pageSize = nextSize
      this.currentPage = 1
      this.fetchOnlineUsers()
    }
  },

  handleSearch() {
    window.clearTimeout(this._searchTimer)
    this._searchTimer = window.setTimeout(() => {
      this.currentPage = 1
      this.fetchOnlineUsers()
    }, 250)
  },

  handleReset() {
    window.clearTimeout(this._searchTimer)
    this.searchForm = { name: '', dept: '', status: '' }
    this.currentPage = 1
    this.$nextTick(() => this.fetchOnlineUsers())
  },

  showAllWarnings() {
    this.searchForm = { ...this.searchForm, status: 'warning' }
    this.currentPage = 1
    this.$nextTick(() => this.fetchOnlineUsers())
  },

  openWarningCenter() {
    this.$router.push('/alert-management/notifications')
  },

  goToPage(page) {
    const next = Math.max(1, Math.min(this.totalPages, page))
    if (next === this.currentPage) return
    this.currentPage = next
    this.fetchOnlineUsers()
  },

  initAutoScrollLoop() {
    if (this._autoScrollLoop) return this._autoScrollLoop
    this._autoScrollLoop = createScrollLoop({
      getElement: () => this.$el?.querySelector('.el-table__body-wrapper .el-scrollbar__wrap, .el-scrollbar__wrap'),
      intervalMs: 80,
      endPauseMs: 1500,
      shouldScroll: () => this.autoScrollEnabled && !this.scrollPaused && !this.isMobile,
      onReachEnd: () => {
        this.goToPage(this.currentPage < this.totalPages ? this.currentPage + 1 : 1)
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
    if (this.isMobile || !this.autoScrollEnabled) return
    this.initAutoScrollLoop().start()
  },

  toggleAutoScroll() {
    this.autoScrollEnabled = !this.autoScrollEnabled
    if (this.autoScrollEnabled) this.startAutoScroll()
    else this._autoScrollLoop?.stop()
  },

  pauseAutoScroll() {
    this.scrollPaused = true
  },

  resumeAutoScroll() {
    this.scrollPaused = false
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
        ElMessage.success('语音提醒已推送，手表将在数秒内播放')
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
