<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">线索管理</div>
        <div class="page-sub">{{ summaryText }}</div>
      </div>
    </div>

    <div class="layout">
      <!-- 主区 -->
      <div class="layout-main">
        <!-- 工具栏 -->
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索线索名称、联系人、电话"
            class="search"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="全部状态" class="filter" clearable @change="handleSearch">
            <el-option label="未跟进" :value="1" />
            <el-option label="跟进中" :value="2" />
            <el-option label="已转客户" :value="3" />
            <el-option label="已死线索" :value="4" />
          </el-select>
          <el-select v-model="query.source" placeholder="全部来源" class="filter" clearable @change="handleSearch">
            <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <div class="spacer" />
          <el-button @click="handleSearch" :icon="Search">查询</el-button>
          <el-button :icon="Upload" @click="openImportDialog">导入</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
          <el-button class="btn-zen-primary" :icon="Plus" @click="handleCreate">新建线索</el-button>
        </div>

        <!-- 表格 -->
        <el-card class="table-card" v-loading="loading">
          <el-table :data="list" stripe @sort-change="handleSortChange">
            <el-table-column prop="leadName" label="线索名称" min-width="160">
              <template #default="{ row }">
                <span class="name-link">{{ row.leadName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="contactName" label="联系人" width="100" />
            <el-table-column prop="phone" label="电话" width="130">
              <template #default="{ row }">
                <span class="mono">{{ row.phone || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.source" type="info" effect="plain">{{ row.source }}</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="负责人" width="90" />
            <el-table-column prop="createTime" label="创建时间" width="120" sortable="custom">
              <template #default="{ row }">
                <span class="mono">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link class="action-link" @click.stop="goDetail(row)">详情</el-button>
                <el-button link class="action-link" @click.stop="handleEdit(row)">编辑</el-button>
                <el-button
                  :disabled="row.status === 3 || row.status === 4"
                  link
                  class="action-link"
                  @click.stop="handleConvert(row)"
                >转客户</el-button>
                <el-tooltip
                  :content="deleteDisabledReason(row)"
                  placement="top"
                  :disabled="canDeleteLead(row)"
                >
                  <el-button
                    link
                    class="action-link danger"
                    :disabled="!canDeleteLead(row)"
                    @click.stop="handleDelete(row)"
                  >删除</el-button>
                </el-tooltip>
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
      </div>

      <!-- 右侧辅助面板 -->
      <aside class="layout-side">
        <div class="side-panel">
          <div class="side-panel-title">
            今日待办
          </div>
          <div
            v-for="t in todos"
            :key="t.relatedId || t.id"
            class="todo-item"
            @click="goTodoDetail(t)"
            style="cursor: pointer;"
          >
            <div class="todo-dot" :style="{ background: t.overdue ? '#b91c1c' : '#166534' }" />
            <div class="todo-content">
              <div class="todo-title">{{ t.subjectName || '跟进任务' }}</div>
              <div class="todo-sub">
                {{ t.overdue ? '已逾期 · ' : '今天 · ' }}{{ formatNextTime(t.nextFollowTime) }}
              </div>
            </div>
          </div>
          <div v-if="!todos || todos.length === 0" class="empty">今天没有待办 🎉</div>
        </div>

        <div class="side-panel">
          <div class="side-panel-title">
            本月统计
            <span class="more">{{ currentMonthLabel }}</span>
          </div>
          <div class="stat-grid">
            <div class="stat accent">
              <div class="stat-label">新增线索</div>
              <div class="stat-value">{{ stats.created }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">已转化</div>
              <div class="stat-value">{{ stats.converted }}</div>
            </div>
            <div class="stat info">
              <div class="stat-label">跟进中</div>
              <div class="stat-value">{{ stats.following }}</div>
            </div>
            <div class="stat danger">
              <div class="stat-label">死线索</div>
              <div class="stat-value">{{ stats.dead }}</div>
            </div>
          </div>
        </div>

      </aside>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editing.id ? '编辑线索' : '新建线索'"
      width="480px"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-width="84px" label-position="top">
        <el-form-item label="线索名称" prop="leadName">
          <el-input v-model="editing.leadName" placeholder="如：北京星辰科技" />
        </el-form-item>
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="editing.contactName" placeholder="如：王晓东" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="editing.phone" placeholder="如：138 0013 8000" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="editing.source" placeholder="选择来源" clearable allow-create filterable>
            <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" type="textarea" :rows="3" placeholder="客户背景、需求要点等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 转客户弹窗 -->
    <el-dialog
      v-model="convertVisible"
      title="线索转客户"
      width="480px"
    >
      <div class="convert-banner">
        <el-icon><WarningFilled /></el-icon>
        <div>
          转客户后，原线索「<strong>{{ converting.leadName }}</strong>」将标记为「已转客户」且不可撤销。原线索会保留作为审计追溯。
        </div>
      </div>
      <el-form ref="convertFormRef" :model="convertForm" :rules="convertRules" label-width="100px" label-position="top">
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="convertForm.customerName" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-input v-model="convertForm.industry" placeholder="如：智能硬件、互联网" />
        </el-form-item>
        <el-form-item label="客户级别">
          <el-radio-group v-model="convertForm.level">
            <el-radio value="A">A 重要</el-radio>
            <el-radio value="B">B 普通</el-radio>
            <el-radio value="C">C 意向</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主联系人职务">
          <el-input v-model="convertForm.post" />
        </el-form-item>
        <el-form-item label="主联系人手机" prop="phone">
          <el-input v-model="convertForm.phone" />
          <div class="form-hint">默认沿用线索电话，可在此修改</div>
        </el-form-item>
        <el-form-item label="决策权重">
          <el-radio-group v-model="convertForm.decisionWeight">
            <el-radio :value="1">1 核心决策者</el-radio>
            <el-radio :value="2">2 弱影响者</el-radio>
            <el-radio :value="3">3 普通职员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="convertVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting_" @click="handleConvertSubmit">确认转客户</el-button>
      </template>
    </el-dialog>

    <!-- 阶段四:Excel 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入线索"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        accept=".xlsx"
        :on-change="onImportFileChange"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽 xlsx 文件到此或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            必填列:线索名称、联系人。<br>
            状态文字:未跟进/跟进中/已转客户/已死线索(留空=未跟进)。<br>
            负责人:填 username 或昵称,留空=当前用户。
          </div>
        </template>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-alert
          :type="importResult.failRows === 0 ? 'success' : 'warning'"
          :closable="false"
          show-icon
        >
          <div>总计 {{ importResult.totalRows }} 行,成功 {{ importResult.successRows }} 行,失败 {{ importResult.failRows }} 行</div>
          <div v-if="importResult.failRows > 0" class="import-errors">
            <div v-for="(msg, line) in importResult.errors" :key="line">
              第 {{ line }} 行: {{ msg }}
            </div>
          </div>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Upload, Download, WarningFilled, UploadFilled } from '@element-plus/icons-vue'
import {
  pageLead,
  addLead,
  updateLead,
  deleteLead,
  convertLead,
  exportLeadExcel,
  importLeadExcel,
  getLeadStats
} from '@/api/lead'
import { todoList } from '@/api/record'

defineOptions({ name: 'LeadList' })

const router = useRouter()

// ---------- 查询 / 列表 ----------
const query = reactive({
  keyword: '',
  status: null,
  source: '',
  sortBy: 'createTime',  // v0.17 列头点击排序
  order: 'desc',
  pageNum: 1,
  pageSize: 10
})

// v0.17:列头点击排序回调(与商机 list 一致)
function handleSortChange({ prop, order }) {
  // el-table 排序事件:order = ascending | descending | null
  // 不支持 prop 的列不传 sortable,不进 switch
  if (order === null) {
    query.sortBy = ''
    query.order = ''
  } else {
    query.sortBy = prop
    query.order = order === 'ascending' ? 'asc' : 'desc'
  }
  query.pageNum = 1
  loadList()
}
const list = ref([])
const total = ref(0)
const loading = ref(false)

const sourceOptions = ['线上留单', '展会', '广告', '客户介绍', '官网注册', '电话咨询']

const statusText = (s) => ({ 1: '未跟进', 2: '跟进中', 3: '已转客户', 4: '已死线索' }[s] || '-')
const statusColor = (s) => ({ 1: '#a1a1aa', 2: '#1e40af', 3: '#166534', 4: '#b91c1c' }[s] || '#a1a1aa')
const statusTagType = (s) => ({ 1: 'info', 2: 'primary', 3: 'success', 4: 'danger' }[s] || 'info')
const formatDate = (t) => t ? t.substring(0, 10) : '-'

const summaryText = computed(() => {
  const c = list.value.length
  return c === 0 && !loading.value ? '暂无数据' : `共 ${total.value} 条线索 · 当前显示 ${c} 条`
})

async function loadList() {
  loading.value = true
  try {
    const res = await pageLead({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      source: query.source || undefined,
      sortBy: query.sortBy || undefined,  // v0.16
      order: query.order || undefined,     // v0.16
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

// ---------- 右侧辅助面板(真实数据) ----------
// 今日待办:走跟进中心今日 Tab 同款接口,过滤 relatedType='lead',取 5 条
const todos = ref([])
// 本月统计:走 /crm/lead/stats?range=month
const stats = reactive({ created: 0, converted: 0, following: 0, dead: 0 })
const currentMonthLabel = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1} 月`
})

function formatNextTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function goTodoDetail(t) {
  if (!t?.relatedId) return
  router.push(`/lead/${t.relatedId}`)
}

async function loadSidePanel() {
  // 今日待办
  try {
    const { data } = await todoList({ range: 'today', pageNum: 1, pageSize: 5 })
    const records = (data && data.records) || []
    todos.value = records.filter(r => r.relatedType === 'lead')
  } catch (e) { todos.value = [] }
  // 本月统计
  try {
    const { data } = await getLeadStats({ range: 'month' })
    Object.assign(stats, data || { created: 0, converted: 0, following: 0, dead: 0 })
  } catch (e) { /* 保持 0 */ }
}

// ---------- 新建 / 编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, leadName: '', contactName: '', phone: '', source: '', remark: '' })
// 中国手机号校验：空值可选；非空时先剥离空格/中划线/括号再匹配 11 位 1[3-9] 开头
const phoneRule = {
  validator: (rule, value, callback) => {
    if (!value) return callback()
    const normalized = String(value).replace(/[\s\-()]/g, '')
    if (!/^1[3-9]\d{9}$/.test(normalized)) {
      return callback(new Error('请输入正确的 11 位手机号'))
    }
    callback()
  },
  trigger: 'blur'
}
const editRules = {
  leadName: [{ required: true, message: '请输入线索名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [phoneRule]
}

function resetEditForm() {
  editing.id = null
  editing.leadName = ''
  editing.contactName = ''
  editing.phone = ''
  editing.source = ''
  editing.remark = ''
}

function handleCreate() {
  resetEditForm()
  editVisible.value = true
}

function handleEdit(row) {
  Object.assign(editing, row)
  editVisible.value = true
}

async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await updateLead(editing)
      ElMessage.success('已更新')
    } else {
      await addLead(editing)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

/**
 * P17:线索删除按钮始终显示
 * <p>已转客户(status=3)或死线索(status=4)可删除;其他状态(status=1 未跟进 / 2 跟进中)灰化禁用。</p>
 */
function canDeleteLead(row) {
  return row.status === 3 || row.status === 4
}
function deleteDisabledReason(row) {
  if (row.status === 1) return '未跟进的线索不可删除,请先跟进或转客户'
  if (row.status === 2) return '跟进中的线索不可删除,请先转客户或标记死线索'
  return ''
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除线索「${row.leadName}」？此操作不可恢复。`, '提示', { type: 'warning', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' })
    await deleteLead(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) { /* 取消 */ }
}

// ---------- 跳详情(独立详情页) ----------
function goDetail(row) {
  if (!row?.id) return
  router.push(`/lead/${row.id}`)
}

// ---------- 导入 / 导出(EasyExcel) ----------
const importDialogVisible = ref(false)
const importFile = ref(null)
const importLoading = ref(false)
const importResult = ref(null)

function openImportDialog() {
  importFile.value = null
  importResult.value = null
  importDialogVisible.value = true
}

function onImportFileChange(file) {
  importFile.value = file?.raw || null
}

async function doImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选 xlsx 文件')
    return
  }
  importLoading.value = true
  try {
    const { data } = await importLeadExcel(importFile.value)
    importResult.value = data
    ElMessage.success(`导入完成: 成功 ${data.successRows} / 失败 ${data.failRows}`)
    loadList()
  } finally {
    importLoading.value = false
  }
}

async function handleExport() {
  ElMessage.info('正在生成 Excel,文件下载即将开始...')
  try {
    const blob = await exportLeadExcel()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `线索列表_${Date.now()}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// ---------- 转客户 ----------
const convertVisible = ref(false)
const converting_ = ref(false)
const convertFormRef = ref(null)
const converting = ref({})
const convertForm = reactive({
  customerName: '',
  industry: '',
  level: 'B',
  post: '',
  phone: '',
  decisionWeight: 1
})
const convertRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [phoneRule]
}

function handleConvert(row) {
  converting.value = row
  Object.assign(convertForm, {
    customerName: row.leadName + '有限公司',
    industry: '',
    level: 'B',
    post: '',
    phone: row.phone || '',
    decisionWeight: 1
  })
  convertVisible.value = true
}

async function handleConvertSubmit() {
  await convertFormRef.value.validate()
  converting_.value = true
  try {
    const res = await convertLead(converting.value.id, convertForm)
    ElMessage.success(`已转客户，新客户 ID = ${res.data}`)
    convertVisible.value = false
    loadList()
  } finally {
    converting_.value = false
  }
}

onMounted(() => {
  loadList()
  loadSidePanel()
})
</script>

<style lang="scss" scoped>
.page {
  padding: 32px 32px 48px;
}

.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--ink);
}
.page-sub {
  margin-top: 4px;
  font-size: 13.5px;
  color: var(--muted);
}

/* 两栏布局 */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 1280px) {
  .layout { grid-template-columns: 1fr; }
}
.layout-main { min-width: 0; }
.layout-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 20px;
}
@media (max-width: 1280px) { .layout-side { position: static; } }

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.search { width: 320px; }
.filter { width: 140px; }
.filter.sort { width: 130px; }
.spacer { flex: 1; }

/* 表格 */
.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name { font-weight: 500; color: var(--ink); }
.mono {
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
  color: var(--ink-soft);
}
.text-muted { color: var(--subtle); }
.dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: 1px;
}
.pagination {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
}

/* 右侧 panel */
.side-panel {
  background: var(--bg-warm);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 18px;
}
.side-panel-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.more {
  font-size: 11.5px;
  color: var(--muted);
  font-weight: normal;
  cursor: pointer;
  &:hover { color: var(--accent); }
}

/* todo list */
.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--hairline-soft);
  &:last-child { border-bottom: none; padding-bottom: 0; }
  &:first-child { padding-top: 0; }
}
.todo-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.todo-content { flex: 1; min-width: 0; }
.todo-title { font-size: 12.5px; line-height: 1.4; }
.todo-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; }

/* stat grid */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.stat {
  padding: 10px 12px;
  background: var(--bg);
  border-radius: var(--radius);
}
.stat-label { font-size: 11.5px; color: var(--muted); margin-bottom: 4px; }
.stat-value {
  font-size: 20px;
  font-weight: 600;
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
  letter-spacing: -0.01em;
}
.stat.accent .stat-value { color: var(--accent); }
.stat.info .stat-value { color: var(--info); }
.stat.danger .stat-value { color: var(--danger); }

/* quick list */
.quick-list { display: flex; flex-direction: column; }
.quick-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 12.5px;
  color: var(--ink-soft);
  cursor: pointer;
  border-bottom: 1px solid var(--hairline-soft);
  &:last-child { border-bottom: none; }
  &:hover { color: var(--accent); }
  .count {
    font-size: 11.5px;
    color: var(--muted);
    font-family: var(--font-mono);
    font-feature-settings: 'tnum' 1;
  }
  &:hover .count { color: var(--accent); }
}

/* 转客户 banner */
.convert-banner {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: var(--warn-soft);
  border-radius: var(--radius);
  margin-bottom: 16px;
  font-size: 12.5px;
  color: var(--warn);
  line-height: 1.5;
  .el-icon { flex-shrink: 0; margin-top: 1px; }
}

.form-hint {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 4px;
}
</style>
