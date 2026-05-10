// 全局导航守卫：
// - 有 token 时恢复用户信息并放行业务路由
// - 无 token 时只允许白名单
// - 统一处理登录重定向和顶部进度条

import router from './router'
import store from './store'
import NProgress from 'nprogress'     // 页面顶部进度条库
import 'nprogress/nprogress.css'      // 进度条样式
import { getToken } from '@/utils/auth'

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
