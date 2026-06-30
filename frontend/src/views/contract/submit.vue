<template>
  <div class="page">
    <div class="breadcrumb">
      <a @click="$router.push('/contract/list')">合同列表</a>
      <span> / </span>
      <span class="current">新建合同</span>
    </div>

    <!-- P12:借鉴 detail.vue 的 header-grid 4 列布局,实时联动表单输入 -->
    <div class="detail-header">
      <div class="header-grid">
        <!-- 合同名称(主) -->
        <div class="header-cell primary">
          <div class="cell-label">合同名称</div>
          <div class="cell-value-primary">{{ form.contractName || '— 待填写 —' }}</div>
          <div class="mono text-muted cell-sub">{{ form.contractNum || '留空将自动生成 HT-YYYYMMDD-XXXX' }}</div>
        </div>
        <!-- 客户 -->
        <div class="header-cell">
          <div class="cell-label">客户</div>
          <div class="cell-value">
            <svg class="cell-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M3 21V9l9-6 9 6v12"/>
              <path d="M9 21V12h6v9"/>
            </svg>
            {{ selectedCustomerName || '— 待选择 —' }}
          </div>
        </div>
        <!-- 商机 -->
        <div class="header-cell">
          <div class="cell-label">商机</div>
          <div class="cell-value">
            <svg class="cell-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M3 17l6-6 4 4 8-8"/>
              <path d="M14 7h7v7"/>
            </svg>
            {{ selectedBusinessName || '— 可选 —' }}
          </div>
        </div>
        <!-- 合同总金额 -->
        <div class="header-cell amount">
          <div class="cell-label">合同总金额</div>
          <div class="amount-value">¥ {{ totalAmount.toLocaleString() }}</div>
          <div v-if="minDiscount < 8.5 && hasItems" class="amount-sub warn">折扣 {{ minDiscount.toFixed(1) }} 折 · 低于审批线</div>
          <div v-else-if="hasItems" class="amount-sub ok">折扣合规</div>
          <div v-else class="amount-sub">—</div>
        </div>
      </div>
    </div>

    <!-- P12:借鉴 detail.vue 的 summary 4 列卡(新建时回款数据全 0) -->
    <div class="summary">
      <div class="item highlight">
        <div class="label">合同总额</div>
        <div class="value">¥ {{ totalAmount.toLocaleString() }}</div>
      </div>
      <div class="item">
        <div class="label">已实收</div>
        <div class="value">¥ 0</div>
        <div class="sub">回款记录 0 笔</div>
      </div>
      <div class="item">
        <div class="label">待回款</div>
        <div class="value">¥ {{ totalAmount.toLocaleString() }}</div>
        <div class="sub">未收比例 100%</div>
      </div>
      <div class="item">
        <div class="label">回款完成度</div>
        <div class="value">0%</div>
        <el-progress :percentage="0" :stroke-width="6" :show-text="false" style="margin-top: 8px;" />
      </div>
    </div>

    <!-- Section 1: 基础信息(P12:借鉴 detail.vue section 风格 + icon) -->
    <el-card class="section">
      <div class="section-header">
        <span class="section-title">
          <svg class="title-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="4" width="18" height="18" rx="1"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
          </svg>
          基础信息
        </span>
      </div>
      <el-form ref="baseFormRef" :model="form" :rules="baseRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="客户" prop="customerId">
            <el-select v-model="form.customerId" placeholder="搜索客户名称" filterable style="width: 100%;">
              <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="商机">
            <el-select v-model="form.businessId" placeholder="可选,从商机带出" clearable style="width: 100%;">
              <el-option v-for="b in businessOptions" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="合同名称" prop="contractName">
            <el-input v-model="form.contractName" placeholder="如: 蓝海科技 200 席位旗舰版" />
          </el-form-item>
          <el-form-item label="合同编号">
            <el-input v-model="form.contractNum" placeholder="留空将自动生成 HT-YYYYMMDD-XXXX" />
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <!-- Section 2: 商品明细(P12:借鉴 detail 风格 + icon) -->
    <el-card class="section">
      <div class="section-header">
        <span class="section-title">
          <svg class="title-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M3 7l9-4 9 4-9 4-9-4z"/>
            <path d="M3 7v10l9 4 9-4V7"/>
            <path d="M12 11v10"/>
          </svg>
          商品明细 · {{ form.items.filter(r => r.productId).length }} 项
        </span>
        <el-button text class="add-link" @click="addItem">+ 添加一行</el-button>
      </div>

      <el-table :data="form.items" class="items-table">
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="产品" min-width="240">
          <template #default="{ row, $index }">
            <el-select v-model="row.productId" placeholder="选择产品" filterable style="width: 100%;" @change="(v) => onProductChange($index, v)">
              <el-option v-for="p in productOptions" :key="p.id" :label="`${p.productName} (¥${Number(p.price).toLocaleString()})`" :value="p.id" />
            </el-select>
            <div v-if="row.productId" class="text-muted micro" style="margin-top: 4px;">{{ getProduct(row.productId).spec }} · {{ getProduct(row.productId).unit }}</div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.count" :min="1" :max="9999" controls-position="right" style="width: 100px;" @change="recalc" />
          </template>
        </el-table-column>
        <el-table-column label="折扣(折)" width="140" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.discount" :min="0.1" :max="10" :step="0.1" :precision="1" controls-position="right" style="width: 110px;" @change="recalc" />
          </template>
        </el-table-column>
        <el-table-column label="标准单价" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.productId" class="text-muted">¥ {{ Number(row.standardPrice || 0).toLocaleString() }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="实际单价" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.productId" class="final">¥ {{ computeSalesPrice(row).toLocaleString() }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="小计" width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.productId" class="final">¥ {{ computeSubtotal(row).toLocaleString() }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="" width="48" align="center">
          <template #default="{ $index }">
            <el-button circle size="small" type="danger" :icon="Close" @click="removeItem($index)" :disabled="form.items.length === 1" />
          </template>
        </el-table-column>
      </el-table>

      <div class="total-bar">
        <div>
          <div class="total-label">合同总金额 (按明细实时核算)</div>
          <div v-if="hasItems" class="text-muted micro" style="margin-top: 4px;">
            最低折扣: <b :style="{color: minDiscount < 8.5 ? '#ef4444' : 'inherit'}">{{ minDiscount.toFixed(1) }} 折</b>
            <span v-if="minDiscount < 8.5"> · 低于 8.5 折审批线</span>
          </div>
        </div>
        <div class="total-value">¥ {{ totalAmount.toLocaleString() }}</div>
      </div>

      <div v-if="minDiscount < 8.5 && hasItems" class="warn-banner">
        <span class="icon">⚠️</span>
        <div>
          <b>该合同最低折扣 {{ minDiscount.toFixed(1) }} 折,低于 8.5 折审批线</b><br>
          提交后将自动进入【销售总监】审批流程,<b>合同状态置为"审批中"</b>,审批通过后转为"执行中"。
        </div>
      </div>
      <div v-else-if="hasItems" class="info-banner">
        <span>✓</span>
        <div>折扣合规,提交后合同将直接进入【执行中】状态。</div>
      </div>
    </el-card>

    <!-- Section 3: 回款计划(P12:借鉴 detail 风格 + icon) -->
    <el-card class="section">
      <div class="section-header">
        <span class="section-title">
          <svg class="title-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="4" width="18" height="18" rx="1"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
          </svg>
          回款计划 <span class="text-muted micro" style="font-weight: normal;">(可选)</span>
        </span>
        <el-button text class="add-link" :disabled="minDiscount < 8.5" @click="addPlan">+ 添加分期</el-button>
      </div>

      <div v-if="minDiscount < 8.5" class="text-muted micro" style="margin-bottom: 12px;">
        ⚠ 折扣低于 8.5 折,合同将进入审批;审批通过后再在详情页录入回款计划
      </div>
      <div v-else class="text-muted micro" style="margin-bottom: 12px;">
        提交时同步创建;期数 period 不能重复,合计金额应等于合同总金额
      </div>

      <el-table v-if="form.plans.length" :data="form.plans" class="plans-table">
        <el-table-column label="期数" width="80" align="center">
          <template #default="{ row }">
            <span class="mono accent">第 {{ row.period }} 期</span>
          </template>
        </el-table-column>
        <el-table-column label="预计回款日" width="170">
          <template #default="{ row }">
            <el-date-picker v-model="row.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="金额" min-width="220" align="left">
          <template #header>
            <div class="col-header-stack">
              <span class="col-header-main">金额</span>
              <span class="col-header-sub">(元)</span>
            </div>
          </template>
          <template #default="{ row }">
            <el-input-number v-model="row.expectedAmount" :min="0.01" :precision="2" :step="1000" controls-position="right" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="如:首款 40%" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="" width="48" align="center">
          <template #default="{ $index }">
            <el-button circle size="small" type="danger" :icon="Close" @click="removePlan($index)" />
          </template>
        </el-table-column>
      </el-table>

      <div v-if="form.plans.length" class="plan-summary">
        <span class="text-muted micro">合计 {{ form.plans.length }} 期 · 预计 ¥ {{ plansTotal.toLocaleString() }}</span>
        <span v-if="Math.abs(plansTotal - totalAmount) > 0.01" class="warn-text micro">
          ⚠ 与合同总额差 ¥ {{ Math.abs(totalAmount - plansTotal).toLocaleString() }}
        </span>
        <span v-else class="ok-text micro">✓ 与合同总额一致</span>
      </div>
    </el-card>

    <div class="actions">
      <el-button @click="$router.push('/contract/list')">取消</el-button>
      <el-button class="btn-zen-primary" :loading="submitting" @click="handleSubmit">
        {{ minDiscount < 8.5 ? '提交至销售总监审批' : '提交合同' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { createContract } from '@/api/contract'
import { pageProduct } from '@/api/product'
import { pageCustomer } from '@/api/customer'
import { pageBusiness } from '@/api/business'
import { createReceivablePlanBatch } from '@/api/receivable-plan'

defineOptions({ name: 'ContractSubmit' })

const router = useRouter()

const form = reactive({
  contractName: '',
  customerId: null,
  businessId: null,
  contractNum: '',
  startDate: '',
  endDate: '',
  items: [{ productId: null, count: 1, discount: 10.0, standardPrice: 0 }],
  plans: []
})

const baseFormRef = ref(null)
const baseRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }]
}

const submitting = ref(false)
const productOptions = ref([])
const customerOptions = ref([])
const businessOptions = ref([])

/* P12:header-grid 实时联动显示 */
const selectedCustomerName = computed(() => {
  if (!form.customerId) return null
  const c = customerOptions.value.find(x => x.id === form.customerId)
  return c ? c.name : null
})
const selectedBusinessName = computed(() => {
  if (!form.businessId) return null
  const b = businessOptions.value.find(x => x.id === form.businessId)
  return b ? b.name : null
})

onMounted(async () => {
  await Promise.all([loadProducts(), loadCustomers(), loadBusinesses()])
})

async function loadProducts() {
  try {
    const res = await pageProduct({ pageNum: 1, pageSize: 200, status: 1 })
    productOptions.value = res.data?.records || []
  } catch (e) { productOptions.value = [] }
}
async function loadCustomers() {
  try {
    const res = await pageCustomer({ pageNum: 1, pageSize: 200 })
    customerOptions.value = (res.data?.records || []).map(c => ({ id: c.id, name: c.customerName }))
  } catch (e) { customerOptions.value = [] }
}
async function loadBusinesses() {
  try {
    const res = await pageBusiness({ pageNum: 1, pageSize: 200 })
    businessOptions.value = (res.data?.records || []).map(b => ({ id: b.id, name: b.businessName || b.name }))
  } catch (e) { businessOptions.value = [] }
}

const getProduct = (id) => productOptions.value.find(p => p.id === id) || {}
function onProductChange(idx, pid) {
  const p = getProduct(pid)
  form.items[idx].standardPrice = p.price || 0
  if (form.items[idx].discount == null) form.items[idx].discount = 10.0
  recalc()
}

function computeSalesPrice(row) {
  if (!row.productId || !row.standardPrice) return 0
  const p = Number(row.standardPrice)
  const d = Number(row.discount || 0)
  return Math.round(p * d * 10) / 100
}
function computeSubtotal(row) {
  const c = Number(row.count || 0)
  return Math.round(computeSalesPrice(row) * c * 100) / 100
}

const hasItems = computed(() => form.items.some(r => r.productId))
const totalAmount = computed(() => {
  const sum = form.items.reduce((s, r) => s + computeSubtotal(r), 0)
  return Math.round(sum * 100) / 100
})
const minDiscount = computed(() => {
  const ds = form.items.filter(r => r.productId).map(r => Number(r.discount || 10))
  if (!ds.length) return 10
  return Math.min(...ds)
})

function recalc() { /* 触发 computed */ }
function addItem() {
  form.items.push({ productId: null, count: 1, discount: 10.0, standardPrice: 0 })
}
function removeItem(idx) {
  form.items.splice(idx, 1)
}

function addPlan() {
  form.plans.push({
    period: form.plans.length + 1,
    expectedDate: dayjs().add(30 * (form.plans.length + 1), 'day').format('YYYY-MM-DD'),
    expectedAmount: 0,
    remark: ''
  })
}
function removePlan(idx) {
  form.plans.splice(idx, 1)
  form.plans.forEach((p, i) => { p.period = i + 1 })
}
const plansTotal = computed(() => {
  return Math.round(form.plans.reduce((s, p) => s + Number(p.expectedAmount || 0), 0) * 100) / 100
})

async function handleSubmit() {
  await baseFormRef.value.validate()
  if (!hasItems.value) {
    ElMessage.warning('请至少添加一项商品明细')
    return
  }
  submitting.value = true
  try {
    const payload = {
      contractName: form.contractName,
      customerId: form.customerId,
      businessId: form.businessId || null,
      contractNum: form.contractNum || null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      totalAmount: totalAmount.value,
      items: form.items.map(r => ({
        productId: r.productId, count: r.count, discount: r.discount
      }))
    }
    const res = await createContract(payload)
    const contractId = res.data

    if (form.plans.length > 0 && minDiscount.value >= 8.5) {
      try {
        await createReceivablePlanBatch({
          contractId,
          plans: form.plans.map(p => ({
            period: p.period,
            expectedAmount: p.expectedAmount,
            expectedDate: p.expectedDate,
            remark: p.remark || ''
          }))
        })
        ElMessage.success('合同已提交,回款计划已同步创建')
      } catch (e) {
        ElMessage.warning('合同已提交,但回款计划创建失败,请到详情页补录')
      }
    } else {
      ElMessage.success(minDiscount.value < 8.5 ? '合同已提交,等待销售总监审批' : '合同已提交,直接进入执行中')
    }

    router.push(`/contract/${contractId}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 24px 32px 48px; max-width: 1280px; margin: 0 auto; }
.breadcrumb { font-size: 12.5px; color: var(--muted); margin-bottom: 8px; }
.breadcrumb a { color: var(--muted); text-decoration: none; cursor: pointer; }
.breadcrumb a:hover { color: var(--accent); }
.breadcrumb .current { color: var(--ink); }

/* P12:借鉴 detail.vue 的 detail-header 风格 */
.detail-header {
  background: #fff; border-radius: var(--radius);
  border: 1px solid var(--hairline);
  padding: 20px 24px; margin-bottom: 16px;
}
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.text-muted { color: var(--subtle); }
.text-muted.micro { font-size: 12px; }

.header-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr 220px;
  gap: 18px;
  align-items: start;
}
.header-cell { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.header-cell.primary { padding-right: 16px; border-right: 1px solid var(--hairline-soft); }
.cell-label {
  font-size: 11.5px; color: var(--subtle);
  font-weight: 500; text-transform: uppercase; letter-spacing: 0.04em;
}
.cell-value {
  font-size: 15px; font-weight: 600; color: var(--ink);
  display: flex; align-items: center; gap: 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cell-value-primary {
  font-size: 18px; font-weight: 600; color: var(--ink);
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  line-height: 1.3;
}
.cell-sub { font-size: 12px; }
.cell-icon { color: var(--ink-soft); flex-shrink: 0; }
.header-cell.amount { text-align: right; align-items: flex-end; }
.header-cell.amount .amount-value {
  font-size: 24px; font-weight: 700; color: var(--accent);
  font-family: var(--font-mono); font-feature-settings: 'tnum' 1;
}
.amount-sub { font-size: 11.5px; margin-top: 4px; color: var(--subtle); }
.amount-sub.warn { color: var(--warn); font-weight: 500; }
.amount-sub.ok { color: var(--accent); font-weight: 500; }

@media (max-width: 960px) {
  .header-grid { grid-template-columns: 1fr 1fr; }
  .header-cell.primary { grid-column: span 2; border-right: none; padding-bottom: 14px; border-bottom: 1px solid var(--hairline-soft); }
  .header-cell.amount { text-align: left; align-items: flex-start; }
}

/* P12:借鉴 detail.vue 的 summary 卡片 */
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

/* P12:section 风格(借鉴 detail.vue) */
.section { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }
.title-icon { color: var(--ink); flex-shrink: 0; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.items-table { border-radius: 0; }
.items-table :deep(.el-input-number .el-input__inner) { text-align: center; }
.final { font-weight: 600; color: var(--ink); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

.total-bar {
  background: #f9fafb; border: 1px dashed var(--hairline);
  border-radius: var(--radius); padding: 14px 20px;
  display: flex; justify-content: space-between; align-items: center; margin-top: 16px;
}
.total-label { font-size: 13px; color: var(--muted); }
.total-value { font-size: 24px; font-weight: 700; color: var(--accent); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

.warn-banner {
  background: #fffbeb; border: 1px solid #fcd34d; border-radius: var(--radius);
  padding: 12px 16px; margin-top: 16px;
  display: flex; gap: 10px; align-items: flex-start; font-size: 13px; color: #92400e;
  .icon { font-size: 16px; flex-shrink: 0; }
  b { color: #78350f; }
}

.plans-table { border-radius: 0; }
.plans-table :deep(.el-input-number .el-input__inner) { text-align: center; }
.plans-table :deep(.cell) { padding: 6px 8px; }

/* 列 header 上下排版(用于"金额"列 + "(元)" 副标题) */
.col-header-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}
.col-header-main { font-weight: 600; }
.col-header-sub { font-size: 10.5px; color: var(--muted); font-weight: normal; }
.plan-summary {
  margin-top: 12px; padding: 8px 12px;
  background: #f9fafb; border-radius: var(--radius);
  display: flex; align-items: center; gap: 12px; font-size: 12.5px;
}
.warn-text { color: var(--warn); font-weight: 500; }
.ok-text { color: var(--accent); font-weight: 500; }
.info-banner {
  background: #f0fdf4; border: 1px solid #86efac; border-radius: var(--radius);
  padding: 12px 16px; margin-top: 16px;
  display: flex; gap: 10px; align-items: center; font-size: 13px; color: var(--accent);
}

.add-link { color: var(--accent); }

.actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
</style>