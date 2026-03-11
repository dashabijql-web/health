import Layout from '@/layout/index.vue'

export default {
  path: '/personnel-management',
  component: Layout,
  name: 'PersonnelManagement',
  meta: { title: '人员管理', icon: 'Avatar' },
  children: [
    {
      path: 'employee',
      name: 'Employee',
      component: () => import('@/views/personnel-management/employee/index.vue'),
      meta: { title: '职工信息', icon: 'UserFilled', permCode: 'personnel:employee' }
    },
    {
      path: 'health-portrait',
      name: 'HealthPortrait',
      component: () => import('@/views/personnel-management/health-portrait/index.vue'),
      hidden: true,
      meta: { title: '健康画像', icon: 'FirstAidKit', permCode: 'personnel:portrait' }
    }
  ]
}
