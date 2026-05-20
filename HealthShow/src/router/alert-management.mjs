const Layout = () => import('@/layout/index.vue')

export default {
  path: '/alert-management',
  component: Layout,
  name: 'AlertManagement',
  meta: { title: '预警管理', icon: 'Bell' },
  children: [
    {
      path: 'notifications',
      name: 'NotificationCenter',
      component: () => import('@/views/alert-management/notifications/index.vue'),
      meta: {
        title: '消息通知中心',
        icon: 'Bell',
        permCode: 'alert:notifications',
        navGroup: 'warning',
        navOrder: 12
      }
    },
    {
      path: 'sos',
      name: 'SosPage',
      component: () => import('@/views/alert-management/sos/index.vue'),
      meta: {
        title: 'SOS 紧急救援',
        icon: 'Warning',
        permCode: 'alert:sos',
        navGroup: 'warning',
        navOrder: 15
      }
    },
    {
      path: 'config',
      name: 'AlertConfig',
      component: () => import('@/views/alert-management/config/index.vue'),
      meta: {
        title: '阈值配置',
        icon: 'Setting',
        permCode: 'alert:config',
        navGroup: 'warning',
        navOrder: 14
      }
    },
    {
      path: 'records',
      name: 'AlertRecords',
      component: () => import('@/views/alert-management/records/index.vue'),
      meta: {
        title: '预警记录',
        icon: 'List',
        permCode: 'alert:records',
        navGroup: 'warning',
        navOrder: 13
      }
    }
  ]
}
