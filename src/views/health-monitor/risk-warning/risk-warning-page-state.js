import { PERIOD_OPTIONS } from '@/constants/periods'

export default {
  data() {
    return {
      currentTime: '',
      warningStats: [
        { label: '心率预警', value: 0, color: '#ef4444' },
        { label: '血氧预警', value: 0, color: '#f97316' },
        { label: '体温预警', value: 0, color: '#22c55e' },
        { label: '压力预警', value: 0, color: '#00d4ff' }
      ],
      warningList: [],
      currentPage: 1,
      pageSize: 20,
      filterName: '',
      filterLevel: '',
      filterType: '',
      trendData: { dates: [], series: { heartRate: [], bloodOxygen: [], temperature: [], pressure: [] } },
      deptData: [],
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {},
      detailVisible: false, detailRow: null,
      handleNote: '', handling: false,
      selectedIds: [], batchHandling: false,
      autoScrollPaused: false,
      scrollTop: 0,
      // 体征曲线
      vitalLoading: false,
      vitalEmpty: false,
      vitalChartRange: '',
      vitalSummary: [],
      vitalChartInst: null
    }
  },
}

