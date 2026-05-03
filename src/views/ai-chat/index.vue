<template>
  <div class="ai-chat-page">
    <div class="chat-header">
      <span class="title">AI 健康助手</span>
      <span class="subtitle">你可以问我关于员工健康数据的任何问题</span>
      <div class="header-btns">
        <el-button size="small" @click="exportChat" :disabled="messages.length === 0" class="new-chat-btn">
          导出对话
        </el-button>
        <el-button size="small" @click="newChat" :disabled="loading" class="new-chat-btn">
          新对话
        </el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <!-- 欢迎消息 -->
      <div class="message assistant">
        <div class="avatar">AI</div>
        <div class="bubble">
          你好！我是健康管理 AI 助手。<br>
          你可以问我：<br>
          • 综采一队本周有几人心率超标？<br>
          • 张三最近的血氧情况如何？<br>
          • 哪个部门平均心率最高？<br>
          • 本月预警次数最多的是谁？
        </div>
      </div>

      <!-- 对话记录 -->
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="msg-body">
          <div class="bubble" v-html="formatMessage(msg.content)"></div>
          <!-- SQL 调试块：仅 AI 消息且有 SQL 时显示 -->
          <div v-if="msg.sql" class="sql-debug">
            <div class="sql-toggle" @click="msg.sqlOpen = !msg.sqlOpen">
              <span>{{ msg.sqlOpen ? '▼' : '▶' }} 查看生成的 SQL</span>
            </div>
            <pre v-if="msg.sqlOpen" class="sql-block">{{ msg.sql }}</pre>
          </div>
          <!-- P1/P2: 数据可视化块 -->
          <template v-if="msg.queryData && msg.queryData.length >= 2">
            <div v-if="getVizType(msg.queryData) === 'table'" class="viz-table-wrap">
              <table class="viz-table">
                <thead>
                  <tr><th v-for="col in Object.keys(msg.queryData[0])" :key="col">{{ col }}</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in msg.queryData" :key="ri">
                    <td v-for="col in Object.keys(msg.queryData[0])" :key="col">{{ formatCell(row[col]) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else-if="getVizType(msg.queryData) !== 'none'"
                 :id="'viz-chart-' + index"
                 :style="{ height: getVizType(msg.queryData) === 'bar' ? Math.min(msg.queryData.length * 28 + 20, 420) + 'px' : '220px' }"
                 class="viz-chart">
            </div>
          </template>
        </div>
      </div>

      <!-- 加载中 -->
      <div class="message assistant" v-if="loading">
        <div class="avatar">AI</div>
        <div class="bubble loading">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        placeholder="输入你的问题，按 Enter 发送..."
        :disabled="loading"
        @keyup.enter="sendMessage"
        class="chat-input"
      />
      <el-button
        type="primary"
        :loading="loading"
        @click="sendMessage"
        class="send-btn"
      >
        发送
      </el-button>
    </div>

    <!-- 快捷问题 -->
    <div class="quick-questions">
      <span class="label">快捷提问：</span>
      <el-tag
        v-for="q in quickQuestions"
        :key="q"
        @click="askQuick(q)"
        class="quick-tag"
        :class="{ disabled: loading }"
      >{{ q }}</el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { clearAiSession } from '@/api/ai'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { getMarked } from '@/utils/lazy-vendors'
import * as echarts from '@/utils/echarts-setup'

const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const messageListRef = ref(null)
const markdownParser = ref(null)

// 会话ID：每次打开页面生成一个，同一会话内保持不变
const sessionId = ref(generateSessionId())

function generateSessionId() {
  return 'sess-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
}

// 默认快捷问题（兜底，获取部门失败时使用）
const quickQuestions = ref([
  '本周有多少人心率异常？',
  '各部门平均血氧对比',
  '睡眠不足的员工有哪些？',
  '今天有哪些预警未处理？'
])

/**
 * B5: 动态快捷问题 —— 从数据库拉取真实部门名称生成个性化问题
 *
 * 生成策略：
 *   - 随机选 2 个部门，分别生成"该部门血氧/心率"问题
 *   - 保留 2 个通用聚合问题（睡眠、预警）
 *   - 总共 4 个快捷标签
 */
async function loadDynamicQuickQuestions() {
  try {
    const res = await request({ url: '/department/list', method: 'get' })
    const depts = res.data || []
    if (depts.length === 0) return

    // 随机打乱，取前 2 个部门
    const shuffled = [...depts].sort(() => Math.random() - 0.5)
    const picked = shuffled.slice(0, 2)

    // 问题模板：每个部门随机选一种维度
    const templates = [
      (d) => `${d.deptName}平均心率是多少？`,
      (d) => `${d.deptName}血氧正常率如何？`,
      (d) => `${d.deptName}有多少人压力偏高？`,
      (d) => `${d.deptName}本周有哪些预警？`,
    ]

    const deptQuestions = picked.map((d, i) =>
      templates[(i + Math.floor(Math.random() * templates.length)) % templates.length](d)
    )

    quickQuestions.value = [
      ...deptQuestions,
      '睡眠不足的员工有哪些？',
      '各部门平均血氧排名'
    ]
  } catch (e) {
    // 网络失败时保留默认问题，不报错
  }
}

onMounted(() => {
  loadDynamicQuickQuestions()
  ensureMarkdownParser()
})

async function ensureMarkdownParser() {
  if (!markdownParser.value) {
    markdownParser.value = await getMarked()
  }
  return markdownParser.value
}

/**
 * 流式发送消息
 *
 * 【核心原理】
 * 1. 先在消息列表加一条空的 assistant 消息（占位）
 * 2. 用 fetch 发 POST 请求，获取可读流（ReadableStream）
 * 3. 逐块读取流数据，解析 SSE 格式的 "data: token\n\n"
 * 4. 每收到一个 token 就追加到那条 assistant 消息里
 * 5. 收到 [DONE] 信号时结束
 */
async function sendMessage() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', content: question })
  inputText.value = ''
  loading.value = true

  // 添加 AI 占位消息（content 逐字填入，sql/queryData 在流结束时填入）
  const aiMsgIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '', sql: null, sqlOpen: false, queryData: null })
  scrollToBottom()

  try {
    // 用原生 fetch 请求流式接口（axios 不支持流式，必须用 fetch）
    // 注意：必须走 /dev-api/ 代理路径，Vite 会转发到后端 /health/
    const response = await fetch('/dev-api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'satoken': getToken() || ''
      },
      body: JSON.stringify({ question, sessionId: sessionId.value })
    })

    if (!response.ok) {
      messages.value[aiMsgIndex].content = '请求失败，请稍后重试。'
      return
    }

    // 读取流
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 按行解析 SSE（格式: "data: xxx\n\n"）
      const lines = buffer.split('\n')
      buffer = lines.pop() // 最后一行可能不完整，留到下次

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()

        if (data === '[DONE]') {
          loading.value = false
          scrollToBottom()
          break
        }
        if (data.startsWith('[SESSION]:')) {
          sessionId.value = data.slice(10)
          continue
        }
        if (data.startsWith('[DATA]:')) {
          // P1/P2: 解析原始查询结果，用于渲染表格或图表
          try {
            const queryData = JSON.parse(decodeBase64Utf8(data.slice(7)))
            messages.value[aiMsgIndex].queryData = queryData
            const vtype = getVizType(queryData)
            if (vtype === 'bar' || vtype === 'line') {
              nextTick(() => initChart(aiMsgIndex, queryData, vtype))
            }
          } catch (e) { /* 忽略解析错误 */ }
          continue
        }
        if (data.startsWith('[SQL]:')) {
          // Base64 解码 SQL
          const sqlB64 = data.slice(6)
          messages.value[aiMsgIndex].sql = decodeBase64Utf8(sqlB64)
          continue
        }
        if (data.startsWith('[ERROR]')) {
          messages.value[aiMsgIndex].content = data.slice(7)
          continue
        }

        // 正常 token：追加到 AI 消息
        messages.value[aiMsgIndex].content += data
        scrollToBottom()
      }
    }
  } catch (e) {
    messages.value[aiMsgIndex].content = '网络错误，请稍后重试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function decodeBase64Utf8(base64Text) {
  const binary = atob(base64Text)
  const bytes = Uint8Array.from(binary, ch => ch.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

/**
 * B6: 导出对话 —— 生成 HTML 打印页，让用户另存为 PDF 或直接打印
 *
 * 策略：
 *   1. 生成一个包含样式的独立 HTML 字符串
 *   2. 用 window.open 打开新窗口
 *   3. 写入 HTML，延迟调用 window.print()（弹出系统打印对话框）
 *   4. 用户可选择"另存为 PDF"或打印纸质版
 */
async function exportChat() {
  if (messages.value.length === 0) return

  const parser = await ensureMarkdownParser()
  const now = new Date().toLocaleString('zh-CN')
  const rows = messages.value.map(msg => {
    const role = msg.role === 'user' ? '用户' : 'AI助手'
    const cls = msg.role === 'user' ? 'user' : 'ai'
    // 使用 marked 渲染 AI 的 Markdown 内容
    const content = msg.role === 'assistant'
      ? parser.parse(msg.content || '')
      : `<p>${(msg.content || '').replace(/</g, '&lt;')}</p>`
    const sqlPart = msg.sql ? `<details><summary>生成的 SQL</summary><pre>${msg.sql.replace(/</g, '&lt;')}</pre></details>` : ''
    return `<div class="msg ${cls}"><div class="role">${role}</div><div class="content">${content}${sqlPart}</div></div>`
  }).join('')

  const html = `<!DOCTYPE html>
<html lang="zh-CN"><head>
<meta charset="UTF-8">
<title>AI健康助手对话记录 ${now}</title>
<style>
  body { font-family: 'Microsoft YaHei', sans-serif; max-width: 800px; margin: 0 auto; padding: 24px; color: #222; }
  h1 { font-size: 18px; color: #0066cc; border-bottom: 2px solid #0066cc; padding-bottom: 8px; }
  .meta { font-size: 12px; color: #666; margin-bottom: 20px; }
  .msg { display: flex; gap: 12px; margin-bottom: 16px; }
  .role { min-width: 54px; font-weight: 700; font-size: 13px; padding-top: 3px; }
  .msg.user .role { color: #0066cc; }
  .msg.ai .role { color: #cc6600; }
  .content { flex: 1; background: #f5f8ff; border-radius: 6px; padding: 10px 14px; font-size: 14px; line-height: 1.7; }
  .msg.user .content { background: #e8f0ff; }
  details { margin-top: 8px; }
  summary { cursor: pointer; font-size: 12px; color: #888; }
  pre { background: #f0f0f0; padding: 8px; border-radius: 4px; font-size: 12px; white-space: pre-wrap; word-break: break-all; }
  p { margin: 4px 0; }
  strong { font-weight: 700; }
  @media print { body { padding: 0; } }
</style>
</head><body>
<h1>AI 健康助手 · 对话记录</h1>
<div class="meta">导出时间：${now}　共 ${messages.value.filter(m=>m.role==='user').length} 轮对话</div>
${rows}
</body></html>`

  const win = window.open('', '_blank')
  if (!win) { alert('请允许弹出窗口以导出对话'); return }
  win.document.write(html)
  win.document.close()
  setTimeout(() => win.print(), 600)
}

// ─── P1/P2: 数据可视化 ─────────────────────────────────────────────────────

// ECharts 实例 Map，key 为消息 index，用于 dispose 防止内存泄漏
const chartInstances = new Map()

/**
 * 判断数据适合哪种可视化方式
 * - 'line'  : 含时间列 + 数值列，>= 2 行 → 折线图
 * - 'bar'   : 含文本列 + 数值列，>= 2 行 → 水平柱状图
 * - 'table' : 多行多列，无明显分类 → 表格
 * - 'none'  : 0行 / 1行单列 → 不显示
 */
function getVizType(data) {
  if (!data || data.length === 0) return 'none'
  const keys = Object.keys(data[0])
  if (keys.length === 0) return 'none'

  const timeKeys = ['record_time', 'create_time', 'update_time', 'warning_time', 'date', 'month', 'week', 'day']
  const hasTime = keys.some(k => timeKeys.some(t => k.toLowerCase().includes(t)))
  const numCols = keys.filter(k => typeof data[0][k] === 'number' && isFinite(data[0][k]))
  const textCols = keys.filter(k => typeof data[0][k] === 'string')

  if (data.length < 2) return 'none'
  if (hasTime && numCols.length >= 1) return 'line'
  // 多指标（≥2个数值列）→ 表格展示，避免 bar 只显示第一列
  if (textCols.length >= 1 && numCols.length >= 2) return 'table'
  if (textCols.length >= 1 && numCols.length === 1) return 'bar'
  if (data.length >= 2 && keys.length >= 2) return 'table'
  return 'none'
}

/** 格式化单元格数值：数字保留2位小数，其他原样返回 */
function formatCell(val) {
  if (typeof val === 'number' && !isFinite(val)) return '-'
  if (typeof val === 'number') return Number.isInteger(val) ? val : val.toFixed(2)
  if (val === null || val === undefined) return '-'
  return val
}

/**
 * 初始化 ECharts（bar 或 line）
 * 在 nextTick 后调用，确保 DOM 已渲染
 */
function initChart(msgIndex, data, type) {
  const el = document.getElementById('viz-chart-' + msgIndex)
  if (!el) return
  // 如果已有实例先销毁
  if (chartInstances.has(msgIndex)) {
    chartInstances.get(msgIndex).dispose()
  }
  const chart = echarts.init(el)
  chartInstances.set(msgIndex, chart)

  const keys = Object.keys(data[0])
  const timeKeys = ['record_time', 'create_time', 'update_time', 'warning_time', 'date', 'month', 'week', 'day']
  const numCols = keys.filter(k => typeof data[0][k] === 'number' && isFinite(data[0][k]))

  if (type === 'bar') {
    const textCol = keys.find(k => typeof data[0][k] === 'string')
    const categories = data.map(r => r[textCol])
    // 主数值列（取第一个数值列作为主轴）
    const mainCol = numCols[0]
    const values = data.map(r => Number(r[mainCol]).toFixed(2))

    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: '30%', right: '8%', top: '5%', bottom: '5%', containLabel: false },
      tooltip: {
        trigger: 'axis', axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(0,20,50,0.9)',
        borderColor: 'rgba(0,212,255,0.3)',
        textStyle: { color: '#c8d8e8' }
      },
      xAxis: { type: 'value', axisLabel: { color: '#4a7098', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(0,212,255,0.08)' } } },
      yAxis: {
        type: 'category', data: categories,
        axisLabel: { color: '#c8d8e8', fontSize: 11 },
        inverse: false
      },
      series: [{
        type: 'bar', data: values, barMaxWidth: 18,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(0,100,200,0.7)' },
            { offset: 1, color: '#00d4ff' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: { show: true, position: 'right', color: '#c8d8e8', fontSize: 10, formatter: '{c}' }
      }]
    })
  } else if (type === 'line') {
    const timeCol = keys.find(k => timeKeys.some(t => k.toLowerCase().includes(t)))
    const categories = data.map(r => {
      const v = r[timeCol]
      return typeof v === 'string' ? v.slice(0, 16) : v  // 截掉秒数，保持简洁
    })
    const series = numCols.map((col, i) => ({
      name: col,
      type: 'line', smooth: true,
      data: data.map(r => Number(r[col]).toFixed(2)),
      lineStyle: { color: i === 0 ? '#00d4ff' : '#00ff88', width: 2 },
      itemStyle: { color: i === 0 ? '#00d4ff' : '#00ff88' },
      areaStyle: i === 0 ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(0,212,255,0.2)' },
        { offset: 1, color: 'rgba(0,212,255,0)' }
      ])} : undefined
    }))
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: '8%', right: '5%', top: '8%', bottom: '18%' },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0,20,50,0.9)',
        borderColor: 'rgba(0,212,255,0.3)',
        textStyle: { color: '#c8d8e8' }
      },
      legend: numCols.length > 1 ? { textStyle: { color: '#c8d8e8' }, top: 0 } : { show: false },
      xAxis: {
        type: 'category', data: categories,
        axisLabel: { color: '#4a7098', fontSize: 10, rotate: 30 },
        axisLine: { lineStyle: { color: 'rgba(0,212,255,0.2)' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#4a7098', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(0,212,255,0.08)' } }
      },
      series
    })
  }
}

onUnmounted(() => {
  chartInstances.forEach(c => c.dispose())
  chartInstances.clear()
})

// ─── 对话操作 ─────────────────────────────────────────────────────────────

// 开始新对话：清除后端历史 + 重置前端消息 + 生成新 sessionId
async function newChat() {
  if (loading.value) return
  try {
    await clearAiSession(sessionId.value)
  } catch (e) { /* 忽略清除失败 */ }
  // 销毁所有图表实例
  chartInstances.forEach(c => c.dispose())
  chartInstances.clear()
  sessionId.value = generateSessionId()
  messages.value = []
}

function askQuick(question) {
  if (loading.value) return
  inputText.value = question
  sendMessage()
}

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function formatMessage(text) {
  if (!text) return ''
  // Fix inline numbered lists: LLM sometimes outputs "1.**A** 2.**B**" without newlines.
  // Insert \n before each "N." that is NOT already at line start (N = 1-2 digits).
  const processed = text.replace(/([^\n])(\s{0,2})(\d{1,2}\.\s*\*\*)/g, (_, before, _sp, item) => {
    return before + '\n' + item
  })
  if (markdownParser.value) {
    return markdownParser.value.parse(processed)
  }
  return escapeHtml(processed).replace(/\n/g, '<br>')
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   科技风深色主题 — 与其他健康监测页面保持一致
   主色调：#0a1628（背景）/ #00d4ff（青色强调）/ #c8d8e8（主文字）
   ═══════════════════════════════════════════════════ */

.ai-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  padding: 16px 20px;
  background: #0a1628;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

/* ─── 页头 ─────────────────────────────────────────── */
.chat-header {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
}
.chat-header .title {
  font-size: 18px;
  font-weight: 700;
  color: #00d4ff;
  letter-spacing: 1px;
  margin-right: 12px;
  text-shadow: 0 0 12px rgba(0, 212, 255, 0.5);
}
.chat-header .subtitle {
  font-size: 12px;
  color: #4a7098;
  flex: 1;
}
.header-btns {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.new-chat-btn {
  background: rgba(0, 212, 255, 0.08) !important;
  border: 1px solid rgba(0, 212, 255, 0.35) !important;
  color: #00d4ff !important;
  font-size: 12px !important;
}
.new-chat-btn:hover {
  background: rgba(0, 212, 255, 0.18) !important;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.3);
}

/* ─── 消息列表 ─────────────────────────────────────── */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: rgba(0, 20, 50, 0.5);
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 8px;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.message-list::-webkit-scrollbar { width: 4px; }
.message-list::-webkit-scrollbar-track { background: transparent; }
.message-list::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.2); border-radius: 2px; }

/* ─── 消息行 ──────────────────────────────────────── */
.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.message.user { flex-direction: row-reverse; }

/* 头像 */
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  background: linear-gradient(135deg, #0066cc, #00aaff);
  color: #fff;
  border: 1px solid rgba(0, 212, 255, 0.4);
  box-shadow: 0 0 8px rgba(0, 170, 255, 0.3);
}
.message.user .avatar {
  background: linear-gradient(135deg, #006633, #00cc66);
  border-color: rgba(0, 255, 136, 0.4);
  box-shadow: 0 0 8px rgba(0, 204, 102, 0.3);
}

/* msg-body 包裹气泡 + SQL 调试块 */
.msg-body {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 72%;
}
.message.user .msg-body { align-items: flex-end; }

/* ─── 气泡 ────────────────────────────────────────── */
.bubble {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13.5px;
  line-height: 1.7;
  word-break: break-word;
  /* AI 气泡：深蓝卡片 */
  background: rgba(0, 40, 80, 0.7);
  color: #c8d8e8;
  border: 1px solid rgba(0, 212, 255, 0.15);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}
.message.user .bubble {
  /* 用户气泡：青色渐变 */
  background: linear-gradient(135deg, rgba(0,100,180,0.8), rgba(0,170,255,0.6));
  color: #e8f4ff;
  border-color: rgba(0, 212, 255, 0.4);
}

/* ─── SQL 调试块 ─────────────────────────────────── */
.sql-debug { font-size: 12px; }
.sql-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: rgba(0, 212, 255, 0.5);
  cursor: pointer;
  user-select: none;
  padding: 2px 0;
  transition: color 0.2s;
}
.sql-toggle:hover { color: #00d4ff; }
.sql-block {
  background: rgba(0, 10, 25, 0.9);
  color: #a6e3a1;
  border: 1px solid rgba(0, 212, 255, 0.15);
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-family: 'Consolas', 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

/* ─── Markdown 渲染 ───────────────────────────────── */
.bubble :deep(p) { margin: 0 0 6px 0; }
.bubble :deep(p:last-child) { margin-bottom: 0; }
.bubble :deep(strong) { font-weight: 700; color: #00d4ff; }
.bubble :deep(h2), .bubble :deep(h3) {
  font-size: 14px;
  font-weight: 700;
  margin: 8px 0 4px 0;
  color: #00d4ff;
  letter-spacing: 0.5px;
}
.bubble :deep(ul), .bubble :deep(ol) {
  margin: 4px 0;
  padding-left: 18px;
}
.bubble :deep(li) { margin: 3px 0; color: #b0c8e0; }
.bubble :deep(code) {
  background: rgba(0, 212, 255, 0.1);
  color: #7dd3fc;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: 'Consolas', monospace;
  font-size: 12.5px;
  border: 1px solid rgba(0, 212, 255, 0.2);
}
.bubble :deep(pre) {
  background: rgba(0, 10, 25, 0.9);
  color: #a6e3a1;
  border: 1px solid rgba(0, 212, 255, 0.15);
  padding: 10px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 6px 0;
}
.bubble :deep(pre code) {
  background: none; color: inherit; padding: 0; border: none;
}
/* 用户气泡内的 strong/h 不用青色 */
.message.user .bubble :deep(strong) { color: #e8f4ff; }
.message.user .bubble :deep(h2),
.message.user .bubble :deep(h3) { color: #e8f4ff; }

/* ─── 加载动画 ────────────────────────────────────── */
.loading {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 12px 14px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #00d4ff;
  animation: bounce 1.2s infinite;
  box-shadow: 0 0 6px rgba(0,212,255,0.6);
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* ─── 输入区 ──────────────────────────────────────── */
.input-area {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.chat-input { flex: 1; }
.chat-input :deep(.el-input__wrapper) {
  background: rgba(0, 20, 50, 0.8) !important;
  border: 1px solid rgba(0, 212, 255, 0.25) !important;
  box-shadow: none !important;
}
.chat-input :deep(.el-input__wrapper):hover,
.chat-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(0, 212, 255, 0.6) !important;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.2) !important;
}
.chat-input :deep(.el-input__inner) {
  color: #c8d8e8 !important;
  font-size: 13.5px;
}
.chat-input :deep(.el-input__inner::placeholder) { color: #3a5878; }
.send-btn {
  width: 72px;
  background: linear-gradient(135deg, #0066cc, #00aaff) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
}
.send-btn:hover {
  box-shadow: 0 0 12px rgba(0, 170, 255, 0.5) !important;
  opacity: 0.9;
}

/* ─── 快捷问题 ────────────────────────────────────── */
.quick-questions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.label {
  font-size: 11px;
  color: #3a5878;
}
.quick-tag {
  cursor: pointer;
  user-select: none;
  background: rgba(0, 212, 255, 0.05) !important;
  border-color: rgba(0, 212, 255, 0.25) !important;
  color: #5a9abd !important;
  font-size: 12px !important;
  transition: all 0.2s;
}
.quick-tag:hover {
  background: rgba(0, 212, 255, 0.15) !important;
  border-color: rgba(0, 212, 255, 0.5) !important;
  color: #00d4ff !important;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.2);
}
.quick-tag.disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* ─── P1/P2: 数据可视化 ──────────────────────────────── */
.viz-table-wrap {
  max-height: 280px;
  overflow-y: auto;
  margin-top: 8px;
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 6px;
}
.viz-table-wrap::-webkit-scrollbar { width: 4px; height: 4px; }
.viz-table-wrap::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.2); border-radius: 2px; }
.viz-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  color: #c8d8e8;
}
.viz-table thead tr { background: rgba(0, 212, 255, 0.08); position: sticky; top: 0; }
.viz-table th {
  padding: 6px 10px;
  text-align: left;
  color: #00d4ff;
  font-weight: 600;
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
  white-space: nowrap;
}
.viz-table td {
  padding: 5px 10px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.06);
  white-space: nowrap;
}
.viz-table tbody tr:hover { background: rgba(0, 212, 255, 0.05); }
.viz-table tbody tr:last-child td { border-bottom: none; }

.viz-chart {
  margin-top: 8px;
  width: 100%;
  min-height: 160px;
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 6px;
  background: rgba(0, 10, 30, 0.4);
}
</style>
