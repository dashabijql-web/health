/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              路由守卫（导航权限控制，新手必读）                        ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【什么是路由守卫？】
 *
 * router.beforeEach 是"全局前置守卫"：
 *   每次路由跳转前都会执行这个函数。
 *   next() = 放行（允许跳转）
 *   next('/xxx') = 重定向到 /xxx
 *   不调用 next() = 卡住（跳转被阻止）
 *
 * 【NProgress 进度条】
 *
 * NProgress 是页面顶部的蓝色进度条（类似 YouTube）。
 *   NProgress.start() = 显示进度条（路由跳转开始时）
 *   NProgress.done() = 隐藏进度条（跳转完成时）
 *
 * 【完整权限验证流程】
 *
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  路由跳转触发                                              │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  有 Token？                                              │
 *   │  ├─ 是：目标是 /login？                                  │
 *   │  │     ├─ 是：重定向到首页（已登录不需要再登录）            │
 *   │  │     └─ 否：store 中有 roles？                          │
 *   │  │           ├─ 是：直接放行（用户信息已加载）             │
 *   │  │           └─ 否：调用 getInfo 获取用户信息              │
 *   │  │                 ├─ 成功：放行（replace 避免历史记录问题）│
 *   │  │                 └─ 失败：清除 Token → 跳登录            │
 *   │  └─ 否：目标在白名单？                                    │
 *   │        ├─ 是：直接放行（如 /login 不需要 Token）           │
 *   │        └─ 否：重定向到 /login（携带 redirect 参数）        │
 *   └─────────────────────────────────────────────────────────┘
 *
 * 【为什么刷新页面后 roles 为空？】
 *
 * Vuex store 是内存中的数据，页面刷新（F5）后全部清空。
 * 但 Token 存在 Cookie 中，刷新后仍然有效。
 * 所以刷新后：有 Token 但 roles 为空 → 触发 getInfo 重新获取用户信息。
 *
 * 【next({ ...to, replace: true }) 为什么要 replace？】
 *
 * getInfo 是异步的，等它完成的过程中，路由跳转被暂停了。
 * 用 next(to) 放行后，Vue Router 内部会再次触发 beforeEach。
 * 用 replace: true 可以避免这次"重入"被记录到浏览器历史记录中。
 */

import router from './router'
import store from './store'
import NProgress from 'nprogress'     // 页面顶部进度条库
import 'nprogress/nprogress.css'      // 进度条样式
import { getToken } from '@/utils/auth'
import { ensureAppRoutes, hasLoadedAppRoutes } from '@/router'

// 不显示右侧转圈的 spinner（只显示顶部进度条）
NProgress.configure({ showSpinner: false })

/**
 * 不需要登录即可访问的路径白名单
 * /login 登录页：未登录时可以直接访问
 */
const whiteList = ['/login']

// ─── 全局前置守卫 ──────────────────────────────────────────────────

router.beforeEach(async (to, from, next) => {
  NProgress.start()  // 显示顶部进度条

  if (getToken()) {
    // ─── 情况1：有 Token（已登录或 Cookie 未过期）────────────────
    if (to.path === '/login') {
      // 已登录的用户访问登录页 → 重定向到首页（无需再次登录）
      next({ path: '/' })
      NProgress.done()
    } else {
      if (store.getters.roles && store.getters.roles.length > 0) {
        if (!hasLoadedAppRoutes() || (to.path !== '/404' && to.matched.length === 0)) {
          await ensureAppRoutes()
          next({ ...to, replace: true })
          return
        }
        // store 中已有用户信息（正常已登录状态）→ 直接放行
        next()
      } else {
        // store 中没有用户信息（刷新页面后 Vuex 被清空）
        // → 携带 Token 重新请求 /auth/info 获取用户信息
        try {
          await store.dispatch('user/getInfo')
          // getInfo 成功：用 replace:true 放行，避免产生多余的历史记录
          next({ ...to, replace: true })
        } catch (error) {
          // getInfo 失败（Token 过期/无效）→ 清除 Token 并重定向登录
          await store.dispatch('user/resetToken')
          next('/login')
          NProgress.done()
        }
      }
    }
  } else {
    // ─── 情况2：没有 Token（未登录或已退出）─────────────────────
    if (whiteList.indexOf(to.path) !== -1) {
      // 目标在白名单（如 /login）→ 直接放行
      next()
    } else {
      // 目标需要登录 → 重定向到登录页，并带上 redirect 参数
      // 登录成功后可以直接跳回原本要访问的页面
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

// ─── 全局后置守卫 ──────────────────────────────────────────────────

/**
 * 路由跳转完成后执行（无论成功还是失败）
 * 隐藏进度条（beforeEach 中 done() 只覆盖了部分情况）
 */
router.afterEach(() => {
  NProgress.done()
})
