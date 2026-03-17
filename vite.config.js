/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              Vite 构建工具配置（新手必读）                           ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Vite 是本项目的构建工具（代替 Webpack）：
 *   - 开发模式（npm run dev）：极速热更新，修改代码秒级生效
 *   - 生产模式（npm run build）：打包为静态文件，部署到 Nginx
 *
 * 【环境变量】
 *   通过 loadEnv(mode, ...) 读取 .env.xxx 文件：
 *   - 开发：.env.development（VITE_BASE_API=/dev-api，VITE_TARGET=http://localhost:8080）
 *   - 测试：.env.staging
 *   - 生产：.env.production（VITE_BASE_API=/prod-api）
 *
 * 【插件说明】
 *   - @vitejs/plugin-vue：支持 .vue 单文件组件
 *   - unplugin-auto-import：自动导入 Vue 3 组合式 API（ref/computed 等）
 *   - unplugin-vue-components：自动导入 Element Plus 组件（el-button 等不需要手动 import）
 *   - vite-plugin-svg-icons：将 src/icons/svg/ 目录下的 SVG 文件注册为图标
 *
 * 【代理配置（重点）】
 *   开发时浏览器无法直接跨域请求后端，Vite dev server 做了一层代理：
 *   浏览器请求 /dev-api/xxx → Vite 代理转发到 http://localhost:8080/health/xxx
 *   rewrite: (p) => p.replace(/^\/dev-api/, '/health') 的意思：
 *     把 URL 前缀 /dev-api 替换为 /health（后端的 context-path）
 *   agent: new http.Agent() 的意思：
 *     强制使用直连，绕过系统 http_proxy 代理（VPN 环境下防止 502 错误）
 */

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import http from 'node:http'
import fs from 'node:fs'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { VitePWA } from 'vite-plugin-pwa'

// Vite dev server 插件：接收前端 POST /perf-log，追加写入 perf.log
const perfLogPlugin = () => ({
  name: 'perf-log',
  configureServer(server) {
    const logFile = path.resolve(process.cwd(), 'perf.log')
    server.middlewares.use('/perf-log', (req, res) => {
      if (req.method !== 'POST') { res.end(); return }
      let body = ''
      req.on('data', chunk => body += chunk)
      req.on('end', () => {
        const line = `${new Date().toISOString()}  ${body}\n`
        fs.appendFileSync(logFile, line)
        res.end('ok')
      })
    })
  }
})

export default defineConfig(({ mode }) => {
  // 根据当前运行模式（development/staging/production）加载对应 .env 文件
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [
      perfLogPlugin(),
      vue(),
      // 自动导入 Element Plus 组件（无需在每个 .vue 文件手动 import）
      AutoImport({ resolvers: [ElementPlusResolver({ importStyle: false })] }),
      Components({ resolvers: [ElementPlusResolver({ importStyle: false })] }),
      // SVG 图标插件：把 src/icons/svg/*.svg 注册为 <svg-icon name="xxx"> 组件
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/icons/svg')],
        symbolId: 'icon-[name]',  // 图标 ID 格式：icon-文件名
      }),
      // PWA 支持：手机上"添加到主屏幕"后像原生 App 使用
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.ico', 'pwa-192.png', 'pwa-512.png'],
        manifest: {
          name: '职业健康监测管理系统',
          short_name: '健康监测',
          description: '信智科技职业健康监测管理系统',
          theme_color: '#0d2847',
          background_color: '#0a1e3d',
          display: 'standalone',
          orientation: 'portrait',
          start_url: '/',
          scope: '/',
          lang: 'zh-CN',
          icons: [
            { src: '/pwa-192.png', sizes: '192x192', type: 'image/png' },
            { src: '/pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
          ],
        },
        workbox: {
          // 只缓存静态资源，API 请求不走缓存（始终走网络）
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
          runtimeCaching: [
            {
              urlPattern: /^\/dev-api\//,
              handler: 'NetworkOnly',
            },
          ],
          // 跳过等待，新版本立即生效
          skipWaiting: true,
          clientsClaim: true,
        },
        // 开发环境也启用 PWA（方便调试）
        devOptions: { enabled: false },
      }),
    ],
    // 路径别名：@ 映射到 src/ 目录，import '@/api/user' = import 'src/api/user'
    resolve: { alias: { '@': path.resolve(__dirname, './src') } },
    css: {
      preprocessorOptions: {
        // SCSS 编译选项：使用新版 API，压制废弃警告
        scss: { api: 'modern-compiler', silenceDeprecations: ['import', 'legacy-js-api'] }
      }
    },
    // 预构建依赖（加快冷启动速度）
    optimizeDeps: {
      include: ['@vue/shared', 'element-plus', 'vue', 'vue-router', 'vuex']
    },
    build: {
      chunkSizeWarningLimit: 1500,
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'vuex'],
            'vendor-element': ['element-plus', '@element-plus/icons-vue'],
            'vendor-echarts': ['echarts'],
          }
        }
      }
    },
    server: {
      host: '0.0.0.0', // 监听所有网卡（允许局域网内其他设备访问 http://本机IP:9528）
      port: 9528,       // 前端开发服务器端口
      open: true,       // 启动后自动打开浏览器
      proxy: {
        // 将所有 /dev-api 开头的请求代理到后端
        '/dev-api': {
          target: env.VITE_TARGET || 'http://localhost:8080', // 后端地址
          changeOrigin: true,  // 修改请求头中的 Origin 为 target 地址（解决跨域）
          // URL 重写：/dev-api/auth/login → /health/auth/login
          rewrite: (p) => p.replace(/^\/dev-api/, '/health'),
          // 使用直连 Agent，绕过系统 http_proxy / VPN 代理
          // 避免 Node.js 把对 localhost:8080 的请求路由到 VPN 代理导致 502/ECONNRESET
          agent: new http.Agent(),
        },
      },
    },
  }
})
