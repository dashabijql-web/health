<template>
  <div class="wc-root">
    <header class="wc-header">
      <div>
        <h1 class="wc-title">手表控制</h1>
        <p class="wc-subtitle">服务端主动下发手表操作与配置命令，测量类 BPXL/BPXY/BPXZ/BPXT 和定位 BP16 已由后台每分钟自动发送</p>
      </div>
      <div class="wc-header-actions">
        <span class="wc-refresh-time">{{ lastRefreshTime || '未刷新' }}</span>
        <el-button :loading="loadingDevices" @click="loadDevices">刷新设备</el-button>
        <el-button type="primary" @click="goRawPackets">查看原始数据</el-button>
      </div>
    </header>

    <section class="wc-target-panel">
      <div class="wc-target-main">
        <div class="wc-target-title">目标手表</div>
        <div class="wc-target-meta">
          <el-tag :type="targetOnline ? 'success' : 'info'" effect="dark">
            {{ targetOnline ? '在线' : '离线/未知' }}
          </el-tag>
          <span class="wc-target-imei">{{ targetImei || '未选择' }}</span>
          <span v-if="targetDevice?.userName">{{ targetDevice.userName }} / {{ targetDevice.deptName || '--' }}</span>
        </div>
      </div>
      <div class="wc-manual">
        <el-input
          v-model.trim="manualImei"
          class="wc-manual-input"
          clearable
          maxlength="15"
          placeholder="手动输入 15 位 IMEI"
          @input="selectedImei = ''"
        />
        <el-button :disabled="!isValidImei(manualImei)" @click="selectManualTarget">使用该 IMEI</el-button>
      </div>
    </section>

    <section class="wc-content">
      <div class="wc-left">
        <section class="wc-device-panel">
          <div class="wc-section-head">
            <div>
              <h2>在线设备</h2>
              <p>点击设备行即可作为命令目标，未建档但已 TCP 在线的手表也会显示。</p>
            </div>
            <el-tag :type="onlineCount > 0 ? 'success' : 'info'" effect="dark">
              在线 {{ onlineCount }} / {{ devices.length }}
            </el-tag>
          </div>
          <el-table
            v-loading="loadingDevices"
            :data="devices"
            height="100%"
            border
            stripe
            highlight-current-row
            empty-text="暂无设备"
            @row-click="selectDevice"
          >
            <el-table-column prop="imei" label="IMEI" min-width="170" />
            <el-table-column label="状态" width="92" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="dark">
                  {{ row.status === 1 ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="绑定人员" min-width="120">
              <template #default="{ row }">{{ row.userName || '--' }}</template>
            </el-table-column>
            <el-table-column prop="deptName" label="部门" min-width="130">
              <template #default="{ row }">{{ row.deptName || '--' }}</template>
            </el-table-column>
            <el-table-column prop="batteryLevel" label="电量" width="90" align="center">
              <template #default="{ row }">
                {{ row.batteryLevel === null || row.batteryLevel === undefined ? '--' : `${row.batteryLevel}%` }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" :type="row.imei === targetImei ? 'primary' : 'default'" @click.stop="selectDevice(row)">
                  选择
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="wc-command-panel">
          <el-tabs v-model="activeTab" class="wc-tabs">
            <el-tab-pane label="基础设置" name="basic">
              <div class="wc-command-list">
                <div class="wc-command-row wc-danger-row">
                  <div>
                    <div class="wc-command-title">重启终端</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP18</el-tag><span>手表返回 AP18 后执行重启。</span></div>
                  </div>
                  <el-button type="danger" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP18'" @click="sendRestart">
                    重启
                  </el-button>
                </div>

                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">设置时区</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP20</el-tag><span>格式：流水号、语言、整数时区。</span></div>
                  </div>
                  <div class="wc-command-controls">
                    <el-input-number v-model="timezone.language" :min="0" :max="9" controls-position="right" />
                    <el-input-number v-model="timezone.offset" :min="-12" :max="14" controls-position="right" />
                    <el-button type="primary" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP20'" @click="sendTimezone">
                      下发
                    </el-button>
                  </div>
                </div>

                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">心率/血压测量周期</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP86</el-tag><span>后台登录时默认设为 1 分钟，这里用于现场调整。</span></div>
                  </div>
                  <div class="wc-command-controls">
                    <el-switch v-model="healthPeriod.enabled" active-text="开启" inactive-text="关闭" />
                    <el-input-number v-model="healthPeriod.minutes" :min="1" :max="1440" controls-position="right" />
                    <el-button type="primary" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP86'" @click="sendPeriod('BP86', healthPeriod, '心率/血压测量周期')">
                      下发
                    </el-button>
                  </div>
                </div>

                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">体温测量周期</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP87</el-tag><span>后台登录时默认设为 1 分钟。</span></div>
                  </div>
                  <div class="wc-command-controls">
                    <el-switch v-model="temperaturePeriod.enabled" active-text="开启" inactive-text="关闭" />
                    <el-input-number v-model="temperaturePeriod.minutes" :min="1" :max="1440" controls-position="right" />
                    <el-button type="primary" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP87'" @click="sendPeriod('BP87', temperaturePeriod, '体温测量周期')">
                      下发
                    </el-button>
                  </div>
                </div>

                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">睡眠时间段</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP96</el-tag><span>格式：是否上传、HHmm@HHmm。</span></div>
                  </div>
                  <div class="wc-command-controls wc-time-controls">
                    <el-switch v-model="sleepPeriod.enabled" active-text="上传" inactive-text="关闭" />
                    <el-input v-model.trim="sleepPeriod.start" maxlength="4" placeholder="2300" />
                    <span class="wc-separator">@</span>
                    <el-input v-model.trim="sleepPeriod.end" maxlength="4" placeholder="0800" />
                    <el-button type="primary" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP96'" @click="sendSleepPeriod">
                      下发
                    </el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="通讯与名单" name="contacts">
              <div class="wc-command-list">
                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">发送文字</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP40</el-tag><span>正文按协议转为 UNICODE 十六进制。</span></div>
                  </div>
                  <div class="wc-command-controls wc-text-controls">
                    <el-input v-model.trim="messageText" maxlength="60" show-word-limit placeholder="发送到手表的文字" />
                    <el-button type="primary" :disabled="!canSendTarget || !messageText" :loading="sendingProtocol === 'BP40'" @click="sendTextMessage">
                      发送
                    </el-button>
                  </div>
                </div>

                <div class="wc-command-block">
                  <div class="wc-block-head">
                    <div>
                      <div class="wc-command-title">设置 SOS 号码</div>
                      <div class="wc-command-meta"><el-tag effect="plain">BP12</el-tag><span>3 个号码，空号码会保留协议位置。</span></div>
                    </div>
                    <el-button type="primary" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP12'" @click="sendSosNumbers">
                      下发 SOS
                    </el-button>
                  </div>
                  <div class="wc-inline-inputs">
                    <el-input v-for="(_, index) in sosNumbers" :key="index" v-model.trim="sosNumbers[index]" :placeholder="`SOS ${index + 1}`" />
                  </div>
                </div>

                <div class="wc-command-row">
                  <div>
                    <div class="wc-command-title">白名单开关</div>
                    <div class="wc-command-meta"><el-tag effect="plain">BP84</el-tag><span>开启后仅允许名单内联系人。</span></div>
                  </div>
                  <div class="wc-command-controls">
                    <el-switch v-model="whitelistEnabled" active-text="开启" inactive-text="关闭" />
                    <el-button type="warning" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP84'" @click="sendWhitelistSwitch">
                      下发
                    </el-button>
                  </div>
                </div>

                <div class="wc-command-block">
                  <div class="wc-block-head">
                    <div>
                      <div class="wc-command-title">联系人白名单</div>
                      <div class="wc-command-meta"><el-tag effect="plain">BP14</el-tag><span>10 个位置，姓名转 UNICODE，空位置保留。</span></div>
                    </div>
                    <el-button type="warning" :disabled="!canSendTarget" :loading="sendingProtocol === 'BP14'" @click="sendWhitelistContacts">
                      下发白名单
                    </el-button>
                  </div>
                  <div class="wc-contact-grid">
                    <div v-for="(contact, index) in whitelistContacts" :key="index" class="wc-contact-row">
                      <span>{{ index + 1 }}</span>
                      <el-input v-model.trim="contact.name" placeholder="姓名" />
                      <el-input v-model.trim="contact.phone" placeholder="电话号码" />
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>

      <section class="wc-log-card">
        <div class="wc-section-head">
          <div>
            <h2>最近操作记录</h2>
            <p>显示控制台下发命令及手表确认，完整报文在原始数据页查看。</p>
          </div>
          <el-button :loading="loadingLogs" @click="loadCommandLogs">刷新记录</el-button>
        </div>
        <div v-loading="loadingLogs" class="wc-log-list">
          <div v-for="item in commandLogs" :key="item.sequence" class="wc-log-item">
            <div class="wc-log-top">
              <el-tag size="small" :type="item.direction === 'TX' ? 'success' : 'primary'" effect="dark">
                {{ item.direction === 'TX' ? '发送' : '接收' }}
              </el-tag>
              <span class="wc-log-code">{{ item.protocolCode }}</span>
              <span class="wc-log-meaning">{{ protocolMeaning(item.protocolCode) }}</span>
              <span class="wc-log-time">{{ item.receiveTime }}</span>
            </div>
            <code class="wc-log-raw">{{ item.rawMessage }}</code>
          </div>
          <el-empty v-if="!commandLogs.length && !loadingLogs" description="暂无控制命令记录" />
        </div>
      </section>
    </section>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOnlineDevices } from '@/api/device'
import { getWatchRawPackets, sendWatchCommand } from '@/api/watch-raw'
import './watch-control.scss'

const COMMAND_MEANINGS = {
  BP12: '设置 SOS 号码',
  AP12: 'SOS 号码设置确认',
  BP14: '设置联系人白名单',
  AP14: '联系人白名单确认',
  BP18: '重启终端',
  AP18: '重启终端确认',
  BP20: '设置时区',
  AP20: '时区设置确认',
  BP40: '发送文字',
  AP40: '文字发送确认',
  BP84: '白名单开关',
  AP84: '白名单开关确认',
  BP86: '设置心率/血压周期',
  AP86: '心率/血压周期确认',
  BP87: '设置体温周期',
  AP87: '体温周期确认',
  BP96: '设置睡眠时间段',
  AP96: '睡眠时间段确认'
}

const COMMAND_LOG_CODES = Object.keys(COMMAND_MEANINGS)

export default {
  name: 'WatchControl',
  data() {
    return {
      activeTab: 'basic',
      loadingDevices: false,
      loadingLogs: false,
      sendingProtocol: '',
      manualImei: '',
      selectedImei: '',
      devices: [],
      commandLogs: [],
      lastRefreshTime: '',
      timezone: {
        language: 0,
        offset: 8
      },
      healthPeriod: {
        enabled: true,
        minutes: 1
      },
      temperaturePeriod: {
        enabled: true,
        minutes: 1
      },
      sleepPeriod: {
        enabled: true,
        start: '2300',
        end: '0800'
      },
      messageText: '',
      sosNumbers: ['', '', ''],
      whitelistEnabled: true,
      whitelistContacts: Array.from({ length: 10 }, () => ({ name: '', phone: '' }))
    }
  },
  computed: {
    onlineCount() {
      return this.devices.filter(device => device.status === 1).length
    },
    targetImei() {
      return this.selectedImei || this.manualImei
    },
    targetDevice() {
      return this.devices.find(device => device.imei === this.targetImei) || null
    },
    targetOnline() {
      return this.targetDevice ? this.targetDevice.status === 1 : false
    },
    canSendTarget() {
      return this.isValidImei(this.targetImei) && !this.sendingProtocol
    }
  },
  mounted() {
    this.loadDevices()
    this.loadCommandLogs()
  },
  methods: {
    async loadDevices() {
      this.loadingDevices = true
      try {
        const res = await getOnlineDevices()
        const data = res.data || {}
        this.devices = data.devices || []
        if (!this.selectedImei && !this.manualImei) {
          const firstOnline = this.devices.find(device => device.status === 1)
          if (firstOnline?.imei) this.selectDevice(firstOnline)
        }
        this.lastRefreshTime = this.formatTime(new Date())
      } catch (error) {
        ElMessage.error(error.message || '获取设备列表失败')
      } finally {
        this.loadingDevices = false
      }
    },
    async loadCommandLogs() {
      this.loadingLogs = true
      try {
        const responses = await Promise.all(
          COMMAND_LOG_CODES.map(code => getWatchRawPackets({ protocolCode: code, limit: 12 }))
        )
        const logs = responses.flatMap(response => ((response.data || {}).list || []))
        this.commandLogs = logs.sort((a, b) => b.sequence - a.sequence).slice(0, 40)
      } catch (error) {
        ElMessage.error(error.message || '获取操作记录失败')
      } finally {
        this.loadingLogs = false
      }
    },
    selectDevice(device) {
      if (!device?.imei) return
      this.selectedImei = device.imei
      this.manualImei = device.imei
    },
    selectManualTarget() {
      if (!this.isValidImei(this.manualImei)) {
        ElMessage.warning('IMEI 必须是 15 位数字')
        return
      }
      this.selectedImei = ''
      ElMessage.success(`已选择手表 ${this.manualImei}`)
    },
    async sendRestart() {
      await this.sendProtocolCommand('BP18', [this.nextSerial()], {
        title: '重启手表',
        message: `确认向手表 ${this.targetImei} 下发重启命令 BP18？`,
        confirmButtonText: '确认重启',
        type: 'warning'
      })
    },
    async sendTimezone() {
      const language = String(this.timezone.language ?? 0)
      const offset = String(this.timezone.offset ?? 8)
      await this.sendProtocolCommand('BP20', [this.nextSerial(), language, offset], {
        title: '设置时区',
        message: `确认把手表 ${this.targetImei} 的时区设置为 UTC${Number(offset) >= 0 ? '+' : ''}${offset}？`,
        type: 'warning'
      })
    },
    async sendPeriod(protocolCode, period, label) {
      const minutes = Number(period.minutes)
      if (!Number.isInteger(minutes) || minutes < 1 || minutes > 1440) {
        ElMessage.warning('周期必须是 1 到 1440 分钟之间的整数')
        return
      }
      await this.sendProtocolCommand(protocolCode, [this.nextSerial(), period.enabled ? '1' : '0', String(minutes)], {
        title: label,
        message: `确认${period.enabled ? `设置 ${label} 为 ${minutes} 分钟` : `关闭 ${label}`}？`,
        type: 'warning'
      })
    },
    async sendSleepPeriod() {
      const start = this.sleepPeriod.start
      const end = this.sleepPeriod.end
      if (!this.isHhmm(start) || !this.isHhmm(end)) {
        ElMessage.warning('睡眠时间必须是 HHmm 格式，例如 2300 和 0800')
        return
      }
      await this.sendProtocolCommand('BP96', [this.nextSerial(), this.sleepPeriod.enabled ? '1' : '0', `${start}@${end}`], {
        title: '设置睡眠时间段',
        message: `确认下发睡眠时间段 ${start}@${end}？`,
        type: 'warning'
      })
    },
    async sendTextMessage() {
      if (!this.messageText) {
        ElMessage.warning('请输入要发送的文字')
        return
      }
      await this.sendProtocolCommand('BP40', [this.nextSerial(), this.unicodeHex(this.messageText)], {
        successText: '文字命令已发送'
      })
    },
    async sendSosNumbers() {
      if (!this.validatePhones(this.sosNumbers)) return
      await this.sendProtocolCommand('BP12', [this.nextSerial(), ...this.sosNumbers.map(value => value.trim())], {
        title: '设置 SOS 号码',
        message: '确认覆盖手表当前 3 个 SOS 号码？空输入会清空对应位置。',
        type: 'warning'
      })
    },
    async sendWhitelistSwitch() {
      await this.sendProtocolCommand('BP84', [this.nextSerial(), this.whitelistEnabled ? '1' : '0'], {
        title: '白名单开关',
        message: `确认${this.whitelistEnabled ? '开启' : '关闭'}手表联系人白名单？`,
        type: 'warning'
      })
    },
    async sendWhitelistContacts() {
      const params = []
      for (const contact of this.whitelistContacts) {
        const name = (contact.name || '').trim()
        const phone = (contact.phone || '').trim()
        if (name && !phone) {
          ElMessage.warning(`联系人 ${name} 缺少电话号码`)
          return
        }
        if (phone && !this.isPhone(phone)) {
          ElMessage.warning(`电话号码格式不正确：${phone}`)
          return
        }
        params.push(name || phone ? `${this.unicodeHex(name)}|${phone}` : '')
      }
      await this.sendProtocolCommand('BP14', [this.nextSerial(), ...params], {
        title: '设置联系人白名单',
        message: '确认覆盖手表当前 10 个联系人白名单位置？空位置会按协议保留。',
        type: 'warning'
      })
    },
    async sendProtocolCommand(protocolCode, params, options = {}) {
      const imei = this.targetImei
      if (!this.isValidImei(imei)) {
        ElMessage.warning('请先选择或输入 15 位 IMEI')
        return
      }
      if (this.targetDevice && this.targetDevice.status !== 1) {
        ElMessage.warning('设备离线，不能发送命令')
        return
      }
      if (options.message) {
        try {
          await ElMessageBox.confirm(
            options.message,
            options.title || '确认下发命令',
            {
              confirmButtonText: options.confirmButtonText || '确认下发',
              cancelButtonText: '取消',
              type: options.type || 'warning',
              distinguishCancelAndClose: true
            }
          )
        } catch (_) {
          return
        }
      }

      this.sendingProtocol = protocolCode
      try {
        const res = await sendWatchCommand({ imei, protocolCode, params })
        ElMessage.success(options.successText || `命令已发送：${res.data?.rawCommand || protocolCode}`)
        await this.loadCommandLogs()
      } catch (error) {
        ElMessage.error(error.message || `${protocolCode} 发送失败`)
      } finally {
        this.sendingProtocol = ''
      }
    },
    validatePhones(values) {
      for (const value of values) {
        const phone = (value || '').trim()
        if (phone && !this.isPhone(phone)) {
          ElMessage.warning(`电话号码格式不正确：${phone}`)
          return false
        }
      }
      return true
    },
    isPhone(value) {
      return /^[+\d][+\d -]{2,19}$/.test(value)
    },
    isValidImei(value) {
      return /^\d{15}$/.test(value || '')
    },
    isHhmm(value) {
      return /^(?:[01]\d|2[0-3])[0-5]\d$/.test(value || '')
    },
    unicodeHex(text) {
      let encoded = ''
      const value = String(text || '')
      for (let index = 0; index < value.length; index += 1) {
        encoded += value.charCodeAt(index).toString(16).padStart(4, '0')
      }
      return encoded
    },
    protocolMeaning(protocolCode) {
      return COMMAND_MEANINGS[String(protocolCode || '').toUpperCase()] || '控制命令'
    },
    goRawPackets() {
      this.$router.push('/health-monitor/watch-raw')
    },
    nextSerial() {
      return String(Date.now() % 1000000).padStart(6, '0')
    },
    formatTime(date) {
      const pad = value => String(value).padStart(2, '0')
      return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  }
}
</script>
