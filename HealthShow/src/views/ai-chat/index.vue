<template>
  <div class="ai-chat-page">
    <PageHeroHeader
      class="chat-hero"
      eyebrow="AI Assistant"
      title="AI 健康助手"
      description="你可以问我关于员工健康数据的任何问题"
    >
      <template #meta>
        <div class="chat-hero-meta">
          <span class="chat-live-dot"></span>
          <span class="chat-hero-meta-label">会话中</span>
          <span class="chat-hero-meta-count">{{ messages.length }} 条消息</span>
        </div>
      </template>
      <template #actions>
        <div class="header-btns">
        <el-button size="small" @click="exportChat" :disabled="messages.length === 0" class="new-chat-btn">
          导出对话
        </el-button>
        <el-button size="small" @click="newChat" :disabled="loading" class="new-chat-btn">
          新对话
        </el-button>
        </div>
      </template>
    </PageHeroHeader>

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
                  <tr><th v-for="col in getQueryColumns(msg)" :key="col">{{ col }}</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in msg.queryData" :key="ri">
                    <td v-for="col in getQueryColumns(msg)" :key="col">{{ formatCell(row[col]) }}</td>
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
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import { clearAiSession } from '@/api/ai'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { getMarked } from '@/utils/lazy-vendors'
import { createAiChatChartRegistry } from './ai-chat-chart.js'
import { printChatTranscript } from './ai-chat-export.js'
import {
  decodeBase64Utf8,
  formatCell,
  getQueryColumns,
  getVizType,
  normalizeQueryPayload
} from './ai-chat-query-result.js'
import { DEFAULT_QUICK_QUESTIONS, buildDynamicQuickQuestions, generateSessionId } from './ai-chat-session.js'
import { formatChatMessage } from './ai-chat-text.js'

const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const messageListRef = ref(null)
const markdownParser = ref(null)

const sessionId = ref(generateSessionId())
const chartRegistry = createAiChatChartRegistry()
const quickQuestions = ref([...DEFAULT_QUICK_QUESTIONS])

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
    const questions = buildDynamicQuickQuestions(res.data || [])
    if (questions) quickQuestions.value = questions
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
  messages.value.push({ role: 'assistant', content: '', sql: null, sqlOpen: false, queryData: null, queryColumns: null, queryResult: null })
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
            const queryResult = normalizeQueryPayload(JSON.parse(decodeBase64Utf8(data.slice(7))))
            messages.value[aiMsgIndex].queryResult = queryResult
            messages.value[aiMsgIndex].queryData = queryResult.rows
            messages.value[aiMsgIndex].queryColumns = queryResult.columns
            const vtype = getVizType(queryResult.rows)
            if (vtype === 'bar' || vtype === 'line') {
              nextTick(() => chartRegistry.initChart(aiMsgIndex, queryResult.rows, vtype))
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
  printChatTranscript(messages.value, parser)
}

onUnmounted(() => {
  chartRegistry.disposeAll()
})

// ─── 对话操作 ─────────────────────────────────────────────────────────────

// 开始新对话：清除后端历史 + 重置前端消息 + 生成新 sessionId
async function newChat() {
  if (loading.value) return
  try {
    await clearAiSession(sessionId.value)
  } catch (e) { /* 忽略清除失败 */ }
  chartRegistry.disposeAll()
  sessionId.value = generateSessionId()
  messages.value = []
}

function askQuick(question) {
  if (loading.value) return
  inputText.value = question
  sendMessage()
}

function formatMessage(text) {
  return formatChatMessage(text, markdownParser.value)
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
@import './ai-chat.scss';
</style>
