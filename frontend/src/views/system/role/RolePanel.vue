<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索角色名 / roleKey" class="search" clearable @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="spacer" />
      <el-button :icon="Search" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button v-if="hasPerm('sys:role:edit')" :icon="Plus" class="btn-zen-primary" @click="handleCreate">新建角色</el-button>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column label="角色名 / roleKey" min-width="220">
          <template #default="{ row }">
            <div class="name-block">
              <span class="name">
                {{ row.roleName }}
                <el-tag v-if="isBuiltin(row)" type="warning" size="small" effect="light" class="builtin-tag">内置</el-tag>
              </span>
              <span class="sub">{{ row.roleKey }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数据范围" width="160">
          <template #default="{ row }">
            <el-tag :type="scopeType(row.dataScope)" effect="light" size="small">{{ row.dataScopeText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span :class="['status-text', row.status === 1 ? 'on' : 'off']">{{ row.status === 1 ? '正常' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="90" align="center">
          <template #default="{ row }">
            <span class="mono user-num">{{ row.userCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link class="action-link" @click="goDetail(row, 'permission')">编辑权限</el-button>
            <el-button link class="action-link" :class="{ danger: !isBuiltin(row) }" :disabled="isBuiltin(row)" @click="handleDelete(row)">
              {{ isBuiltin(row) ? '保护' : '删除' }}
            </el-button>
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

    <!-- 新建/编辑角色 Dialog -->
    <el-dialog v-model="editVisible" :title="editing.id ? '编辑角色' : '新建角色'" width="520px" :close-on-click-modal="false" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="角色名" prop="roleName">
              <el-input v-model="editing.roleName" placeholder="如 大区经理" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="roleKey" prop="roleKey">
              <el-input v-model="editing.roleKey" placeholder="如 regional_manager" :disabled="!!editing.id" />
              <div class="help">全局唯一,创建后不可修改</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="数据范围" prop="dataScope">
              <el-select v-model="editing.dataScope" style="width: 100%">
                <el-option label="1 - 全部" :value="1" />
                <el-option label="2 - 自定义(暂未启用)" :value="2" disabled />
                <el-option label="3 - 本部门" :value="3" />
                <el-option label="4 - 本部门及以下" :value="4" />
                <el-option label="5 - 仅本人" :value="5" />
              </el-select>
              <div class="help">决定本角色用户能看哪些数据</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" v-if="editing.id">
              <el-radio-group v-model="editing.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-else label="初始状态">
              <el-tag type="success" effect="light">正常</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { pageRole, addRole, updateRole, deleteRole } from '@/api/sys-role'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits(['count'])
const router = useRouter()
const { hasPerm } = useAuth()

const BUILTIN_KEYS = new Set(['admin', 'sales_director', 'sales_lead', 'sales', 'finance'])
const isBuiltin = (row) => BUILTIN_KEYS.has(row.roleKey)
const scopeType = (s) => (s === 1 || s === 5) ? 'warning' : (s === 2 ? 'info' : 'success')

const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await pageRole({
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
    emit('count', total.value)
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; loadList() }
function handleReset() { query.keyword = ''; query.pageNum = 1; loadList() }
function goDetail(row) { router.push(`/system/role/${row.id}`) }

const editVisible = ref(false); const saving = ref(false); const editFormRef = ref(null)
const editing = reactive({ id: null, roleName: '', roleKey: '', dataScope: 4, status: 1 })
const editRules = {
  roleName: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  roleKey: [
    { required: true, message: '请输入 roleKey', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '小写字母开头,只能含小写字母/数字/下划线', trigger: 'blur' }
  ],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
}
function resetEditForm() {
  editing.id = null; editing.roleName = ''; editing.roleKey = ''; editing.dataScope = 4; editing.status = 1
  editFormRef.value?.clearValidate()
}
function handleCreate() { resetEditForm(); editVisible.value = true }
async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) await updateRole(editing); else await addRole(editing)
    ElMessage.success(editing.id ? '已更新' : '已创建')
    editVisible.value = false; loadList()
  } finally { saving.value = false }
}
async function handleDelete(row) {
  if (isBuiltin(row)) return
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」?`, '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary' })
  await deleteRole(row.id); ElMessage.success('已删除'); loadList()
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 280px; }
.spacer { flex: 1; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name-block { display: flex; flex-direction: column; gap: 2px; }
.name-block .name { font-weight: 500; color: var(--ink); display: flex; align-items: center; gap: 6px; }
.name-block .sub { font-size: 11.5px; color: var(--muted); font-family: var(--font-mono); }
.builtin-tag { margin-left: 4px; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.user-num { font-weight: 600; color: var(--ink); }

/* 状态列 — 纯中文文字 + 颜色 */
.status-text { font-size: 13px; font-weight: 500; display: inline-block; }
.status-text.on { color: var(--accent); }
.status-text.off { color: var(--subtle); }

.action-link { padding: 0 6px; }
.action-link.danger { color: var(--danger); }
.action-link:disabled { color: var(--subtle); }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }
.help { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
</style>
