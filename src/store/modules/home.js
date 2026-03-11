/**
 * home Vuex 模块（历史遗留，当前项目已不再使用）
 *
 * 原用途：旧版项目的首页 mock 数据加载（/home/list 接口）。
 * 当前状态：接口和 mock 均已移除，此模块保留但不再调用。
 * 可安全删除，保留仅为兼容 store/index.js 中的 modules 注册。
 */
const state = {
  list: {},      // 首页列表数据（已废弃）
  averages: {}   // 平均值数据（已废弃）
}

const mutations = {
  GETDATA(state, list) {
    state.list = list
  }
}

const actions = {}  // 原 getData action 已废弃，接口不存在

const getters = {}

export default {
  state,
  mutations,
  actions,
  getters
}