<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">商机管理</div>
        <div class="page-sub">共 {{ total }} 个商机 · 商机总金额 ¥ {{ formatAmount(totalAmount) }}</div>
      </div>
      <div>
        <el-button :icon="Plus" @click="handleCreate">新建商机</el-button>
      </div>
    </div>

    <div class="layout">
      <div class="layout-main">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="搜索商机名称" class="search" clearable @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.stage" placeholder="全部阶段" class="filter" clearable @change="handleSearch">
            <el-option v-for="s in STAGES" :key="s" :label="s" :value="s" />
          </el-select>
          <el-input v-model.number="query.customerId" placeholder="客户 ID（可选）" class="filter-sm" clearable @keyup.enter="handleSearch" />
          <div class="spacer" />
          <el-button :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
        </div>

        <el-card class="table-card" v-loading="loading">
          <el-table :data="list" stripe @row-dblclick="handleRowDblClick">
            <el-table-column prop="businessName" label="商机名称" min-width="180">
              <template #default="{ row }">
                <span class="name">{{ row.businessName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="所属客户" min-width="140">
              <template #default="{ row }">
                <a class="customer-link" @click="goCustomer(row.customerId)">{{ row.customerName || '#' + row.customerId }}</a>
              </template>
            </el-table-column>
            <el-table-column label="阶段" width="130">
              <template #default="{ row }">
                <el-tag :type="stageTagType(row.stage)" effect="light" size="small">{{ row.stage }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预计金额" width="140" align="right">
              <template #default="{ row }">
                <span class="amount">¥ {{ formatAmount(row.expectedAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="预计结单" width="110">
              <template #default="{ row }">
                <span class="mono">{{ row.expectedDealDate || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="负责人" width="90" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link class="action-link" @click="handleStage(row)" :disabled="isTerminalStage(row.stage)">推进阶段</el-button>
                <el-button link class="action-link" @click="handleEdit(row)">编辑</el-button>
                <el-button link class="action-link danger" @click="handleDelete(row)">删除</el-button>
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

      <aside class="layout-side">
        <div class="side-panel">
          <div class="side-panel-title">商机漏斗</div>
          <div class="funnel-stage" v-for="(s, i) in STAGES.filter(x => x !== '输单')" :key="s">
            <div class="funnel-name">{{ s }}</div>
            <div class="funnel-track">
              <div class="funnel-fill" :style="{ width: funnelPercent(i) + '%', background: stageColor(s) }"></div>
            </div>
            <div class="funnel-num">{{ funnelCount(s) }}</div>
          </div>
        </div>
        <div class="side-panel">
          <div class="side-panel-title">阶段金额</div>
          <div v-for="s in STAGES" :key="s" class="amount-row">
            <span class="label">{{ s }}</span>
            <span class="value mono">¥ {{ formatAmount(stageAmount(s)) }}</span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editing.id ? '编辑商机' : '新建商机'" width="480px" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top">
        <el-form-item label="所属客户" prop="customerId">
          <el-input v-model.number="editing.customerId" placeholder="客户 ID（请从客户列表点击「新建商机」自动带入）" />
          <div class="form-hint">阶段二先用输入 ID 方式，阶段三加客户选择器</div>
        </el-form-item>
        <el-form-item label="商机名称" prop="businessName">
          <el-input v-model="editing.businessName" placeholder="如：全集团 CRM 部署" />
        </el-form-item>
        <el-form-item label="预计金额">
          <el-input v-model="editing.expectedAmount" placeholder="如：2400000.00" />
        </el-form-item>
        <el-form-item label="预计结单日期">
          <el-date-picker v-model="editing.expectedDealDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 阶段推进弹窗 -->
    <el-dialog v-model="stageVisible" :title="`推进商机阶段 - ${staging?.businessName || ''}`" width="460px">
      <div class="stage-current">当前阶段：<el-tag :type="stageTagType(staging?.stage)" effect="light">{{ staging?.stage }}</el-tag></div>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-position="top" style="margin-top: 16px;">
        <el-form-item label="目标阶段" prop="stage">
          <el-select v-model="stageForm.stage" placeholder="选择目标阶段" style="width: 100%">
            <el-option v-for="s in nextStageOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="stageForm.followContent" type="textarea" :rows="3" placeholder="说明本次推进的背景与客户反馈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="savingStage" @click="handleSaveStage">确认推进</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { pageBusiness, addBusiness, updateBusiness, deleteBusiness, updateBusinessStage } from '@/api/business'

defineOptions({ name: 'BusinessList' })

const route = useRoute()
const router = useRouter()

const STAGES = ['需求分析', '方案报价', '商务谈判', '赢单', '输单']
const stageColor = (s) => ({ '需求分析': '#a1a1aa', '方案报价': '#b45309', '商务谈判': '#1e40af', '赢单': '#166534', '输单': '#b91c1c' }[s] || '#a1a1aa')
const stageTagType = (s) => ({ '需求分析': 'info', '方案报价': 'warning', '商务谈判': 'primary', '赢单': 'success', '输单': 'danger' }[s] || 'info')
const isTerminalStage = (s) => s === '赢单' || s === '输单'
const formatAmount = (a) => {
  if (a == null) return '0'
  return Number(a).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

const query = reactive({ keyword: '', customerId: null, stage: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const totalAmount = computed(() => list.value.reduce((s, b) => s + (Number(b.expectedAmount) || 0), 0))
const funnelCount = (s) => list.value.filter(b => b.stage === s).length
const funnelPercent = (i) => {
  const max = Math.max(1, ...STAGES.filter(s => s !== '输单').map(s => funnelCount(s)))
  return Math.round((funnelCount(STAGES.filter(s => s !== '输单')[i]) / max) * 100)
}
const stageAmount = (s) => list.value.filter(b => b.stage === s).reduce((sum, b) => sum + (Number(b.expectedAmount) || 0), 0)

async function loadList() {
  loading.value = true
  try {
    const res = await pageBusiness({
      keyword: query.keyword || undefined,
      customerId: query.customerId || undefined,
      stage: query.stage || undefined,
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

function handleSearch() { query.pageNum = 1; loadList() }
function handleExport() { ElMessage.info('导出商机 - 阶段二导出当前筛选结果') }
function handleRowDblClick(row) { handleStage(row) }

function goCustomer(id) { router.push(`/customer/${id}`) }

// ---------- 新建/编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, customerId: null, businessName: '', expectedAmount: '', expectedDealDate: '' })
const editRules = {
  customerId: [{ required: true, type: 'number', message: '请输入客户 ID', trigger: 'blur' }],
  businessName: [{ required: true, message: '请输入商机名称', trigger: 'blur' }]
}

function resetEditForm() {
  editing.id = null
  editing.customerId = null
  editing.businessName = ''
  editing.expectedAmount = ''
  editing.expectedDealDate = ''
}

function handleCreate() {
  resetEditForm()
  // 如果从客户列表跳转过来，URL 上带了 customerId
  if (route.query.customerId) editing.customerId = Number(route.query.customerId)
  editVisible.value = true
}

function handleEdit(row) {
  Object.assign(editing, row)
  editing.expectedAmount = row.expectedAmount ? String(row.expectedAmount) : ''
  editVisible.value = true
}

async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await updateBusiness(editing)
      ElMessage.success('已更新')
    } else {
      await addBusiness(editing)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除商机「${row.businessName}」？`, '提示', { type: 'warning' })
    await deleteBusiness(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {}
}

// ---------- 阶段推进 ----------
const stageVisible = ref(false)
const stageFormRef = ref(null)
const savingStage = ref(false)
const staging = ref(null)
const stageForm = reactive({ stage: '', followContent: '' })
const stageRules = { stage: [{ required: true, message: '请选择目标阶段', trigger: 'change' }] }

const nextStageOptions = computed(() => {
  if (!staging.value) return STAGES
  const cur = staging.value.stage
  if (cur === '赢单' || cur === '输单') return []
  const idx = STAGES.indexOf(cur)
  const result = []
  if (idx >= 0 && idx < 3) result.push(STAGES[idx + 1])
  result.push('输单')
  return result
})

function handleStage(row) {
  staging.value = row
  stageForm.stage = ''
  stageForm.followContent = ''
  stageVisible.value = true
}

async function handleSaveStage() {
  await stageFormRef.value.validate()
  savingStage.value = true
  try {
    await updateBusinessStage(staging.value.id, stageForm)
    ElMessage.success('阶段已推进')
    stageVisible.value = false
    loadList()
  } finally {
    savingStage.value = false
  }
}

onMounted(() => {
  // 支持从客户详情跳转过来时预填 customerId
  if (route.query.customerId) query.customerId = Number(route.query.customerId)
  loadList()
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

.layout { display: grid; grid-template-columns: 1fr 300px; gap: 20px; align-items: start; }
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }
.layout-main { min-width: 0; }
.layout-side { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 20px; }
@media (max-width: 1280px) { .layout-side { position: static; } }

.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 240px; }
.filter { width: 140px; }
.filter-sm { width: 160px; }
.spacer { flex: 1; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name { font-weight: 500; color: var(--ink); }
.customer-link { color: var(--accent); cursor: pointer; }
.customer-link:hover { text-decoration: underline; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; color: var(--ink-soft); }
.amount { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; font-weight: 500; color: var(--ink); }
.dot { display: inline-block; width: 5px; height: 5px; border-radius: 50%; margin-right: 4px; vertical-align: 1px; }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

/* side panel */
.side-panel { background: var(--bg-warm); border: 1px solid var(--hairline); border-radius: var(--radius); padding: 16px 18px; }
.side-panel-title { font-size: 12.5px; font-weight: 600; color: var(--ink); margin-bottom: 12px; }

.funnel-stage { display: grid; grid-template-columns: 70px 1fr 32px; align-items: center; gap: 12px; margin-bottom: 10px; font-size: 12.5px; }
.funnel-stage:last-child { margin-bottom: 0; }
.funnel-name { color: var(--muted); }
.funnel-track { height: 8px; background: var(--hairline-soft); border-radius: 4px; position: relative; overflow: hidden; }
.funnel-fill { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 4px; transition: width 0.3s; }
.funnel-num { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; text-align: right; font-weight: 500; }

.amount-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; }
.amount-row .label { flex: 1; color: var(--muted); }
.amount-row .value { font-weight: 500; color: var(--ink); }
.amount-row .dot { width: 6px; height: 6px; }

.stage-current { font-size: 13.5px; color: var(--ink-soft); }
.form-hint { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
</style>
