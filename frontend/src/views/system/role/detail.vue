<template>
  <div class="page" v-loading="loadingRole">
    <div class="breadcrumb">
      <router-link to="/system/role">角色管理</router-link>
      <span class="sep">/</span>
      <span>{{ role?.roleName || '...' }}</span>
    </div>

    <div class="page-header" v-if="role">
      <div>
        <div class="page-title">
          {{ role.roleName }}
          <span class="role-key">{{ role.roleKey }}</span>
          <el-tag v-if="isBuiltin" type="warning" size="small" effect="light">内置</el-tag>
          <el-tag v-if="role.status === 1" type="success" size="small" effect="light">● 正常</el-tag>
          <el-tag v-else effect="plain" size="small">● 停用</el-tag>
        </div>
        <div class="page-sub">
          <el-tag :type="scopeType(role.dataScope)" effect="light" size="small">{{ role.dataScopeText }}</el-tag>
          <span>·</span>
          <span>用户数 <span class="mono">{{ role.userCount ?? 0 }}</span></span>
          <span>·</span>
          <span>创建于 {{ formatTime(role.createTime) }}</span>
        </div>
      </div>
    </div>

    <div class="page-nav">
      <el-button link @click="$router.push('/system/role')">← 返回角色列表</el-button>
      <span class="hint mono">{{ matrixCheckedCount }} 项已开</span>
    </div>

    <!-- 权限 Tab(单页直接渲染,无需切换) -->
    <div class="section">
      <div class="section-title">数据权限</div>
      <div class="scope-options">
        <label v-for="opt in scopeOptions" :key="opt.value"
               :class="['scope-option', { disabled: opt.disabled }]"
               :title="opt.disabled ? 'V1 暂未启用' : ''">
          <input type="radio" name="scope" :value="opt.value" v-model="formData.dataScope" :disabled="opt.disabled" />
          {{ opt.label }}
        </label>
      </div>
      <div class="section-help">本角色用户登录后能"看到"的数据范围;与功能权限独立,组合生效。</div>
    </div>

    <!-- 功能权限矩阵(摊平单层 v-for) -->
    <div class="section">
      <div class="section-title">功能权限</div>
      <div class="matrix">
        <table>
          <thead>
            <tr>
              <th style="width:120px">功能</th>
              <th style="width:180px">操作对象</th>
              <th>权限</th>
              <th style="width:80px;text-align:right">启用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in flatMatrix" :key="idx"
                :class="row.type === 'group' ? 'group-header' : ''"
                :style="row.type === 'group' ? 'cursor:default' : ''">
              <td v-if="row.type === 'group'" colspan="4">{{ row.groupName }}</td>
              <template v-else>
                <td class="function-cell">{{ row.groupName }}</td>
                <td class="object-cell">{{ row.objectName }}</td>
                <td>
                  <span v-for="action in row.actions" :key="action.label"
                        :class="['perm-checkbox', { checked: isActionOn(action.code) }]"
                        @click="toggleAction(action.code)">
                    <span class="box" :class="{ on: isActionOn(action.code) }">✓</span>
                    {{ action.label }}
                  </span>
                </td>
                <td style="text-align:right">
                  <div :class="['toggle', { on: isRowOn(row) }]" @click="toggleRow(row)"></div>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- footer -->
    <div class="footer-bar">
      <span :class="['help', { dirty: dirty }]">● {{ dirty ? `有 ${dirtyCount} 处更改未保存` : '所有更改已保存' }}</span>
      <div class="footer-spacer" />
      <el-button @click="resetDirty" :disabled="!dirty">撤销更改</el-button>
      <el-button class="btn-zen-primary" :loading="saving" :disabled="!dirty" @click="savePermission">更新</el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 阶段六 commit 1 v0.4 · 角色详情 · 仅权限 Tab
 *
 * v0.4 收尾:删除"成员"Tab;角色成员管理改走"用户管理"Tab → 分配 Dialog。
 *
 * 权限矩阵按 UI/权限.png 严格设计:
 *   - teal #0d9488 主色(radio / checkbox / toggle)
 *   - group-header 配色 + 中文 friendly action labels
 *   - 矩阵摊平为 flatMatrix 单层 v-for 渲染
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getRole, updateRole, assignMenus } from '@/api/sys-role'
import { listAllMenus } from '@/api/sys-menu'

defineOptions({ name: 'SystemRoleDetail' })

const route = useRoute()

const BUILTIN_KEYS = new Set(['admin', 'sales_director', 'sales_lead', 'sales', 'finance'])
const isBuiltin = computed(() => role.value && BUILTIN_KEYS.has(role.value.roleKey))
const scopeType = (s) => (s === 1 || s === 5) ? 'warning' : (s === 2 ? 'info' : 'success')
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

// ---------- 静态矩阵定义(按 phase6-role-permission.html 严格对齐) ----------
// action.code=null 表示 UI 占位(系统无对应 permCode)
const MATRIX_GROUPS = [
  {
    groupName: '工作台',
    rows: [
      { objectName: '跟进中心', actions: [
        { label: '查看', code: 'crm:record:center' },
        { label: '添加跟进', code: 'crm:record:add' }
      ] }
    ]
  },
  {
    groupName: '业务',
    rows: [
      { objectName: '线索', actions: [
        { label: '查看', code: 'crm:lead:list' },
        { label: '添加', code: 'crm:lead:add' },
        { label: '编辑', code: 'crm:lead:edit' },
        { label: '删除', code: 'crm:lead:delete' }
      ] },
      { objectName: '客户', actions: [
        { label: '查看', code: 'crm:customer:list' },
        { label: '添加', code: 'crm:customer:add' },
        { label: '编辑', code: 'crm:customer:edit' },
        { label: '删除', code: 'crm:customer:delete' },
        { label: '共享', code: 'crm:customer:share' }
      ] },
      { objectName: '商机', actions: [
        { label: '查看', code: 'crm:business:list' },
        { label: '添加', code: 'crm:business:add' },
        { label: '编辑', code: 'crm:business:edit' },
        { label: '删除', code: 'crm:business:delete' }
      ] }
    ]
  },
  {
    groupName: '交易',
    rows: [
      { objectName: '合同', actions: [
        { label: '查看', code: 'crm:contract:list' },
        { label: '添加', code: 'crm:contract:add' },
        { label: '编辑', code: 'crm:contract:edit' },
        { label: '删除', code: 'crm:contract:delete' }
      ] },
      { objectName: '回款', actions: [
        { label: '查看', code: 'crm:receivable:list' },
        { label: '添加', code: 'crm:receivable:add' },
        { label: '编辑', code: 'crm:receivable:edit' },
        { label: '删除', code: 'crm:receivable:delete' }
      ] },
      { objectName: '产品', actions: [
        { label: '查看', code: 'crm:product:list' },
        { label: '添加', code: 'crm:product:add' },
        { label: '编辑', code: 'crm:product:edit' },
        { label: '删除', code: 'crm:product:delete' }
      ] }
    ]
  },
  {
    groupName: '可视化',
    rows: [
      { objectName: '报表中心', actions: [
        { label: '查看', code: 'crm:report:view' }
      ] }
    ]
  },
  {
    groupName: '系统设置',
    rows: [
      { objectName: '用户管理', actions: [
        { label: '查看', code: 'sys:user:list' },
        { label: '添加', code: 'sys:user:add' },
        { label: '编辑', code: 'sys:user:edit' },
        { label: '删除', code: 'sys:user:delete' },
        { label: '分配角色', code: 'sys:user:assign_role' }
      ] },
      { objectName: '角色管理', actions: [
        { label: '查看', code: 'sys:role:list' },
        { label: '编辑', code: 'sys:role:edit' },
        { label: '分配菜单', code: 'sys:role:assign_menu' }
      ] },
      { objectName: '菜单权限', actions: [
        { label: '查看', code: 'sys:menu:list' },
        { label: '编辑', code: 'sys:menu:edit' }
      ] }
    ]
  }
]

// 摊平矩阵(摊成单层数组给 v-for),data row 多带 groupName/objectName 字段
const flatMatrix = computed(() => {
  const rows = []
  for (const g of MATRIX_GROUPS) {
    rows.push({ type: 'group', groupName: g.groupName })
    for (const r of g.rows) {
      rows.push({ type: 'row', groupName: g.groupName, objectName: r.objectName, actions: r.actions })
    }
  }
  return rows
})

const scopeOptions = [
  { label: '全部数据', value: 1, disabled: false },
  { label: '本部门数据', value: 3, disabled: false },
  { label: '仅本人数据', value: 5, disabled: false },
  { label: '指定部门数据(V1 暂未启用)', value: 2, disabled: true }
]

const tab = ref('permission')  // v0.4 收尾:只剩 1 个 Tab,固定值

// ---------- Tabs ----------
const role = ref(null)
const loadingRole = ref(false)
async function loadRole() {
  loadingRole.value = true
  try {
    const res = await getRole(route.params.id)
    role.value = res.data
    formData.dataScope = res.data.dataScope
    // 把 menuIds 翻回 permCodes 用于渲染勾选状态
    const codes = (res.data.menuIds || [])
      .map((id) => menuById.value[id]?.perms)
      .filter(Boolean)
    boundCodes.value = new Set(codes)
    originalCodes = new Set(codes)
    originalDataScope = res.data.dataScope
  } finally { loadingRole.value = false }
}

// ---------- 权限矩阵 ----------
const boundCodes = ref(new Set())
let originalCodes = new Set()
let originalDataScope = null
const formData = reactive({ dataScope: 5 })
const saving = ref(false)
const permToIdMap = ref({})
const menuById = ref({})

async function loadMenuMaps() {
  try {
    const res = await listAllMenus()
    const map = {}
    const byId = {}
    for (const m of (res.data || [])) {
      byId[m.id] = m
      if (m.perms) map[m.perms] = m.id
    }
    permToIdMap.value = map
    menuById.value = byId
  } catch (e) { /* 静默降级 */ }
}

function isActionOn(code) {
  if (!code) return false
  return boundCodes.value.has(code)
}

function toggleAction(code) {
  if (!code) return
  const s = new Set(boundCodes.value)
  s.has(code) ? s.delete(code) : s.add(code)
  boundCodes.value = s
}

function isRowOn(row) {
  if (row.type !== 'row') return false
  return row.actions.some((a) => a.code && boundCodes.value.has(a.code))
}

function toggleRow(row) {
  if (row.type !== 'row') return
  const codes = row.actions.map((a) => a.code).filter(Boolean)
  const s = new Set(boundCodes.value)
  if (isRowOn(row)) {
    codes.forEach((c) => s.delete(c))
  } else {
    codes.forEach((c) => s.add(c))
  }
  boundCodes.value = s
}

const dirty = computed(() => {
  if (formData.dataScope !== originalDataScope) return true
  if (boundCodes.value.size !== originalCodes.size) return true
  for (const c of boundCodes.value) if (!originalCodes.has(c)) return true
  return false
})
const dirtyCount = computed(() => {
  let n = 0
  if (formData.dataScope !== originalDataScope) n++
  return n + [...boundCodes.value].filter((c) => !originalCodes.has(c)).length
        + [...originalCodes].filter((c) => !boundCodes.value.has(c)).length
})
const matrixCheckedCount = computed(() => {
  let n = 0
  for (const r of flatMatrix.value) {
    if (r.type !== 'row') continue
    for (const a of r.actions) if (isActionOn(a.code)) n++
  }
  return n
})

function resetDirty() {
  boundCodes.value = new Set(originalCodes)
  formData.dataScope = originalDataScope
}

async function savePermission() {
  saving.value = true
  try {
    if (formData.dataScope !== originalDataScope) {
      await updateRole({ id: role.value.id, dataScope: formData.dataScope })
    }
    const menuIds = new Set()
    for (const code of boundCodes.value) {
      const id = permToIdMap.value[code]
      if (id) menuIds.add(id)
    }
    await assignMenus(role.value.id, [...menuIds])
    ElMessage.success('权限已更新')
    originalCodes = new Set(boundCodes.value)
    originalDataScope = formData.dataScope
    loadRole()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally { saving.value = false }
}

// ---------- 成员(v0.4 收尾:此页不再管成员增删,改走用户管理页) ----------

onMounted(async () => {
  await loadMenuMaps()
  await loadRole()
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.breadcrumb { font-size: 12.5px; color: var(--muted); margin-bottom: 12px; }
.breadcrumb a { color: var(--muted); text-decoration: none; }
.breadcrumb .sep { margin: 0 6px; color: var(--subtle); }

.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; display: flex; align-items: center; gap: 10px; }
.page-title .role-key { font-size: 14px; font-weight: 400; color: var(--muted); font-family: var(--font-mono); }
.page-sub { color: var(--muted); font-size: 13.5px; margin-top: 4px; display: flex; align-items: center; gap: 8px; }

/* v0.4:替代原 tabs 区,显示"返回"链接 + 矩阵勾选计数 */
.page-nav { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-nav .hint { color: var(--muted); font-size: 12.5px; }

.section { background: var(--surface); border: 1px solid var(--hairline); border-radius: var(--radius); padding: 20px 24px; margin-bottom: 20px; }
.section-title { font-size: 14px; font-weight: 600; color: var(--ink); margin-bottom: 12px; }
.section-help { font-size: 12px; color: var(--muted); margin-top: 8px; }

.scope-options { display: flex; flex-wrap: wrap; gap: 24px; }
.scope-option { display: flex; align-items: center; gap: 6px; font-size: 13.5px; color: var(--ink-soft); cursor: pointer; }
.scope-option input[type=radio] { accent-color: #0d9488; cursor: pointer; }
.scope-option.disabled { color: var(--subtle); cursor: not-allowed; }
.scope-option.disabled input { cursor: not-allowed; }

.matrix { border: 1px solid var(--hairline); border-radius: var(--radius); overflow: hidden; }
.matrix table { width: 100%; border-collapse: collapse; font-size: 13px; }
.matrix thead th { padding: 10px 16px; text-align: left; font-weight: 500; color: var(--muted); font-size: 12px; background: var(--bg); border-bottom: 1px solid var(--hairline); }
.matrix tbody td { padding: 12px 16px; border-bottom: 1px solid var(--hairline-soft); vertical-align: middle; }
.matrix tbody tr:last-child td { border-bottom: none; }
.matrix tbody tr:hover:not(.group-header) { background: var(--bg); }
.matrix tbody tr.group-header td { background: var(--accent-pale); font-weight: 600; color: var(--accent); font-size: 12.5px; text-transform: uppercase; letter-spacing: 0.04em; padding: 8px 16px; }
.matrix .function-cell { color: var(--ink-soft); font-weight: 500; }
.matrix .object-cell { color: var(--ink); }

.perm-checkbox { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; padding: 2px 6px; border-radius: 4px; margin-right: 4px; font-size: 12.5px; color: var(--muted); transition: color 0.12s, background 0.12s; }
.perm-checkbox:hover { background: var(--bg); }
/* box 永远 12×12 + 始终含 ✓ 字符（visibility 控制显示），避免行 baseline 抖动 */
.perm-checkbox .box {
  width: 12px; height: 12px;
  border: 1.5px solid var(--hairline);
  border-radius: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--surface);
  color: transparent;            /* 未勾选时透明(✓ 占位) */
  font-size: 9px;
  line-height: 1;
}
.perm-checkbox.checked { color: #0d9488; }
.perm-checkbox .box.on {
  background: #0d9488;
  border-color: #0d9488;
  color: white;
}

.toggle { width: 30px; height: 16px; background: var(--hairline); border-radius: 8px; position: relative; cursor: pointer; transition: background 0.18s; flex-shrink: 0; display: inline-block; }
.toggle::after { content: ''; position: absolute; top: 2px; left: 2px; width: 12px; height: 12px; background: white; border-radius: 50%; transition: left 0.18s; box-shadow: 0 1px 2px rgba(0,0,0,0.15); }
.toggle.on { background: #0d9488; }
.toggle.on::after { left: 16px; }

.footer-bar { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding-top: 4px; }
.footer-spacer { flex: 1; }
.help { font-size: 12.5px; color: var(--muted); }
.help.dirty { color: var(--warn); }
</style>
