<template>
  <div class="ea-page">
    <!-- 顶部标题栏 -->
    <div class="ea-header">
      <div class="ea-title">
        <el-icon class="ea-title-icon"><UserFilled /></el-icon>
        职工健康档案库
      </div>
      <div class="ea-header-right">
        <span class="ea-total">共 <b>{{ loading ? '--' : filtered.length }}</b> 名员工</span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="ea-search-bar">
      <el-input v-model="keyword" placeholder="输入姓名或工号搜索" class="ea-input" clearable @clear="keyword = ''">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="selectedDept" placeholder="请选择部门" class="ea-dept-select" clearable>
        <el-option v-for="d in deptList" :key="d" :label="d" :value="d" />
      </el-select>
      <button class="ea-reset-btn" @click="keyword = ''; selectedDept = ''">重置</button>
      <button class="ea-dept-report-btn" v-if="selectedDept" @click="openDeptReport" title="生成该部门AI健康报告">
        🏥 AI部门报告
      </button>
      <button class="ea-add-btn" @click="handleAdd"><el-icon><Plus /></el-icon> 新增职工</button>
    </div>

    <!-- 员工卡片网格 -->
    <div v-loading="loading" class="ea-grid">
      <div v-for="emp in paginated" :key="emp.empCode" class="ea-card" @click="openPortrait(emp)">
        <div class="ea-card-top-bar"></div>
        <div class="ea-card-body">
          <div class="ea-info">
            <div class="ea-name-row">
              <span class="ea-name">{{ emp.empName }}</span>
              <span :class="['ea-gender', emp.gender === 2 ? 'f' : 'm']">{{ emp.gender === 2 ? '女' : '男' }}</span>
            </div>
            <div class="ea-fields">
              <div class="ea-field"><span class="ef-label">部门</span><span class="ef-val">{{ emp.deptName || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">岗位</span><span class="ef-val">{{ emp.jobTypeName || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">工号</span><span class="ef-val code">{{ emp.empCode || '--' }}</span></div>
              <div class="ea-field"><span class="ef-label">年龄</span><span class="ef-val">{{ calcAge(emp.birthDate) }}</span></div>
              <div class="ea-field full"><span class="ef-label">手机</span><span class="ef-val">{{ emp.phone || '--' }}</span></div>
            </div>
          </div>
        </div>
        <div class="ea-health-bar">
          <div class="ea-hb-label">健康状态</div>
          <div class="ea-hb-track">
            <div class="ea-hb-fill" :style="{ width: (emp._healthScore || 0) + '%', background: healthBarColor(emp._healthScore || 0) }"></div>
          </div>
          <div class="ea-hb-score" :style="{ color: healthBarColor(emp._healthScore || 0) }">{{ emp._healthScore || '--' }}</div>
        </div>
        <!-- 操作按钮区 -->
        <div class="ea-card-actions" @click.stop>
          <button class="ea-act-btn realtime" @click="openRealtime(emp)"><el-icon><Monitor /></el-icon> 实时</button>
          <button class="ea-act-btn report" @click="openAiReport(emp)"><el-icon><Document /></el-icon> AI报告</button>
          <button class="ea-act-btn edit" @click="handleEdit(emp)"><el-icon><Edit /></el-icon> 编辑</button>
          <button class="ea-act-btn delete" @click="handleDelete(emp)"><el-icon><Delete /></el-icon> 删除</button>
        </div>
      </div>

      <div v-if="!loading && filtered.length === 0" class="ea-empty">
        <el-icon size="48" color="#4a6080"><Search /></el-icon>
        <p>未找到符合条件的员工</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="ea-pagination" v-if="filtered.length > pageSize">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="filtered.length"
        layout="prev, pager, next, total" background class="ea-pager" />
    </div>

    <!-- AI 健康诊断报告 Dialog -->
    <el-dialog v-model="reportVisible" title="AI 健康诊断报告" width="820px"
      :close-on-click-modal="false" class="ea-dialog report-dialog">
      <div v-if="reportLoading" class="report-loading">
        <div class="report-dots"><span></span><span></span><span></span></div>
        <p>正在生成健康诊断报告，请稍候（约15~30秒）...</p>
      </div>
      <div v-else-if="reportContent" class="report-content" v-html="reportHtml"></div>
      <div v-else class="report-empty">生成失败，请重试</div>
      <template #footer>
        <el-button @click="reportVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!reportContent || reportLoading" @click="printReport">
          打印 / 导出 PDF
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 部门报告 Dialog -->
    <el-dialog v-model="deptReportVisible" :title="'AI 部门健康报告 — ' + selectedDept" width="820px"
      :close-on-click-modal="false" class="ea-dialog report-dialog">
      <div v-if="deptReportLoading" class="report-loading">
        <div class="report-dots"><span></span><span></span><span></span></div>
        <p>正在生成部门健康报告，请稍候（约20~40秒）...</p>
      </div>
      <div v-else-if="deptReportContent" class="report-content" v-html="deptReportHtml"></div>
      <div v-else class="report-empty">生成失败，请重试</div>
      <template #footer>
        <el-button @click="deptReportVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!deptReportContent || deptReportLoading" @click="printDeptReport">
          打印 / 导出 PDF
        </el-button>
      </template>
    </el-dialog>

    <!-- CRUD Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px"
      :close-on-click-modal="false" class="ea-dialog" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="empName"><el-input v-model="form.empName" placeholder="请输入姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="empCode"><el-input v-model="form.empCode" placeholder="请输入工号" /></el-form-item>
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
            <el-form-item label="手机" prop="phone"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
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
            <el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" placeholder="联系人姓名" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急电话"><el-input v-model="form.emergencyPhone" placeholder="联系人手机号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.statusBool" active-text="在职" inactive-text="离职"
                active-color="#38ef7d" inactive-color="#ff6b6b" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="既往病史">
              <el-checkbox-group v-model="form.medicalHistory">
                <el-checkbox value="高血压">高血压</el-checkbox>
                <el-checkbox value="糖尿病">糖尿病</el-checkbox>
                <el-checkbox value="心脏病">心脏病</el-checkbox>
                <el-checkbox value="哮喘">哮喘</el-checkbox>
                <el-checkbox value="颈椎病">颈椎病</el-checkbox>
                <el-checkbox value="腰椎病">腰椎病</el-checkbox>
              </el-checkbox-group>
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
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, Search, Plus, Edit, Delete, Monitor, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEmployeeListDetail, createEmployee, updateEmployee, deleteEmployee } from '@/api/employee'
import { getDepartmentList } from '@/api/department'
import { getJobTypeList } from '@/api/job-type'
import { generateEmployeeReport, generateDepartmentReport } from '@/api/ai'
import { renderMarkdown } from '@/utils/lazy-vendors'

const router = useRouter()
const loading = ref(false)
const employees = ref([])
const keyword = ref('')
const selectedDept = ref('')
const currentPage = ref(1)
const pageSize = 20

const deptOptions = ref([])
const jobTypeOptions = ref([])

const deptList = computed(() => {
  const set = new Set()
  employees.value.forEach(e => { if (e.deptName) set.add(e.deptName) })
  return [...set].sort()
})

const filtered = computed(() => {
  let list = employees.value
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(e => (e.empName || '').toLowerCase().includes(kw) || (e.empCode || '').toLowerCase().includes(kw))
  }
  if (selectedDept.value) list = list.filter(e => e.deptName === selectedDept.value)
  return list
})

const paginated = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

watch([keyword, selectedDept], () => { currentPage.value = 1 })

function calcAge(birthDate) {
  if (!birthDate) return '--'
  const age = new Date().getFullYear() - new Date(birthDate).getFullYear()
  return age > 0 && age < 100 ? age + '岁' : '--'
}

function healthBarColor(score) {
  if (score >= 80) return '#00e676'
  if (score >= 60) return '#ffaa00'
  return '#ff3b3b'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getEmployeeListDetail()
    if (res.code === 200) {
      employees.value = (res.data || []).map(e => ({ ...e, _healthScore: e.healthScore ?? null }))
    }
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

async function loadOptions() {
  try {
    const [dRes, jRes] = await Promise.all([getDepartmentList(), getJobTypeList()])
    if (dRes.code === 200) deptOptions.value = dRes.data?.list || dRes.data || []
    if (jRes.code === 200) jobTypeOptions.value = jRes.data?.list || jRes.data || []
  } catch (e) { /* silent */ }
}

// ── 导航 ──
function openPortrait(emp) {
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode: emp.empCode || '',
      empName: emp.empName || '',
      gender: emp.gender ?? 1,
      birthDate: emp.birthDate || '',
      deptName: emp.deptName || '',
      jobTypeName: emp.jobTypeName || '',
      phone: emp.phone || ''
    }
  })
}

function openRealtime(emp) {
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode: emp.empCode || '', empName: emp.empName || '',
      gender: emp.gender ?? 1, birthDate: emp.birthDate || '',
      deptName: emp.deptName || '', jobTypeName: emp.jobTypeName || '',
      phone: emp.phone || ''
    }
  })
}

// ── CRUD ──
const dialogVisible = ref(false)
const dialogTitle = ref('新增职工')
const submitting = ref(false)
const formRef = ref(null)
const isEdit = ref(false)

const emptyForm = () => ({
  id: null, empName: '', empCode: '', gender: 1, phone: '',
  deptId: null, jobTypeId: null, birthDate: '', hireDate: '',
  height: null, weight: null, bloodType: '',
  emergencyContact: '', emergencyPhone: '',
  medicalHistory: [],
  statusBool: true
})
const form = reactive(emptyForm())

const rules = {
  empName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  empCode: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  gender:  [{ required: true, message: '请选择性别', trigger: 'change' }],
  deptId:  [{ required: true, message: '请选择部门', trigger: 'change' }]
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增职工'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function handleEdit(emp) {
  isEdit.value = true
  dialogTitle.value = '编辑职工'
  Object.assign(form, {
    id: emp.id, empName: emp.empName || '', empCode: emp.empCode || '',
    gender: emp.gender ?? 1, phone: emp.phone || '',
    deptId: emp.deptId || null, jobTypeId: emp.jobTypeId || null,
    birthDate: emp.birthDate || '', hireDate: emp.hireDate || '',
    height: emp.height || null, weight: emp.weight || null,
    bloodType: emp.bloodType || '',
    emergencyContact: emp.emergencyContact || '',
    emergencyPhone: emp.emergencyPhone || '',
    medicalHistory: emp.medicalHistory ? (Array.isArray(emp.medicalHistory) ? emp.medicalHistory : emp.medicalHistory.split(',').filter(Boolean)) : [],
    statusBool: emp.status === 0
  })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form, status: form.statusBool ? 0 : 1 }
    delete data.statusBool
    if (Array.isArray(data.medicalHistory)) data.medicalHistory = data.medicalHistory.join(',')
    const res = isEdit.value ? await updateEmployee(data) : await createEmployee(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally { submitting.value = false }
}

async function handleDelete(emp) {
  try {
    await ElMessageBox.confirm(`确定要删除职工「${emp.empName}」吗？删除后不可恢复。`, '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' })
    const res = await deleteEmployee(emp.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败，请重试')
  }
}

function resetForm() { formRef.value?.resetFields() }

// ─── AI 健康报告 ─────────────────────────────────────────────────────────────
const reportVisible = ref(false)
const reportLoading = ref(false)
const reportContent = ref('')
const reportCurrentEmp = ref(null)
const reportHtml = ref('')

async function openAiReport(emp) {
  reportCurrentEmp.value = emp
  reportContent.value = ''
  reportHtml.value = ''
  reportLoading.value = true
  reportVisible.value = true
  try {
    const res = await generateEmployeeReport(emp.empCode)
    if (res.code === 200) {
      reportContent.value = res.data.report || ''
      reportHtml.value = await renderMarkdown(reportContent.value)
    } else {
      ElMessage.error(res.message || '报告生成失败')
      reportContent.value = ''
      reportHtml.value = ''
    }
  } catch (e) {
    ElMessage.error('报告生成失败，请稍后重试')
    reportContent.value = ''
    reportHtml.value = ''
  } finally {
    reportLoading.value = false
  }
}

function printReport() {
  if (!reportContent.value) return
  const emp = reportCurrentEmp.value
  const now = new Date().toLocaleString('zh-CN')
  const html = `<!DOCTYPE html>
<html lang="zh-CN"><head>
<meta charset="UTF-8">
<title>健康诊断报告 - ${emp?.empName || ''}</title>
<style>
  body { font-family: 'Microsoft YaHei', sans-serif; max-width: 800px; margin: 0 auto; padding: 24px; color: #1a1a2e; }
  h1,h2,h3 { color: #0066cc; }
  h2 { border-bottom: 1px solid #dde; padding-bottom: 6px; margin-top: 24px; }
  table { border-collapse: collapse; width: 100%; margin: 12px 0; }
  th { background: #e8f0ff; color: #003399; padding: 8px 12px; text-align: left; }
  td { padding: 7px 12px; border-bottom: 1px solid #eee; }
  blockquote { border-left: 4px solid #0066cc; margin: 12px 0; padding: 8px 16px; background: #f5f8ff; color: #444; }
  .meta { font-size: 12px; color: #888; margin-bottom: 20px; }
  @media print { body { padding: 0; } }
</style>
</head><body>
<div class="meta">生成时间：${now}</div>
${reportHtml.value}
</body></html>`
  const win = window.open('', '_blank')
  if (!win) { ElMessage.warning('请允许弹出窗口以打印报告'); return }
  win.document.write(html)
  win.document.close()
  setTimeout(() => win.print(), 600)
}

// AI 部门报告
const deptReportVisible = ref(false)
const deptReportLoading = ref(false)
const deptReportContent = ref('')
const deptReportHtml = ref('')

async function openDeptReport() {
  if (!selectedDept.value) return
  deptReportContent.value = ''
  deptReportHtml.value = ''
  deptReportLoading.value = true
  deptReportVisible.value = true
  try {
    const res = await generateDepartmentReport(selectedDept.value)
    if (res.code === 200) {
      deptReportContent.value = res.data.report || ''
      deptReportHtml.value = await renderMarkdown(deptReportContent.value)
    } else {
      ElMessage.error(res.message || '部门报告生成失败')
      deptReportContent.value = ''
      deptReportHtml.value = ''
    }
  } catch (e) {
    ElMessage.error('部门报告生成失败，请稍后重试')
    deptReportContent.value = ''
    deptReportHtml.value = ''
  } finally {
    deptReportLoading.value = false
  }
}

function printDeptReport() {
  if (!deptReportContent.value) return
  const now = new Date().toLocaleString('zh-CN')
  const html = `<!DOCTYPE html><html lang="zh-CN"><head>
<meta charset="UTF-8"><title>部门健康报告 - ${selectedDept.value}</title>
<style>body{font-family:'Microsoft YaHei',sans-serif;max-width:800px;margin:0 auto;padding:24px;color:#1a1a2e}
h2{color:#0066cc;border-bottom:1px solid #dde;padding-bottom:6px;margin-top:24px}
table{border-collapse:collapse;width:100%;margin:12px 0}
th{background:#e8f0ff;color:#003399;padding:8px 12px;text-align:left}
td{padding:7px 12px;border-bottom:1px solid #eee}
blockquote{border-left:4px solid #0066cc;margin:12px 0;padding:8px 16px;background:#f5f8ff;color:#444}
.meta{font-size:12px;color:#888;margin-bottom:20px}</style>
</head><body><div class="meta">生成时间：${now}</div>${deptReportHtml.value}</body></html>`
  const win = window.open('', '_blank')
  if (!win) { ElMessage.warning('请允许弹出窗口'); return }
  win.document.write(html)
  win.document.close()
  setTimeout(() => win.print(), 600)
}

onMounted(() => { loadData(); loadOptions() })
</script>

<style scoped>
.ea-page {
  min-height: 100vh;
  background: #0b0f1e;
  color: #c8d8f0;
  display: flex;
  flex-direction: column;
  padding: 0 0 24px;
}

/* 头部 */
.ea-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 28px 14px;
  border-bottom: 1px solid #1a2545;
  background: linear-gradient(180deg, #0f1628 0%, #0b0f1e 100%);
}
.ea-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #e8f4ff;
  letter-spacing: 2px;
  flex: 1;
  justify-content: center;
  text-shadow: 0 0 20px rgba(0, 180, 255, 0.4);
}
.ea-title-icon { color: #00b4ff; font-size: 24px; }
.ea-header-right { min-width: 100px; text-align: right; }
.ea-total { font-size: 13px; color: #7a9abf; }
.ea-total b { color: #00d4ff; font-size: 15px; }

/* 搜索栏 */
.ea-search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 28px;
  background: rgba(15, 22, 40, 0.8);
  border-bottom: 1px solid #1a2545;
}
.ea-input { width: 300px; }
.ea-dept-select { width: 200px; }
:deep(.ea-input .el-input__wrapper),
:deep(.ea-dept-select .el-input__wrapper) {
  background: rgba(10, 20, 40, 0.8) !important;
  border: 1px solid #1e3060 !important;
  box-shadow: none !important;
  border-radius: 6px;
}
:deep(.ea-input .el-input__inner),
:deep(.ea-dept-select .el-input__inner) { color: #c8d8f0 !important; background: transparent !important; }
:deep(.el-select-dropdown) { background: #0f1a35 !important; border: 1px solid #1e3060 !important; }
:deep(.el-select-dropdown__item) { color: #a0b8d8 !important; }
:deep(.el-select-dropdown__item:hover) { background: #1a2c50 !important; }

.ea-reset-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid #2a3a5a;
  color: #7a9abf;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.ea-reset-btn:hover { background: rgba(255, 255, 255, 0.1); color: #c8d8f0; }

.ea-dept-report-btn {
  padding: 8px 16px;
  background: rgba(0, 212, 100, 0.12);
  border: 1px solid rgba(0, 212, 100, 0.35);
  color: #00e676;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
  white-space: nowrap;
}
.ea-dept-report-btn:hover { background: rgba(0, 212, 100, 0.22); box-shadow: 0 0 10px rgba(0, 212, 100, 0.25); }

.ea-add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  padding: 8px 20px;
  background: linear-gradient(135deg, rgba(0, 100, 255, 0.3), rgba(0, 180, 255, 0.2));
  border: 1px solid rgba(0, 150, 255, 0.5);
  color: #00c8ff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
}
.ea-add-btn:hover { background: linear-gradient(135deg, rgba(0, 100, 255, 0.5), rgba(0, 180, 255, 0.35)); box-shadow: 0 0 12px rgba(0, 150, 255, 0.3); }

/* 卡片网格 */
.ea-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  padding: 20px 28px;
  flex: 1;
}
@media (max-width: 1400px) { .ea-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1000px) { .ea-grid { grid-template-columns: repeat(2, 1fr); } }

/* 卡片 */
.ea-card {
  background: linear-gradient(145deg, #0f1a35 0%, #0d1528 100%);
  border: 1px solid #1a2d50;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}
.ea-card:hover {
  transform: translateY(-3px);
  border-color: #00b4ff55;
  box-shadow: 0 8px 24px rgba(0, 100, 255, 0.15);
}
.ea-card-top-bar { height: 3px; background: linear-gradient(90deg, #00b4ff, #0050ff, transparent); }
.ea-card-body { display: flex; gap: 14px; padding: 14px 14px 10px; }
.ea-info { flex: 1; overflow: hidden; }
.ea-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.ea-name { font-size: 16px; font-weight: 700; color: #e8f4ff; }
.ea-gender { font-size: 11px; padding: 1px 6px; border-radius: 3px; font-weight: 600; }
.ea-gender.m { background: rgba(0, 150, 255, 0.2); color: #4ea8ff; border: 1px solid #0064ff44; }
.ea-gender.f { background: rgba(255, 60, 130, 0.15); color: #ff7eb0; border: 1px solid #ff3c8244; }
.ea-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 3px 8px; }
.ea-field { display: flex; align-items: center; gap: 5px; }
.ea-field.full { grid-column: span 2; }
.ef-label { font-size: 11px; color: #4a7090; min-width: 26px; }
.ef-val { font-size: 12px; color: #9ab8d8; }
.ef-val.code { font-family: monospace; color: #00c8ff; font-size: 11px; }

/* 健康条 */
.ea-health-bar { display: flex; align-items: center; gap: 8px; padding: 6px 14px 8px; }
.ea-hb-label { font-size: 11px; color: #4a7090; min-width: 44px; }
.ea-hb-track { flex: 1; height: 4px; background: #1a2a45; border-radius: 2px; overflow: hidden; }
.ea-hb-fill { height: 100%; border-radius: 2px; transition: width 0.5s; }
.ea-hb-score { font-size: 12px; font-weight: 700; min-width: 24px; text-align: right; }

/* 操作按钮 */
.ea-card-actions {
  display: flex;
  border-top: 1px solid #1a2d50;
}
.ea-act-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 7px 0;
  font-size: 12px;
  cursor: pointer;
  border: none;
  background: transparent;
  transition: background 0.2s, color 0.2s;
}
.ea-act-btn + .ea-act-btn { border-left: 1px solid #1a2d50; }
.ea-act-btn.realtime { color: #00c8ff; }
.ea-act-btn.realtime:hover { background: rgba(0, 200, 255, 0.1); }
.ea-act-btn.edit { color: #ffd200; }
.ea-act-btn.edit:hover { background: rgba(255, 210, 0, 0.1); }
.ea-act-btn.delete { color: #ff5252; }
.ea-act-btn.delete:hover { background: rgba(255, 82, 82, 0.1); }
.ea-act-btn.report { color: #38ef7d; }
.ea-act-btn.report:hover { background: rgba(56, 239, 125, 0.1); }

/* AI 报告弹窗 */
.report-loading {
  display: flex; flex-direction: column; align-items: center;
  padding: 48px 0; gap: 16px; color: #6a88b0;
}
.report-dots { display: flex; gap: 8px; }
.report-dots span {
  width: 10px; height: 10px; border-radius: 50%;
  background: #00d4ff; animation: dot-bounce 1.4s infinite ease-in-out both;
}
.report-dots span:nth-child(2) { animation-delay: 0.2s; }
.report-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}
.report-content {
  max-height: 65vh; overflow-y: auto; padding: 4px 8px;
  font-size: 14px; line-height: 1.8; color: #c8d8e8;
}
.report-content :deep(h2) { color: #00d4ff; font-size: 16px; border-bottom: 1px solid rgba(0,212,255,0.2); padding-bottom: 6px; margin: 20px 0 10px; }
.report-content :deep(h3) { color: #7dd3fc; font-size: 14px; margin: 14px 0 6px; }
.report-content :deep(strong) { color: #e8f4ff; font-weight: 700; }
.report-content :deep(table) { border-collapse: collapse; width: 100%; margin: 10px 0; }
.report-content :deep(th) { background: rgba(0,100,200,0.3); color: #7dd3fc; padding: 7px 12px; text-align: left; border: 1px solid rgba(0,212,255,0.2); }
.report-content :deep(td) { padding: 6px 12px; border: 1px solid rgba(0,212,255,0.1); color: #b0c8e0; }
.report-content :deep(tr:hover td) { background: rgba(0,212,255,0.04); }
.report-content :deep(blockquote) { border-left: 3px solid rgba(0,212,255,0.4); margin: 10px 0; padding: 6px 14px; background: rgba(0,212,255,0.05); color: #6a88b0; font-size: 12px; }
.report-content :deep(p) { margin: 5px 0; }
.report-content :deep(ul), .report-content :deep(ol) { padding-left: 20px; margin: 4px 0; }
.report-content :deep(li) { margin: 3px 0; color: #b0c8e0; }
.report-content :deep(hr) { border: none; border-top: 1px solid rgba(0,212,255,0.15); margin: 14px 0; }
.report-empty { text-align: center; padding: 40px; color: #6a88b0; }

/* 空状态 */
.ea-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #4a6080;
  gap: 12px;
  font-size: 14px;
}

/* 分页 */
.ea-pagination { display: flex; justify-content: center; padding: 8px 0 4px; }
:deep(.ea-pager .el-pagination__total),
:deep(.ea-pager .el-pager li),
:deep(.ea-pager button) {
  background: rgba(15, 26, 53, 0.8) !important;
  color: #6a88b0 !important;
  border: 1px solid #1a2d50 !important;
}
:deep(.ea-pager .el-pager li.is-active) {
  background: rgba(0, 100, 255, 0.3) !important;
  color: #00c8ff !important;
  border-color: #0064ff55 !important;
}

/* Dialog */
:deep(.ea-dialog) {
  background: #0f1628 !important;
  border: 1px solid #1e3060 !important;
  border-radius: 10px !important;
}
:deep(.ea-dialog .el-dialog__header) { border-bottom: 1px solid #1a2545; padding: 16px 20px; }
:deep(.ea-dialog .el-dialog__title) { color: #e8f4ff; font-weight: 600; }
:deep(.ea-dialog .el-dialog__body) { padding: 20px; }
:deep(.ea-dialog .el-dialog__footer) { border-top: 1px solid #1a2545; padding: 12px 20px; }
:deep(.ea-dialog .el-form-item__label) { color: #7a9abf; }
:deep(.ea-dialog .el-input__wrapper) { background: #0a1228 !important; box-shadow: 0 0 0 1px #1e3060 inset !important; }
:deep(.ea-dialog .el-input__inner) { color: #c8d8f0 !important; }
:deep(.ea-dialog .el-radio__label) { color: #c8d8f0; }
:deep(.ea-dialog .el-input-number) { width: 100%; }
:deep(.ea-dialog .el-input-number .el-input__wrapper) { background: #0a1228 !important; box-shadow: 0 0 0 1px #1e3060 inset !important; }
</style>
