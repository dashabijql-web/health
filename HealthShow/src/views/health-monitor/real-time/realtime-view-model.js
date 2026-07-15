import dayjs from 'dayjs'

export function createRealtimePageState() {
  const isMobile = typeof window !== 'undefined' && window.innerWidth < 992
  return {
    onlineUsers: {
      list: [],
      total: 0,
      summary: {
        onlineCount: 0,
        normalCount: 0,
        warningCount: 0,
        staleCount: 0,
        noDataCount: 0,
        onlineWindowMinutes: 15,
        freshnessMinutes: 5
      },
      departments: [],
      warningPreview: []
    },
    searchForm: { name: '', dept: '', status: '' },
    currentPage: 1,
    pageSize: isMobile ? 20 : 50,
    viewportWidth: typeof window !== 'undefined' ? window.innerWidth : 1440,
    autoScrollEnabled: false,
    scrollPaused: false,
    detailUser: null,
    detailVisible: false,
    messageTarget: null,
    messageDialogVisible: false,
    messageText: '',
    voiceTarget: null,
    voiceDialogVisible: false,
    voiceTemplateId: '',
    voiceTemplates: [],
    isLoading: false,
    isRefreshing: false,
    refreshError: '',
    isStale: false,
    lastSuccessfulRefresh: '',
    tblHeadStyle: {
      background: 'rgba(0,40,90,0.9)',
      color: '#00d4ff',
      borderColor: 'rgba(0,212,255,0.3)',
      fontSize: '13px',
      fontWeight: 'bold',
      padding: '10px 0'
    },
    tblCellStyle: {
      background: 'transparent',
      borderColor: 'rgba(0,212,255,0.15)',
      color: '#a8c5e6',
      fontSize: '13px',
      padding: '8px 0',
      cursor: 'pointer'
    }
  }
}

export const realtimeComputed = {
  isMobile() {
    return this.viewportWidth < 992
  },
  allUsers() {
    return this.onlineUsers.list || []
  },
  summary() {
    return this.onlineUsers.summary || {}
  },
  warningUsers() {
    return this.onlineUsers.warningPreview || []
  },
  dataIssueCount() {
    return Number(this.summary.staleCount || 0) + Number(this.summary.noDataCount || 0)
  },
  totalPages() {
    return Math.max(1, Math.ceil((this.onlineUsers.total || 0) / this.pageSize))
  },
  refreshLabel() {
    if (this.refreshError) return this.refreshError
    if (!this.lastSuccessfulRefresh) return '正在加载实时数据'
    const time = dayjs(this.lastSuccessfulRefresh)
    if (!time.isValid()) return '已更新'
    return `${time.format('HH:mm:ss')} 更新`
  }
}
