<template>
  <div class="rt-root">
    <!-- Header -->
    <header class="rt-hd">
      <div class="rt-hd-left">
        <span class="rt-live-dot"></span>
        <h1 class="rt-hd-title">实时健康监控</h1>
      </div>

      <!-- 实时预警 ticker -->
      <div class="rt-ticker-wrap">
        <span class="rt-ticker-label">实时预警</span>
        <div class="rt-ticker-scroll">
          <template v-if="warningUsers.length">
            <!-- 双份内容实现无缝循环 -->
            <div class="rt-ticker-inner">
              <span
                v-for="(u, i) in [...warningUsers, ...warningUsers]"
                :key="u.userCode + '_' + i"
                class="rt-ticker-tag">
                {{ u.userName }} <em>{{ getUserIndicator(u) }}</em>
              </span>
            </div>
          </template>
          <span v-else class="rt-ticker-empty">暂无预警人员</span>
        </div>
      </div>

      <div class="rt-hd-right">
        <div class="rt-hd-stat">
          <span class="rt-hd-stat-val st-ok">{{ normalCount }}</span>
          <span class="rt-hd-stat-lbl">正常</span>
        </div>
        <div class="rt-hd-sep"></div>
        <div class="rt-hd-stat">
          <span class="rt-hd-stat-val st-warn" :class="{ 'val-blink': warningCount > 0 }">{{ warningCount }}</span>
          <span class="rt-hd-stat-lbl">预警中</span>
        </div>
        <div class="rt-hd-sep"></div>
        <div class="rt-hd-time">{{ currentTime }}</div>
      </div>
    </header>

    <!-- Body -->
    <section class="rt-bd">
      <main class="rt-main">
        <div class="rt-panel rt-table-panel">
          <!-- panel header with search bar -->
          <div class="rt-ph">
            <div class="rt-ph-left">
              <span class="rt-ph-dot"></span>
              <span class="rt-ph-title">在线用户实时状态</span>
              <span class="rt-badge-online">{{ filteredUserList.length }} 人在线</span>
              <span v-if="hrFilter" class="rt-badge-filter" @click="hrFilter = null; currentPage = 1">
                心率: {{ hrFilter.label }} &times;
              </span>
            </div>
            <div class="rt-ph-right">
              <el-input
                v-model="searchForm.name"
                placeholder="姓名/工号"
                clearable
                class="rt-inp"
                @clear="handleSearch"
                @keyup.enter="handleSearch" />
              <el-select
                v-model="searchForm.dept"
                placeholder="全部部门"
                clearable
                class="rt-sel"
                @change="handleSearch">
                <el-option v-for="d in deptList" :key="d" :label="d" :value="d" />
              </el-select>
              <el-select
                v-model="searchForm.status"
                placeholder="全部状态"
                clearable
                class="rt-sel-sm"
                @change="handleSearch">
                <el-option label="仅正常" value="normal" />
                <el-option label="仅预警" value="warning" />
              </el-select>
              <button class="rt-btn" @click="handleSearch">
                <el-icon><Search /></el-icon> 查询
              </button>
              <button class="rt-btn rt-btn-g" @click="handleReset" title="重置">
                <el-icon><RefreshLeft /></el-icon>
              </button>
              <button
                class="rt-btn rt-btn-g"
                @click="toggleAutoScroll"
                :title="autoScrollEnabled ? '暂停滚动' : '开启滚动'">
                <el-icon><component :is="autoScrollEnabled ? 'VideoPause' : 'VideoPlay'" /></el-icon>
              </button>
            </div>
          </div>

          <!-- table -->
          <div
            class="rt-tbl-wrap"
            ref="tableWrapper"
            @mouseenter="pauseAutoScroll"
            @mouseleave="resumeAutoScroll">
            <el-table
              :data="paginatedUserList"
              v-loading="isLoading"
              element-loading-background="rgba(10,30,61,0.8)"
              element-loading-text="加载中..."
              height="100%"
              style="width: 100%"
              :header-cell-style="tblHeadStyle"
              :cell-style="tblCellStyle"
              :row-class-name="rowClass"
              @row-click="showUserDetail">
              <el-table-column prop="userName" label="姓名" min-width="70" align="center">
                <template #default="{ row }">
                  <span class="c-name">{{ row.userName || '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="gender" label="性别" width="55" align="center">
                <template #default="{ row }">
                  <span :style="{color: row.gender===1?'#60a5fa':'#f472b6'}">
                    {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '--' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="age" label="年龄" width="55" align="center">
                <template #default="{ row }">
                  <span class="c-code">{{ row.age != null ? row.age : '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="userCode" label="工号" min-width="90" align="center">
                <template #default="{ row }">
                  <span class="c-code">{{ row.userCode || '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="deptName" label="部门" min-width="100" align="center">
                <template #default="{ row }">
                  <span class="c-dept">{{ row.deptName || '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="heartRate" label="心率(bpm)" width="85" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="!row.heartRate" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="hrCls(row.heartRate)">{{ row.heartRate }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="bloodOxygen" label="血氧(%)" width="75" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="!row.bloodOxygen" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="spo2Cls(row.bloodOxygen)">{{ row.bloodOxygen }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="temperature" label="体温(°C)" width="80" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="!row.temperature" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="tempCls(row.temperature)">{{ row.temperature }}°</span>
                </template>
              </el-table-column>
              <el-table-column prop="steps" label="步数" width="70" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.steps == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else class="c-steps">{{ row.steps }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="calories" label="卡路里(kcal)" width="105" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.calories == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else class="c-calories">{{ row.calories }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="bloodPressureHigh" label="收缩压" width="70" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="!row.bloodPressureHigh" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="bpCls(row.bloodPressureHigh)">{{ row.bloodPressureHigh }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="bloodPressureLow" label="舒张压" width="70" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="!row.bloodPressureLow" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="bpLowCls(row.bloodPressureLow)">{{ row.bloodPressureLow }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pressure" label="压力指数" width="80" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.pressure == null" content="设备暂未上报该项数据" placement="top" :show-after="500">
                    <span class="c-na">--</span>
                  </el-tooltip>
                  <span v-else :class="pressureCls(row.pressure)">{{ row.pressure }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="60" align="center">
                <template #default="{ row }">
                  <span :class="['rt-status', row.status === 'normal' ? 'st-ok' : 'st-warn']">
                    {{ row.status === 'normal' ? '正常' : '预警' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="lastUpdate" label="时间" min-width="120" align="center">
                <template #default="{ row }">
                  <span class="c-time">{{ fmtTime(row.lastUpdate) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <div v-if="row.imei" style="display:flex;gap:4px;justify-content:center">
                    <button class="rt-msg-btn" @click.stop="handleSendMessage(row)" title="文字消息" aria-label="发送文字消息">
                      <el-icon><ChatDotRound /></el-icon>
                    </button>
                    <button class="rt-msg-btn rt-voice-btn" @click.stop="handleSendVoice(row)" title="语音广播" aria-label="语音广播">
                      <el-icon><Bell /></el-icon>
                    </button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- pagination -->
          <div class="rt-pg">
            <button class="rt-pg-btn" :disabled="currentPage === 1" @click="currentPage = 1">首页</button>
            <button class="rt-pg-btn" :disabled="currentPage === 1" @click="currentPage--">&#8249;</button>
            <span class="rt-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="rt-pg-btn" :disabled="currentPage >= totalPages" @click="currentPage++">&#8250;</button>
            <button class="rt-pg-btn" :disabled="currentPage >= totalPages" @click="currentPage = totalPages">末页</button>
            <span class="rt-pg-total">共 {{ filteredUserList.length }} 条</span>
          </div>
        </div>
      </main>
    </section>

    <!-- 发消息对话框 -->
    <el-dialog
      v-model="messageDialogVisible"
      title="发送消息到手表"
      width="420px"
      :append-to-body="true"
      :close-on-click-modal="false">
      <div v-if="messageTarget" class="rt-msg-meta">
        <span>{{ messageTarget.userName }}</span>
        <span class="rt-msg-dept">{{ messageTarget.deptName }}</span>
        <span class="rt-msg-imei">{{ messageTarget.imei }}</span>
      </div>
      <el-input
        v-model="messageText"
        type="textarea"
        :rows="4"
        placeholder="请输入要推送到手表的消息内容（最多 50 个字符）"
        :maxlength="50"
        show-word-limit
        resize="none"
      />
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSendMessage" :disabled="!messageText.trim()">发送</el-button>
      </template>
    </el-dialog>

    <!-- 语音广播对话框 -->
    <el-dialog
      v-model="voiceDialogVisible"
      title="语音广播到手表"
      width="400px"
      :append-to-body="true"
      :close-on-click-modal="false">
      <div v-if="voiceTarget" class="rt-msg-meta" style="margin-bottom:16px">
        <span>{{ voiceTarget.userName }}</span>
        <span class="rt-msg-dept">{{ voiceTarget.deptName }}</span>
        <span class="rt-msg-imei">{{ voiceTarget.imei }}</span>
      </div>
      <div style="display:flex;flex-direction:column;gap:8px">
        <div
          v-for="t in voiceTemplates"
          :key="t.id"
          :class="['rt-voice-tpl', voiceTemplateId === t.id ? 'rt-voice-tpl--active' : '']"
          @click="voiceTemplateId = t.id">
          {{ t.name }}
        </div>
      </div>
      <template #footer>
        <el-button @click="voiceDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmSendVoice" :disabled="!voiceTemplateId">
          立即播报
        </el-button>
      </template>
    </el-dialog>

    <!-- Detail dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="(detailUser && detailUser.userName ? detailUser.userName : '') + ' 实时体征'"
      width="420px"
      :append-to-body="true"
      :close-on-click-modal="true">
      <div v-if="detailUser" style="display:grid;grid-template-columns:1fr 1fr;gap:16px;padding:8px 0">
        <div
          v-for="item in detailItems"
          :key="item.label"
          style="background:rgba(0,212,255,0.06);border:1px solid rgba(0,212,255,0.15);border-radius:8px;padding:12px 16px">
          <div style="font-size:11px;color:#8ba6c8;margin-bottom:4px">{{ item.label }}</div>
          <div :style="{ fontSize: '22px', fontWeight: '700', fontFamily: 'Consolas', color: item.color }">{{ item.value }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Bell } from '@element-plus/icons-vue'
import { getOnlineUsers } from '@/api/realtime'
import { sendWatchMessage, sendVoiceMessage, getVoiceTemplates } from '@/api/device'

export default {
  name: 'RealtimeMonitor',
  data() {
    return {
      currentTime: '',
      onlineUsers: { list: [], total: 0 },
      searchForm: { name: '', dept: '', status: '' },
      hrFilter: null,
      deptList: [],
      currentPage: 1,
      pageSize: 20,
      autoScrollEnabled: true,
      scrollPaused: false,
      refreshTimer: null,
      autoScrollTimer: null,
      clockTimer: null,
      detailUser: null,
      detailVisible: false,
      messageTarget: null,
      messageDialogVisible: false,
      messageText: '',
      voiceTarget: null,
      voiceDialogVisible: false,
      voiceTemplateId: '',
      voiceTemplates: [],
      isLoading: false,
      tblHeadStyle: {
        background: 'rgba(0,40,90,0.9)',
        color: '#00d4ff',
        borderColor: 'rgba(0,212,255,0.3)',
        fontSize: '13px',
        fontWeight: 'bold',
        padding: '10px 0'
      },
      tblCellStyle: {
        background: 'transparent',
        borderColor: 'rgba(0,212,255,0.15)',
        color: '#a8c5e6',
        fontSize: '13px',
        padding: '8px 0',
        cursor: 'pointer'
      }
    }
  },
  computed: {
    allUsers() {
      return this.onlineUsers.list || []
    },
    warningUsers() {
      return this.allUsers
        .filter(u => u.status === 'warning')
        .sort((a, b) => new Date(b.lastUpdate) - new Date(a.lastUpdate))
    },
    normalCount() {
      return this.allUsers.filter(u => u.status === 'normal').length
    },
    warningCount() {
      return this.warningUsers.length
    },
    filteredUserList() {
      let list = this.allUsers
      if (this.searchForm.name) {
        const q = this.searchForm.name.trim().toLowerCase()
        list = list.filter(u =>
          (u.userName || '').toLowerCase().includes(q) ||
          (u.userCode || '').toLowerCase().includes(q)
        )
      }
      if (this.searchForm.dept) {
        list = list.filter(u => u.deptName === this.searchForm.dept)
      }
      if (this.searchForm.status) {
        list = list.filter(u => u.status === this.searchForm.status)
      }
      if (this.hrFilter) {
        list = list.filter(u => u.heartRate >= this.hrFilter.min && u.heartRate <= this.hrFilter.max)
      }
      return list
    },
    paginatedUserList() {
      const s = (this.currentPage - 1) * this.pageSize
      return this.filteredUserList.slice(s, s + this.pageSize)
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredUserList.length / this.pageSize))
    },
    detailItems() {
      const u = this.detailUser
      if (!u) return []
      return [
        {
          label: '心率',
          value: u.heartRate ? u.heartRate + ' bpm' : '--',
          color: this.hrCls(u.heartRate).includes('danger') ? '#ff5252'
            : this.hrCls(u.heartRate).includes('warn') ? '#ffd200' : '#52c41a'
        },
        {
          label: '血氧',
          value: u.bloodOxygen ? u.bloodOxygen + '%' : '--',
          color: this.spo2Cls(u.bloodOxygen).includes('danger') ? '#ff5252'
            : this.spo2Cls(u.bloodOxygen).includes('warn') ? '#ffd200' : '#52c41a'
        },
        {
          label: '体温',
          value: u.temperature ? u.temperature + '°C' : '--',
          color: this.tempCls(u.temperature).includes('danger') ? '#ff5252'
            : this.tempCls(u.temperature).includes('warn') ? '#ffd200' : '#52c41a'
        },
        { label: '步数',   value: u.steps != null ? u.steps + ' 步' : '--', color: '#22c55e' },
        {
          label: '收缩压',
          value: u.bloodPressureHigh ? u.bloodPressureHigh + ' mmHg' : '--',
          color: this.bpCls(u.bloodPressureHigh).includes('danger') ? '#ff5252'
            : this.bpCls(u.bloodPressureHigh).includes('warn') ? '#ffd200' : '#a78bfa'
        },
        {
          label: '舒张压',
          value: u.bloodPressureLow ? u.bloodPressureLow + ' mmHg' : '--',
          color: this.bpLowCls(u.bloodPressureLow).includes('danger') ? '#ff5252'
            : this.bpLowCls(u.bloodPressureLow).includes('warn') ? '#ffd200' : '#a78bfa'
        },
        {
          label: '压力指数',
          value: u.pressure != null ? u.pressure : '--',
          color: this.pressureCls(u.pressure).includes('danger') ? '#ff5252'
            : this.pressureCls(u.pressure).includes('warn') ? '#ffd200' : '#fb923c'
        },
        { label: '部门',   value: u.deptName || '--', color: '#a8c5e6' },
        { label: '工号',   value: u.userCode || '--', color: '#a8c5e6' }
      ]
    }
  },
  mounted() {
    this.initTime()
    this.fetchOnlineUsers()
    this.autoRefresh()
    this.startAutoScroll()
    document.addEventListener('visibilitychange', this.onVisibilityChange)
    this.$nextTick(() => {
      this._ro = new ResizeObserver(() => this.updatePageSize())
      const el = this.$refs.tableWrapper
      if (el) { this._ro.observe(el); this.updatePageSize() }
    })
  },
  beforeUnmount() {
    clearInterval(this.refreshTimer)
    clearInterval(this.autoScrollTimer)
    clearInterval(this.clockTimer)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    if (this._ro) this._ro.disconnect()
  },
  methods: {
    updatePageSize() {
      const el = this.$refs.tableWrapper
      if (!el) return
      const HEADER_H = 44
      const ROW_H    = 41
      const n = Math.max(10, Math.floor((el.clientHeight - HEADER_H) / ROW_H))
      if (n !== this.pageSize) {
        this.pageSize = n
        this.currentPage = 1
      }
    },
    initTime() {
      this.currentTime = dayjs().format('HH:mm:ss')
      this.clockTimer = setInterval(() => {
        this.currentTime = dayjs().format('HH:mm:ss')
      }, 1000)
    },

    async fetchOnlineUsers() {
      if (this._fetching) return
      this._fetching = true
      const isFirst = !this._loaded
      if (isFirst) this.isLoading = true
      try {
        const r = await getOnlineUsers(1, 10000)
        if (r.code === 200) {
          this.onlineUsers = r.data
          const depts = new Set((r.data.list || []).map(u => u.deptName).filter(Boolean))
          this.deptList = [...depts].sort()
        }
        this._loaded = true
      } finally {
        this._fetching = false
        if (isFirst) this.isLoading = false
      }
    },

    autoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.fetchOnlineUsers()
      }, 5000)
    },

    onVisibilityChange() {
      if (document.hidden) {
        clearInterval(this.refreshTimer)
        clearInterval(this.autoScrollTimer)
      } else {
        this.fetchOnlineUsers()
        this.autoRefresh()
        this.startAutoScroll()
      }
    },

    handleSearch() { this.currentPage = 1 },
    handleReset() {
      this.searchForm = { name: '', dept: '', status: '' }
      this.hrFilter = null
      this.currentPage = 1
    },

    startAutoScroll() {
      clearInterval(this.autoScrollTimer)
      this.autoScrollTimer = setInterval(() => {
        if (!this.autoScrollEnabled || this.scrollPaused) return
        const el = this.$el?.querySelector('.el-table__body-wrapper .el-scrollbar__wrap')
        if (!el) return
        const max = el.scrollHeight - el.clientHeight
        if (max <= 0) return
        el.scrollTop += 1
        if (el.scrollTop >= max - 1) {
          this.scrollPaused = true
          setTimeout(() => {
            if (this.currentPage < this.totalPages) {
              // 翻到下一页，滚到顶
              this.currentPage++
              this.$nextTick(() => { if (el) el.scrollTop = 0 })
            } else {
              // 最后一页回到第一页
              this.currentPage = 1
              this.$nextTick(() => { if (el) el.scrollTop = 0 })
            }
            this.scrollPaused = false
          }, 1500)
        }
      }, 50)
    },

    toggleAutoScroll() { this.autoScrollEnabled = !this.autoScrollEnabled },
    pauseAutoScroll()  { this.scrollPaused = true },
    resumeAutoScroll() { this.scrollPaused = false },

    rowClass({ row }) {
      return row.status === 'warning' ? 'row-warning' : ''
    },

    getUserIndicator(u) {
      if (u.heartRate && (u.heartRate < 50 || u.heartRate > 120)) return `心率 ${u.heartRate}`
      if (u.bloodOxygen && u.bloodOxygen < 90)                     return `血氧 ${u.bloodOxygen}%`
      if (u.temperature && (u.temperature < 35 || u.temperature > 38)) return `体温 ${u.temperature}°`
      if (u.bloodPressureHigh && u.bloodPressureHigh >= 160)        return `血压 ${u.bloodPressureHigh}`
      if (u.pressure && u.pressure >= 85)                           return `压力 ${u.pressure}`
      return '体征异常'
    },

    hrCls(v) {
      if (!v) return 'c-dim'
      if (v < 50 || v > 120) return 'c-danger'
      if (v < 60 || v > 100) return 'c-warn'
      return 'c-ok'
    },
    spo2Cls(v) {
      if (!v) return 'c-dim'
      if (v < 90) return 'c-danger'
      if (v < 95) return 'c-warn'
      return 'c-ok'
    },
    tempCls(v) {
      if (!v) return 'c-dim'
      if (v < 35 || v > 38) return 'c-danger'
      if (v < 36 || v > 37.5) return 'c-warn'
      return 'c-ok'
    },
    bpCls(v) {
      if (!v) return 'c-dim'
      if (v >= 160) return 'c-danger'
      if (v >= 140 || v < 90) return 'c-warn'
      return 'c-bp'
    },
    bpLowCls(v) {
      if (!v) return 'c-dim'
      if (v >= 100) return 'c-danger'
      if (v >= 90 || v < 60) return 'c-warn'
      return 'c-bp'
    },
    pressureCls(v) {
      if (v == null) return 'c-dim'
      if (v >= 85) return 'c-danger'
      if (v >= 70) return 'c-warn'
      return 'c-pressure'
    },

    fmtTime(t) {
      if (!t) return '--'
      return dayjs(t).format('HH:mm:ss')
    },

    showUserDetail(row) {
      this.detailUser = row
      this.detailVisible = true
    },

    handleSendMessage(row) {
      this.messageTarget = row
      this.messageText = ''
      this.messageDialogVisible = true
    },

    async handleSendVoice(row) {
      this.voiceTarget = row
      this.voiceTemplateId = ''
      if (this.voiceTemplates.length === 0) {
        try {
          const res = await getVoiceTemplates()
          if (res.code === 200) this.voiceTemplates = res.data || []
        } catch { /* ignore */ }
      }
      this.voiceDialogVisible = true
    },

    async confirmSendVoice() {
      if (!this.voiceTemplateId) return
      try {
        const res = await sendVoiceMessage(this.voiceTarget.imei, this.voiceTemplateId)
        if (res.code === 200) {
          ElMessage.success('语音广播已推送，手表将在数秒内播放')
          this.voiceDialogVisible = false
        } else {
          ElMessage.error(res.message || '推送失败')
        }
      } catch {
        ElMessage.error('推送失败')
      }
    },

    async confirmSendMessage() {
      const text = this.messageText.trim()
      if (!text) return
      try {
        const res = await sendWatchMessage(this.messageTarget.imei, text)
        if (res.code === 200) {
          ElMessage.success('消息已推送到手表')
          this.messageDialogVisible = false
        } else {
          ElMessage.error(res.message || '推送失败')
        }
      } catch {
        ElMessage.error('推送失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.rt-root {
  width: 100%;
  height: calc(100vh - 50px);
  min-height: 600px;
  background: #0a0e27;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
  color: #a8c5e6;
}

/* ===== Header ===== */
.rt-hd {
  height: 54px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: rgba(0, 8, 28, 0.7);
  border-bottom: 1px solid rgba(0, 212, 255, 0.2);
  gap: 16px;
}

.rt-hd-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.rt-live-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 8px #00d4ff;
  animation: blink 1.5s infinite;
}

.rt-hd-title {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin: 0;
  white-space: nowrap;
  text-shadow: 0 0 12px rgba(0,212,255,0.5);
}

/* ticker */
.rt-ticker-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  min-width: 0;
}

.rt-ticker-label {
  flex-shrink: 0;
  font-size: 11px;
  color: #E6A23C;
  background: rgba(230,162,60,0.15);
  border: 1px solid rgba(230,162,60,0.35);
  border-radius: 4px;
  padding: 2px 8px;
  white-space: nowrap;
}

.rt-ticker-scroll {
  flex: 1;
  overflow: hidden;
  min-width: 0;
  position: relative;
  height: 28px;
  display: flex;
  align-items: center;
}

.rt-ticker-inner {
  display: flex;
  gap: 16px;
  align-items: center;
  white-space: nowrap;
  animation: tickerScroll 30s linear infinite;
  &:hover { animation-play-state: paused; }
}

@keyframes tickerScroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

.rt-ticker-tag {
  flex-shrink: 0;
  font-size: 12px;
  color: #ffd200;
  background: rgba(255,210,0,0.08);
  border: 1px solid rgba(255,210,0,0.25);
  border-radius: 12px;
  padding: 2px 10px;
  em { font-style: normal; color: #ff9800; margin-left: 4px; }
}

.rt-ticker-empty {
  font-size: 12px;
  color: #22c55e;
  white-space: nowrap;
}

/* right stats */
.rt-hd-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.rt-hd-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rt-hd-stat-val {
  font-size: 20px;
  font-weight: 700;
  font-family: 'Consolas', monospace;
  line-height: 1.1;
  &.st-ok   { color: #22c55e; }
  &.st-warn { color: #E6A23C; }
  &.val-blink { animation: blink 1.5s infinite; }
}

.rt-hd-stat-lbl {
  font-size: 11px;
  color: #8ba6c8;
}

.rt-hd-sep {
  width: 1px;
  height: 28px;
  background: rgba(0,212,255,0.2);
}

.rt-hd-time {
  flex-shrink: 0;
  font-family: 'Consolas', monospace;
  font-size: 14px;
  color: #8ba6c8;
  white-space: nowrap;
}

/* ===== Body ===== */
.rt-bd {
  flex: 1;
  display: flex;
  padding: 10px;
  overflow: hidden;
}

.rt-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.rt-panel {
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.2);
  border-radius: 10px;
  overflow: hidden;
}

.rt-table-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rt-ph {
  padding: 10px 16px;
  border-bottom: 1px solid rgba(0,212,255,0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  gap: 10px;
}

.rt-ph-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.rt-ph-dot {
  width: 6px; height: 6px;
  background: #00d4ff;
  border-radius: 50%;
  box-shadow: 0 0 6px #00d4ff;
}

.rt-ph-title {
  font-size: 15px;
  font-weight: bold;
  color: #e0f0ff;
  white-space: nowrap;
}

.rt-badge-online {
  font-size: 12px;
  color: #22c55e;
  background: rgba(34,197,94,0.15);
  border: 1px solid rgba(34,197,94,0.35);
  border-radius: 10px;
  padding: 1px 10px;
}

.rt-badge-filter {
  font-size: 12px;
  color: #00d4ff;
  background: rgba(0,212,255,0.12);
  border: 1px solid rgba(0,212,255,0.4);
  border-radius: 10px;
  padding: 1px 10px;
  cursor: pointer;
  &:hover { background: rgba(0,212,255,0.22); }
}

.rt-ph-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rt-inp {
  width: 150px;
  :deep(.el-input__wrapper) {
    background: rgba(0,25,60,0.7) !important;
    border: 1px solid rgba(0,212,255,0.3) !important;
    box-shadow: none !important;
  }
  :deep(.el-input__inner) {
    color: #a8c5e6;
    &::placeholder { color: #5a6a80; }
  }
}

.rt-sel {
  width: 130px;
  :deep(.el-select__wrapper) {
    background: rgba(0,25,60,0.7) !important;
    border: 1px solid rgba(0,212,255,0.3) !important;
    box-shadow: none !important;
  }
  :deep(.el-select__placeholder)        { color: #5a6a80 !important; }
  :deep(.el-select__selected-item span) { color: #a8c5e6 !important; }
  :deep(.el-select__caret)              { color: #00d4ff !important; }
}

.rt-sel-sm {
  width: 110px;
  :deep(.el-select__wrapper) {
    background: rgba(0,25,60,0.7) !important;
    border: 1px solid rgba(0,212,255,0.3) !important;
    box-shadow: none !important;
  }
  :deep(.el-select__placeholder)        { color: #5a6a80 !important; }
  :deep(.el-select__selected-item span) { color: #a8c5e6 !important; }
  :deep(.el-select__caret)              { color: #00d4ff !important; }
}

.rt-btn {
  padding: 7px 14px;
  background: linear-gradient(135deg, rgba(0,212,255,0.18), rgba(0,80,200,0.18));
  border: 1px solid rgba(0,212,255,0.4);
  border-radius: 5px;
  color: #00d4ff;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
  transition: all 0.25s;
  &:hover { background: linear-gradient(135deg, rgba(0,212,255,0.28), rgba(0,80,200,0.28)); box-shadow: 0 0 8px rgba(0,212,255,0.3); }
  &:active { transform: scale(0.96); }
}

.rt-btn-g {
  padding: 7px 10px;
  background: rgba(255,255,255,0.06);
  border-color: rgba(255,255,255,0.15);
  color: #8ba6c8;
  &:hover { background: rgba(255,255,255,0.1); box-shadow: none; }
}

/* Table */
.rt-tbl-wrap {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  &::-webkit-scrollbar       { width: 4px; }
  &::-webkit-scrollbar-track { background: rgba(0,20,50,0.4); }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 2px; }

  :deep(.el-table) {
    --el-table-border-color: rgba(0,212,255,0.2) !important;
    --el-table-row-hover-bg-color: rgba(0,80,180,0.3) !important;
    --el-table-bg-color: transparent !important;
    --el-table-tr-bg-color: transparent !important;
    background: transparent !important;
    &::before { display: none; }
    .el-table__body-wrapper { overflow-x: auto !important; }
    .el-table__header th.el-table__cell {
      background: rgba(0,40,90,0.9) !important;
      border-color: rgba(0,212,255,0.3) !important;
      color: #00d4ff !important;
    }
    .el-table__body tr {
      background: transparent;
      &:nth-child(even) td { background: rgba(0,30,70,0.25) !important; }
      &:hover > td        { background: rgba(0,80,180,0.3) !important; }
    }
    td.el-table__cell { border-color: rgba(0,212,255,0.15) !important; }
  }

  /* 预警行高亮 */
  :deep(.row-warning) td {
    background: rgba(230,162,60,0.06) !important;
  }
}

.c-name     { color: #00d4ff; font-weight: 500; }
.c-code     { color: #8ba6c8; }
.c-dept     { color: #a8c5e6; }
.c-steps    { color: #22c55e; font-weight: 500; }
.c-calories { color: #f97316; font-weight: 500; }
.c-time     { color: #8ba6c8; font-size: 12px; }
.c-dim      { color: #6b7b94; }
.c-na       { color: #3d4a5c; cursor: help; }
.c-ok       { color: #67C23A; font-weight: 600; }
.c-warn     { color: #E6A23C; font-weight: 600; }
.c-danger   { color: #F56C6C; font-weight: 600; }
.c-bp       { color: #a78bfa; font-weight: 600; }
.c-pressure { color: #fb923c; font-weight: 600; }

.rt-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  &.st-ok   { background: rgba(103,194,58,0.18);  color: #67C23A; border: 1px solid rgba(103,194,58,0.4); }
  &.st-warn { background: rgba(230,162,60,0.18);  color: #E6A23C; border: 1px solid rgba(230,162,60,0.4); animation: blink 2s infinite; }
}

.rt-msg-btn {
  padding: 3px 8px;
  font-size: 13px;
  background: rgba(99,102,241,0.15);
  border: 1px solid rgba(99,102,241,0.4);
  color: #a5b4fc;
  border-radius: 4px;
  cursor: pointer;
  &:hover { background: rgba(99,102,241,0.3); color: #c7d2fe; }
}
.rt-voice-btn {
  background: rgba(245,158,11,0.15);
  border-color: rgba(245,158,11,0.4);
  color: #fbbf24;
  &:hover { background: rgba(245,158,11,0.3); color: #fde68a; }
}
.rt-voice-tpl {
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid rgba(0,212,255,0.15);
  background: rgba(0,212,255,0.04);
  color: #c8d8e8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { background: rgba(0,212,255,0.1); border-color: rgba(0,212,255,0.3); }
}
.rt-voice-tpl--active {
  background: rgba(245,158,11,0.15) !important;
  border-color: rgba(245,158,11,0.5) !important;
  color: #fbbf24 !important;
}
.rt-msg-meta {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 8px 12px; margin-bottom: 14px;
  background: rgba(0,212,255,0.06); border: 1px solid rgba(0,212,255,0.15);
  border-radius: 6px; font-size: 13px; color: #e8f4ff;
}
.rt-msg-dept { color: #7eb8d4; }
.rt-msg-imei { color: #7eb8d4; font-family: monospace; font-size: 12px; }

/* Pagination */
.rt-pg {
  padding: 8px 16px;
  border-top: 1px solid rgba(0,212,255,0.15);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.rt-pg-btn {
  min-width: 50px; height: 30px; padding: 0 10px;
  background: rgba(20,60,120,0.3);
  border: 1px solid rgba(0,212,255,0.3);
  border-radius: 4px;
  color: #00d4ff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s;
  &:hover:not(:disabled) { background: rgba(0,212,255,0.2); border-color: #00d4ff; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

.rt-pg-info  { color: #00d4ff; font-size: 14px; font-weight: 500; min-width: 70px; text-align: center; }
.rt-pg-total { color: #8ba6c8; font-size: 12px; margin-left: 6px; }

:deep(.el-dialog) {
  background: #0d1535 !important;
  border: 1px solid rgba(0,212,255,0.25) !important;
  border-radius: 10px !important;
  box-shadow: 0 0 40px rgba(0,80,200,0.4) !important;
  .el-dialog__header {
    background: rgba(0,25,70,0.8) !important;
    border-bottom: 1px solid rgba(0,212,255,0.2) !important;
    padding: 14px 20px !important;
    margin-right: 0 !important;
    border-radius: 10px 10px 0 0 !important;
  }
  .el-dialog__title { color: #00d4ff !important; font-size: 15px !important; font-weight: bold !important; }
  .el-dialog__headerbtn {
    top: 14px !important; right: 16px !important;
    .el-dialog__close { color: #8ba6c8 !important; font-size: 18px !important; &:hover { color: #00d4ff !important; } }
  }
  .el-dialog__body { background: transparent !important; padding: 16px 20px 20px !important; color: #a8c5e6 !important; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.3; }
}
</style>
