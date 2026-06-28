<template>
  <div class="page" v-loading="loading">
    <div v-if="business.id">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <a @click="goBusinessList">商机</a>
        <span class="sep">›</span>
        <a @click="goBusinessList">我的商机</a>
        <span class="sep">›</span>
        <span>{{ business.businessName }}</span>
      </div>

      <!-- Header card -->
      <div class="detail-header">
        <div class="detail-header-top">
          <div>
            <div class="detail-title">{{ business.businessName }}</div>
            <div class="detail-sub">
              客户:<a class="customer-link" @click="goCustomer(business.customerId)">{{ business.customerName }}</a>
              <span class="dot">·</span>
              商机编号 <span class="mono">BIZ-{{ String(business.id).padStart(4, '0') }}</span>
            </div>
          </div>
          <div class="detail-actions">
            <el-button :disabled="isTerminal || !canEdit" @click="openAdvance">推进阶段</el-button>
            <el-button v-if="canWin" type="success" plain :disabled="!canEdit" @click="quickWin">✓ 标记为赢单</el-button>
            <el-button v-if="canLose" type="danger" plain :disabled="!canEdit" @click="quickLose">判定输单</el-button>
          </div>
        </div>

        <!-- Meta -->
        <div class="meta-row">
          <div class="meta-item">
            <div class="meta-label">预计金额</div>
            <div class="meta-value is-amount mono">¥ {{ formatAmount(business.expectedAmount) }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">当前阶段</div>
            <div class="meta-value">
              <el-tag :type="stageTagType(business.stage)" effect="light" size="small">{{ business.stage }}</el-tag>
            </div>
          </div>
          <div class="meta-item">
            <div class="meta-label">预计成交日</div>
            <div class="meta-value mono">{{ business.expectedDealDate || '-' }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">赢率</div>
            <div class="meta-value">{{ winRate }}%</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">负责人</div>
            <div class="meta-value owner">
              <span class="owner-avatar">{{ ownerChar }}</span>
              <span>{{ business.ownerName || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 阶段流转(方案 A:4 段 step box + SVG 三角箭头,单行展示) -->
        <div class="stage-pipeline-row">
          <div class="meta-label inline">阶段流转</div>
          <div class="stage-pipeline">
            <template v-for="(s, idx) in pipelineStages" :key="s">
              <div
                class="stage-step"
                :class="{ done: isPast(s), current: s === business.stage }"
              >
                <span class="step-dot"></span>
                <span class="step-name">{{ s }}</span>
              </div>
              <span
                v-if="idx < pipelineStages.length - 1"
                class="stage-arrow"
                :class="{ done: isPast(pipelineStages[idx + 1]) || pipelineStages[idx + 1] === business.stage }"
                aria-hidden="true"
              >
                <svg width="14" height="12" viewBox="0 0 14 12"><path d="M0 6h10M6 2l4 4-4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </span>
            </template>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs-row">
        <div class="tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">基本信息</div>
        <div class="tab" :class="{ active: activeTab === 'record' }" @click="activeTab = 'record'">
          跟进记录 <span class="count">{{ recordCount }}</span>
        </div>
      </div>

      <!-- Layout -->
      <div class="layout">
        <!-- 主区:跟进时间轴 -->
        <div class="layout-main">
          <div v-if="activeTab === 'basic'" class="info-panel">
            <div class="info-grid">
              <div class="info-item">
                <div class="info-label">商机名称</div>
                <div class="info-value">{{ business.businessName }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">所属客户</div>
                <div class="info-value">
                  <a class="customer-link" @click="goCustomer(business.customerId)">{{ business.customerName }}</a>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">预计金额</div>
                <div class="info-value mono">¥ {{ formatAmount(business.expectedAmount) }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">当前阶段</div>
                <div class="info-value">
                  <el-tag :type="stageTagType(business.stage)" effect="light" size="small">{{ business.stage }}</el-tag>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">预计成交日</div>
                <div class="info-value mono">{{ business.expectedDealDate || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">赢率</div>
                <div class="info-value">{{ winRate }}%</div>
              </div>
              <div class="info-item">
                <div class="info-label">负责人</div>
                <div class="info-value">{{ business.ownerName || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">商机 ID</div>
                <div class="info-value mono">#{{ business.id }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">创建于</div>
                <div class="info-value mono">{{ formatDateTime(business.createTime) }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">最近更新</div>
                <div class="info-value mono">{{ formatDateTime(business.updateTime) }}</div>
              </div>
            </div>
          </div>

          <div v-else-if="activeTab === 'record'" class="timeline-card">
            <div class="timeline-head">
              <div class="timeline-title">
                📋 跟进记录
                <span class="count">· 共 {{ recordCount }} 条</span>
              </div>
              <el-button
                class="btn-zen-primary"
                size="small"
                @click="openAddRecord"
              >✚ 新建跟进</el-button>
            </div>
            <div class="timeline-body">
              <RecordTimeline
                :key="timelineKey"
                related-type="business"
                :related-id="business.id"
              />
            </div>
          </div>
        </div>

        <!-- 侧栏 -->
        <aside class="layout-side">
          <!-- 跟进摘要 -->
          <div class="side-card">
            <div class="side-card-title">📊 跟进摘要</div>
            <div class="stat-row"><span class="label">总跟进数</span><span class="value mono">{{ recordCount }} 条</span></div>
            <div class="stat-row"><span class="label">本阶段跟进</span><span class="value mono">{{ currentStageRecordCount }} 条</span></div>
            <div class="stat-row"><span class="label">距上次跟进</span>
              <span class="value mono" :class="lastFollowClass">{{ lastFollowText }}</span>
            </div>
            <div class="stat-row" v-if="nextFollowTime"><span class="label">下次跟进</span>
              <span class="value mono" :class="{ warn: isNextOverdue }">{{ formatNextShort(nextFollowTime) }}</span>
            </div>
          </div>

          <!-- 阶段流转历史 -->
          <div class="side-card">
            <div class="side-card-title">🔄 阶段流转历史</div>
            <div v-if="stageHistory.length === 0" class="empty-mini">暂无阶段变更</div>
            <div class="stage-hist">
              <div v-for="(h, i) in stageHistory" :key="i" class="stage-hist-item">
                <div class="stage-hist-dot" :class="{ 'is-past': i !== 0 }"></div>
                <div class="stage-hist-body">
                  <div class="stage-hist-name">{{ h.stage }}</div>
                  <div class="stage-hist-time">{{ formatStageTime(h.time, i) }}</div>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 写跟进弹窗 -->
    <AddRecordDialog
      v-if="business.id"
      v-model:visible="recordVisible"
      related-type="business"
      :related-id="business.id"
      :related-name="business.businessName"
      @saved="onRecordSaved"
    />

    <!-- 阶段推进弹窗 -->
    <el-dialog
      v-model="stageVisible"
      :title="`推进商机阶段 - ${business.businessName || ''}`"
      width="460px"
    >
      <div class="stage-current">
        当前阶段:
        <el-tag :type="stageTagType(business.stage)" effect="light" size="small">{{ business.stage }}</el-tag>
      </div>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-position="top" style="margin-top: 16px">
        <el-form-item label="目标阶段" prop="stage">
          <el-select v-model="stageForm.stage" placeholder="选择目标阶段" style="width: 100%">
            <el-option v-for="s in nextStageOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input
            v-model="stageForm.followContent"
            type="textarea"
            :rows="3"
            placeholder="说明本次推进的背景与客户反馈"
          />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getBusiness, updateBusinessStage } from '@/api/business'
import { getTimeline } from '@/api/record'
import { useUserStore } from '@/store/user'
import RecordTimeline from '@/components/RecordTimeline.vue'
import AddRecordDialog from '@/components/AddRecordDialog.vue'

defineOptions({ name: 'BusinessDetail' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const business = ref({})
const records = ref([])
const loading = ref(false)
const activeTab = ref('record')
const timelineKey = ref(0)

const STAGES = ['需求分析', '方案报价', '商务谈判', '赢单']
const pipelineStages = computed(() => STAGES)
const STAGE_ORDER = { '需求分析': 1, '方案报价': 2, '商务谈判': 3, '赢单': 4, '输单': 0 }

// 赢率按阶段启发式(后端没有此字段,前端按 PRD 规则推)
const winRateMap = { '需求分析': 30, '方案报价': 50, '商务谈判': 70, '赢单': 100, '输单': 0 }
const winRate = computed(() => winRateMap[business.value.stage] ?? 0)

// ---------- 权限 ----------
const isOwner = computed(() => business.value.ownerUserId && Number(userStore.userId) === Number(business.value.ownerUserId))
const canEdit = computed(() => isOwner.value && !isTerminal.value)
const isTerminal = computed(() => business.value.stage === '赢单' || business.value.stage === '输单')
const canWin = computed(() => business.value.stage === '商务谈判' && isOwner.value)
const canLose = computed(() => !isTerminal.value && isOwner.value)

// ---------- 工具 ----------
const stageTagType = (s) => ({ '需求分析': 'info', '方案报价': 'warning', '商务谈判': 'primary', '赢单': 'success', '输单': 'danger' }[s] || 'info')
const formatAmount = (a) => {
  if (a == null) return '0'
  return Number(a).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}
const formatDateTime = (t) => t ? t.substring(0, 16).replace('T', ' ') : '-'
const ownerChar = computed(() => (business.value.ownerName || '·').charAt(0))

const isPast = (s) => {
  const cur = STAGE_ORDER[business.value.stage] || 0
  const ord = STAGE_ORDER[s] || 0
  return ord > 0 && ord < cur
}

// ---------- 数据加载 ----------
async function loadBusiness() {
  const id = route.params.id
  loading.value = true
  try {
    const { data } = await getBusiness(id)
    business.value = data || {}
  } catch (e) {
    business.value = {}
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  if (!business.value.id) return
  try {
    const { data } = await getTimeline({ relatedType: 'business', relatedId: business.value.id })
    records.value = data || []
  } catch (e) {
    records.value = []
  }
}

function loadAll() {
  loadBusiness().then(() => {
    loadRecords()
  })
}

// ---------- 跟进统计 ----------
const recordCount = computed(() => records.value.length)
const currentStageRecordCount = computed(() => {
  // 统计:在当前阶段后产生的跟进(粗略用 createTime > 当前阶段变更时间)
  const stageHist = stageHistory.value
  if (stageHist.length === 0) return recordCount.value
  const current = stageHist[0]
  return records.value.filter(r => dayjs(r.createTime).isAfter(dayjs(current.time))).length
})
const lastRecord = computed(() => records.value[0] || null)
const lastFollowText = computed(() => {
  if (!lastRecord.value) return '从未'
  const days = dayjs().diff(dayjs(lastRecord.value.createTime), 'day')
  if (days < 1) return '今天'
  return days + ' 天'
})
const lastFollowClass = computed(() => {
  if (!lastRecord.value) return 'warn'
  const days = dayjs().diff(dayjs(lastRecord.value.createTime), 'day')
  if (days > 7) return 'danger'
  if (days > 3) return 'warn'
  return ''
})

const nextFollowTime = computed(() => lastRecord.value?.nextFollowTime || null)
const isNextOverdue = computed(() => nextFollowTime.value && dayjs(nextFollowTime.value).isBefore(dayjs()))
const formatNextShort = (t) => {
  const d = dayjs(t)
  const now = dayjs()
  if (d.isSame(now, 'day')) return '今天 ' + d.format('HH:mm')
  if (d.isSame(now.add(1, 'day'), 'day')) return '明天 ' + d.format('HH:mm')
  if (d.isSame(now.subtract(1, 'day'), 'day')) return '昨日 ' + d.format('HH:mm')
  if (d.year() === now.year()) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD HH:mm')
}

// ---------- 阶段流转历史 ----------
const stageHistory = computed(() => {
  // 从 records 里筛 "阶段从X推进到Y" 的系统记录
  const events = []
  for (const r of records.value) {
    if (r.followType !== '系统') continue
    const c = r.content || ''
    const fromMatch = c.match(/阶段从「([^」]+)」/)
    const toMatch = c.match(/推进到「([^」]+)」/)
    if (fromMatch && toMatch) {
      events.push({ from: fromMatch[1], stage: toMatch[1], time: r.createTime })
    } else if (c.startsWith('商机「') && c.includes('创建')) {
      events.push({ from: null, stage: '商机创建', time: r.createTime })
    }
  }
  // 加上当前阶段(若无变更历史)
  if (events.length === 0 && business.value.createTime) {
    events.push({ from: null, stage: business.value.stage, time: business.value.createTime })
  }
  // 倒序:最新的在前
  return events.slice().reverse()
})

const formatStageTime = (t, idx) => {
  if (!t) return '-'
  if (idx === 0) {
    const days = dayjs().diff(dayjs(t), 'day')
    return days >= 0 ? `${formatDateTime(t)} · 已停留 ${days} 天` : formatDateTime(t)
  }
  // 历史阶段:显示起止时间
  const next = stageHistory.value[idx - 1]
  if (next?.time) {
    return `${formatDateTime(t)} ~ ${dayjs(next.time).format('MM-DD')} · 共 ${dayjs(next.time).diff(dayjs(t), 'day')} 天`
  }
  return formatDateTime(t)
}

// ---------- 阶段推进 ----------
const stageVisible = ref(false)
const savingStage = ref(false)
const stageFormRef = ref(null)
const stageForm = reactive({ stage: '', followContent: '' })
const stageRules = { stage: [{ required: true, message: '请选择目标阶段', trigger: 'change' }] }

const nextStageOptions = computed(() => {
  const cur = business.value.stage
  if (!cur) return []
  if (cur === '赢单' || cur === '输单') return []
  const idx = STAGES.indexOf(cur)
  const result = []
  if (idx >= 0 && idx < 3) result.push(STAGES[idx + 1])
  result.push('输单')
  return result
})

function openAdvance() {
  stageForm.stage = ''
  stageForm.followContent = ''
  stageVisible.value = true
}

async function handleSaveStage() {
  await stageFormRef.value.validate()
  savingStage.value = true
  try {
    await updateBusinessStage(business.value.id, stageForm)
    ElMessage.success('阶段已推进')
    stageVisible.value = false
    await loadBusiness()
    await loadRecords()
    timelineKey.value++
  } finally {
    savingStage.value = false
  }
}

async function quickWin() {
  try {
    await ElMessageBox.confirm(
      `确认将商机「${business.value.businessName}」标记为赢单?本次变更会同步记录一条阶段跟进。`,
      '标记赢单',
      { type: 'success', confirmButtonText: '确认赢单', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' }
    )
    savingStage.value = true
    try {
      await updateBusinessStage(business.value.id, { stage: '赢单', followContent: '商机赢单' })
      ElMessage.success('已标记为赢单')
      await loadBusiness()
      await loadRecords()
      timelineKey.value++
    } finally {
      savingStage.value = false
    }
  } catch (e) { /* 取消 */ }
}

async function quickLose() {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请简要说明输单原因(将记入跟进)',
      '判定输单',
      {
        confirmButtonText: '确认输单',
        cancelButtonText: '取消',
        inputPlaceholder: '如:价格原因 / 选了竞品 / 项目暂停…',
        confirmButtonClass: 'btn-zen-primary',
        customClass: 'msgbox-zen-confirm',
      }
    )
    savingStage.value = true
    try {
      await updateBusinessStage(business.value.id, { stage: '输单', followContent: reason || '' })
      ElMessage.success('已判定为输单')
      await loadBusiness()
      await loadRecords()
      timelineKey.value++
    } finally {
      savingStage.value = false
    }
  } catch (e) { /* 取消 */ }
}

// ---------- 写跟进 ----------
const recordVisible = ref(false)
function openAddRecord() {
  recordVisible.value = true
}
async function onRecordSaved() {
  await loadRecords()
  timelineKey.value++
}

// ---------- 跳转 ----------
function goBusinessList() { router.push('/business/list') }
function goCustomer(id) {
  if (id) router.push(`/customer/${id}`)
}

// ---------- 生命周期 ----------
watch(() => route.params.id, loadAll)
onMounted(() => {
  loadAll()
  if (route.query.action === 'addRecord') recordVisible.value = true
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; min-height: 100%; }

.breadcrumb {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 12px;
  a { color: var(--muted); text-decoration: none; cursor: pointer; }
  a:hover { color: var(--accent); }
  .sep { margin: 0 6px; color: var(--subtle); }
}

.customer-link {
  color: var(--accent);
  text-decoration: none;
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

/* Header */
.detail-header {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 20px 24px;
  margin-bottom: 16px;
}
.detail-header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 16px;
}
.detail-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--ink);
  margin-bottom: 6px;
}
.detail-sub {
  font-size: 13px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  .dot { color: var(--subtle); }
}
.detail-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

.meta-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--hairline-soft);
}
@media (max-width: 1100px) { .meta-row { grid-template-columns: repeat(3, 1fr); } }
.meta-item { display: flex; flex-direction: column; gap: 4px; }
.meta-label {
  font-size: 11.5px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  &.inline { margin-bottom: 6px; }
}
.meta-value { font-size: 14px; font-weight: 500; color: var(--ink); }
.meta-value.is-amount {
  font-size: 18px;
  font-weight: 600;
  color: var(--accent);
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
  letter-spacing: -0.01em;
}
.meta-value.owner { display: flex; align-items: center; gap: 6px; }
.owner-avatar {
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--accent); color: white;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10.5px; font-weight: 600;
}

.stage-pipeline-row {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--hairline-soft);
}
/* 方案 A:横向 step box + SVG 三角箭头 */
.stage-pipeline {
  display: flex;
  align-items: stretch;
  gap: 0;
  margin: 6px 0;
  flex-wrap: nowrap;
  width: 100%;                 /* 占满父容器,4 段均分,每段约 200-240px */
}
.stage-step {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 13px;
  background: var(--bg);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  color: var(--muted);
  .step-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--hairline);
    flex-shrink: 0;
  }
  .step-name {
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &.done {
    color: var(--ink-soft);
    background: var(--accent-pale);
    border-color: var(--accent-soft);
    .step-dot { background: var(--accent); }
  }
  &.current {
    color: var(--accent);
    font-weight: 600;
    background: var(--accent-pale);
    border-color: var(--accent);
    box-shadow: 0 0 0 3px var(--accent-ring);
    .step-dot {
      background: var(--accent);
      box-shadow: 0 0 0 3px var(--accent-ring);
    }
  }
}
.stage-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  color: var(--hairline);
  flex-shrink: 0;
  &.done { color: var(--accent); }
}

/* Tabs */
.tabs-row {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 20px;
}
.tab {
  padding: 10px 16px;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.12s;
  display: flex;
  align-items: center;
  gap: 6px;
  &:hover { color: var(--ink); }
  &.active {
    color: var(--ink);
    font-weight: 500;
    border-bottom-color: var(--accent);
  }
  &.disabled {
    color: var(--subtle);
    cursor: not-allowed;
  }
  .count {
    font-size: 11.5px;
    padding: 1px 7px;
    background: var(--hairline-soft);
    color: var(--muted);
    border-radius: 8px;
    font-family: var(--font-mono);
    font-feature-settings: 'tnum' 1;
  }
  &.active .count {
    background: var(--accent-soft);
    color: var(--accent);
  }
}

/* Layout */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }
.layout-main { min-width: 0; }
.layout-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 76px;
}
@media (max-width: 1280px) { .layout-side { position: static; } }

/* Info panel */
.info-panel {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 20px 24px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px 32px;
}
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, 1fr); } }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label {
  font-size: 11.5px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.info-value {
  font-size: 14px;
  color: var(--ink);
  font-weight: 500;
}

/* Timeline card */
.timeline-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
}
.timeline-head {
  padding: 14px 18px;
  border-bottom: 1px solid var(--hairline-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.timeline-title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  .count {
    font-size: 12px;
    color: var(--muted);
    font-weight: 400;
  }
}
.timeline-body { padding: 8px 18px 18px; }

/* Side */
.side-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 14px 16px;
}
.side-card-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 12.5px;
  & + & { border-top: 1px solid var(--hairline-soft); }
  .label { color: var(--muted); }
  .value {
    font-weight: 500;
    font-family: var(--font-mono);
    font-feature-settings: 'tnum' 1;
    &.warn { color: var(--warn); }
    &.danger { color: var(--danger); }
  }
}

.stage-hist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.stage-hist-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 12.5px;
}
.stage-hist-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  margin-top: 6px;
  flex-shrink: 0;
  &.is-past { background: var(--hairline); }
}
.stage-hist-body { flex: 1; min-width: 0; }
.stage-hist-name { font-weight: 500; }
.stage-hist-time {
  font-size: 11px;
  color: var(--muted);
  margin-top: 2px;
}

.empty-mini {
  font-size: 12.5px;
  color: var(--muted);
  padding: 4px 0;
  text-align: center;
}

.stage-current { font-size: 13.5px; color: var(--ink-soft); }

.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
</style>