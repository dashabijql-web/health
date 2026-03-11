<template>
  <div class="user-list-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><UserFilled /></el-icon>
        <div>
          <h1 class="main-title">用户管理</h1>
          <p class="sub-title">管理平台注册用户及健康数据</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><User /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.activeUsers }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap warning"><el-icon size="26"><Monitor /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.onlineUsers }}</div>
            <div class="stat-label">在线用户</div>
          </div>
          <div class="stat-badge">实时</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap info"><el-icon size="26"><TrendCharts /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.onlineRate }}<span class="unit">%</span></div>
            <div class="stat-label">在线率</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索区 -->
    <div class="panel mb-16">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索用户名 / 姓名 / 手机号"
            clearable style="width:260px" @clear="handleSearch" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.status" placeholder="账号状态" clearable style="width:130px">
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.deptId" placeholder="所属部门" clearable style="width:140px">
            <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户列表 -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>用户列表</div>
        <div class="panel-header-right">
          <span class="total-badge">共 {{ pagination.total }} 人</span>
          <el-button v-if="hasPerm('user:create')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增用户</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table :data="userList" v-loading="loading" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="{ background:'#1a1f3a' }">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">{{ (row.realName || row.nickname || '?')[0] }}</div>
              <div>
                <div class="user-name">{{ row.realName }}</div>
                <div class="user-code">{{ row.userCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门" width="110" />
        <el-table-column label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="hasPerm('user:status')"
              :model-value="row.status === 0"
              active-color="#38ef7d"
              inactive-color="#4a5578"
              size="small"
              @change="handleStatusChange(row)"
            />
            <el-tag v-else :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线" width="80" align="center">
          <template #default="{ row }">
            <span :class="['online-dot', row.isOnline ? 'online' : 'offline']"></span>
            <span class="online-text">{{ row.isOnline ? '在线' : '离线' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="155">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-ops">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                <el-icon><InfoFilled /></el-icon> 详情
              </el-button>
              <el-button v-if="hasPerm('user:update')" type="warning" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button v-if="hasPerm('user:delete')" type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination-wrap">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :current-page="pagination.page" :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.size" :total="pagination.total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </div>

    <!-- ══ 新增 / 编辑用户弹窗 ══ -->
    <el-dialog v-model="formDialog.visible"
      :title="formDialog.isEdit ? '编辑用户' : '新增用户'"
      width="620px" :close-on-click-modal="false" class="dark-dialog"
      @closed="resetForm">
      <el-form ref="userFormRef" :model="formDialog.form" :rules="formRules"
        label-width="90px" class="form-body">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="formDialog.form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="formDialog.form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formDialog.form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formDialog.form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="formDialog.form.password" type="password"
                :placeholder="formDialog.isEdit ? '留空表示不修改' : '请输入密码'"
                show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="formDialog.form.deptId" placeholder="请选择部门" style="width:100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formDialog.form.gender">
                <el-radio :label="0">男</el-radio>
                <el-radio :label="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态" prop="status">
              <el-switch v-model="formDialog.form.statusBool"
                active-text="正常" inactive-text="禁用"
                active-color="#38ef7d" inactive-color="#ff6b6b" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="formDialog.form.roleIds" multiple placeholder="请选择角色"
                style="width:100%" :loading="rolesLoading">
                <el-option v-for="r in availableRoles" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="formDialog.form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="handleFormSubmit">
          {{ formDialog.isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ══ 用户详情弹窗 ══ -->
    <el-dialog v-model="detailDialog.visible" title="用户详情" width="580px"
      :close-on-click-modal="false" class="dark-dialog">
      <div v-if="detailDialog.data" class="detail-body">
        <div class="detail-avatar-row">
          <div class="detail-avatar">{{ (detailDialog.data.realName || '?')[0] }}</div>
          <div>
            <div class="detail-name">{{ detailDialog.data.realName }}</div>
            <el-tag :type="detailDialog.data.status === 0 ? 'success' : 'danger'" size="small" effect="dark">
              {{ detailDialog.data.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </div>
          <el-button type="primary" size="small" plain style="margin-left:auto"
            @click="openHealthDialog(detailDialog.data)">
            <el-icon><DataLine /></el-icon> 健康统计
          </el-button>
        </div>
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="用户编码">{{ detailDialog.data.userCode }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detailDialog.data.nickname }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detailDialog.data.gender === 0 ? '男' : '女' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailDialog.data.deptName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailDialog.data.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailDialog.data.email }}</el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">{{ formatDate(detailDialog.data.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后在线" :span="2">{{ formatDate(detailDialog.data.lastOnlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailDialog.data.remark || '无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- ══ 健康统计弹窗 ══ -->
    <el-dialog v-model="healthDialog.visible" title="健康统计" width="560px"
      :close-on-click-modal="false" class="dark-dialog">
      <div v-if="healthDialog.data" class="health-grid">
        <div class="health-item">
          <el-icon class="hi-icon" color="#00d4ff"><DataAnalysis /></el-icon>
          <div class="hi-value">{{ healthDialog.data.recordCount || 0 }}</div>
          <div class="hi-label">记录总数</div>
        </div>
        <div class="health-item">
          <el-icon class="hi-icon" color="#ff6b6b"><Histogram /></el-icon>
          <div class="hi-value">{{ (healthDialog.data.avgHeartRate || 0).toFixed(0) }}<span class="hi-unit">次/分</span></div>
          <div class="hi-label">平均心率</div>
        </div>
        <div class="health-item">
          <el-icon class="hi-icon" color="#4fc3f7"><TrendCharts /></el-icon>
          <div class="hi-value">{{ (healthDialog.data.avgBloodOxygen || 0).toFixed(0) }}<span class="hi-unit">%</span></div>
          <div class="hi-label">平均血氧</div>
        </div>
        <div class="health-item">
          <el-icon class="hi-icon" color="#81c784"><Moon /></el-icon>
          <div class="hi-value">{{ (healthDialog.data.avgSleepHours || 0).toFixed(1) }}<span class="hi-unit">h</span></div>
          <div class="hi-label">平均睡眠</div>
        </div>
        <div class="health-item-wide">
          <span class="hi-label-sm">最后记录时间</span>
          <span class="hi-value-sm">{{ formatDate(healthDialog.data.lastRecordTime) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  getUserList, getUserDetail, getUserStats, getUserHealthStats,
  createUser, updateUser, deleteUser, updateUserStatus
} from '@/api/user'
import { getAvailableRoles } from '@/api/role'
import { getDepartmentList } from '@/api/department'

const emptyForm = () => ({
  id: null,
  realName: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  deptId: null,
  gender: 0,
  statusBool: true,
  roleIds: [],
  remark: ''
})

export default {
  name: 'UserList',
  data() {
    return {
      Search, Refresh, Plus,
      currentTime: '',
      loading: false,
      userList: [],
      stats: { totalUsers: 0, activeUsers: 0, onlineUsers: 0, inactiveUsers: 0, onlineRate: 0 },
      searchForm: { keyword: '', status: null, deptId: null },
      pagination: { page: 1, size: 20, total: 0 },
      deptList: [],
      availableRoles: [],
      rolesLoading: false,
      // 新增/编辑弹窗
      formDialog: {
        visible: false,
        isEdit: false,
        submitting: false,
        form: emptyForm()
      },
      // 详情弹窗
      detailDialog: { visible: false, data: null },
      // 健康统计弹窗
      healthDialog: { visible: false, data: null },
      // 表单校验规则
      formRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
        password: [
          {
            validator: (rule, value, callback) => {
              if (!this.formDialog.isEdit && !value) {
                callback(new Error('请输入密码'))
              } else if (value && value.length < 6) {
                callback(new Error('密码长度至少 6 位'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  mounted() {
    this.updateTime()
    this._timer = setInterval(this.updateTime, 1000)
    this.loadUserStats()
    this.loadUserList()
    this.loadAvailableRoles()
    this.loadDepts()
  },
  beforeUnmount() {
    clearInterval(this._timer)
  },
  methods: {
    hasPerm(code) {
      const buttons = this.$store.getters.buttons
      if (!buttons || buttons.length === 0) return true
      return buttons.includes(code)
    },

    updateTime() {
      this.currentTime = new Date().toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      })
    },
    async loadUserStats() {
      try {
        const res = await getUserStats()
        if (res.code === 200) this.stats = res.data
      } catch (e) { console.error('加载用户统计失败:', e) }
    },
    async loadUserList() {
      this.loading = true
      try {
        const res = await getUserList({
          ...this.searchForm,
          page: this.pagination.page,
          size: this.pagination.size
        })
        if (res.code === 200) {
          this.userList = res.data.list
          this.pagination.total = res.data.total
        }
      } catch (e) {
        ElMessage.error('加载用户列表失败')
      } finally {
        this.loading = false
      }
    },
    async loadAvailableRoles() {
      this.rolesLoading = true
      try {
        const res = await getAvailableRoles()
        if (res.code === 200) this.availableRoles = res.data
      } catch (e) {
        console.error('加载角色列表失败:', e)
      } finally {
        this.rolesLoading = false
      }
    },
    async loadDepts() {
      try {
        const res = await getDepartmentList()
        if (res.code === 200) {
          const list = Array.isArray(res.data) ? res.data : (res.data?.list || [])
          this.deptList = list.map(d => ({ id: d.id, name: d.deptName || d.dept_name || d.name }))
        }
      } catch (e) {
        console.error('加载部门列表失败:', e)
      }
    },
    handleSearch() { this.pagination.page = 1; this.loadUserList() },
    handleReset() {
      this.searchForm = { keyword: '', status: null, deptId: null }
      this.handleSearch()
    },
    handleSizeChange(size) { this.pagination.size = size; this.loadUserList() },
    handleCurrentChange(page) { this.pagination.page = page; this.loadUserList() },

    // ── 新增
    handleAdd() {
      this.formDialog.isEdit = false
      this.formDialog.form = emptyForm()
      this.formDialog.visible = true
    },

    // ── 编辑
    async handleEdit(row) {
      try {
        const res = await getUserDetail(row.id)
        if (res.code === 200) {
          const d = res.data
          this.formDialog.form = {
            id: d.id,
            realName: d.realName || '',
            nickname: d.nickname || '',
            phone: d.phone || '',
            email: d.email || '',
            password: '',
            deptId: d.deptId || null,
            gender: d.gender ?? 0,
            statusBool: d.status === 0,
            roleIds: d.roleIds || [],
            remark: d.remark || ''
          }
          this.formDialog.isEdit = true
          this.formDialog.visible = true
        }
      } catch (e) {
        ElMessage.error('加载用户信息失败')
      }
    },

    // ── 提交新增/编辑
    handleFormSubmit() {
      this.$refs.userFormRef.validate(async (valid) => {
        if (!valid) return
        this.formDialog.submitting = true
        try {
          const form = this.formDialog.form
          const data = {
            ...form,
            status: form.statusBool ? 0 : 1
          }
          if (this.formDialog.isEdit && !data.password) delete data.password

          const res = this.formDialog.isEdit
            ? await updateUser(data)
            : await createUser(data)

          if (res.code === 200) {
            ElMessage.success(this.formDialog.isEdit ? '修改成功' : '新增成功')
            this.formDialog.visible = false
            this.loadUserList()
            this.loadUserStats()
          } else {
            ElMessage.error(res.message || '操作失败')
          }
        } catch (e) {
          ElMessage.error('操作失败，请重试')
        } finally {
          this.formDialog.submitting = false
        }
      })
    },

    // ── 删除
    async handleDelete(row) {
      try {
        await ElMessageBox.confirm(
          `确定要删除用户「${row.realName}」吗？删除后不可恢复。`,
          '删除确认',
          { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
        )
        const res = await deleteUser(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          this.loadUserList()
          this.loadUserStats()
        } else {
          ElMessage.error(res.message || '删除失败')
        }
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败，请重试')
      }
    },

    // ── 状态切换
    async handleStatusChange(row) {
      const newStatus = row.status === 0 ? 1 : 0
      const label = newStatus === 0 ? '启用' : '禁用'
      try {
        await ElMessageBox.confirm(
          `确定要${label}用户「${row.realName}」吗？`,
          '状态确认',
          { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )
        const res = await updateUserStatus(row.id, newStatus)
        if (res.code === 200) {
          ElMessage.success(`${label}成功`)
          row.status = newStatus
        } else {
          ElMessage.error(res.message || '操作失败')
        }
      } catch (e) {
        // 取消，不做任何更改
      }
    },

    // ── 详情
    async handleViewDetail(row) {
      try {
        const res = await getUserDetail(row.id)
        if (res.code === 200) {
          this.detailDialog.data = res.data
          this.detailDialog.visible = true
        }
      } catch (e) { ElMessage.error('加载用户详情失败') }
    },

    // ── 健康统计
    async openHealthDialog(row) {
      try {
        const res = await getUserHealthStats(row.userCode)
        if (res.code === 200) {
          this.healthDialog.data = res.data
          this.healthDialog.visible = true
        }
      } catch (e) { ElMessage.error('加载健康统计失败') }
    },

    resetForm() {
      this.$refs.userFormRef?.resetFields()
    },

    formatDate(date) {
      if (!date) return '-'
      return new Date(date).toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped lang="scss">
.user-list-container {
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
  .unit { font-size: 14px; font-weight: 400; color: #7eb8d4; margin-left: 2px; }
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
.search-form { flex-shrink: 0; padding: 16px 20px; }
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
.user-code { font-size: 11px; color: #7eb8d4; margin-top: 1px; }
.online-dot {
  display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px;
  &.online  { background: #38ef7d; box-shadow: 0 0 6px #38ef7d; }
  &.offline { background: #4a5578; }
}
.online-text { font-size: 12px; }

/* ── 分页 ── */
.pagination-wrap {
  flex-shrink: 0;
  display: flex; justify-content: flex-end;
  padding: 14px 20px; border-top: 1px solid #232b4d;
}

/* ── 详情弹窗 ── */
.detail-avatar-row {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 0 20px;
}
.detail-avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.detail-name { font-size: 18px; font-weight: 700; color: #e8f4ff; margin-bottom: 6px; }

/* ── 健康统计弹窗 ── */
.health-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 12px; padding: 8px 0;
}
.health-item {
  display: flex; flex-direction: column; align-items: center;
  padding: 18px 10px; background: #1a1f3a; border: 1px solid #232b4d;
  border-radius: 10px; text-align: center;
}
.hi-icon { font-size: 28px; margin-bottom: 10px; }
.hi-value {
  font-size: 22px; font-weight: 700; color: #e8f4ff;
  .hi-unit { font-size: 12px; color: #7eb8d4; margin-left: 2px; }
}
.hi-label { font-size: 12px; color: #7eb8d4; margin-top: 4px; }
.health-item-wide {
  grid-column: 1 / -1;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: #1a1f3a; border: 1px solid #232b4d; border-radius: 10px;
}
.hi-label-sm { font-size: 13px; color: #7eb8d4; }
.hi-value-sm { font-size: 13px; color: #e8f4ff; font-weight: 600; }

/* ── 表单弹窗 ── */
.form-body { padding: 8px 0; }

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
:deep(.el-input__prefix) { color: #7eb8d4; }
:deep(.el-textarea__inner) {
  background: #1a1f3a; color: #c8d8e8;
  box-shadow: 0 0 0 1px #2d3561 inset;
  &:hover { box-shadow: 0 0 0 1px #00d4ff inset; }
}
:deep(.el-select__wrapper) {
  background: #1a1f3a; box-shadow: 0 0 0 1px #2d3561 inset; color: #c8d8e8;
  &:hover { box-shadow: 0 0 0 1px #00d4ff inset; }
}
:deep(.el-form-item__label) { color: #7eb8d4; }
:deep(.el-radio__label) { color: #c8d8e8; }
:deep(.el-dialog) {
  background: #141830; border: 1px solid #2d3561; border-radius: 12px;
  .el-dialog__header { border-bottom: 1px solid #232b4d; padding-bottom: 14px; }
  .el-dialog__title { color: #e8f4ff; font-weight: 600; }
  .el-dialog__headerbtn .el-dialog__close { color: #7eb8d4; &:hover { color: #00d4ff; } }
  .el-dialog__body { padding: 16px 24px 24px; }
  .el-dialog__footer { border-top: 1px solid #232b4d; padding-top: 14px; }
}
:deep(.el-descriptions) {
  --el-descriptions-item-bordered-label-background: #1e2545;
  --el-descriptions-table-border: 1px solid #2d3561;
  .el-descriptions__label { color: #7eb8d4; font-weight: 500; }
  .el-descriptions__content { color: #c8d8e8; background: #1a1f3a; }
}
:deep(.el-switch__label) { color: #7eb8d4; }
</style>
