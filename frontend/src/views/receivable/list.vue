<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">回款管理</div>
        <div class="page-sub">录入实际回款 · 系统自动联动回款计划与合同状态</div>
      </div>
      <div v-if="hasPerm('crm:receivable:edit')">
        <el-button :icon="Plus" class="btn-zen-primary" @click="openAdd">录入回款</el-button>
      </div>
    </div>

    <!-- 阶段八 commit 11·2026-06-30:移除统计卡(本月回款笔数 / 本月合计 / 全部回款笔数 / 全部回款合计),
         统计口径在报表中心 Tab ④ 已覆盖,这里不再重复展示 -->

    <!-- 逾期计划提醒 -->
    <div v-if="overduePlans.length" class="warn-banner">
      <span class="icon">⚠️</span>
      <div>
        <b>有 {{ overduePlans.length }} 期回款计划已逾期</b>
        <ul class="overdue-list">
          <li v-for="p in overduePlans.slice(0, 5)" :key="`${p.contractId}-${p.id}`">
            <a @click="$router.push(`/contract/${p.contractId}`)">{{ p.contractName }}</a>
            · 第 {{ p.period }} 期 · ¥ {{ Number(p.expectedAmount).toLocaleString() }} · 预计 {{ p.expectedDate }}
          </li>
          <li v-if="overduePlans.length > 5" class="more">还有 {{ overduePlans.length - 5 }} 期...</li>
        </ul>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="query.contractId" placeholder="全部合同" filterable clearable class="filter" @change="handleSearch">
        <el-option v-for="c in contracts" :key="c.id" :label="`${c.contractNum} ${c.contractName}`" :value="c.id" />
      </el-select>
      <div style="width: 320px">
        <el-date-picker
          style="width: 100%"
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
      </div>
      <div class="spacer" />
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="list" stripe empty-text="暂无回款记录" style="width: 100%">
        <el-table-column prop="receivableNum" label="回款编号" width="160">
          <template #default="{ row }"><span class="mono accent">{{ row.receivableNum }}</span></template>
        </el-table-column>
        <el-table-column label="合同" min-width="240">
          <template #default="{ row }">
            <a class="accent link" @click="$router.push(`/contract/${row.contractId}`)">{{ row.contractNum }} {{ row.contractName }}</a>
          </template>
        </el-table-column>
        <el-table-column label="对应计划" width="120">
          <template #default="{ row }">
            <span v-if="row.planPeriod">第 {{ row.planPeriod }} 期</span>
            <span v-else class="text-muted">计划外</span>
          </template>
        </el-table-column>
        <el-table-column label="回款金额" width="140" align="right">
          <template #default="{ row }"><b>¥ {{ Number(row.actualAmount).toLocaleString() }}</b></template>
        </el-table-column>
        <el-table-column prop="returnDate" label="回款日期" width="120" />
        <el-table-column prop="paymentMethod" label="支付方式" width="100" />
        <el-table-column prop="createBy" label="录入人" width="100" />
        <el-table-column prop="createTime" label="录入时间" width="160">
          <template #default="{ row }"><span class="text-muted">{{ formatTime(row.createTime) }}</span></template>
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

    <!-- 录入回款弹窗 -->
    <el-dialog v-model="addVisible" title="录入回款" width="500px" @closed="resetAdd">
      <el-form :model="form" label-width="100px">
        <el-form-item label="合同">
          <el-select v-model="form.contractId" placeholder="选择合同(仅执行中)" filterable style="width: 100%;" @change="onContractChange">
            <el-option v-for="c in activeContracts" :key="c.id" :label="`${c.contractNum} ${c.contractName}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="对应计划">
          <el-select v-model="form.planId" placeholder="可选,留空为计划外" clearable :disabled="!form.contractId" style="width: 100%;">
            <el-option v-for="p in availablePlans" :key="p.id" :label="`第 ${p.period} 期 · ¥ ${Number(p.expectedAmount).toLocaleString()}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回款金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="回款日期">
          <el-date-picker v-model="form.returnDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="form.method" style="width: 100%;">
            <el-option label="银行转账" value="银行转账" />
            <el-option label="微信" value="微信" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="现金" value="现金" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="planHint" class="info-banner">
        <span>ℹ️</span>
        <div>{{ planHint }}</div>
      </div>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="confirmAdd">确认录入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { pageReceivable, createReceivable } from '@/api/receivable'
import { pageContract } from '@/api/contract'
import { listReceivablePlan } from '@/api/receivable-plan'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'ReceivableList' })

const { hasPerm } = useAuth()

const query = reactive({ contractId: null, returnDateStart: null, returnDateEnd: null, pageNum: 1, pageSize: 10 })
const dateRange = ref(null)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const contracts = ref([])
const activeContracts = ref([])  // status=1
const overduePlans = ref([])
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

// 阶段八 commit 11:移除 monthCount / monthTotal / allTotal 三个 computed(统计口径改走报表中心 Tab ④)

async function loadList() {
  loading.value = true
  try {
    const res = await pageReceivable({
      contractId: query.contractId || undefined,
      returnDateStart: query.returnDateStart || undefined,
      returnDateEnd: query.returnDateEnd || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []; total.value = 0
  } finally { loading.value = false }
}

async function loadContracts() {
  try {
    const res = await pageContract({ pageNum: 1, pageSize: 200 })
    const all = res.data?.records || []
    contracts.value = all
    activeContracts.value = all.filter(c => c.status === 1)
  } catch (e) { contracts.value = []; activeContracts.value = [] }
}

async function loadOverduePlans() {
  // 收集所有执行中合同的 plan
  try {
    const plansAll = []
    for (const c of activeContracts.value) {
      const res = await listReceivablePlan({ contractId: c.id })
      const list = res.data || []
      for (const p of list) {
        plansAll.push({ ...p, contractId: c.id, contractName: c.contractName, contractNum: c.contractNum })
      }
    }
    const today = dayjs().format('YYYY-MM-DD')
    overduePlans.value = plansAll.filter(p => p.status !== 2 && p.expectedDate && p.expectedDate < today)
  } catch (e) { overduePlans.value = [] }
}

function handleSearch() { query.pageNum = 1; loadList() }
function handleReset() { query.contractId = null; query.returnDateStart = null; query.returnDateEnd = null; dateRange.value = null; query.pageNum = 1; loadList() }
function handleDateChange(range) {
  if (range && range.length === 2) {
    query.returnDateStart = range[0]
    query.returnDateEnd = range[1]
  } else {
    query.returnDateStart = null
    query.returnDateEnd = null
  }
  query.pageNum = 1
  loadList()
}

// 录入回款
const addVisible = ref(false)
const saving = ref(false)
const form = reactive({ contractId: null, planId: null, amount: 0, returnDate: '', method: '银行转账' })
const availablePlans = ref([])

function resetAdd() {
  form.contractId = null
  form.planId = null
  form.amount = 0
  form.returnDate = ''
  form.method = '银行转账'
  availablePlans.value = []
}
function openAdd() {
  form.returnDate = dayjs().format('YYYY-MM-DD')
  addVisible.value = true
}
async function onContractChange(cid) {
  form.planId = null
  if (!cid) { availablePlans.value = []; return }
  const res = await listReceivablePlan({ contractId: cid })
  availablePlans.value = (res.data || []).filter(p => p.status !== 2)
}

const planHint = computed(() => {
  if (!form.planId) return null
  const p = availablePlans.value.find(x => x.id === form.planId)
  if (!p) return null
  const cur = Number(p.receivedAmount || 0)
  const next = cur + Number(form.amount || 0)
  const willComplete = next >= p.expectedAmount
  return `本次录入后,该计划累计实收 ¥ ${next.toLocaleString()} / ¥ ${Number(p.expectedAmount).toLocaleString()}` + (willComplete ? ` · 提交后该计划将自动置为"已回款"` : '')
})

async function confirmAdd() {
  if (!form.contractId) { ElMessage.warning('请选择合同'); return }
  if (!form.amount) { ElMessage.warning('请填写金额'); return }
  if (!form.returnDate) { ElMessage.warning('请填写日期'); return }
  saving.value = true
  try {
    await createReceivable({
      contractId: form.contractId,
      planId: form.planId || null,
      actualAmount: form.amount,
      returnDate: form.returnDate,
      paymentMethod: form.method
    })
    ElMessage.success('回款已录入,Spring Event 已触发联动')
    addVisible.value = false
    setTimeout(async () => {
      await loadList()
      await loadContracts()
      await loadOverduePlans()
    }, 1000)
  } finally { saving.value = false }
}

onMounted(async () => {
  await loadContracts()
  await loadList()
  await loadOverduePlans()
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

/* 阶段八 commit 11:.summary / .item CSS 已随模板移除 */

.warn-banner {
  background: #fffbeb; border: 1px solid #fcd34d; border-radius: var(--radius);
  padding: 12px 16px; margin-bottom: 16px;
  display: flex; gap: 10px; align-items: flex-start; font-size: 13px; color: #92400e;
  .icon { font-size: 16px; flex-shrink: 0; }
  b { color: #78350f; }
}
.overdue-list { margin: 6px 0 0; padding-left: 18px; }
.overdue-list li { font-size: 12.5px; }
.overdue-list li.more { color: var(--subtle); font-style: italic; list-style: none; margin-left: -18px; }
.overdue-list a { color: var(--accent); cursor: pointer; }
.overdue-list a:hover { text-decoration: underline; }

.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.filter { width: 260px; }
.spacer { flex: 1; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.accent { color: var(--accent); }
.accent.link { cursor: pointer; }
.accent.link:hover { text-decoration: underline; }
.text-muted { color: var(--subtle); }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

.info-banner {
  background: #f0fdf4; border: 1px solid #86efac; border-radius: var(--radius);
  padding: 12px 16px; margin-top: 8px;
  display: flex; gap: 10px; align-items: center; font-size: 13px; color: var(--accent);
}
</style>
