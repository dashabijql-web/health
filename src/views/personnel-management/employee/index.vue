<template>
  <div class="page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><User /></el-icon>
        <div>
          <h1 class="main-title">职工信息管理</h1>
          <p class="sub-title">管理全部职工基本信息及健康档案</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><User /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">职工总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.active }}</div>
            <div class="stat-label">在职人数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap info"><el-icon size="26"><Monitor /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.online }}</div>
            <div class="stat-label">今日在线</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap warning"><el-icon size="26"><Male /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.maleRatio }}<span class="unit">%</span></div>
            <div class="stat-label">男性比例</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Data Panel -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>职工列表</div>
        <div class="panel-header-right">
          <span class="total-badge">共 {{ pagination.total }} 条</span>
          <el-button type="primary" size="small" :icon="Plus" @click="handleAdd">新增职工</el-button>
        </div>
      </div>

      <!-- Search -->
      <div class="search-form">
        <el-form :inline="true" :model="searchForm">
          <el-form-item>
            <el-input v-model="searchForm.keyword" placeholder="搜索姓名 / 工号"
              clearable style="width:200px" @clear="handleSearch" @keyup.enter="handleSearch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.deptId" placeholder="部门" clearable style="width:160px" @change="handleSearch">
              <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.jobTypeId" placeholder="工种" clearable style="width:140px" @change="handleSearch">
              <el-option v-for="j in jobTypeOptions" :key="j.id" :label="j.typeName" :value="j.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:110px" @change="handleSearch">
              <el-option label="在职" :value="0" />
              <el-option label="离职" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-body">
      <el-table :data="pagedData" v-loading="loading" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="{ background:'#1a1f3a', cursor:'pointer' }"
        @row-click="goToPortrait">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="empCode" label="工号" width="110" sortable />
        <el-table-column label="姓名" min-width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="goToPortrait(row)">{{ row.empName }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70" align="center" sortable :sort-method="(a,b) => (a.gender||0) - (b.gender||0)">
          <template #default="{ row }">
            <span>{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" min-width="120" show-overflow-tooltip sortable />
        <el-table-column prop="jobTypeName" label="工种" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="hireDate" label="入职日期" width="110" sortable />
        <el-table-column label="状态" width="80" align="center" sortable :sort-method="(a,b) => (a.status||0) - (b.status||0)">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 0 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-ops" @click.stop>
              <el-button type="warning" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
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

    <!-- CRUD Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px"
      :close-on-click-modal="false" class="dark-dialog" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="form-body">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="empName">
              <el-input v-model="form.empName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="empCode">
              <el-input v-model="form.empCode" placeholder="请输入工号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择部门" filterable style="width:100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工种" prop="jobTypeId">
              <el-select v-model="form.jobTypeId" placeholder="请选择工种" filterable style="width:100%">
                <el-option v-for="j in jobTypeOptions" :key="j.id" :label="j.typeName" :value="j.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker v-model="form.hireDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="0" :max="300" :precision="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="0" :max="500" :precision="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="请选择" style="width:100%">
                <el-option label="A型" value="A" />
                <el-option label="B型" value="B" />
                <el-option label="AB型" value="AB" />
                <el-option label="O型" value="O" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系">
              <el-input v-model="form.emergencyContact" placeholder="姓名+电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.statusBool" active-text="在职" inactive-text="离职"
                active-color="#38ef7d" inactive-color="#ff6b6b" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  Timer, Search, Refresh, Plus, Edit, Delete,
  User, CircleCheck, Monitor, Male
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEmployeeListDetail, getEmployeeStats, createEmployee, updateEmployee, deleteEmployee } from '@/api/employee'
import { getDepartmentList } from '@/api/department'
import { getJobTypeList } from '@/api/job-type'

const router = useRouter()

// ── Time ──
const currentTime = ref('')
const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}
updateTime()
const clockTimer = setInterval(updateTime, 1000)
onBeforeUnmount(() => clearInterval(clockTimer))

// ── Stats ──
const stats = reactive({ total: 0, active: 0, online: 0, maleRatio: 0 })

// ── Dropdown options ──
const deptOptions = ref([])
const jobTypeOptions = ref([])
const loadOptions = async () => {
  try {
    const [dRes, jRes] = await Promise.all([
      getDepartmentList(), getJobTypeList()
    ])
    if (dRes.code === 200) deptOptions.value = dRes.data?.list || dRes.data || []
    if (jRes.code === 200) jobTypeOptions.value = jRes.data?.list || jRes.data || []
  } catch (e) { /* silent */ }
}

// ── Search & Pagination ──
const searchForm = reactive({ keyword: '', deptId: null, jobTypeId: null, status: null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const loading = ref(false)
const allData = ref([])

const filteredData = computed(() => {
  let list = allData.value
  if (searchForm.keyword) {
    const kw = searchForm.keyword.toLowerCase()
    list = list.filter(r => (r.empName || '').toLowerCase().includes(kw) || (r.empCode || '').toLowerCase().includes(kw))
  }
  if (searchForm.deptId != null) list = list.filter(r => r.deptId === searchForm.deptId)
  if (searchForm.jobTypeId != null) list = list.filter(r => r.jobTypeId === searchForm.jobTypeId)
  if (searchForm.status != null) list = list.filter(r => r.status === searchForm.status)
  return list
})

const pagedData = computed(() => {
  pagination.total = filteredData.value.length
  const start = (pagination.page - 1) * pagination.size
  return filteredData.value.slice(start, start + pagination.size)
})

const loadTableData = async () => {
  loading.value = true
  try {
    const res = await getEmployeeListDetail()
    if (res.code === 200) {
      allData.value = res.data?.list || res.data || []
      pagination.total = allData.value.length
    }
  } catch (e) {
    ElMessage.error('加载职工列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await getEmployeeStats()
    if (res.code === 200 && res.data) Object.assign(stats, res.data)
  } catch (e) { /* silent */ }
}

const handleSearch = () => { pagination.page = 1 }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.deptId = null
  searchForm.jobTypeId = null
  searchForm.status = null
  pagination.page = 1
}
const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

// ── Navigate to health portrait ──
const goToPortrait = (row) => {
  router.push({ path: '/personnel-management/health-portrait', query: { empCode: row.empCode || row.emp_code } })
}

// ── CRUD Dialog ──
const dialogVisible = ref(false)
const dialogTitle = ref('新增职工')
const submitting = ref(false)
const formRef = ref(null)
const isEdit = ref(false)

const emptyForm = () => ({
  id: null, empName: '', empCode: '', gender: 1, phone: '',
  deptId: null, jobTypeId: null,
  birthDate: '', hireDate: '', height: null, weight: null,
  bloodType: '', emergencyContact: '', statusBool: true
})

const form = reactive(emptyForm())

const rules = {
  empName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  empCode: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增职工'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑职工'
  Object.assign(form, {
    id: row.id,
    empName: row.empName || '',
    empCode: row.empCode || '',
    gender: row.gender ?? 1,
    phone: row.phone || '',
    deptId: row.deptId || null,
    jobTypeId: row.jobTypeId || null,
    birthDate: row.birthDate || '',
    hireDate: row.hireDate || '',
    height: row.height || null,
    weight: row.weight || null,
    bloodType: row.bloodType || '',
    emergencyContact: row.emergencyContact || '',
    statusBool: row.status === 0
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form, status: form.statusBool ? 0 : 1 }
    delete data.statusBool
    const res = isEdit.value ? await updateEmployee(data) : await createEmployee(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      loadTableData()
      loadStats()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除职工「${row.empName}」吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteEmployee(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTableData()
      loadStats()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败，请重试')
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
}

// ── Init ──
onMounted(() => {
  loadOptions()
  loadTableData()
  loadStats()
})
</script>

<style scoped lang="scss">
@import '@/styles/dark-admin.scss';

.page-container {
  height: calc(100vh - 50px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: $da-bg;
  color: $da-text;
}
.page-header { @include da-page-header; flex-shrink: 0; }
.page-header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 36px; color: $da-accent; background: rgba(0,212,255,.1); border-radius: 10px; padding: 8px; }
.main-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 2px; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: $da-text-dim; margin: 0; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $da-text-dim; background: $da-accent-dim; padding: 6px 14px; border-radius: 20px; border: 1px solid $da-accent-hover; }

.mb-16 { margin-bottom: 16px; flex-shrink: 0; }
.stat-card { @include da-stat-card; }
.stat-icon-wrap { @include da-icon-wrap; &.primary { background: $da-grad-primary; } &.success { background: $da-grad-success; } &.warning { background: $da-grad-warning; } &.info { background: $da-grad-info; } &.danger { background: $da-grad-danger; } }
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: $da-text-bright; line-height: 1.1; .unit { font-size: 14px; font-weight: 400; color: $da-text-dim; margin-left: 2px; } }
.stat-label { font-size: 12px; color: $da-text-dim; margin-top: 4px; }

.panel { @include da-panel; }
.panel.table-panel { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-body { flex: 1; min-height: 0; overflow: hidden; }
.search-form { flex-shrink: 0; padding: 16px 20px; }
.panel-header { flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid $da-border; }
.panel-header-right { display: flex; align-items: center; gap: 12px; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: $da-text; }
.title-bar { display: inline-block; width: 3px; height: 16px; background: $da-accent; border-radius: 2px; }
.total-badge { font-size: 12px; color: $da-text-dim; background: $da-accent-dim; border: 1px solid $da-accent-border; padding: 3px 12px; border-radius: 12px; }

.table-ops { display: flex; align-items: center; justify-content: center; gap: 4px; flex-wrap: wrap; }
.pagination-wrap { flex-shrink: 0; display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid $da-border; }
.form-body { padding: 8px 0; }

@include da-el-overrides;
:deep(.el-input-number) { .el-input__wrapper { background: #0d1228; box-shadow: 0 0 0 1px $da-border-light inset; &:hover { box-shadow: 0 0 0 1px $da-accent inset; } } .el-input__inner { color: $da-text; } }
</style>
