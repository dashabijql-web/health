/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              Vuex User 模块（用户状态管理，新手必读）                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【这个文件管理什么？】
 *
 * 与用户登录状态相关的所有数据：
 *   - token：认证令牌（Cookie 中持久化，store 中响应式）
 *   - name：显示名（顶部导航栏显示）
 *   - avatar：头像 URL
 *   - routes：过滤后的可见路由（侧边栏菜单）
 *   - roles：角色列表（['user'] 等）
 *   - buttons：按钮权限码（['user:add', 'user:delete'] 等）
 *
 * 【Vuex 模块的结构】
 *
 * state：数据（类似 Vue 组件的 data）
 * mutations：同步修改 state 的函数（只能是同步！）
 * actions：异步操作（调用 API 等），完成后 commit mutation
 * namespaced：开启命名空间，避免名称冲突
 *
 * 调用方式：
 *   store.dispatch('user/login', {username, password})  → 调用 login action
 *   store.dispatch('user/getInfo')                      → 获取用户信息
 *   store.dispatch('user/logout')                       → 退出登录
 *   store.dispatch('user/resetToken')                   → 清除 token（静默）
 *
 * 【filterRoutes 函数说明（核心功能）】
 *
 * 用途：根据用户的权限码列表，过滤出当前用户可以看到的路由（菜单）
 *
 * 输入：
 *   routes = 全部路由（constantRoutes）
 *   permCodes = 当前用户的权限码列表（如 ['health:dashboard', 'user:list']）
 *
 * 输出：
 *   过滤后的路由数组（只包含用户有权限的路由）
 *
 * 过滤规则：
 *   1. hidden=true 的路由（login/404）始终保留（不受权限控制）
 *   2. 没有 permCode 的父路由：至少有一个子路由通过过滤才保留
 *   3. 叶子路由（没有 children）：有 permCode 的，需要在 permCodes 中才保留；
 *      没有 permCode 的直接保留
 *   4. permCodes 为空（权限未加载时）→ 返回全部路由（降级兼容）
 *
 * 示例：
 *   输入 permCodes = ['health:dashboard', 'health:realtime']
 *   输出：healthMonitorRouter 只保留 dashboard 和 real-time 两个子路由
 *         其他（heart-rate、blood-oxygen 等）被过滤掉不显示
 */

import { login, logout, getInfo } from '@/api/user'         // 登录/退出/获取信息 API
import { getToken, setToken, removeToken } from '@/utils/auth'  // Cookie 操作
import { resetRouter, ensureAppRoutes, getAppRoutes } from '@/router'

// ─── 路由权限过滤函数 ──────────────────────────────────────────────

/**
 * 递归过滤路由
 *
 * 为什么递归？因为路由是嵌套的（父路由包含子路由）。
 * 对每一层路由都要过滤，最终得到"树形剪枝"后的路由树。
 *
 * @param {Array} routes    - 路由配置数组（可能包含 children 子路由）
 * @param {Array} permCodes - 当前用户拥有的权限码列表
 * @returns {Array} 过滤后的路由数组
 */
function filterRoutes(routes, permCodes) {
  // 权限码未加载（null/undefined）→ 降级兼容：返回全部路由
  if (permCodes === null || permCodes === undefined) return routes
  // 权限码为空数组 [] → 用户没有任何权限：返回空数组（不显示任何菜单）
  if (permCodes.length === 0) return []

  const res = []

  for (const route of routes) {
    const r = { ...route }  // 浅拷贝（避免修改原始路由对象）

    // 规则1：hidden=true 的路由不受权限控制（如 /login、/404）
    if (r.hidden) {
      res.push(r)
      continue  // 直接保留，进入下一个路由
    }

    if (r.children && r.children.length > 0) {
      // 有子路由的父路由：先递归过滤子路由
      r.children = filterRoutes(r.children, permCodes)
      // 父路由的显示条件：至少有一个子路由通过过滤
      if (r.children.length > 0) {
        res.push(r)  // 有可见子路由 → 保留父路由
      }
      // 没有可见子路由 → 父路由也不显示（隐式过滤）

    } else {
      // 叶子路由（没有 children 或 children 为空）
      if (!r.meta?.permCode || permCodes.includes(r.meta.permCode)) {
        // 无权限码（不受控制）或 用户拥有此权限码 → 保留
        res.push(r)
      }
      // 有权限码但用户没有 → 过滤掉（不放入 res）
    }
  }

  return res
}

// ─── 状态初始化函数 ────────────────────────────────────────────────

/**
 * 返回初始状态对象
 *
 * 封装为函数的原因：
 *   退出登录时需要重置状态，如果直接 Object.assign(state, {...}) 写死初始值，
 *   万一哪个字段初始值有误，所有地方都需要改。
 *   用函数封装后，只需修改这一处。
 */
const getDefaultState = () => ({
  token: getToken(),     // 从 Cookie 读取（页面刷新后仍然有效）
  name: '',              // 用户显示名（登录后通过 /auth/info 获取）
  avatar: '',            // 头像 URL
  routes: [],            // 从后端获取的原始路由权限码数组
  roles: [],             // 角色列表（roles.length > 0 是"已登录"的标志）
  buttons: [],           // 按钮权限码列表
  resultAsyncRoutes: [], // 动态路由（本项目暂未使用）
  resultAllRoutes: []    // 登录后加载业务路由，再按权限过滤
})

const state = getDefaultState()

// ─── Mutations（同步修改 state）────────────────────────────────────

const mutations = {
  /**
   * 重置为初始状态
   * 退出登录时调用，清空所有用户数据
   * Object.assign(state, getDefaultState()) 将初始状态的所有属性覆盖到 state
   */
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },

  /**
   * 设置 Token
   * @param {string} token - 从登录接口获取的 Token 字符串
   */
  SET_TOKEN: (state, token) => {
    state.token = token
  },

  /**
   * 设置用户信息（登录成功或 getInfo 成功后调用）
   * @param {Object} userInfo - 包含 name、avatar、roles、buttons、routes 的对象
   */
  SET_USERINFO: (state, userInfo) => {
    state.name    = userInfo.name
    state.avatar  = userInfo.avatar
    state.routes  = userInfo.routes  || []  // 后端返回的路由权限码数组
    state.buttons = userInfo.buttons || []  // 按钮权限码数组
    state.roles   = userInfo.roles   || []  // 角色数组
  },

  /**
   * 设置过滤后的路由（侧边栏菜单用）
   * @param {Array} routes - 已按权限过滤后的可见路由数组
   */
  SET_RESULTASYNCROUTES: (state, routes) => {
    state.resultAllRoutes = routes
  }
}

// ─── Actions（异步操作）────────────────────────────────────────────

const actions = {
  /**
   * 登录 Action
   *
   * 流程：
   *   1. 调用 /auth/login 接口，传用户名和密码
   *   2. 接口成功返回 Token
   *   3. 提交 SET_TOKEN mutation，更新 store 中的 token
   *   4. 调用 setToken() 把 Token 存入 Cookie（持久化）
   *   5. 返回 'ok'（让组件知道登录成功）
   *
   * 失败时：抛出 Error，组件的 catch 分支处理（如显示错误提示）
   *
   * @param {Object} userInfo - { username, password }
   */
  async login({ commit }, userInfo) {
    const { username, password } = userInfo
    // login() 是 api/user.js 中的函数，调用 POST /auth/login 接口
    const result = await login({ username: username.trim(), password })

    if (result.code == 200) {
      commit('SET_TOKEN', result.data.token)  // 更新 store
      setToken(result.data.token)             // 存入 Cookie
      return 'ok'                             // 告知调用方登录成功
    }

    // 业务状态码非 200，说明登录失败（用户名错误、密码错误等）
    return Promise.reject(new Error(result.message || '登录失败'))
  },

  /**
   * 获取用户信息 Action
   *
   * 触发时机：
   *   1. 首次登录成功后（permission.js 中获取权限然后放行路由）
   *   2. 页面刷新后（store 被清空，roles 为空，触发 getInfo 重新获取）
   *
   * 流程：
   *   1. 调用 /auth/info 接口（携带 Token）
   *   2. 接口返回用户名、头像、角色、权限码等
   *   3. 提交 SET_USERINFO，更新用户基本信息
   *   4. 提交 SET_RESULTASYNCROUTES，过滤路由（更新侧边栏菜单）
   *
   * @returns {Promise} 解析后的用户信息
   */
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(async response => {
        const { data } = response
        if (!data) {
          reject('获取用户信息失败')
          return
        }

        await ensureAppRoutes()
        const allRoutes = await getAppRoutes()

        // 存储用户信息到 state
        commit('SET_USERINFO', data)

        // 根据路由权限码过滤路由（更新侧边栏菜单）
        // data.routes 是后端返回的权限码数组，如 ['health:dashboard', 'user:list']
        commit('SET_RESULTASYNCROUTES', filterRoutes(allRoutes, data.routes || []))

        resolve(data)
      }).catch(reject)
    })
  },

  /**
   * 退出登录 Action
   *
   * 流程：
   *   1. 调用 /auth/logout 接口（服务端销毁 Token）
   *   2. 无论成功失败，都清理本地状态：
   *      - removeToken()：删除 Cookie 中的 Token
   *      - resetRouter()：重置路由（本项目是 no-op）
   *      - commit('RESET_STATE')：清空 store 中的用户数据
   *
   * 为什么失败了也要清理？
   *   即使服务端接口调用失败（网络错误），也要确保本地 Token 被清除，
   *   防止用户以为自己还在登录状态（即使实际上 Token 已失效）。
   */
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        removeToken()           // 删除 Cookie Token
        resetRouter()           // 重置路由（本项目 no-op）
        commit('RESET_STATE')   // 清空 store
        resolve()
      }).catch(() => {
        // 退出接口失败（如网络断开），仍然清理本地状态
        removeToken()
        commit('RESET_STATE')
        resolve()  // 仍然 resolve（退出操作对用户来说"成功了"）
      })
    })
  },

  /**
   * 静默清除 Token Action（不调用服务端接口）
   *
   * 与 logout 的区别：
   *   logout：调用服务端 /auth/logout，然后清理本地
   *   resetToken：只清理本地（用于 Token 已经失效的情况，无需再通知服务端）
   *
   * 触发场景：
   *   - permission.js：getInfo 失败时（Token 可能已过期）
   *   - heartbeat.js：心跳检测失败时（服务端 Token 已注销）
   */
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken()           // 删除 Cookie Token
      commit('RESET_STATE')   // 清空 store
      resolve()
    })
  }
}

// 导出模块配置
export default {
  namespaced: true,  // 开启命名空间，调用时需要加 'user/' 前缀
  state,
  mutations,
  actions
}
