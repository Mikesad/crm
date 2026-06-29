<template>
  <div class="page">
    <!-- 顶部面包屑 + 标题 + actions -->
    <div class="breadcrumb">系统设置 <span class="sep">/</span> <span class="current">部门</span></div>

    <div class="page-header">
      <div>
        <div class="page-title">部门</div>
        <div class="page-sub">
          组织架构维护 · 共 <span class="mono">{{ depts.length }}</span> 个部门
          <span class="dot">·</span>
          <span class="text-muted">仅 admin / 销售总监 可访问</span>
        </div>
      </div>
      <div class="actions">
        <el-button @click="toggleExpandAll">{{ allExpanded ? '全部折叠' : '全部展开' }}</el-button>
      </div>
    </div>

    <!-- Variant B:左树 + 右详情 双栏 -->
    <div class="layout">
      <!-- 左:部门树 -->
      <div class="tree-panel">
        <div class="tree-header">
          <span>部门树</span>
          <span class="mono text-muted">{{ depts.length }} 节点</span>
        </div>
        <div class="tree-search">
          <el-input v-model="filterText" placeholder="筛选部门..." clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="tree-body">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            :filter-node-method="filterNode"
            :default-expanded-keys="defaultExpandedKeys"
            :expand-on-click-node="false"
            class="dept-tree"
            @node-click="onSelect"
          >
            <template #default="{ node, data }">
              <div :class="['tree-row', { selected: selectedId === data.id }]">
                <span class="tree-icon" :style="iconStyle(data)" v-html="iconSvg(data)"></span>
                <span :class="['tree-name', { disabled: data.status === 0 }]">{{ data.deptName }}</span>
                <span v-if="data.userCount" class="tree-count">{{ data.userCount }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右:详情面板 -->
      <div class="detail-panel" v-loading="loadingDetail">
        <!-- 未选中空状态 -->
        <div v-if="!selectedId" class="empty">
          <div class="empty-icon">🏢</div>
          <div class="empty-title">选择左侧部门查看详情</div>
          <div class="empty-desc">点击节点会显示该部门的详细信息、用户列表,以及对部门的新建/编辑/启停用/删除操作。</div>
        </div>

        <!-- 选中节点详情 -->
        <template v-else-if="detail">
          <div class="detail-header">
            <div>
              <div class="detail-title">
                <span class="tree-icon" :style="iconStyle(detail)" v-html="iconSvg(detail)"></span>
                {{ detail.deptName }}
                <el-tag v-if="detail.status === 1" type="success" size="small" effect="light">● 正常</el-tag>
                <el-tag v-else size="small" effect="plain">○ 停用</el-tag>
              </div>
              <div class="detail-meta">
                <span v-if="detail.parentId" class="crumb-part">
                  上级: <a class="crumb-link" @click="jumpToParent(detail.parentId)">{{ detail.parentName || ('ID ' + detail.parentId) }}</a>
                </span>
                <span v-else class="crumb-part"><span class="text-muted">顶级部门</span></span>
                <span class="sep-dot">·</span>
                <span class="mono">ancestors <strong>{{ detail.ancestors }}</strong></span>
                <span class="sep-dot">·</span>
                <span class="mono">orderNum <strong>{{ detail.orderNum }}</strong></span>
              </div>
            </div>
            <div class="quick-actions" v-if="hasPerm('sys:dept:edit')">
              <el-button :icon="Plus" @click="handleAddChild">新建子部门</el-button>
              <el-button @click="handleEdit">编辑</el-button>
              <el-button v-if="detail.status === 1" @click="handleToggleStatus(0)">停用</el-button>
              <el-button v-else @click="handleToggleStatus(1)">启用</el-button>
              <el-button :icon="Delete" plain class="btn-danger-ghost" @click="handleDelete">删除</el-button>
            </div>
          </div>

          <!-- 2 格信息卡(创建/最近更新已移除,见 v0.3 简化) -->
          <div class="info-card">
            <div class="info-cell">
              <div class="k">直接子部门</div>
              <div class="v mono">{{ detail.childCount }}</div>
            </div>
            <div class="info-cell">
              <div class="k">启用用户数</div>
              <div class="v mono">{{ detail.userCount }}</div>
            </div>
          </div>

          <!-- 用户列表 -->
          <div class="user-section">
            <div class="user-section-header">
              <div class="user-section-title">
                部门下用户 <span class="mono text-muted" style="margin-left:6px">{{ users.length }} 人</span>
              </div>
            </div>
            <el-card class="table-card user-card" v-loading="loadingUsers">
              <el-table :data="users" stripe size="default">
                <el-table-column prop="username" label="账号" min-width="120">
                  <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
                </el-table-column>
                <el-table-column prop="nickname" label="昵称" min-width="120" />
                <el-table-column label="角色" min-width="160">
                  <template #default="{ row }">
                    <span v-if="row.roleNames && row.roleNames.length" class="role-chips">
                      <el-tag v-for="r in row.roleNames" :key="r" size="small" effect="light" class="role-chip">{{ r }}</el-tag>
                    </span>
                    <span v-else class="text-muted">未分配</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.status === 1" type="success" size="small" effect="light">正常</el-tag>
                    <el-tag v-else size="small" effect="plain">停用</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="phone" label="手机" width="140">
                  <template #default="{ row }"><span class="mono">{{ row.phone || '—' }}</span></template>
                </el-table-column>
              </el-table>
              <div v-if="!loadingUsers && users.length === 0" class="no-users">该部门下暂无用户</div>
            </el-card>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建/编辑 Dialog -->
    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? '编辑部门' : '新建部门'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="editForm.deptName" placeholder="如 华北销售部" maxlength="50" />
          <div class="help">同级部门下唯一</div>
        </el-form-item>
        <el-form-item label="上级部门" prop="parentId">
          <el-cascader
            v-model="editForm.parentId"
            :options="parentCascaderOptions"
            :props="cascaderProps"
            placeholder="选择上级部门"
            class="parent-cascader"
            clearable
          />
          <div class="help">展开后选择;父级选项不包含自己和自己的后代,防死循环</div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="editForm.orderNum" :min="0" :max="999" controls-position="right" class="order-input" />
              <div class="help">同级从小到大排序</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="editForm.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :icon="Check" class="btn-zen-primary" :loading="submitting" @click="submitEdit">
          {{ editForm.id ? '保存' : '新建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Search, Check } from '@element-plus/icons-vue'
import {
  listAllDept, pageDept, getDept, addDept, updateDept, deleteDept, toggleDeptStatus
} from '@/api/sys-dept'
import { pageUser } from '@/api/sys-user'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'SystemDeptHome' })
const { hasPerm } = useAuth()

// ========== 数据 ==========
const depts = ref([])              // 全量平铺(后端 listAllDept 返回)
const detail = ref(null)           // 当前选中部门详情
const users = ref([])              // 当前选中部门下用户列表
const selectedId = ref(null)
const loadingDetail = ref(false)
const loadingUsers = ref(false)
const allExpanded = ref(false)
const treeRef = ref(null)
const filterText = ref('')

// 默认展开:顶级 + 1 级,首次加载给一个展开感
const defaultExpandedKeys = ref([])

const treeProps = {
  children: 'children',
  label: 'deptName',
  isLeaf: (d) => !d.children || d.children.length === 0
}

// ========== 树形构造 ==========
// 注意:sys_dept 表约定顶级 parent_id = 0(crm_full.sql DEFAULT 0),不是 null
// filter 条件 `d.parentId === 0` 才能捞到顶级部门;用 null 会导致 treeData = []
const treeData = computed(() => buildTree(depts.value, 0))

function buildTree(list, parentId) {
  const subset = list
    .filter(d => d.parentId === parentId)
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0))
  return subset.map(d => {
    const children = buildTree(list, d.id)
    return { ...d, children: children.length ? children : undefined }
  })
}

// 默认展开顶级 + 一级
watch(treeData, (val) => {
  if (val && val.length && defaultExpandedKeys.value.length === 0) {
    const keys = val.map(d => d.id)
    val.forEach(d => {
      if (d.children) keys.push(...d.children.map(c => c.id))
    })
    defaultExpandedKeys.value = keys
  }
}, { immediate: true })

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})
function filterNode(value, data) {
  if (!value) return true
  return data.deptName && data.deptName.toLowerCase().includes(value.toLowerCase())
}

// 展开/折叠
function toggleExpandAll() {
  if (!treeRef.value) return
  allExpanded.value = !allExpanded.value
  const nodes = allExpanded.value ? expandAll : collapseAll
  // 简化:遍历当前可见节点触发展开/折叠
  const allNodes = collectAllIds(treeData.value)
  allNodes.forEach(id => {
    const node = treeRef.value.getNode(id)
    if (node) {
      if (allExpanded.value) node.expand()
      else node.collapse()
    }
  })
}
function expandAll() {}
function collapseAll() {}
function collectAllIds(nodes, acc = []) {
  for (const n of nodes) {
    acc.push(n.id)
    if (n.children) collectAllIds(n.children, acc)
  }
  return acc
}

// 节点视觉
// level 算法:ancestors 字段以 ',' 分隔,但顶级 ancestors='0' 长度为 1,所以减 1 得到真实深度
//   顶级(ancestors='0'):level=0 → 大号实心六边形
//   一级(ancestors='0,1'):level=1 → 中号描边六边形
//   二级(ancestors='0,1,2'):level=2 → 小描边六边形
// 财务部:任何层级都用 warn 棕色调(在 iconStyle 中判定)
function level(data) {
  const anc = (data.ancestors || '').split(',').filter(Boolean)
  return Math.max(0, anc.length - 1)  // 减 1 是因为顶级 ancestors='0' 长度为 1
}

// 阶段七 v0.3:d 六边形 SVG(用户从 6 个备选中拍板)
// 见 frontend-design/phase7-dept-icons-preview.html 变体 D
function iconSvg(data) {
  const lv = level(data)
  if (lv === 0) {
    // 顶级:大实心六边形
    return '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="12,2 22,7 22,17 12,22 2,17 2,7"/></svg>'
  }
  if (lv === 1) {
    // 一级:中描边六边形
    return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12,3 21,7.5 21,16.5 12,21 3,16.5 3,7.5"/></svg>'
  }
  if (lv === 2) {
    // 二级:小描边六边形
    return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12,4 19,8 19,16 12,20 5,16 5,8"/></svg>'
  }
  // 三级及以上:微型实心六边形(叶子)
  return '<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="12,5 18,8 18,14 12,17 6,14 6,8"/></svg>'
}
function iconStyle(data) {
  // 财务部 warn 棕色调优先级最高(覆盖层级默认色)
  if (data.deptName && data.deptName.includes('财务')) {
    return 'background:var(--warn-soft);color:var(--warn)'
  }
  const lv = level(data)
  if (lv === 0) return 'background:var(--accent);color:white'
  if (lv === 1) return 'background:var(--info-soft);color:var(--info)'
  return 'background:var(--hairline-soft);color:var(--ink-soft)'
}

function onSelect(data) {
  selectedId.value = data.id
  loadDetail(data.id)
  loadUsers(data.id)
}

async function loadDetail(id) {
  loadingDetail.value = true
  try {
    const res = await getDept(id)
    detail.value = res.data
  } catch (e) {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function loadUsers(id) {
  loadingUsers.value = true
  try {
    const res = await pageUser({ deptId: id, pageNum: 1, pageSize: 200 })
    users.value = res.data?.records || []
  } catch (e) {
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

function jumpToParent(parentId) {
  const parent = depts.value.find(d => d.id === parentId)
  if (!parent) return
  const node = treeRef.value?.getNode(parentId)
  if (node) {
    // 展开祖先
    let cur = node.parent
    while (cur) { cur.expand(); cur = cur.parent }
    selectedId.value = parentId
    loadDetail(parentId)
    loadUsers(parentId)
  }
}

// ========== Dialog 新建/编辑 ==========
const editVisible = ref(false)
const submitting = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  deptName: '',
  parentId: null,
  orderNum: 1,
  status: 1
})
const editRules = {
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  // parentId 现在是数组(emitPath:true),空数组 = 未选,需要校验
  parentId: [{
    validator: (rule, value, cb) => {
      if (!Array.isArray(value) || value.length === 0) {
        return cb(new Error('上级部门不能为空'))
      }
      cb()
    },
    trigger: 'change'
  }],
  orderNum: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

// cascader 父级选项:剔除自己和自己的后代
// 关键:v0.3 修复之前 line 400 写 buildTree(depts.value, null) 的 bug
// sys_dept 顶级 parent_id = 0(crm_full.sql DEFAULT 0),不是 null
const cascaderProps = {
  value: 'id',
  label: 'deptName',
  children: 'children',
  checkStrictly: true,
  emitPath: true   // 关键:返回数组路径(让用户看到完整上级链路)
}
const parentCascaderOptions = computed(() => {
  const all = buildTree(depts.value, 0)   // ← 顶级参数 0(非 null,见 [[crm-dept-icon-design]])
  if (!editForm.id) return all  // 新建模式:全量可选(V1 后端阻止顶级)
  // 编辑模式:剔除自己 + 自己所有后代
  const blocked = new Set([editForm.id])
  const collect = (n) => { for (const c of (n.children || [])) { blocked.add(c.id); collect(c) } }
  const selfTree = buildTree(depts.value.filter(d => d.id === editForm.id), editForm.id)
  collect({ children: selfTree })
  return filterTreeById(all, blocked)
})
function filterTreeById(nodes, blocked) {
  return nodes
    .filter(n => !blocked.has(n.id))
    .map(n => ({ ...n, children: n.children ? filterTreeById(n.children, blocked) : undefined }))
}
function filterTree(nodes, blocked) {
  return nodes
    .filter(n => !blocked.includes(n.id))
    .map(n => ({ ...n, children: n.children ? filterTree(n.children, blocked) : undefined }))
}

function handleAddChild() {
  if (!detail.value) {
    ElMessage.warning('请先选中一个上级部门')
    return
  }
  if (!hasPerm('sys:dept:edit')) {
    ElMessage.warning('请联系管理员授予权限')
    return
  }
  resetEditForm()
  // cascader emitPath:true 需要数组路径:[总公司] 或 [总公司, 华东销售部]
  editForm.parentId = buildParentPath(detail.value.id)
  editForm.orderNum = (depts.value.filter(d => d.parentId === detail.value.id).length || 0) + 1
  editVisible.value = true
}
function handleEdit() {
  if (!detail.value) return
  if (!hasPerm('sys:dept:edit')) {
    ElMessage.warning('请联系管理员授予权限')
    return
  }
  resetEditForm()
  Object.assign(editForm, {
    id: detail.value.id,
    deptName: detail.value.deptName,
    parentId: buildParentPath(detail.value.parentId),
    orderNum: detail.value.orderNum,
    status: detail.value.status
  })
  editVisible.value = true
}

// 根据部门 id 构造 cascader 路径数组
// 返回如 []/[总公司]/[总公司, 华东销售部]
function buildParentPath(id) {
  if (!id || id === 0) return []
  const cur = depts.value.find(d => d.id === id)
  if (!cur) return [id]
  // 递归向上
  return [...buildParentPath(cur.parentId), cur.id]
}
function resetEditForm() {
  editFormRef.value?.resetFields()
  Object.assign(editForm, {
    id: null, deptName: '', parentId: [], orderNum: 1, status: 1
  })
}

async function submitEdit() {
  await editFormRef.value.validate()
  submitting.value = true
  try {
    // cascader emitPath:true → 数组;后端要的是单值 parentId(顶级=0)
    const parentId = Array.isArray(editForm.parentId)
      ? (editForm.parentId[editForm.parentId.length - 1] || 0)
      : (editForm.parentId || 0)
    if (editForm.id) {
      await updateDept({
        id: editForm.id,
        deptName: editForm.deptName,
        parentId,
        orderNum: editForm.orderNum,
        status: editForm.status
      })
      ElMessage.success('部门已更新')
    } else {
      await addDept({
        deptName: editForm.deptName,
        parentId,
        orderNum: editForm.orderNum,
        status: editForm.status
      })
      ElMessage.success('部门已新建')
    }
    editVisible.value = false
    await loadDepts()
    if (selectedId.value) {
      // 选中态可能因父变更丢失,重新找
      const targetId = selectedId.value
      const exists = depts.value.find(d => d.id === targetId)
      if (exists) {
        loadDetail(targetId)
        loadUsers(targetId)
      }
    }
  } catch (e) {
    // axios interceptor 已弹错
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(target) {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `确认要${target === 1 ? '启用' : '停用'}「${detail.value.deptName}」?`,
      '操作确认',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await toggleDeptStatus(detail.value.id, target)
    ElMessage.success(target === 1 ? '已启用' : '已停用')
    await loadDepts()
    loadDetail(detail.value.id)
  } catch (e) {}
}

async function handleDelete() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `确认删除部门「${detail.value.deptName}」?\n（顶级/有子部门/有用户 三类不可删）`,
      '删除确认',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await deleteDept(detail.value.id)
    ElMessage.success('已删除')
    selectedId.value = null
    detail.value = null
    users.value = []
    await loadDepts()
  } catch (e) {}
}

// ========== 加载 ==========
async function loadDepts() {
  try {
    const res = await listAllDept()
    depts.value = res.data || []
  } catch (e) {
    depts.value = []
  }
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadDepts)
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.breadcrumb { font-size: 12.5px; color: var(--muted); margin-bottom: 12px; }
.breadcrumb .sep { margin: 0 6px; color: var(--subtle); }
.breadcrumb .current { color: var(--ink); }

.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }
.actions { display: flex; gap: 8px; }
.dot { margin: 0 6px; color: var(--subtle); }
.text-muted { color: var(--subtle); }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

.layout { display: grid; grid-template-columns: 280px 1fr; gap: 16px; min-height: 580px; }

/* tree panel */
.tree-panel { background: var(--surface); border: 1px solid var(--hairline); border-radius: var(--radius); display: flex; flex-direction: column; overflow: hidden; }
.tree-header { padding: 12px 16px; border-bottom: 1px solid var(--hairline); font-size: 12px; color: var(--muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.04em; display: flex; justify-content: space-between; align-items: center; background: var(--bg); }
.tree-search { padding: 10px 12px; border-bottom: 1px solid var(--hairline-soft); }
.tree-body { padding: 8px 6px; flex: 1; overflow-y: auto; }
.dept-tree :deep(.el-tree-node__content) { height: 32px; padding-left: 4px !important; }
.dept-tree :deep(.el-tree-node__expand-icon) { font-size: 11px; color: var(--subtle); }
.tree-row { display: flex; align-items: center; gap: 6px; width: 100%; cursor: pointer; padding: 2px 4px; border-radius: 4px; }
.tree-row:hover { background: var(--accent-pale); }
.tree-row.selected { background: var(--accent-ring); }
.tree-icon { width: 20px; height: 20px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 11px; flex-shrink: 0; }
.tree-name { flex: 1; font-size: 13px; color: var(--ink-soft); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-name.disabled { color: var(--subtle); }
.tree-row.selected .tree-name { color: var(--accent); font-weight: 500; }
.tree-count { font-size: 10.5px; color: var(--muted); padding: 0 5px; background: var(--hairline-soft); border-radius: 3px; font-family: var(--font-mono); }
.tree-row.selected .tree-count { background: var(--accent-soft); color: var(--accent); }

/* detail panel */
.detail-panel { background: var(--surface); border: 1px solid var(--hairline); border-radius: var(--radius); overflow: hidden; }
.detail-header { padding: 18px 24px; border-bottom: 1px solid var(--hairline); display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.detail-title { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; color: var(--ink); }
.detail-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-top: 6px; color: var(--muted); font-size: 12.5px; }
.detail-meta strong { color: var(--ink); font-weight: 500; }
.sep-dot { color: var(--subtle); }
.crumbs-part { }
.crumb-link { color: var(--accent); cursor: pointer; text-decoration: none; }
.crumb-link:hover { text-decoration: underline; }
.quick-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.info-card { padding: 14px 24px; border-bottom: 1px solid var(--hairline); background: var(--bg); display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.info-cell .k { color: var(--muted); font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 4px; }
.info-cell .v { color: var(--ink); font-weight: 500; font-size: 14px; }
.info-cell .time { font-size: 12px; color: var(--muted); margin-left: 4px; }

.user-section { padding: 16px 24px 24px; }
.user-section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.user-section-title { font-size: 14px; font-weight: 600; color: var(--ink); }
.user-card { border: 1px solid var(--hairline) !important; border-radius: var(--radius) !important; }
.user-card :deep(.el-card__body) { padding: 0; }
.role-chips { display: inline-flex; gap: 4px; flex-wrap: wrap; }
.role-chip { font-size: 11.5px !important; }
.no-users { padding: 32px; text-align: center; color: var(--subtle); font-size: 13px; }

/* empty state */
.empty { padding: 80px 32px; text-align: center; color: var(--muted); }
.empty-icon { font-size: 48px; opacity: 0.5; margin-bottom: 16px; }
.empty-title { font-size: 15px; font-weight: 500; color: var(--ink-soft); margin-bottom: 6px; }
.empty-desc { font-size: 12.5px; color: var(--subtle); max-width: 320px; margin: 0 auto; line-height: 1.6; }

/* dialog */
.parent-cascader { width: 100%; }
.parent-cascader :deep(.el-cascader) { width: 100%; }
.order-input { width: 100%; }
.help { font-size: 11.5px; color: var(--muted); margin-top: 4px; line-height: 1.5; }
.btn-danger-ghost { color: var(--danger); border-color: var(--danger-soft); background: white; }
.btn-danger-ghost:hover { background: var(--danger-soft); }
</style>
