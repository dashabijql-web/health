/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              Vue 应用入口文件（新手必读）                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【main.js 的作用】
 *
 * 这是整个前端应用的"总开关"，负责：
 *   1. 创建 Vue 应用实例
 *   2. 注册全局依赖（UI 框架、图标库）
 *   3. 安装插件（路由、状态管理）
 *   4. 注册全局组件
 *   5. 挂载应用到 HTML 页面的 #app 元素
 *
 * 这个文件在 index.html 加载时会被 Vite 自动引入并执行。
 *
 * 【Vue 3 vs Vue 2 的入口差异】
 *
 * Vue 2 写法（老项目可能见到）：
 *   new Vue({ el: '#app', router, store, render: h => h(App) })
 *
 * Vue 3 写法（本项目）：
 *   const app = createApp(App)
 *   app.use(router).use(store).mount('#app')
 *
 * 区别：Vue 3 用工厂函数创建实例，而不是直接 new，
 *       可以创建多个独立的 Vue 实例，互不干扰。
 *
 * 【import 语句执行顺序说明】
 *
 * JS 的 import 语句会按顺序执行被引入文件的代码。
 * 因此 './permission' 在 './heartbeat' 之前引入，
 * 路由守卫（permission.js）先于心跳检测（heartbeat.js）注册。
 */

// ─── Vue 3 核心 ───────────────────────────────────────────────────
import { createApp } from 'vue'

// ─── Element Plus UI 框架 ──────────────────────────────────────────
// Element Plus 是饿了么团队开发的 Vue 3 UI 组件库
// 提供按钮、表格、表单、弹窗、分页等大量现成组件
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'           // Element Plus 的样式文件（必须引入）

// zhCn = 中文语言包，让 Element Plus 组件中的文字显示为中文
// 例如：日期选择器的"年月日"、分页的"上一页/下一页"等
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// ─── Element Plus 图标库 ─────────────────────────────────────────
// * as Icons 将所有导出内容合并为一个对象
// 图标有 200+ 个，如：User, Setting, Lock, Search, Edit, Delete...
import * as Icons from '@element-plus/icons-vue'

// ─── 样式文件 ─────────────────────────────────────────────────────
// normalize.css：各浏览器默认样式不一致，normalize.css 将它们统一
// 例如：不同浏览器的 margin/padding 默认值不同，normalize 清除这些差异
import 'normalize.css/normalize.css'

// 项目自定义全局样式（变量、重置样式、通用工具类）
import '@/styles/index.scss'

// ─── 应用核心模块 ─────────────────────────────────────────────────
import App from './App.vue'         // 根组件（所有组件的父级容器）
import store from './store'         // Vuex 状态管理（用户信息、Token 等全局状态）
import router from './router'       // Vue Router（页面路由，URL 与组件的对应关系）

// ─── SVG 图标注册 ────────────────────────────────────────────────
// vite-plugin-svg-icons 插件：
// 将 src/icons/svg/ 目录下所有 .svg 文件打包为 SVG Sprite
// 使用方式：<svg-icon icon-class="user" />
import 'virtual:svg-icons-register'

// ─── 路由守卫（权限控制）────────────────────────────────────────────
// 引入即执行，注册全局路由守卫（beforeEach/afterEach）
// 守卫逻辑：检查 Token → 获取用户信息 → 过滤路由权限
// 必须在 router 和 store 之后引入（因为 permission.js 依赖它们）
import './permission'

// ─── 会话心跳检测 ────────────────────────────────────────────────
// 引入即执行，启动定时心跳机制（每30秒 ping /auth/info）
// 作用：后端重启后，前端自动检测并跳转到登录页，无需用户手动刷新
// 必须在 permission.js 之后引入（心跳依赖路由守卫已就绪）
import './heartbeat'

// ─── 全局自定义组件（已清理未使用的 CategorySelect / HintButton）──

// ════════════════════════════════════════════════════════════════
// 创建并配置 Vue 应用实例
// ════════════════════════════════════════════════════════════════

// 1. 创建 Vue 3 应用实例，以 App.vue 根组件为起点
const app = createApp(App)

// 2. 批量注册 Element Plus 图标
// Object.entries(Icons) 返回 [[名称, 组件], [名称, 组件], ...] 数组
// forEach 遍历：app.component('ArrowLeft', ArrowLeftIcon) ...
// 注册后可以在模板中直接使用：<el-icon><User /></el-icon>
Object.entries(Icons).forEach(([k, v]) => app.component(k, v))

// 3. 安装 Element Plus UI 框架（locale 指定中文）
app.use(ElementPlus, { locale: zhCn })

// 4. 安装 Vuex 状态管理
//    store 中包含：用户信息（user module）、侧边栏状态（app module）等
app.use(store)

// 5. 安装 Vue Router
//    安装后组件内可以用 this.$router（路由跳转）、this.$route（当前路由信息）
app.use(router)

// 6. 将 Vue 应用挂载到 public/index.html 中 id="app" 的 div 元素
//    挂载后，Vue 接管该 div 内的所有 DOM，开始渲染
app.mount('#app')
