import Layout from '@/layout/index.vue'

export default {
  path: '/alert-management',
  component: Layout,
  name: 'AlertManagement',
  meta: { title: '预警管理', icon: 'Bell' },
  children: [
    {
      path: 'config',
      name: 'AlertConfig',
      component: () => import('@/views/alert-management/config/index.vue'),
      meta: { title: '阈值配置', icon: 'Setting', permCode: 'alert:config' }
    },
    {
      path: 'records',
      name: 'AlertRecords',
      component: () => import('@/views/alert-management/records/index.vue'),
      meta: { title: '预警记录', icon: 'List', permCode: 'alert:records' }
    }
  ]
}
