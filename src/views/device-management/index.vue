<template>
  <div class="device-management-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><Monitor /></el-icon>
        <div>
          <h1 class="main-title">设备管理</h1>
          <p class="sub-title">管理智能手表设备及数据绑定</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><Monitor /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ deviceStats.total }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ deviceStats.online }}</div>
            <div class="stat-label">在线设备</div>
          </div>
          <div class="stat-badge">实时</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap warning"><el-icon size="26"><Connection /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ deviceStats.bound }}</div>
            <div class="stat-label">已绑定</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap info"><el-icon size="26"><Document /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ deviceStats.bufferTotal }}</div>
            <div class="stat-label">缓冲数据</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 设备列表 -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>设备列表</div>
        <div class="panel-header-right">
          <span class="total-badge">共 {{ deviceList.length }} 台</span>
          <el-button type="primary" size="small" :icon="Refresh" @click="refreshDevices">刷新</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table :data="paginatedDeviceList" v-loading="loading" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="{ background:'#1a1f3a', cursor:'pointer' }"
        @row-click="openDetail">
        <el-table-column type="index" label="#" width="50" align="center" :index="getTableIndex" />
        <el-table-column prop="imei" label="设备IMEI" width="160" />
        <el-table-column label="在线状态" width="100" align="center" sortable :sort-method="(a,b) => a.status - b.status">
          <template #default="{ row }">
            <span :class="['online-dot', row.status === 1 ? 'online' : 'offline']"></span>
            <span class="online-text">{{ row.status === 1 ? '在线' : '离线' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="绑定状态" width="100" align="center" sortable :sort-method="(a,b) => (a.bindStatus ? 1 : 0) - (b.bindStatus ? 1 : 0)">
          <template #default="{ row }">
            <el-tag :type="row.bindStatus ? 'success' : 'warning'" size="small" effect="dark">
              {{ row.bindStatus ? '已绑定' : '未绑定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定用户" min-width="150">
          <template #default="{ row }">
            <div v-if="row.bindStatus" class="user-cell">
              <div class="user-avatar">{{ (row.userName || '?')[0] }}</div>
              <div>
                <div class="user-name">{{ row.userName }}</div>
                <div class="user-dept">{{ row.deptName || '-' }}</div>
              </div>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="缓冲数据" width="110" align="center" sortable :sort-method="(a,b) => (a.bufferCount||0) - (b.bufferCount||0)">
          <template #default="{ row }">
            <el-tag v-if="row.bufferCount > 0" type="warning" size="small" effect="dark">
              {{ row.bufferCount }} 条
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastOnlineTime" label="最后在线时间" width="155" sortable :sort-method="(a,b) => new Date(a.lastOnlineTime||0) - new Date(b.lastOnlineTime||0)">
          <template #default="{ row }">{{ formatDate(row.lastOnlineTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-ops" @click.stop>
              <el-button v-if="!row.bindStatus" type="primary" link size="small" @click="handleBind(row)">
                <el-icon><Link /></el-icon> 绑定
              </el-button>
              <el-button v-else type="warning" link size="small" @click="handleUnbind(row)">
                <el-icon><Unlock /></el-icon> 解绑
              </el-button>
              <el-button v-if="row.bufferCount > 0" type="success" link size="small" @click="handleTransfer(row)">
                <el-icon><Upload /></el-icon> 转移
              </el-button>
              <el-button v-if="row.bufferCount > 0" type="danger" link size="small" @click="handleDeleteBuffer(row)">
                <el-icon><Delete /></el-icon> 清空
              </el-button>
              <el-button v-if="row.status === 1" type="info" link size="small" @click="handleSendMessage(row)">
                <el-icon><ChatDotRound /></el-icon> 发消息
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="pagination.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.size"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 设备详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="设备详情" direction="rtl" size="380px" class="dark-drawer">
      <div v-if="detailDevice" class="detail-content">
        <div class="detail-header">
          <div class="detail-device-icon">
            <el-icon size="36"><Monitor /></el-icon>
          </div>
          <div class="detail-device-title">
            <div class="detail-imei">{{ detailDevice.imei }}</div>
            <div class="detail-status-row">
              <span :class="['online-dot', detailDevice.status === 1 ? 'online' : 'offline']"></span>
              <span class="online-text">{{ detailDevice.status === 1 ? '在线' : '离线' }}</span>
              <el-tag class="ml-8" :type="detailDevice.bindStatus ? 'success' : 'warning'" size="small" effect="dark">
                {{ detailDevice.bindStatus ? '已绑定' : '未绑定' }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">设备信息</div>
          <div class="detail-row">
            <span class="detail-label">设备IMEI</span>
            <span class="detail-value mono">{{ detailDevice.imei || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">设备ID</span>
            <span class="detail-value mono">{{ detailDevice.id || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">在线状态</span>
            <span class="detail-value">
              <span :class="['online-dot', detailDevice.status === 1 ? 'online' : 'offline']"></span>
              {{ detailDevice.status === 1 ? '在线' : '离线' }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">最后在线</span>
            <span class="detail-value">{{ formatDate(detailDevice.lastOnlineTime) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">缓冲数据</span>
            <span class="detail-value">
              <el-tag v-if="detailDevice.bufferCount > 0" type="warning" size="small" effect="dark">{{ detailDevice.bufferCount }} 条</el-tag>
              <span v-else class="text-muted">无</span>
            </span>
          </div>
        </div>

        <div class="detail-section" v-if="detailDevice.bindStatus">
          <div class="detail-section-title">绑定员工信息</div>
          <div class="detail-row">
            <span class="detail-label">员工姓名</span>
            <span class="detail-value">{{ detailDevice.userName || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">所属部门</span>
            <span class="detail-value">{{ detailDevice.deptName || '-' }}</span>
          </div>
        </div>
        <div class="detail-section" v-else>
          <div class="detail-section-title">绑定状态</div>
          <div class="detail-empty">该设备尚未绑定员工</div>
        </div>
      </div>
    </el-drawer>

    <!-- 绑定用户对话框 -->
    <el-dialog v-model="bindDialogVisible" title="绑定用户" width="600px"
      :close-on-click-modal="false" class="dark-dialog">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="设备IMEI">
          <el-input v-model="currentDevice.imei" disabled />
        </el-form-item>
        <el-form-item label="选择用户">
          <el-autocomplete
            v-model="bindForm.searchKey"
            :fetch-suggestions="searchUsers"
            placeholder="输入用户姓名、手机号搜索"
            clearable
            style="width: 100%"
            :trigger-on-focus="false"
            @select="handleSelectUser"
          >
            <template #default="{ item }">
              <div class="user-suggestion-item">
                <span class="user-name-suggestion">{{ item.realName }}</span>
                <span class="user-dept-suggestion">{{ item.deptName || '未分配部门' }}</span>
                <span class="user-phone-suggestion">{{ item.phone }}</span>
              </div>
            </template>
          </el-autocomplete>
          <div v-if="bindForm.userId" class="selected-user-info">
            已选择：<el-tag type="success" size="small">{{ bindForm.userName }}</el-tag>
            <span v-if="bindForm.userDept" style="margin-left: 10px; color: #7eb8d4;">{{ bindForm.userDept }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBind" :disabled="!bindForm.userId">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发消息对话框 -->
    <el-dialog v-model="messageDialogVisible" title="发送消息到手表" width="480px"
      :close-on-click-modal="false" class="dark-dialog">
      <div class="msg-dialog-meta">
        <span class="msg-meta-label">设备 IMEI：</span>
        <span class="msg-meta-value mono">{{ currentDevice.imei }}</span>
        <span v-if="currentDevice.userName" class="msg-meta-user">（{{ currentDevice.userName }}）</span>
      </div>
      <el-form :model="messageForm" label-width="0">
        <el-form-item>
          <el-input
            v-model="messageForm.text"
            type="textarea"
            :rows="4"
            placeholder="请输入要推送到手表的消息内容（最多 50 个字符）"
            :maxlength="50"
            show-word-limit
            resize="none"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSendMessage" :disabled="!messageForm.text.trim()">发送</el-button>
      </template>
    </el-dialog>

    <!-- 转移数据对话框 -->
    <el-dialog v-model="transferDialogVisible" title="转移缓冲数据" width="600px"
      :close-on-click-modal="false" class="dark-dialog">
      <el-alert
        title="提示：将设备的缓冲数据转移到指定用户的健康记录中"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      />
      <el-form :model="transferForm" label-width="120px">
        <el-form-item label="设备IMEI">
          <el-input v-model="currentDevice.imei" disabled />
        </el-form-item>
        <el-form-item label="缓冲数据量">
          <el-tag type="warning">{{ currentDevice.bufferCount }} 条</el-tag>
        </el-form-item>
        <el-form-item label="目标用户">
          <el-autocomplete
            v-model="transferForm.searchKey"
            :fetch-suggestions="searchUsers"
            placeholder="输入用户姓名、手机号搜索"
            clearable
            style="width: 100%"
            :trigger-on-focus="false"
            @select="handleSelectTransferUser"
          >
            <template #default="{ item }">
              <div class="user-suggestion-item">
                <span class="user-name-suggestion">{{ item.realName }}</span>
                <span class="user-dept-suggestion">{{ item.deptName || '未分配部门' }}</span>
                <span class="user-phone-suggestion">{{ item.phone }}</span>
              </div>
            </template>
          </el-autocomplete>
          <div v-if="transferForm.userId" class="selected-user-info">
            已选择：<el-tag type="success" size="small">{{ transferForm.userName }}</el-tag>
            <span v-if="transferForm.userDept" style="margin-left: 10px; color: #7eb8d4;">{{ transferForm.userDept }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTransfer" :disabled="!transferForm.userId">确定转移</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Monitor, Timer, CircleCheck, Connection, Document, Link, Unlock, Upload, Delete, ChatDotRound } from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  getOnlineDevices,
  getBufferDataCount,
  transferBufferData,
  deleteBufferData,
  bindDeviceToUser,
  unbindDevice,
  sendWatchMessage
} from '@/api/device'

// 当前时间
const currentTime = ref('')
const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

// 设备列表数据
const deviceList = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 当前操作的设备
const currentDevice = ref({})

// 详情抽屉
const detailDrawerVisible = ref(false)
const detailDevice = ref(null)

const openDetail = (row) => {
  detailDevice.value = row
  detailDrawerVisible.value = true
}

// 绑定对话框
const bindDialogVisible = ref(false)
const bindForm = reactive({
  userId: '',
  userName: '',
  userDept: '',
  searchKey: ''
})

// 用户搜索缓存
const userSearchCache = new Map()
let searchTimer = null

// 转移数据对话框
const transferDialogVisible = ref(false)
const transferForm = reactive({
  userId: '',
  userName: '',
  userDept: '',
  searchKey: ''
})

// 发消息对话框
const messageDialogVisible = ref(false)
const messageForm = reactive({ text: '' })

const handleSendMessage = (row) => {
  currentDevice.value = row
  messageForm.text = ''
  messageDialogVisible.value = true
}

const confirmSendMessage = async () => {
  const text = messageForm.text.trim()
  if (!text) return
  try {
    const res = await sendWatchMessage(currentDevice.value.imei, text)
    if (res.code === 200) {
      ElMessage.success('消息发送成功')
      messageDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '消息发送失败')
    }
  } catch (error) {
    ElMessage.error('消息发送失败')
  }
}

// 计算统计数据
const deviceStats = computed(() => {
  const total = deviceList.value.length
  const online = deviceList.value.filter(d => d.status === 1).length
  const bound = deviceList.value.filter(d => d.bindStatus).length
  const bufferTotal = deviceList.value.reduce((sum, d) => sum + (d.bufferCount || 0), 0)
  return { total, online, bound, bufferTotal }
})

// 分页后的设备列表
const paginatedDeviceList = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return deviceList.value.slice(start, end)
})

// 表格序号（考虑分页）
const getTableIndex = (index) => {
  return (pagination.page - 1) * pagination.size + index + 1
}

/**
 * 刷新设备列表
 */
const refreshDevices = async () => {
  loading.value = true
  try {
    const res = await getOnlineDevices()
    if (res.code === 200) {
      deviceList.value = res.data?.devices || []
      pagination.total = deviceList.value.length
      // bufferCount 已由后端在设备列表中一并返回，无需逐台单独请求
    }
  } catch (error) {
    ElMessage.error('获取设备列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索用户（带缓存和防抖）
 */
const searchUsers = (queryString, callback) => {
  if (!queryString || queryString.trim().length === 0) {
    callback([])
    return
  }

  const query = queryString.trim()

  // 防抖：延迟300ms执行
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    // 先查缓存
    if (userSearchCache.has(query)) {
      callback(userSearchCache.get(query))
      return
    }

    // 缓存未命中，调用后端API
    try {
      const res = await request({
        url: '/user/search',
        method: 'get',
        params: { query }
      })

      if (res.code === 200) {
        const users = res.data || []
        // 格式化为 autocomplete 需要的格式
        const suggestions = users.map(u => ({
          value: u.realName, // autocomplete显示的值
          id: u.id,
          realName: u.realName,
          deptName: u.deptName,
          phone: u.phone
        }))

        // 存入缓存
        userSearchCache.set(query, suggestions)
        callback(suggestions)
      } else {
        callback([])
      }
    } catch (error) {
      callback([])
    }
  }, 300) // 300ms防抖
}

/**
 * 选择用户
 */
const handleSelectUser = (item) => {
  bindForm.userId = item.id
  bindForm.userName = item.realName
  bindForm.userDept = item.deptName
  bindForm.searchKey = item.realName
}

/**
 * 绑定用户
 */
const handleBind = (row) => {
  currentDevice.value = row
  bindForm.userId = ''
  bindForm.userName = ''
  bindForm.userDept = ''
  bindForm.searchKey = ''
  bindDialogVisible.value = true
}

/**
 * 确认绑定
 */
const confirmBind = async () => {
  if (!bindForm.userId) {
    ElMessage.warning('请选择用户')
    return
  }

  try {
    const res = await bindDeviceToUser(currentDevice.value.id, bindForm.userId)
    if (res.code === 200) {
      ElMessage.success('绑定成功')
      bindDialogVisible.value = false
      refreshDevices()
    } else {
      ElMessage.error(res.message || '绑定失败')
    }
  } catch (error) {
    ElMessage.error('绑定设备失败')
  }
}

/**
 * 解绑设备
 */
const handleUnbind = (row) => {
  ElMessageBox.confirm(
    '确定要解绑此设备吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await unbindDevice(row.id)
      if (res.code === 200) {
        ElMessage.success('解绑成功')
        refreshDevices()
      } else {
        ElMessage.error(res.message || '解绑失败')
      }
    } catch (error) {
      ElMessage.error('解绑设备失败')
    }
  }).catch(() => {
    // 取消操作
  })
}

/**
 * 选择转移目标用户
 */
const handleSelectTransferUser = (item) => {
  transferForm.userId = item.id
  transferForm.userName = item.realName
  transferForm.userDept = item.deptName
  transferForm.searchKey = item.realName
}

/**
 * 转移数据
 */
const handleTransfer = (row) => {
  currentDevice.value = row
  transferForm.userId = ''
  transferForm.userName = ''
  transferForm.userDept = ''
  transferForm.searchKey = ''
  transferDialogVisible.value = true
}

/**
 * 确认转移
 */
const confirmTransfer = async () => {
  if (!transferForm.userId) {
    ElMessage.warning('请选择目标用户')
    return
  }

  try {
    const res = await transferBufferData(currentDevice.value.id, transferForm.userId)
    if (res.code === 200) {
      ElMessage.success(`成功转移 ${res.data.transferred_count} 条数据`)
      transferDialogVisible.value = false
      refreshDevices()
    } else {
      ElMessage.error(res.message || '转移失败')
    }
  } catch (error) {
    ElMessage.error('转移数据失败')
  }
}

/**
 * 删除缓冲数据
 */
const handleDeleteBuffer = (row) => {
  ElMessageBox.confirm(
    `确定要清空设备 ${row.imei} 的 ${row.bufferCount} 条缓冲数据吗？此操作不可恢复！`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteBufferData(row.id)
      if (res.code === 200) {
        ElMessage.success('清空成功')
        refreshDevices()
      } else {
        ElMessage.error(res.message || '清空失败')
      }
    } catch (error) {
      ElMessage.error('清空缓冲数据失败')
    }
  }).catch(() => {
    // 取消操作
  })
}

/**
 * 格式化日期
 */
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

/**
 * 分页大小改变
 */
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1 // 重置到第一页
}

/**
 * 当前页改变
 */
const handleCurrentChange = (page) => {
  pagination.page = page
}

// 组件挂载时刷新设备列表和更新时间
let timer = null
let deviceTimer = null
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  refreshDevices()
  deviceTimer = setInterval(refreshDevices, 30000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (deviceTimer) clearInterval(deviceTimer)
  clearTimeout(searchTimer)
})
</script>

<style scoped lang="scss">
.device-management-container {
  padding: 20px;
  background: #0a0e27;
  height: calc(100vh - 50px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  color: #c8d8e8;
}

/* ── 标题栏 ── */
.page-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #0d1b4b 0%, #1a2a6c 50%, #0d1b4b 100%);
  border: 1px solid #2a3f7a;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 100, 200, 0.2);
}
.page-header-left { display: flex; align-items: center; gap: 14px; }
.header-icon {
  font-size: 36px; color: #00d4ff;
  background: rgba(0, 212, 255, 0.1); border-radius: 10px; padding: 8px;
}
.main-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 2px; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: #7eb8d4; margin: 0; }
.header-time {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #7eb8d4;
  background: rgba(0, 212, 255, 0.06); padding: 6px 14px;
  border-radius: 20px; border: 1px solid rgba(0, 212, 255, 0.2);
}

/* ── 统计卡片 ── */
.mb-16 { margin-bottom: 16px; flex-shrink: 0; }
.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 20px;
  background: #141830; border: 1px solid #232b4d; border-radius: 10px;
  position: relative; overflow: hidden;
  transition: transform 0.25s, box-shadow 0.25s;
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0, 212, 255, 0.15); }
}
.stat-icon-wrap {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &.primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  &.success { background: linear-gradient(135deg, #11998e, #38ef7d); color: #fff; }
  &.warning { background: linear-gradient(135deg, #f7971e, #ffd200); color: #fff; }
  &.info    { background: linear-gradient(135deg, #4facfe, #00f2fe); color: #fff; }
}
.stat-body { flex: 1; }
.stat-value {
  font-size: 28px; font-weight: 700; color: #e8f4ff; line-height: 1.1;
}
.stat-label { font-size: 12px; color: #7eb8d4; margin-top: 4px; }
.stat-badge {
  font-size: 11px; color: #ffd200;
  background: rgba(255, 210, 0, 0.12); border: 1px solid rgba(255, 210, 0, 0.3);
  padding: 2px 8px; border-radius: 10px;
}

/* ── 面板 ── */
.panel { background: #141830; border: 1px solid #232b4d; border-radius: 10px; overflow: hidden; }
.panel.table-panel { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-body { flex: 1; min-height: 0; overflow: hidden; }
.panel-header {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #232b4d;
}
.panel-header-right { display: flex; align-items: center; gap: 12px; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #c8d8e8; }
.title-bar { display: inline-block; width: 3px; height: 16px; background: #00d4ff; border-radius: 2px; }
.total-badge {
  font-size: 12px; color: #7eb8d4;
  background: rgba(0, 212, 255, 0.06); border: 1px solid rgba(0, 212, 255, 0.15);
  padding: 3px 12px; border-radius: 12px;
}

/* ── 表格 ── */
.table-ops { display: flex; align-items: center; justify-content: center; gap: 4px; flex-wrap: wrap; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.user-name { font-size: 13px; font-weight: 600; color: #e8f4ff; }
.user-dept { font-size: 11px; color: #7eb8d4; margin-top: 1px; }
.online-dot {
  display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px;
  &.online  { background: #38ef7d; box-shadow: 0 0 6px #38ef7d; }
  &.offline { background: #4a5578; }
}
.online-text { font-size: 12px; }
.text-muted { color: #7eb8d4; font-size: 12px; }

/* ── 用户搜索建议项样式 ── */
.user-suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}
.user-name-suggestion {
  font-weight: 600;
  color: #303133;
  min-width: 80px;
}
.user-dept-suggestion {
  color: #909399;
  font-size: 13px;
  flex: 1;
}
.user-phone-suggestion {
  color: #606266;
  font-size: 12px;
  font-family: monospace;
}

/* ── 已选择用户信息样式 ── */
.selected-user-info {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: rgba(0, 212, 255, 0.06);
  border-radius: 4px;
  font-size: 13px;
  color: #c8d8e8;
}

/* ── 分页 ── */
.pagination-wrap {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #232b4d;
}

/* ── 详情抽屉 ── */
.detail-content {
  padding: 0 4px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0 20px;
  border-bottom: 1px solid #232b4d;
  margin-bottom: 20px;
}
.detail-device-icon {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.detail-device-title {
  flex: 1;
  min-width: 0;
}
.detail-imei {
  font-size: 14px;
  font-weight: 700;
  color: #e8f4ff;
  font-family: monospace;
  word-break: break-all;
  margin-bottom: 8px;
}
.detail-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ml-8 { margin-left: 8px; }
.detail-section {
  margin-bottom: 24px;
}
.detail-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #00d4ff;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #1e2545;
}
.detail-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #1a1f3a;
  &:last-child { border-bottom: none; }
}
.detail-label {
  width: 90px;
  flex-shrink: 0;
  font-size: 12px;
  color: #7eb8d4;
}
.detail-value {
  flex: 1;
  font-size: 13px;
  color: #c8d8e8;
  &.mono { font-family: monospace; }
}
.detail-empty {
  font-size: 13px;
  color: #7eb8d4;
  padding: 12px 0;
  text-align: center;
}

/* ── 发消息对话框 ── */
.msg-dialog-meta {
  display: flex; align-items: center; flex-wrap: wrap; gap: 4px;
  padding: 10px 14px; margin-bottom: 16px;
  background: rgba(0, 212, 255, 0.06); border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 6px; font-size: 13px;
}
.msg-meta-label { color: #7eb8d4; }
.msg-meta-value { color: #e8f4ff; font-family: monospace; }
.msg-meta-user  { color: #7eb8d4; }

/* ── Element Plus 覆盖 ── */
:deep(.el-table) {
  background: transparent; color: #c8d8e8;
  --el-table-border-color: #232b4d;
  --el-table-row-hover-bg-color: #1e2545;
  th.el-table__cell { border-bottom-color: #232b4d !important; }
  td.el-table__cell { border-bottom-color: #1e2545 !important; }
  .el-table__row--striped td { background: #171d38 !important; }
  tr:hover > td { background: #1e2545 !important; }
}
:deep(.el-pagination) {
  --el-pagination-bg-color: #1a1f3a;
  --el-pagination-text-color: #7eb8d4;
  --el-pagination-button-color: #7eb8d4;
  --el-pagination-button-bg-color: #1a1f3a;
  --el-pagination-button-disabled-color: #3a4060;
  --el-pagination-button-disabled-bg-color: #141830;
  --el-pagination-hover-color: #00d4ff;
  --el-color-primary: #00d4ff;
}
:deep(.el-input__wrapper) {
  background: #1a1f3a; box-shadow: 0 0 0 1px #2d3561 inset;
  &:hover { box-shadow: 0 0 0 1px #00d4ff inset; }
}
:deep(.el-input__inner) { color: #c8d8e8; }
:deep(.el-textarea__inner) {
  background: #1a1f3a; color: #c8d8e8;
  box-shadow: 0 0 0 1px #2d3561 inset;
  &:hover { box-shadow: 0 0 0 1px #00d4ff inset; }
}
:deep(.el-dialog) {
  background: #141830; border: 1px solid #2d3561; border-radius: 12px;
  .el-dialog__header { border-bottom: 1px solid #232b4d; padding-bottom: 14px; }
  .el-dialog__title { color: #e8f4ff; font-weight: 600; }
  .el-dialog__headerbtn .el-dialog__close { color: #7eb8d4; &:hover { color: #00d4ff; } }
  .el-dialog__body { padding: 16px 24px 24px; }
  .el-dialog__footer { border-top: 1px solid #232b4d; padding-top: 14px; }
}
:deep(.el-form-item__label) { color: #7eb8d4; }
:deep(.el-loading-mask) { background-color: rgba(10, 14, 39, 0.8); }
:deep(.dark-drawer.el-drawer) {
  background: #0d1228;
  border-left: 1px solid #2d3561;
  color: #c8d8e8;
  .el-drawer__header {
    background: #141830;
    border-bottom: 1px solid #232b4d;
    margin-bottom: 0;
    padding: 16px 20px;
    color: #e8f4ff;
    font-weight: 600;
    font-size: 15px;
  }
  .el-drawer__close-btn { color: #7eb8d4; &:hover { color: #00d4ff; } }
  .el-drawer__body { padding: 20px; }
}
</style>
