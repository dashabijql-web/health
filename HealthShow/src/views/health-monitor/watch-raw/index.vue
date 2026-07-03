<template>
  <div class="wr-root">
    <header class="wr-hd">
      <div class="wr-title-block">
        <span :class="['wr-live-dot', autoRefresh ? 'is-on' : '']"></span>
        <div>
          <h1 class="wr-title">手表原始数据</h1>
          <p class="wr-subtitle">直接展示 TCP 收到的手表上报包，按接收时间倒序保留最近 {{ capacity }} 条</p>
        </div>
      </div>

      <div class="wr-kpis">
        <div class="wr-kpi">
          <span class="wr-kpi-value">{{ totalBuffered }}</span>
          <span class="wr-kpi-label">缓存包数</span>
        </div>
        <div class="wr-kpi">
          <span class="wr-kpi-value">{{ returnedCount }}</span>
          <span class="wr-kpi-label">当前返回</span>
        </div>
        <div class="wr-kpi">
          <span class="wr-kpi-value">{{ lastProtocol || '--' }}</span>
          <span class="wr-kpi-label">最新协议</span>
        </div>
      </div>

      <div class="wr-actions">
        <span class="wr-refresh-time">{{ lastRefreshTime || '未刷新' }}</span>
        <el-switch
          v-model="autoRefresh"
          size="small"
          active-text="自动"
          inactive-text="手动"
        />
        <el-button type="primary" :loading="loading" @click="fetchPackets">
          刷新
        </el-button>
      </div>
    </header>

    <section class="wr-filter">
      <el-input
        v-model.trim="filters.imei"
        class="wr-imei-input"
        clearable
        placeholder="IMEI，例如 861265063894429"
        @keyup.enter="fetchPackets"
      />
      <el-select
        v-model="filters.protocolCode"
        class="wr-protocol-select"
        clearable
        filterable
        allow-create
        default-first-option
        placeholder="协议号"
      >
        <el-option v-for="code in protocolOptions" :key="code" :label="code" :value="code" />
      </el-select>
      <el-select v-model="filters.limit" class="wr-limit-select" placeholder="条数">
        <el-option :value="50" label="最近 50 条" />
        <el-option :value="100" label="最近 100 条" />
        <el-option :value="200" label="最近 200 条" />
        <el-option :value="500" label="最近 500 条" />
        <el-option :value="1000" label="最近 1000 条" />
      </el-select>
      <el-button :disabled="loading" @click="resetFilters">重置</el-button>
    </section>

    <section class="wr-table-wrap" v-loading="loading" element-loading-text="正在读取原始数据...">
      <el-table
        :data="packetList"
        height="100%"
        border
        stripe
        empty-text="还没有收到手表原始包"
      >
        <el-table-column prop="sequence" label="#" width="78" align="center" />
        <el-table-column prop="receiveTime" label="接收时间" width="170" />
        <el-table-column prop="protocolCode" label="协议" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.protocolCode || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="imei" label="IMEI" width="170" />
        <el-table-column prop="remoteAddress" label="来源地址" min-width="190" />
        <el-table-column prop="paramCount" label="参数数" width="86" align="center" />
        <el-table-column label="参数" min-width="220">
          <template #default="{ row }">
            <div v-if="row.params?.length" class="wr-param-list">
              <el-tag
                v-for="(param, index) in row.params"
                :key="`${row.sequence}-${index}`"
                size="small"
                effect="plain"
                class="wr-param-tag"
              >
                {{ index + 1 }}: {{ param }}
              </el-tag>
            </div>
            <span v-else class="wr-empty-text">无</span>
          </template>
        </el-table-column>
        <el-table-column label="原始报文" min-width="420">
          <template #default="{ row }">
            <code class="wr-raw">{{ row.rawMessage || '' }}</code>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { getWatchRawPackets } from '@/api/watch-raw'
import './watch-raw.scss'

const DEFAULT_FILTERS = {
  imei: '',
  protocolCode: '',
  limit: 200
}

export default {
  name: 'WatchRawPackets',
  data() {
    return {
      loading: false,
      autoRefresh: true,
      refreshTimer: null,
      filters: { ...DEFAULT_FILTERS },
      packetList: [],
      totalBuffered: 0,
      returnedCount: 0,
      capacity: 1000,
      lastRefreshTime: ''
    }
  },
  computed: {
    protocolOptions() {
      const codes = new Set(['AP00', 'AP02', 'AP03', 'APHP', 'APHT', 'AP10', 'AP12'])
      this.packetList.forEach(packet => {
        if (packet.protocolCode) codes.add(packet.protocolCode)
      })
      return [...codes].sort()
    },
    lastProtocol() {
      return this.packetList[0]?.protocolCode || ''
    }
  },
  watch: {
    autoRefresh: {
      immediate: true,
      handler(enabled) {
        this.resetRefreshTimer(enabled)
      }
    },
    'filters.protocolCode'() {
      this.fetchPackets()
    },
    'filters.limit'() {
      this.fetchPackets()
    }
  },
  mounted() {
    this.fetchPackets()
  },
  beforeUnmount() {
    this.resetRefreshTimer(false)
  },
  methods: {
    async fetchPackets() {
      if (this.loading) return
      this.loading = true
      try {
        const params = {
          imei: this.filters.imei || undefined,
          protocolCode: this.filters.protocolCode || undefined,
          limit: this.filters.limit
        }
        const res = await getWatchRawPackets(params)
        const data = res.data || {}
        this.packetList = data.list || []
        this.totalBuffered = data.totalBuffered || 0
        this.returnedCount = data.returnedCount || this.packetList.length
        this.capacity = data.capacity || 1000
        this.lastRefreshTime = this.formatTime(new Date())
      } catch (error) {
        ElMessage.error(error.message || '读取手表原始数据失败')
      } finally {
        this.loading = false
      }
    },
    resetFilters() {
      this.filters = { imei: '', protocolCode: '', limit: 200 }
      this.fetchPackets()
    },
    resetRefreshTimer(enabled) {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
      if (enabled) {
        this.refreshTimer = window.setInterval(() => this.fetchPackets(), 3000)
      }
    },
    formatTime(date) {
      const pad = value => String(value).padStart(2, '0')
      return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  }
}
</script>
