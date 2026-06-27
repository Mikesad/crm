<template>
  <div class="page">
    <div class="breadcrumb">
      <a @click="$router.push('/contract/list')">合同列表</a>
      <span> / </span>
      <span class="current">{{ contract?.contractNum || '加载中...' }}</span>
    </div>

    <template v-if="contract">
      <!-- 详情头 -->
      <div class="detail-header">
        <div style="flex: 1; min-width: 0;">
          <div class="title-row">
            <h2>{{ contract.contractName }}</h2>
            <span v-if="contract.status === 0" class="zen-status warn">审批中</span>
            <span v-else-if="contract.status === 1" class="zen-status blue">执行中</span>
            <span v-else-if="contract.status === 2" class="zen-status ok">已结束</span>
            <span v-else class="zen-status gray">已作废</span>
          </div>
          <div class="mono text-muted micro">{{ contract.contractNum }}</div>
        </div>
        <div class="amount-block">
          <div class="text-muted micro">合同总金额</div>
          <div class="amount-value">¥ {{ Number(contract.totalAmount).toLocaleString() }}</div>
        </div>
      </div>

      <!-- 汇总卡 -->
      <div class="summary">
        <div class="item highlight">
          <div class="label">合同总额</div>
          <div class="value">¥ {{ Number(contract.totalAmount).toLocaleString() }}</div>
        </div>
        <div class="item">
          <div class="label">已实收</div>
          <div class="value">¥ {{ Number(actualReceived).toLocaleString() }}</div>
          <div class="sub">回款记录 {{ receivables.length }} 笔</div>
        </div>
        <div class="item">
          <div class="label">待回款</div>
          <div class="value">¥ {{ Number(contract.totalAmount - actualReceived).toLocaleString() }}</div>
          <div class="sub">未收比例 {{ ((1 - actualReceived/contract.totalAmount) * 100).toFixed(1) }}%</div>
        </div>
        <div class="item">
          <div class="label">回款完成度</div>
          <div class="value">{{ Math.round(actualReceived/contract.totalAmount*100) }}%</div>
          <el-progress :percentage="Math.round(actualReceived/contract.totalAmount*100)" :stroke-width="6" :show-text="false" style="margin-top: 8px;" />
        </div>
      </div>

      <!-- Section 1: 商品明细 -->
      <el-card class="section">
        <div class="section-header">
          <span class="section-title">📦 商品明细 · {{ contract.items?.length || 0 }} 项</span>
          <span class="text-muted micro">
            小计合计 ¥ {{ itemsSubtotal.toLocaleString() }}
          </span>
        </div>
        <el-table :data="contract.items || []" :border="false">
          <el-table-column prop="productName" label="产品" min-width="220" />
          <el-table-column prop="spec" label="规格" width="160" />
          <el-table-column prop="unit" label="单位" width="80" align="center" />
          <el-table-column prop="count" label="数量" width="80" align="center" />
          <el-table-column label="标准单价" width="120" align="right">
            <template #default="{ row }"><span class="text-muted">¥ {{ Number(row.standardPrice).toLocaleString() }}</span></template>
          </el-table-column>
          <el-table-column label="折扣" width="90" align="center">
            <template #default="{ row }">
              <span :style="{color: row.discount < 8.5 ? '#ef4444' : 'inherit', fontWeight: row.discount < 8.5 ? 600 : 400}">
                {{ Number(row.discount).toFixed(1) }} 折
              </span>
            </template>
          </el-table-column>
          <el-table-column label="实际单价" width="110" align="right">
            <template #default="{ row }"><b>¥ {{ Number(row.salesPrice).toLocaleString() }}</b></template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template #default="{ row }"><b style="color: var(--accent);">¥ {{ (row.salesPrice * row.count).toLocaleString() }}</b></template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Section 2: 回款计划 -->
      <el-card class="section">
        <div class="section-header">
          <span class="section-title">📅 回款计划 · {{ plans.length }} 期</span>
          <el-button v-if="canAddPlan" class="btn-zen-primary" size="small" @click="openAddPlan">+ 新增回款计划</el-button>
        </div>
        <div v-if="!plans.length" class="empty">暂无回款计划</div>
        <div v-else>
          <div v-for="p in plans" :key="p.id" class="plan-step" :class="planClass(p)">
            <div class="dot">{{ p.period }}</div>
            <div class="body">
              <div class="title">第 {{ p.period }} 期 · {{ p.remark || '—' }}</div>
              <div class="sub">预计回款日: {{ p.expectedDate }} · 预计金额 ¥ {{ Number(p.expectedAmount).toLocaleString() }}</div>
              <div class="progress-row">
                <el-progress :percentage="Math.min(100, Math.round((p.receivedAmount || 0) / p.expectedAmount * 100))" :stroke-width="5" :show-text="false" style="flex: 1; max-width: 240px;" />
                <span class="text-muted micro">{{ Math.round((p.receivedAmount || 0) / p.expectedAmount * 100) }}%</span>
              </div>
            </div>
            <div class="right">
              <div class="amount">¥ {{ Number(p.receivedAmount || 0).toLocaleString() }} / {{ Number(p.expectedAmount).toLocaleString() }}</div>
              <div style="margin-top: 4px;">
                <span v-if="p.status === 0" class="zen-status gray">未到期</span>
                <span v-else-if="p.status === 1" class="zen-status warn">催款中</span>
                <span v-else class="zen-status ok">已回款</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- Section 3: 回款记录 -->
      <el-card class="section">
        <div class="section-header">
          <span class="section-title">💰 回款记录 · {{ receivables.length }} 笔</span>
          <el-button v-if="canAddReceivable" class="btn-zen-primary" size="small" @click="openAddReceivable">+ 录入回款</el-button>
        </div>
        <el-table :data="receivables" :border="false" v-if="receivables.length">
          <el-table-column prop="receivableNum" label="回款编号" width="180">
            <template #default="{ row }"><span class="mono accent">{{ row.receivableNum }}</span></template>
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
        </el-table>
        <div v-else class="empty">暂无回款记录</div>
      </el-card>
    </template>

    <div v-else v-loading="true" class="loading-area"></div>

    <!-- 新增回款计划弹窗 -->
    <el-dialog v-model="addPlanVisible" title="新增回款计划" width="500px" @closed="resetAddPlan">
      <el-form :model="newPlan" label-width="100px">
        <el-form-item label="期数">
          <el-input-number v-model="newPlan.period" :min="1" />
        </el-form-item>
        <el-form-item label="预计金额">
          <el-input-number v-model="newPlan.expectedAmount" :min="0.01" :precision="2" :step="1000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预计日期">
          <el-date-picker v-model="newPlan.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="newPlan.remark" placeholder="如: 首款 40%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addPlanVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="confirmAddPlan">确认</el-button>
      </template>
    </el-dialog>

    <!-- 录入回款弹窗 -->
    <el-dialog v-model="addRecvVisible" title="录入回款" width="500px" @closed="resetAddRecv">
      <el-form :model="newRecv" label-width="100px">
        <el-form-item label="对应计划">
          <el-select v-model="newRecv.planId" placeholder="可选,留空为计划外" clearable style="width: 100%;">
            <el-option v-for="p in plans.filter(p => p.status !== 2)" :key="p.id"
                       :label="`第 ${p.period} 期 · ¥ ${Number(p.expectedAmount).toLocaleString()}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回款金额">
          <el-input-number v-model="newRecv.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="回款日期">
          <el-date-picker v-model="newRecv.returnDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="newRecv.method" style="width: 100%;">
            <el-option label="银行转账" value="银行转账" />
            <el-option label="微信" value="微信" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="现金" value="现金" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="planMatchHint" class="info-banner">
        <span>ℹ️</span>
        <div>{{ planMatchHint }}</div>
      </div>
      <template #footer>
        <el-button @click="addRecvVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="confirmAddRecv">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getContract } from '@/api/contract'
import { listReceivablePlan, createReceivablePlanBatch } from '@/api/receivable-plan'
import { pageReceivable } from '@/api/receivable'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'ContractDetail' })

const route = useRoute()
const { hasPerm, isSales, isFinance } = useAuth()

const contract = ref(null)
const plans = ref([])
const receivables = ref([])
const actualReceived = computed(() => receivables.value.reduce((s, r) => s + Number(r.actualAmount || 0), 0))
const itemsSubtotal = computed(() => (contract.value?.items || []).reduce((s, r) => s + Number(r.salesPrice) * Number(r.count), 0))

const canAddPlan = computed(() => isSales.value && hasPerm('crm:receivable_plan:edit') && contract.value?.status === 1)
const canAddReceivable = computed(() => (isFinance.value || hasPerm('crm:receivable:edit')) && contract.value?.status === 1)

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  const [c, p, r] = await Promise.all([
    getContract(id),
    listReceivablePlan({ contractId: id }),
    pageReceivable({ contractId: id, pageNum: 1, pageSize: 100 })
  ])
  contract.value = c.data
  plans.value = p.data || []
  receivables.value = r.data?.records || []
}

function planClass(p) {
  if (p.status === 2) return 'done'
  if (p.status === 1) return 'urgent'
  return 'pending'
}

// ---- 新增回款计划 ----
const addPlanVisible = ref(false)
const saving = ref(false)
const newPlan = reactive({ period: 1, expectedAmount: 0, expectedDate: '', remark: '' })
function openAddPlan() {
  newPlan.period = plans.value.length + 1
  newPlan.expectedDate = dayjs().add(30, 'day').format('YYYY-MM-DD')
  addPlanVisible.value = true
}
function resetAddPlan() { newPlan.period = 1; newPlan.expectedAmount = 0; newPlan.expectedDate = ''; newPlan.remark = '' }
async function confirmAddPlan() {
  if (!newPlan.expectedAmount || !newPlan.expectedDate) {
    ElMessage.warning('请填写预计金额和日期')
    return
  }
  saving.value = true
  try {
    await createReceivablePlanBatch({ contractId: route.params.id, plans: [{ ...newPlan }] })
    ElMessage.success('已添加')
    addPlanVisible.value = false
    await loadDetail()
  } finally { saving.value = false }
}

// ---- 录入回款 ----
const addRecvVisible = ref(false)
const newRecv = reactive({ planId: null, amount: 0, returnDate: '', method: '银行转账' })
function openAddReceivable() {
  newRecv.returnDate = dayjs().format('YYYY-MM-DD')
  addRecvVisible.value = true
}
function resetAddRecv() { newRecv.planId = null; newRecv.amount = 0; newRecv.returnDate = ''; newRecv.method = '银行转账' }

const planMatchHint = computed(() => {
  if (!newRecv.planId) return null
  const p = plans.value.find(x => x.id === newRecv.planId)
  if (!p) return null
  const cur = Number(p.receivedAmount || 0)
  const next = cur + Number(newRecv.amount || 0)
  const willComplete = next >= p.expectedAmount
  return `本次录入后,该计划累计实收 ¥ ${next.toLocaleString()} / ¥ ${Number(p.expectedAmount).toLocaleString()}` + (willComplete ? ` · 提交后该计划将自动置为"已回款"` : '')
})

async function confirmAddRecv() {
  if (!newRecv.amount || !newRecv.returnDate) {
    ElMessage.warning('请填写金额和日期')
    return
  }
  saving.value = true
  try {
    const { createReceivable } = await import('@/api/receivable')
    await createReceivable({
      contractId: Number(route.params.id),
      planId: newRecv.planId || null,
      actualAmount: newRecv.amount,
      returnDate: newRecv.returnDate,
      paymentMethod: newRecv.method
    })
    ElMessage.success('回款已录入,Spring Event 已触发联动')
    addRecvVisible.value = false
    // 等监听器跑完再 reload
    setTimeout(loadDetail, 1000)
  } finally { saving.value = false }
}

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.page { padding: 24px 32px 48px; max-width: 1280px; margin: 0 auto; }
.breadcrumb { font-size: 12.5px; color: var(--muted); margin-bottom: 8px; }
.breadcrumb a { color: var(--muted); text-decoration: none; cursor: pointer; }
.breadcrumb a:hover { color: var(--accent); }
.breadcrumb .current { color: var(--ink); }
.loading-area { height: 240px; }

.detail-header {
  background: #fff; border-radius: var(--radius);
  border: 1px solid var(--hairline);
  padding: 20px 24px; margin-bottom: 16px;
  display: flex; justify-content: space-between; align-items: flex-start;
}
.title-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.title-row h2 { font-size: 18px; font-weight: 600; margin: 0; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.text-muted { color: var(--subtle); }
.text-muted.micro { font-size: 12px; }
.amount-block { text-align: right; }
.amount-block .micro { margin-bottom: 4px; }
.amount-value { font-size: 24px; font-weight: 700; color: var(--accent); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

.summary {
  background: #fff; border-radius: var(--radius);
  border: 1px solid var(--hairline);
  padding: 16px 20px; margin-bottom: 16px;
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
.summary .item .label { font-size: 12px; color: var(--subtle); margin-bottom: 6px; }
.summary .item .value { font-size: 20px; font-weight: 600; font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.summary .item .sub { font-size: 11px; color: var(--subtle); margin-top: 2px; }
.summary .item.highlight .value { color: var(--accent); }

.section { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; }
.empty { text-align: center; padding: 32px; color: var(--muted); font-size: 13px; }

/* 卡片时间轴 (与 phase3-preview.html 风格一致) */
.plan-step {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border: 1px solid var(--hairline);
  border-radius: var(--radius); background: #fff; margin-bottom: 8px;
}
.plan-step .dot {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; color: #fff;
}
.plan-step.pending .dot { background: #d1d5db; color: #6b7280; }
.plan-step.urgent .dot { background: #f59e0b; }
.plan-step.done .dot { background: #10b981; }
.plan-step .body { flex: 1; }
.plan-step .body .title { font-size: 14px; font-weight: 500; }
.plan-step .body .sub { font-size: 12px; color: var(--muted); margin-top: 2px; }
.plan-step .body .progress-row { margin-top: 6px; display: flex; align-items: center; gap: 8px; }
.plan-step .right { text-align: right; min-width: 180px; }
.plan-step .right .amount { font-size: 16px; font-weight: 600; color: var(--accent); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

/* status badges */
.zen-status { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.zen-status::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; }
.zen-status.gray { background: #f3f4f6; color: #6b7280; } .zen-status.gray::before { background: #9ca3af; }
.zen-status.warn { background: #fef3c7; color: #92400e; } .zen-status.warn::before { background: #f59e0b; }
.zen-status.ok { background: #d1fae5; color: #065f46; } .zen-status.ok::before { background: #10b981; }
.zen-status.blue { background: #dbeafe; color: #1e40af; } .zen-status.blue::before { background: #3b82f6; }

.info-banner {
  background: #f0fdf4; border: 1px solid #86efac; border-radius: var(--radius);
  padding: 12px 16px; margin-top: 12px;
  display: flex; gap: 10px; align-items: center; font-size: 13px; color: var(--accent);
}
</style>
