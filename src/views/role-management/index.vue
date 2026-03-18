<template>
  <div class="role-mgmt-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><Key /></el-icon>
        <div>
          <h1 class="main-title">角色管理</h1>
          <p class="sub-title">管理系统角色及权限分配</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><Setting /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.totalRoles }}</div>
            <div class="stat-label">角色总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.activeRoles }}</div>
            <div class="stat-label">已启用</div>
          </div>
          <div class="stat-badge green">启用</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon-wrap danger"><el-icon size="26"><CircleClose /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.inactiveRoles }}</div>
            <div class="stat-label">已禁用</div>
          </div>
          <div class="stat-badge red">禁用</div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索区 -->
    <div class="panel mb-16">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索角色名称 / 编码"
            clearable style="width:260px" @clear="handleSearch" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.status" placeholder="角色状态" clearable style="width:130px">
            <el-option label="启用" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 角色列表 -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>角色列表</div>
        <div class="panel-header-right">
          <span class="total-badge">共 {{ pagination.total }} 个角色</span>
          <el-button v-if="hasPerm('role:create')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增角色</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table :data="roleList" v-loading="loading" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="{ background:'#1a1f3a' }">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="角色信息" min-width="200">
          <template #default="{ row }">
            <div class="role-cell">
              <div class="role-icon-wrap"><el-icon><Key /></el-icon></div>
              <div>
                <div class="role-name">{{ row.roleName }}</div>
                <div class="role-code">{{ row.roleCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 0"
              active-color="#38ef7d"
              inactive-color="#4a5578"
              size="small"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip content="点击查看该角色的用户列表" placement="top">
              <el-button type="primary" link size="small" @click="handleViewUsers(row)" class="user-count-btn">
                <el-icon><User /></el-icon> {{ row.userCount }} 人
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="155">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-ops">
              <el-button v-if="hasPerm('role:update')" type="warning" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button v-if="hasPerm('role:assign')" type="primary" link size="small" @click="handleAssignPermissions(row)">
                <el-icon><Lock /></el-icon> 权限
              </el-button>
              <el-button v-if="hasPerm('role:delete')" type="danger" link size="small" @click="handleDelete(row)">
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

    <!-- ══ 新增 / 编辑角色弹窗 ══ -->
    <el-dialog v-model="formDialog.visible"
      :title="formDialog.isEdit ? '编辑角色' : '新增角色'"
      width="500px" :close-on-click-modal="false" class="dark-dialog"
      @closed="resetForm">
      <el-form ref="roleFormRef" :model="formDialog.form" :rules="formRules"
        label-width="90px" class="form-body">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formDialog.form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formDialog.form.roleCode" placeholder="请输入角色编码，如 ADMIN" :disabled="formDialog.isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formDialog.form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态" prop="statusBool">
          <el-switch v-model="formDialog.form.statusBool"
            active-text="启用" inactive-text="禁用"
            active-color="#38ef7d" inactive-color="#ff6b6b" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="handleFormSubmit">
          {{ formDialog.isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ══ 分配权限弹窗 ══ -->
    <el-dialog v-model="permDialog.visible" title="分配权限" width="520px"
      :close-on-click-modal="false" class="dark-dialog">
      <div class="perm-header">
        <div class="perm-role-info">
          <el-icon><Key /></el-icon>
          <span class="perm-role-name">{{ permDialog.roleName }}</span>
          <span class="perm-role-code">{{ permDialog.roleCode }}</span>
        </div>
        <div class="perm-actions">
          <el-button type="primary" link size="small" @click="checkAll">全选</el-button>
          <el-divider direction="vertical" />
          <el-button type="danger" link size="small" @click="uncheckAll">清空</el-button>
          <el-divider direction="vertical" />
          <el-button type="warning" link size="small" @click="expandAll">展开全部</el-button>
        </div>
      </div>
      <div v-loading="permDialog.loading" class="perm-tree-wrap">
        <div v-if="!permDialog.treeData || permDialog.treeData.length === 0" class="empty-tree">
          <el-icon size="48" color="#4a5578"><FolderOpened /></el-icon>
          <p>暂无权限数据</p>
        </div>
        <el-tree
          v-else
          ref="permTreeRef"
          :data="permDialog.treeData"
          show-checkbox
          node-key="id"
          :default-checked-keys="permDialog.checkedKeys"
          :default-expanded-keys="permDialog.expandedKeys"
          :props="{ label: 'name', children: 'children' }"
          class="perm-tree"
          @check="updatePermCheckedCount"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <el-icon v-if="data.type === 'menu'" class="tree-icon menu"><Menu /></el-icon>
              <el-icon v-else-if="data.type === 'button'" class="tree-icon btn"><Operation /></el-icon>
              <el-icon v-else class="tree-icon folder"><FolderOpened /></el-icon>
              <span>{{ node.label }}</span>
              <el-tag v-if="data.type === 'button'" size="small" type="info" class="tree-tag">按钮</el-tag>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <div class="perm-footer">
          <span class="perm-tip">已选 <strong>{{ permDialog.checkedCount }}</strong> 项权限</span>
          <div>
            <el-button @click="permDialog.visible = false">取消</el-button>
            <el-button type="primary" :loading="permDialog.submitting" @click="handleSavePermissions">保存权限</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- ══ 角色用户列表弹窗 ══ -->
    <el-dialog v-model="usersDialog.visible" :title="`「${usersDialog.roleName}」的用户列表`"
      width="760px" :close-on-click-modal="false" class="dark-dialog">
      <el-table :data="usersDialog.users" v-loading="usersDialog.loading" stripe
        style="width:100%" max-height="420"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar-sm">{{ (row.realName || '?')[0] }}</div>
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
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  getRoleList, getRoleDetail, getRoleUsers, getRoleStats,
  createRole, updateRole, deleteRole,
  getPermissionTree, getRolePermissions, assignPermissions
} from '@/api/role'
import { formatDate } from '@/utils'

const emptyForm = () => ({
  id: null,
  roleName: '',
  roleCode: '',
  description: '',
  statusBool: true
})

export default {
  name: 'RoleManagement',
  data() {
    return {
      Search: markRaw(Search), Refresh: markRaw(Refresh), Plus: markRaw(Plus),
      currentTime: '',
      loading: false,
      roleList: [],
      stats: { totalRoles: 0, activeRoles: 0, inactiveRoles: 0 },
      searchForm: { keyword: '', status: null },
      pagination: { page: 1, size: 20, total: 0 },

      // 新增/编辑弹窗
      formDialog: {
        visible: false,
        isEdit: false,
        submitting: false,
        form: emptyForm()
      },

      // 分配权限弹窗
      permDialog: {
        visible: false,
        loading: false,
        submitting: false,
        roleId: null,
        roleName: '',
        roleCode: '',
        treeData: [],
        checkedKeys: [],
        expandedKeys: [],
        checkedCount: 0
      },

      // 角色用户弹窗
      usersDialog: {
        visible: false,
        loading: false,
        roleName: '',
        users: []
      },

      // 表单校验
      formRules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleCode: [
          { required: true, message: '请输入角色编码', trigger: 'blur' },
          { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {},
  mounted() {
    this.updateTime()
    this._timer = setInterval(this.updateTime, 1000)
    this.loadRoleStats()
    this.loadRoleList()
  },
  beforeUnmount() {
    clearInterval(this._timer)
  },
  methods: {
    // 检查当前用户是否拥有某按钮权限码
    // buttons 为空（未配置权限）时降级显示全部按钮
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
    async loadRoleStats() {
      try {
        const res = await getRoleStats()
        if (res.code === 200) this.stats = res.data
      } catch (e) { /* ignore */ }
    },
    async loadRoleList() {
      this.loading = true
      try {
        const res = await getRoleList({
          ...this.searchForm,
          page: this.pagination.page,
          size: this.pagination.size
        })
        if (res.code === 200) {
          this.roleList = res.data.list
          this.pagination.total = res.data.total
        }
      } catch (e) {
        ElMessage.error('加载角色列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() { this.pagination.page = 1; this.loadRoleList() },
    handleReset() {
      this.searchForm = { keyword: '', status: null }
      this.handleSearch()
    },
    handleSizeChange(size) { this.pagination.size = size; this.loadRoleList() },
    handleCurrentChange(page) { this.pagination.page = page; this.loadRoleList() },

    // ── 新增
    handleAdd() {
      this.formDialog.isEdit = false
      this.formDialog.form = emptyForm()
      this.formDialog.visible = true
    },

    // ── 编辑
    async handleEdit(row) {
      try {
        const res = await getRoleDetail(row.id)
        if (res.code === 200) {
          const d = res.data
          this.formDialog.form = {
            id: d.id,
            roleName: d.roleName || '',
            roleCode: d.roleCode || '',
            description: d.description || '',
            statusBool: d.status === 0
          }
          this.formDialog.isEdit = true
          this.formDialog.visible = true
        }
      } catch (e) {
        ElMessage.error('加载角色信息失败')
      }
    },

    // ── 提交新增/编辑
    handleFormSubmit() {
      this.$refs.roleFormRef.validate(async (valid) => {
        if (!valid) return
        this.formDialog.submitting = true
        try {
          const form = this.formDialog.form
          const data = { ...form, status: form.statusBool ? 0 : 1 }
          const res = this.formDialog.isEdit
            ? await updateRole(data)
            : await createRole(data)
          if (res.code === 200) {
            ElMessage.success(this.formDialog.isEdit ? '修改成功' : '新增成功')
            this.$refs.roleFormRef?.clearValidate()
            this.formDialog.visible = false
            this.loadRoleList()
            this.loadRoleStats()
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
          `确定要删除角色「${row.roleName}」吗？删除后不可恢复，该角色下的用户权限将被清除。`,
          '删除确认',
          { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
        )
        const res = await deleteRole(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          this.loadRoleList()
          this.loadRoleStats()
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
          `确定要${label}角色「${row.roleName}」吗？`,
          '状态确认',
          { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )
        const res = await updateRole({
          id: row.id,
          roleName: row.roleName,
          roleCode: row.roleCode,
          description: row.description,
          status: newStatus
        })
        if (res.code === 200) {
          ElMessage.success(`${label}成功`)
          row.status = newStatus
          this.loadRoleStats()
        } else {
          ElMessage.error(res.message || '操作失败')
        }
      } catch (e) {
        // 取消
      }
    },

    // ── 分配权限
    async handleAssignPermissions(row) {
      this.permDialog.roleId = row.id
      this.permDialog.roleName = row.roleName
      this.permDialog.roleCode = row.roleCode
      this.permDialog.treeData = []
      this.permDialog.checkedKeys = []
      this.permDialog.expandedKeys = []
      this.permDialog.loading = true
      this.permDialog.visible = true

      try {
        const [treeRes, permRes] = await Promise.all([
          getPermissionTree(),
          getRolePermissions(row.id)
        ])
        if (treeRes.code === 200) {
          this.permDialog.treeData = treeRes.data
          // 展开第一层
          this.permDialog.expandedKeys = treeRes.data.map(n => n.id)
        }
        if (permRes.code === 200) {
          this.permDialog.checkedKeys = permRes.data
          // default-checked-keys 只在树首次渲染时生效；
          // 二次打开弹窗时树已挂载，必须用 setCheckedKeys() 强制刷新勾选状态
          await this.$nextTick()
          this.$refs.permTreeRef?.setCheckedKeys(permRes.data)
          this.updatePermCheckedCount()
        }
      } catch (e) {
        ElMessage.error('加载权限数据失败')
      } finally {
        this.permDialog.loading = false
      }
    },

    // ── 权限树勾选数量更新
    updatePermCheckedCount() {
      const tree = this.$refs.permTreeRef
      if (!tree) return
      this.permDialog.checkedCount = tree.getCheckedKeys().length + tree.getHalfCheckedKeys().length
    },

    // ── 保存权限
    async handleSavePermissions() {
      const tree = this.$refs.permTreeRef
      // 只取叶子节点（fully-checked），不含半选父级
      // 半选父级会导致回显时所有子节点全部被选中
      const permissionIds = tree.getCheckedKeys()

      this.permDialog.submitting = true
      try {
        const res = await assignPermissions(this.permDialog.roleId, permissionIds)
        if (res.code === 200) {
          ElMessage.success('权限保存成功')
          this.permDialog.visible = false
        } else {
          ElMessage.error(res.message || '保存失败')
        }
      } catch (e) {
        ElMessage.error('保存失败，请重试')
      } finally {
        this.permDialog.submitting = false
      }
    },

    // ── 权限树操作
    checkAll() {
      const setChecked = (nodes) => {
        nodes.forEach(node => {
          this.$refs.permTreeRef.setChecked(node.id, true, false)
          if (node.children) setChecked(node.children)
        })
      }
      setChecked(this.permDialog.treeData)
    },
    uncheckAll() {
      this.$refs.permTreeRef.setCheckedKeys([])
    },
    expandAll() {
      const expand = (nodes) => {
        nodes.forEach(node => {
          this.$refs.permTreeRef.store.nodesMap[node.id].expanded = true
          if (node.children) expand(node.children)
        })
      }
      expand(this.permDialog.treeData)
    },

    // ── 查看用户列表
    async handleViewUsers(row) {
      this.usersDialog.roleName = row.roleName
      this.usersDialog.visible = true
      this.usersDialog.loading = true
      try {
        const res = await getRoleUsers(row.id)
        if (res.code === 200) this.usersDialog.users = res.data
      } catch (e) {
        ElMessage.error('加载角色用户失败')
      } finally {
        this.usersDialog.loading = false
      }
    },

    resetForm() {
      this.$refs.roleFormRef?.resetFields()
    },

    formatDate
  }
}
</script>

<style scoped lang="scss">
.role-mgmt-container {
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
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; margin-bottom: 16px;
  background: linear-gradient(135deg, #0d1b4b 0%, #1a2a6c 50%, #0d1b4b 100%);
  border: 1px solid #2a3f7a; border-radius: 10px;
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
  display: flex; align-items: center; gap: 6px; font-size: 13px; color: #7eb8d4;
  background: rgba(0, 212, 255, 0.06); padding: 6px 14px;
  border-radius: 20px; border: 1px solid rgba(0, 212, 255, 0.2);
}

/* ── 统计卡片 ── */
.mb-16 { margin-bottom: 16px; flex-shrink: 0; }
.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 20px; background: #141830; border: 1px solid #232b4d; border-radius: 10px;
  position: relative; overflow: hidden; transition: transform 0.25s, box-shadow 0.25s;
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0, 212, 255, 0.15); }
}
.stat-icon-wrap {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &.primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  &.success { background: linear-gradient(135deg, #11998e, #38ef7d); color: #fff; }
  &.danger  { background: linear-gradient(135deg, #f5576c, #f093fb); color: #fff; }
}
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: #e8f4ff; line-height: 1.1; }
.stat-label { font-size: 12px; color: #7eb8d4; margin-top: 4px; }
.stat-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
  &.green { color: #38ef7d; background: rgba(56,239,125,.1); border: 1px solid rgba(56,239,125,.3); }
  &.red   { color: #ff6b6b; background: rgba(255,107,107,.1); border: 1px solid rgba(255,107,107,.3); }
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
  background: rgba(0,212,255,.06); border: 1px solid rgba(0,212,255,.15);
  padding: 3px 12px; border-radius: 12px;
}

/* ── 表格 ── */
.table-ops { display: flex; align-items: center; justify-content: center; gap: 4px; flex-wrap: wrap; }
.user-count-btn {
  font-weight: 600; transition: all 0.3s;
  &:hover { transform: scale(1.05); }
}
.role-cell { display: flex; align-items: center; gap: 10px; }
.role-icon-wrap {
  width: 34px; height: 34px; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 16px;
}
.role-name { font-size: 13px; font-weight: 600; color: #e8f4ff; }
.role-code { font-size: 11px; color: #7eb8d4; margin-top: 1px; }
.desc-text { font-size: 13px; color: #9baec8; }

/* ── 分页 ── */
.pagination-wrap {
  flex-shrink: 0;
  display: flex; justify-content: flex-end;
  padding: 14px 20px; border-top: 1px solid #232b4d;
}

/* ── 表单弹窗 ── */
.form-body { padding: 8px 0; }

/* ── 权限弹窗 ── */
.perm-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 0 14px;
  border-bottom: 1px solid #232b4d; margin-bottom: 12px;
}
.perm-role-info {
  display: flex; align-items: center; gap: 8px;
  color: #7eb8d4; font-size: 13px;
}
.perm-role-name { font-size: 15px; font-weight: 700; color: #e8f4ff; }
.perm-role-code { color: #7eb8d4; font-size: 12px; }
.perm-actions { display: flex; align-items: center; }
.perm-tree-wrap {
  height: 380px; overflow-y: auto; border: 1px solid #232b4d; border-radius: 8px;
  padding: 8px; background: #141830;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #2d3561; border-radius: 4px; }
}
.empty-tree {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; color: #7eb8d4;
  p { margin-top: 12px; font-size: 14px; }
}
.perm-tree {
  background: transparent;
  --el-tree-node-hover-bg-color: #1e2545;
  --el-tree-text-color: #c8d8e8;
  :deep(.el-tree-node__content) { height: 36px; border-radius: 6px; }
  :deep(.el-checkbox__inner) { background: #1a1f3a; border-color: #2d3561; }
  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background: #00d4ff; border-color: #00d4ff; }
  :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) { background: #4facfe; border-color: #4facfe; }
}
.tree-node {
  display: flex; align-items: center; gap: 6px; font-size: 13px;
}
.tree-icon {
  font-size: 15px;
  &.folder { color: #ffd200; }
  &.menu   { color: #4facfe; }
  &.btn    { color: #81c784; }
}
.tree-tag { margin-left: 6px; }
.perm-footer {
  display: flex; align-items: center; justify-content: space-between;
}
.perm-tip { font-size: 13px; color: #7eb8d4; strong { color: #00d4ff; } }

/* ── 角色用户弹窗 ── */
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar-sm {
  width: 30px; height: 30px; border-radius: 50%;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  color: #fff; font-size: 13px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.user-name { font-size: 13px; font-weight: 600; color: #e8f4ff; }
.user-code { font-size: 11px; color: #7eb8d4; margin-top: 1px; }

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
:deep(.el-dialog) {
  background: #141830; border: 1px solid #2d3561; border-radius: 12px;
  .el-dialog__header { border-bottom: 1px solid #232b4d; padding-bottom: 14px; }
  .el-dialog__title { color: #e8f4ff; font-weight: 600; }
  .el-dialog__headerbtn .el-dialog__close { color: #7eb8d4; &:hover { color: #00d4ff; } }
  .el-dialog__body { padding: 16px 24px 24px; }
  .el-dialog__footer { border-top: 1px solid #232b4d; padding-top: 14px; }
}
:deep(.el-switch__label) { color: #7eb8d4; }
</style>
