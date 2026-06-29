<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索账号 / 昵称" class="search" clearable @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="全部状态" class="filter" clearable @change="handleSearch">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <div class="spacer" />
      <el-button :icon="Search" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button v-if="hasPerm('sys:user:edit')" :icon="Plus" class="btn-zen-primary" @click="handleCreate">新建用户</el-button>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column label="账号" width="120">
          <template #default="{ row }">
            <span class="mono accent">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="昵称 / 部门" min-width="180">
          <template #default="{ row }">
            <div class="name-block">
              <span class="name">{{ row.nickname }}</span>
              <span class="sub">{{ row.deptName || '— 未分配 —' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length">
              <el-tag v-for="rn in row.roleNames" :key="rn" :type="rn === '系统管理员' ? 'warning' : 'success'"
                       effect="light" size="small" class="role-chip">{{ rn }}</el-tag>
            </template>
            <span v-else class="text-muted">— 未分配 —</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center" :cell-style="{ paddingRight: '16px' }">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light">● 正常</el-tag>
            <el-tag v-else effect="plain">● 停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" :cell-style="{ paddingLeft: '16px' }">
          <template #default="{ row }">
            <span class="text-muted mono">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="hasAnyPerm(['sys:user:edit','sys:user:reset_pwd','sys:user:assign_role'])" label="操作" width="380" fixed="right" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link class="action-link" :disabled="isSelf(row)" @click="handleEdit(row)">编辑</el-button>
              <el-button link class="action-link" :disabled="isSelf(row)" @click="handleAssignRole(row)">分配</el-button>
              <el-button link class="action-link" :disabled="isSelf(row)" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button link class="action-link" :disabled="isSelf(row)" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button link class="action-link danger" :disabled="isSelf(row)" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="loadList"
        @size-change="loadList"
      />
    </el-card>

    <!-- 新建/编辑 Dialog -->
    <el-dialog v-model="editVisible" :title="editing.id ? '编辑用户' : '新建用户'" width="640px" :close-on-click-modal="false" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="editing.username" placeholder="如 sales_zhao" :disabled="!!editing.id" />
              <div class="help">全局唯一,创建后不可修改</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editing.nickname" placeholder="如 赵销售" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="editing.sex">
                <el-radio :value="0">男</el-radio>
                <el-radio :value="1">女</el-radio>
                <el-radio :value="2">未知</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号"><el-input v-model="editing.phone" placeholder="11 位手机号" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="邮箱"><el-input v-model="editing.email" placeholder="user@zencrm.local" /></el-form-item>
          </el-col>
          <el-col :span="12" />
        </el-row>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="editing.roleIds" multiple collapse-tags collapse-tags-tooltip placeholder="点击选择角色(可多选)" style="width: 100%">
            <el-option v-for="r in allRoles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
          <div class="help">admin 至少 1 人 — 当前 admin 启用人 {{ adminCount }}</div>
        </el-form-item>
        <el-form-item v-if="!editing.id" label="初始密码">
          <el-input v-model="editing.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item v-else label="状态">
          <el-radio-group v-model="editing.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 Dialog(单独从操作列进入) -->
    <el-dialog v-model="assignVisible" title="分配角色" width="520px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="用户">
          <el-input :model-value="assigning?.nickname" disabled />
        </el-form-item>
        <el-form-item label="角色(多选)">
          <el-select v-model="assignRoleIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in allRoles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <div class="help warn">⚠ 变更后该用户将立即失去原权限,需重新登录</div>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="assigning_loading" @click="confirmAssignRole">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 Dialog -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="用户">
          <el-input :model-value="resetting?.nickname" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwd" placeholder="留空则重置为 123456" />
        </el-form-item>
        <div class="help warn">⚠ 重置后该用户需重新登录</div>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="resetting_loading" @click="confirmResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  pageUser, addUser, updateUser, deleteUser,
  resetPassword, toggleStatus, assignRoles
} from '@/api/sys-user'
import { listAllRoles } from '@/api/sys-role'
import { useUserStore } from '@/store/user'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits(['count'])
const { hasPerm, hasAnyPerm } = useAuth()
const userStore = useUserStore()
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
const isSelf = (row) => row.id === userStore.userId

// ---------- 状态 ----------
const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const allRoles = ref([])
const adminCount = ref(0)

// ---------- 加载 ----------
async function loadList() {
  loading.value = true
  try {
    const res = await pageUser({
      keyword: query.keyword || undefined,
      status: query.status ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
    emit('count', total.value)
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await listAllRoles()
  allRoles.value = res.data || []
  adminCount.value = allRoles.value.filter((r) => r.roleKey === 'admin').length
}

function handleSearch() { query.pageNum = 1; loadList() }
function handleReset() { query.keyword = ''; query.status = null; query.pageNum = 1; loadList() }

// 新建 / 编辑
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, username: '', nickname: '', phone: '', email: '', sex: 0, status: 1, password: '123456', roleIds: [] })
const editRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '至少分配一个角色', trigger: 'change' }]
}
function resetEditForm() {
  editing.id = null; editing.username = ''; editing.nickname = ''
  editing.phone = ''; editing.email = ''; editing.sex = 0; editing.status = 1
  editing.password = '123456'; editing.roleIds = []
  editFormRef.value?.clearValidate()
}
function handleCreate() { resetEditForm(); editVisible.value = true }
async function handleEdit(row) {
  resetEditForm(); Object.assign(editing, row); editing.password = ''
  editVisible.value = true
}
async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await updateUser({ id: editing.id, nickname: editing.nickname,
        phone: editing.phone, email: editing.email, sex: editing.sex, status: editing.status, roleIds: editing.roleIds })
      ElMessage.success('已更新')
    } else {
      const payload = { ...editing }
      if (!payload.password) delete payload.password
      await addUser(payload)
      ElMessage.success('已创建')
    }
    editVisible.value = false; loadList()
  } finally { saving.value = false }
}

// 启停用 / 删除
async function handleToggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定${target === 0 ? '停用' : '启用'}「${row.nickname}」?`,
    `${target === 0 ? '停用' : '启用'}确认`,
    { type: 'warning', confirmButtonText: target === 0 ? '停用' : '启用', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary' }
  )
  await toggleStatus(row.id, target)
  ElMessage.success(target === 0 ? '已停用' : '已启用'); loadList()
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.nickname}」?`, '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary' })
  await deleteUser(row.id); ElMessage.success('已删除'); loadList()
}

// 重置密码
const resetVisible = ref(false); const resetting = ref(null); const resetPwd = ref(''); const resetting_loading = ref(false)
function handleResetPwd(row) { resetting.value = row; resetPwd.value = ''; resetVisible.value = true }
async function confirmResetPwd() {
  resetting_loading.value = true
  try {
    await resetPassword(resetting.value.id, resetPwd.value || undefined)
    ElMessage.success('密码已重置,该用户需重新登录'); resetVisible.value = false
  } finally { resetting_loading.value = false }
}

// 分配角色(单独 Dialog)
const assignVisible = ref(false); const assigning = ref(null); const assignRoleIds = ref([]); const assigning_loading = ref(false)
async function handleAssignRole(row) {
  // 先拉一次最新用户详情确保 roleIds 准确
  try {
    const { getUser } = await import('@/api/sys-user')
    const res = await getUser(row.id)
    assigning.value = res.data
    assignRoleIds.value = res.data.roleIds || []
  } catch {
    assigning.value = row
    assignRoleIds.value = row.roleIds || []
  }
  assignVisible.value = true
}
async function confirmAssignRole() {
  assigning_loading.value = true
  try {
    await assignRoles(assigning.value.id, assignRoleIds.value)
    ElMessage.success('已分配'); assignVisible.value = false; loadList()
  } finally { assigning_loading.value = false }
}

onMounted(() => { loadRoles(); loadList() })
</script>

<style lang="scss" scoped>
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 280px; }
.filter { width: 140px; }
.spacer { flex: 1; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name-block { display: flex; flex-direction: column; gap: 2px; }
.name-block .name { font-weight: 500; color: var(--ink); }
.name-block .sub { font-size: 11.5px; color: var(--muted); }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.role-chip { margin-right: 4px; }
.text-muted { color: var(--subtle); }

/* 操作列紧凑样式:不换行 + 白名单按钮紧凑 padding */
.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
  flex-wrap: nowrap;
  white-space: nowrap;
}
.action-link {
  padding: 0 4px !important;  /* 覆盖 element-plus 默认 0 12px */
  font-size: 12.5px !important;
  white-space: nowrap;
  margin: 0;
}
.action-link.danger { color: var(--danger); }
.action-link.is-disabled { color: var(--subtle); }

.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

.help { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.help.warn { color: var(--warn); }
</style>
