import Layout from '@/layout/index.vue'

export default {
  path: '/statistics',
  component: Layout,
  name: 'Statistics',
  meta: { title: '统计分析', icon: 'TrendCharts' },
  children: [
    {
      path: 'dept-report',
      name: 'DeptReport',
      component: () => import('@/views/statistics/dept-report/index.vue'),
      meta: { title: '部门健康报表', icon: 'DataAnalysis', permCode: 'stats:dept' }
    },
    {
      path: 'monthly',
      name: 'MonthlyStats',
      component: () => import('@/views/statistics/monthly/index.vue'),
      meta: { title: '月度统计', icon: 'Calendar', permCode: 'stats:monthly' }
    }
  ]
}
