/**
 * 健康监测页面通用 mixin
 * 提供：initClock、handleResize、startAutoScroll、fmtTime、periodRange、switchPeriod
 * 自动注册/注销 resize 事件，管理 clock/scroll/resize/refresh 定时器
 * 自动 dispose 所有 ECharts 实例
 *
 * 使用方式：
 *   import chartPageMixin from '@/mixins/chartPage'
 *   export default { mixins: [chartPageMixin], ... }
 *
 * 要求宿主组件：
 *   - data 中有 charts 对象（key→echarts实例）
 *   - data 中有 currentTime（字符串，用于时钟显示）
 *   - data 中有 activePeriod（字符串，用于 periodRange 计算）
 *   - methods 中有 fetchData()（switchPeriod 调用）
 *   - template 中 ref="listRef" 绑定到需要自动滚动的容器
 */
import dayjs from 'dayjs'

export default {
  data() {
    return {
      clockTimer: null,
      scrollTimer: null,
      resizeTimer: null,
      refreshTimer: null,
      filterDept: ''
    }
  },
  computed: {
    periodRange() {
      const today = dayjs().format('YYYY-MM-DD')
      if (this.activePeriod === 'day') return { startDate: today, endDate: today }
      if (this.activePeriod === 'week') return { startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'), endDate: today }
      return { startDate: dayjs().subtract(29, 'day').format('YYYY-MM-DD'), endDate: today }
    },
    /** 经过部门筛选的实时列表（filterDept 为空时返回全量） */
    filteredRealtimeList() {
      const list = this.realtimeList || []
      return this.filterDept
        ? list.filter(x => (x.deptName || x.dept_name) === this.filterDept)
        : list
    },
    pagedList() {
      const s = (this.currentPage - 1) * this.pageSize
      return this.filteredRealtimeList.slice(s, s + this.pageSize)
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredRealtimeList.length / this.pageSize))
    }
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
    document.addEventListener('visibilitychange', this._handlePageVisibility)
  },
  beforeUnmount() {
    clearInterval(this.clockTimer)
    clearInterval(this.scrollTimer)
    clearInterval(this.refreshTimer)
    clearTimeout(this.resizeTimer)
    window.removeEventListener('resize', this.handleResize)
    document.removeEventListener('visibilitychange', this._handlePageVisibility)
    if (this._listScrollCleanup) this._listScrollCleanup()
    if (this.charts) Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    initClock() {
      const tick = () => { this.currentTime = dayjs().format('YYYY年MM月DD日 HH:mm:ss') }
      tick()
      this.clockTimer = setInterval(tick, 1000)
    },
    handleResize() {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        this.$nextTick(() => Object.values(this.charts).forEach(c => c?.resize?.()))
      }, 200)
    },
    startAutoScroll() {
      const el = this.$refs.listRef
      if (!el) return
      let top = 0
      let pausedUntil = 0

      const onScroll = () => {
        // If the scroll position differs significantly from what the timer set,
        // the user manually scrolled — sync top and pause auto-scroll for 2s
        if (Math.abs(el.scrollTop - top) > 2) {
          top = el.scrollTop
          pausedUntil = Date.now() + 2000
        }
      }
      el.addEventListener('scroll', onScroll)
      this._listScrollCleanup = () => el.removeEventListener('scroll', onScroll)

      this.scrollTimer = setInterval(() => {
        if (Date.now() < pausedUntil) return
        const max = el.scrollHeight - el.clientHeight
        if (max <= 0) return
        if (top >= max) { setTimeout(() => { top = 0; el.scrollTop = 0 }, 1500) }
        else { top += 1; el.scrollTop = top }
      }, 80)
    },
    /** 通用页面初始化：时钟 + 首次加载 + 自动滚动 + 定时刷新 */
    initPage(refreshFn, interval = 30000) {
      this.__refreshFn = refreshFn || (() => this.fetchData())
      this.__refreshInterval = interval
      this.initClock()
      this.fetchData()
      this.$nextTick(() => this.startAutoScroll())
      this.refreshTimer = setInterval(this.__refreshFn, interval)
    },
    /** 标签页隐藏时暂停轮询，显示时立即刷新并重启定时器 */
    _handlePageVisibility() {
      if (document.hidden) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      } else if (this.__refreshFn) {
        this.__refreshFn()
        this.refreshTimer = setInterval(this.__refreshFn, this.__refreshInterval)
      }
    },
    switchPeriod(val) {
      if (this.activePeriod === val) return
      this.activePeriod = val
      this.fetchData()
    },
    fmtTime(ts) {
      return ts ? dayjs(ts).format('MM-DD HH:mm') : ''
    },
    goToPortrait(item) {
      const code = item.userCode || item.empCode
      this.$router.push({
        path: '/health-monitor/health-portrait',
        query: code ? { empCode: code } : { name: item.userName }
      })
    },
    /** 根据容器高度自动计算分页大小 */
    setPageSize(rowH = 27) {
      const el = this.$refs.listRef; if (!el) return
      const n = Math.max(10, Math.floor(el.clientHeight / rowH))
      if (n !== this.pageSize) {
        this.pageSize = n
        this.currentPage = 1
      }
    },
    showDetail(item) {
      this.detailItem = item
      this.detailVisible = true
    }
  }
}
