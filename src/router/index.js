/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              路由配置（新手必读）                                    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【什么是前端路由？】
 *
 * 传统网站：每次点击链接，浏览器向服务器请求新页面（重新加载整个页面）。
 *
 * SPA（单页应用）：
 *   - 只加载一次 HTML（index.html）
 *   - 之后所有"页面切换"都是在同一个 HTML 中替换内容（不重新加载）
 *   - URL 的变化由前端 JavaScript 控制
 *   - 速度更快，体验更接近原生 App
 *
 * Vue Router 就是实现 SPA 路由的库：URL 变化 → 渲染对应的 Vue 组件
 *
 * 【Hash 模式 vs History 模式】
 *
 * createWebHashHistory()（本项目使用）：
 *   - URL 格式：http://localhost:3000/#/health-monitor/dashboard
 *   - # 号后面的部分不会发送到服务器，完全由前端处理
 *   - 优点：无需服务器配置，刷新页面不会 404
 *   - 缺点：URL 中有 # 号，不太美观
 *
 * createWebHistory()（另一种模式）：
 *   - URL 格式：http://localhost:3000/health-monitor/dashboard
 *   - 更美观，但需要服务器配置（Nginx）支持，否则刷新 404
 *
 * 本项目选择 Hash 模式，配置简单，适合内网部署。
 *
 * 【路由对象的属性说明】
 *
 * path：URL 路径（如 '/login'、'/health-monitor/dashboard'）
 * component：对应的 Vue 组件（() => import(...) 是懒加载方式）
 * name：路由命名（用于编程式导航 this.$router.push({ name: 'Login' })）
 * redirect：重定向（访问 / 时自动跳转到 /health-monitor/dashboard）
 * hidden：自定义属性，true 表示不在侧边栏菜单中显示
 * meta：路由元信息（存放额外数据）
 *   - title：菜单/标签页标题
 *   - icon：菜单图标（使用 Element Plus Icons 的图标名）
 *   - affix：是否固定在标签页栏（不可关闭）
 *   - permCode：权限码（用于过滤路由，决定哪些用户能看到此菜单）
 *
 * 【懒加载（Lazy Loading）说明】
 *
 * component: () => import('@/views/login/index.vue')
 *
 * 不加懒加载：所有组件在首次加载时一次性下载（首屏慢）
 * 加懒加载：只在第一次访问该路由时才下载对应组件（首屏快）
 *
 * 【constantRoutes vs asyncRoutes】
 *
 * constantRoutes：所有用户都能访问的路由（登录页、404、健康监测等）
 * asyncRoutes：需要动态加载的路由（根据用户权限动态添加，本项目暂未使用）
 *
 * 本项目全部用 constantRoutes，通过 filterRoutes() 函数过滤显示哪些菜单，
 * 而不是动态添加/删除路由（这种方式更简单，适合中小型项目）。
 */

import { createRouter, createWebHashHistory } from 'vue-router'
import healthMonitorRouter from './health-monitor'
import orgManagementRouter from './org-management'
import personnelManagementRouter from './personnel-management'
import alertManagementRouter from './alert-management'
import statisticsRouter from './statistics'
import Layout from '@/layout/index.vue'

/**
 * 静态路由配置（所有用户都有的路由，无需权限控制）
 *
 * 路由结构说明：
 *   /login          → 登录页（hidden=true，不在侧边栏显示）
 *   /404            → 404 页面（hidden=true）
 *   /               → 重定向到 /health-monitor/dashboard
 *   /health-monitor → 健康监测模块（从 health-monitor.js 引入）
 *   /user-management → 用户管理模块
 *   /permission-management → 权限管理模块
 *   /:pathMatch(.*) → 通配符，任何未匹配路径都重定向到 /404
 */
export const constantRoutes = [
  // 登录页（不在菜单中显示）
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true  // 自定义属性：不在侧边栏中渲染此路由
  },

  // 404 页面（不在菜单中显示）
  {
    path: '/404',
    component: () => import('@/views/404.vue'),
    hidden: true
  },

  // 安全指挥中心
  {
    path: '/safety-command',
    component: Layout,
    name: 'SafetyCommand',
    meta: { title: '安全指挥中心', icon: 'Aim' },
    children: [
      {
        path: 'index',
        name: 'SafetyCommandIndex',
        component: () => import('@/views/safety-command/index.vue'),
        meta: { title: '安全指挥中心', icon: 'Aim' }
      }
    ]
  },

  // 根路径重定向
  // 访问 http://localhost:3000/#/ 时自动跳转到 /health-monitor/dashboard
  {
    path: '/',
    redirect: '/health-monitor/dashboard',
    hidden: true
  },

  // 健康监测模块路由（从独立文件引入，保持路由文件整洁）
  healthMonitorRouter,

  // 设备管理模块
  {
    path: '/device-management',
    component: Layout,
    name: 'DeviceManagement',
    meta: { title: '设备管理', icon: 'Monitor' },
    children: [
      {
        path: 'list',
        name: 'DeviceList',
        component: () => import('@/views/device-management/index.vue'),
        meta: {
          title: '设备列表',
          icon: 'Monitor',
          permCode: 'device:list'
        }
      }
    ]
  },

  // 用户管理模块
  {
    path: '/user-management',
    component: Layout,                // 使用主布局（含侧边栏导航）
    name: 'UserManagement',
    meta: { title: '用户管理', icon: 'User' },  // 侧边栏显示标题和图标
    children: [
      {
        path: 'list',                 // 完整路径：/user-management/list
        name: 'UserList',
        component: () => import('@/views/user-list/index.vue'),
        meta: {
          title: '用户列表',
          icon: 'UserFilled',
          permCode: 'user:list'       // 权限码：只有拥有 'user:list' 权限的用户才能看到此菜单
        }
      }
    ]
  },

  // 权限管理模块
  {
    path: '/permission-management',
    component: Layout,
    name: 'PermissionManagement',
    meta: { title: '权限管理', icon: 'Lock' },
    children: [
      {
        path: 'role',
        name: 'RoleManagement',
        component: () => import('@/views/role-management/index.vue'),
        meta: { title: '角色管理', icon: 'Key', permCode: 'role:list' }
      }
    ]
  },

  // 组织管理、人员管理、告警管理、统计分析
  orgManagementRouter,
  personnelManagementRouter,
  alertManagementRouter,
  statisticsRouter,

  // 兜底路由：所有未匹配的路径都跳转到 404（必须放最后）
  // /:pathMatch(.*)*  是 Vue Router 4 的通配符写法（Vue Router 3 用 * 号）
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    hidden: true
  }
]

/**
 * 创建路由实例
 *
 * history：使用 Hash 模式（URL 包含 #）
 * scrollBehavior：每次路由切换后滚动到页面顶部
 */
const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior: () => ({ top: 0 }),  // 切换路由时自动滚动到顶部
  routes: constantRoutes
})

/**
 * 动态路由（本项目暂未使用）
 * 保留定义是为了与其他项目的架构保持一致，方便后续扩展
 */
export const asyncRoutes = []  // 需要根据用户权限动态加载的路由
export const anyRoutes = []    // 所有用户都有但需要放在动态路由后面的路由

/**
 * 重置路由函数
 *
 * 在某些架构中，退出登录后需要删除动态添加的路由（防止权限泄露）。
 * 本项目全部使用 constantRoutes（静态路由），退出登录不需要重置路由，
 * 所以这是一个空实现（no-op）。
 *
 * 这样做还避免了一个坑：如果删除路由，下次登录时路由丢失，
 * 需要重新添加，实现更复杂。静态路由方案更简单可靠。
 */
export function resetRouter() {
  // no-op：本项目使用静态路由，无需重置
}

export default router
