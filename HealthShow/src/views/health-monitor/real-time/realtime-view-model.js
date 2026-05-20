import {
  buildRealtimeDetailItems,
  filterRealtimeUsers,
  sortWarningUsers
} from './realtime-helpers'

export function createRealtimePageState() {
  return {
    onlineUsers: { list: [], total: 0 },
    searchForm: { name: '', dept: '', status: '' },
    hrFilter: null,
    deptList: [],
    currentPage: 1,
    pageSize: 50,
    autoScrollEnabled: true,
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
    return window.innerWidth < 992
  },
  allUsers() {
    return this.onlineUsers.list || []
  },
  warningUsers() {
    return sortWarningUsers(this.allUsers)
  },
  normalCount() {
    return this.allUsers.filter(u => u.status === 'normal').length
  },
  warningCount() {
    return this.warningUsers.length
  },
  filteredUserList() {
    return filterRealtimeUsers(this.allUsers, this.searchForm, this.hrFilter)
  },
  paginatedUserList() {
    if (this.isMobile) return this.filteredUserList
    const start = (this.currentPage - 1) * this.pageSize
    return this.filteredUserList.slice(start, start + this.pageSize)
  },
  totalPages() {
    if (this.isMobile) return 1
    return Math.max(1, Math.ceil(this.filteredUserList.length / this.pageSize))
  },
  detailItems() {
    return buildRealtimeDetailItems(this.detailUser)
  }
}
