/**
 * 健康监测页面通用 mixin
 * 提供：initClock、handleResize、startAutoScroll、fmtTime、periodRange
 * 自动注册/注销 resize 事件，管理 clock/scroll/resize 定时器
 *
 * 使用方式：
 *   import chartPageMixin from '@/mixins/chartPage'
 *   export default { mixins: [chartPageMixin], ... }
 *
 * 要求宿主组件：
 *   - data 中有 charts 对象（key→echarts实例）
 *   - data 中有 currentTime（字符串，用于时钟显示）
 *   - data 中有 activePeriod（字符串，用于 periodRange 计算）
 *   - template 中 ref="listRef" 绑定到需要自动滚动的容器
 */
import dayjs from 'dayjs'

export default {
  data() {
    return {
      clockTimer: null,
      scrollTimer: null,
      resizeTimer: null
    }
  },
  computed: {
    periodRange() {
      const today = dayjs().format('YYYY-MM-DD')
      if (this.activePeriod === 'day') return { startDate: today, endDate: today }
      if (this.activePeriod === 'week') return { startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'), endDate: today }
      return { startDate: dayjs().subtract(29, 'day').format('YYYY-MM-DD'), endDate: today }
    }
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    clearInterval(this.clockTimer)
    clearInterval(this.scrollTimer)
    clearTimeout(this.resizeTimer)
    window.removeEventListener('resize', this.handleResize)
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
      this.scrollTimer = setInterval(() => {
        const max = el.scrollHeight - el.clientHeight
        if (max <= 0) return
        if (top >= max) { setTimeout(() => { top = 0; el.scrollTop = 0 }, 1500) }
        else { top += 1; el.scrollTop = top }
      }, 40)
    },
    fmtTime(ts) {
      return ts ? dayjs(ts).format('MM-DD HH:mm') : ''
    },
    goToPortrait(item) {
      const code = item.userCode || item.empCode
      this.$router.push({
        path: '/personnel-management/health-portrait',
        query: code ? { empCode: code } : { name: item.userName }
      })
    },
    showDetail(item) {
      this.detailItem = item
      this.detailVisible = true
    }
  }
}
