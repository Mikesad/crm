<template>
  <div class="page" v-loading="loading">
    <div v-if="lead && lead.id">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <a @click="goLeadList">线索</a>
        <span class="sep">›</span>
        <a @click="goLeadList">我的线索</a>
        <span class="sep">›</span>
        <span>{{ lead.leadName }}</span>
      </div>

      <!-- Header card -->
      <div class="detail-header">
        <div class="detail-header-top">
          <div>
            <div class="detail-title-row">
              <div class="detail-title">{{ lead.leadName }}</div>
              <el-tag :type="statusTagType(lead.status)" effect="light" size="small">{{ statusText(lead.status) }}</el-tag>
              <el-tag v-if="lead.status === 4 && lead.deadReason" type="info" size="small" class="dead-reason">
                死因: {{ lead.deadReason }}
              </el-tag>
            </div>
            <div class="detail-sub">
              线索人 <strong>{{ lead.contactName }}</strong>
              <span class="dot">·</span>
              {{ lead.phone || '无电话' }}
              <span class="dot">·</span>
              来源 {{ lead.source || '未知' }}
              <span class="dot">·</span>
              归属 {{ lead.ownerName || '-' }}
              <span class="dot">·</span>
              线索编号 <span class="mono">LEAD-{{ String(lead.id).padStart(4, '0') }}</span>
            </div>
          </div>
          <div class="detail-actions">
            <el-button
              v-if="canMarkDead"
              :icon="CircleClose"
              type="warning"
              plain
              @click="openMarkDead"
            >标记为死线索</el-button>
            <el-button
              v-if="canConvert"
              class="btn-zen-primary"
              :icon="Refresh"
              @click="openConvert"
            >转客户</el-button>
          </div>
        </div>

        <!-- Meta -->
        <div class="meta-row">
          <div class="meta-item">
            <div class="meta-label">线索状态</div>
            <div class="meta-value">{{ statusText(lead.status) }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">联系方式</div>
            <div class="meta-value mono">{{ lead.phone || '-' }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">线索来源</div>
            <div class="meta-value">{{ lead.source || '-' }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">归属销售</div>
            <div class="meta-value owner">
              <span class="owner-avatar">{{ ownerChar }}</span>
              <span>{{ lead.ownerName || '-' }}</span>
            </div>
          </div>
          <div class="meta-item">
            <div class="meta-label">创建于</div>
            <div class="meta-value mono">{{ formatDate(lead.createTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 死线索警示条 -->
      <el-alert
        v-if="lead.status === 4"
        type="error"
        show-icon
        :closable="false"
        class="readonly-banner"
        title="该线索已标记为死线索"
        :description="lead.deadReason ? `死因: ${lead.deadReason}` : '时间轴保留可查,不可写新跟进'"
      />

      <!-- 转客户软提示(未跟进过) -->
      <el-alert
        v-if="canConvert && !hasAnyRecord"
        type="warning"
        show-icon
        :closable="false"
        class="readonly-banner"
        title="⚠ 你还未跟进过此线索"
        description="建议先写一条跟进后再转客户,以保留前期接触上下文。"
      />

      <!-- Tabs -->
      <div class="tabs-row">
        <div class="tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">基本信息</div>
        <div class="tab" :class="{ active: activeTab === 'timeline' }" @click="activeTab = 'timeline'">
          跟进记录 <span class="count">{{ recordCount }}</span>
        </div>
      </div>

      <div class="layout">
        <div class="layout-main">
          <!-- 基本信息 Tab -->
          <div v-if="activeTab === 'basic'" class="info-panel">
            <div class="info-grid">
              <div class="info-item"><div class="info-label">线索名称</div><div class="info-value">{{ lead.leadName }}</div></div>
              <div class="info-item"><div class="info-label">联系人</div><div class="info-value">{{ lead.contactName }}</div></div>
              <div class="info-item"><div class="info-label">电话</div><div class="info-value mono">{{ lead.phone || '-' }}</div></div>
              <div class="info-item"><div class="info-label">来源</div><div class="info-value">{{ lead.source || '-' }}</div></div>
              <div class="info-item"><div class="info-label">状态</div><div class="info-value">{{ statusText(lead.status) }}</div></div>
              <div class="info-item"><div class="info-label">归属</div><div class="info-value">{{ lead.ownerName || '-' }}</div></div>
              <div class="info-item"><div class="info-label">创建时间</div><div class="info-value mono">{{ formatDate(lead.createTime) }}</div></div>
              <div class="info-item"><div class="info-label">更新时间</div><div class="info-value mono">{{ formatDate(lead.updateTime) }}</div></div>
              <div class="info-item full">
                <div class="info-label">备注</div>
                <div class="info-value remark">{{ lead.remark || '-' }}</div>
              </div>
              <div v-if="lead.status === 4" class="info-item full">
                <div class="info-label">死因</div>
                <div class="info-value remark">{{ lead.deadReason || '(未填写)' }}</div>
              </div>
              <div v-if="lead.status === 4" class="info-item">
                <div class="info-label">标记时间</div>
                <div class="info-value mono">{{ formatDate(lead.deadTime) }}</div>
              </div>
            </div>
          </div>

          <!-- 跟进记录 Tab -->
          <div v-else-if="activeTab === 'timeline'" class="timeline-card">
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
              <RecordTimeline :key="timelineKey" related-type="lead" :related-id="lead.id" />
            </div>
          </div>
        </div>

        <!-- 右侧辅助面板 -->
        <aside class="layout-side">
          <!-- 线索摘要 -->
          <div class="side-card">
            <div class="side-card-title">📊 线索摘要</div>
            <div class="stat-row"><span class="label">线索状态</span><span class="value">{{ statusText(lead.status) }}</span></div>
            <div class="stat-row"><span class="label">线索来源</span><span class="value">{{ lead.source || '-' }}</span></div>
            <div class="stat-row"><span class="label">总跟进数</span><span class="value mono">{{ recordCount }} 条</span></div>
            <div class="stat-row"><span class="label">距上次跟进</span>
              <span class="value mono" :class="lastFollowClass">{{ lastFollowText }}</span>
            </div>
            <div v-if="nextFollowTime" class="stat-row">
              <span class="label">下次跟进</span>
              <span class="value mono" :class="{ warn: isNextOverdue }">{{ formatNextShort(nextFollowTime) }}</span>
            </div>
          </div>

          <!-- 智能建议 -->
          <div class="side-card">
            <div class="side-card-title">💡 智能建议</div>
            <div v-if="lead.status === 4" class="sug-item">
              <div class="sug-icon danger">!</div>
              <div class="sug-content">
                <div class="sug-title">线索已死,不可继续跟进</div>
                <div class="sug-desc">死因: {{ lead.deadReason || '未填写' }}</div>
              </div>
            </div>
            <div v-else-if="lead.status === 3" class="sug-item">
              <div class="sug-icon accent">✓</div>
              <div class="sug-content">
                <div class="sug-title">已转客户</div>
                <div class="sug-desc">客户 ID #{{ lead.convertedCustomerId || '-' }},跟进已迁移</div>
              </div>
            </div>
            <div v-else-if="daysSinceLastFollow >= 5" class="sug-item">
              <div class="sug-icon warn">!</div>
              <div class="sug-content">
                <div class="sug-title">已 {{ daysSinceLastFollow }} 天未跟进</div>
                <div class="sug-desc">建议立即电话沟通,推进到转客户</div>
                <div class="sug-action" @click="openAddRecord">立即写跟进 →</div>
              </div>
            </div>
            <div v-else class="sug-item">
              <div class="sug-icon accent">★</div>
              <div class="sug-content">
                <div class="sug-title">跟进状态良好</div>
                <div class="sug-desc">继续保持每周一次的跟进节奏,争取尽快转化</div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 写跟进弹窗 -->
    <AddRecordDialog
      v-if="lead && lead.id"
      v-model:visible="addRecordVisible"
      related-type="lead"
      :related-id="lead && lead.id"
      :related-name="lead && lead.leadName"
      @saved="onRecordSaved"
    />

    <!-- 标死线索弹窗 -->
    <MarkDeadDialog
      v-model:visible="markDeadVisible"
      :lead="lead"
      @marked="onMarked"
    />

    <!-- 转客户弹窗 -->
    <el-dialog
      v-model="convertVisible"
      title="线索转客户"
      width="480px"
    >
      <el-form ref="convertFormRef" :model="convertForm" :rules="convertRules" label-position="top">
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="convertForm.customerName" placeholder="例:蓝海科技(北京)有限公司" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="convertForm.industry" placeholder="如 IT/物流/制造" />
        </el-form-item>
        <el-form-item label="客户级别">
          <el-select v-model="convertForm.level" placeholder="默认 C-意向客户">
            <el-option label="A - 重要客户" value="A" />
            <el-option label="B - 普通客户" value="B" />
            <el-option label="C - 意向客户" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="主联系人电话">
          <el-input v-model="convertForm.phone" placeholder="留空则沿用线索电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="convertVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="converting" @click="onConvert">确认转客户</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, CircleClose } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getLead, convertLead } from '@/api/lead'
import { getTimeline } from '@/api/record'
import { useUserStore } from '@/store/user'
import RecordTimeline from '@/components/RecordTimeline.vue'
import AddRecordDialog from '@/components/AddRecordDialog.vue'
import MarkDeadDialog from '@/components/MarkDeadDialog.vue'

defineOptions({ name: 'LeadDetail' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const lead = ref(null)
const records = ref([])
const loading = ref(false)
const activeTab = ref('timeline')   // 默认进跟进记录(本阶段重点)
const hasAnyRecord = ref(false)
const recordCount = ref(0)
const timelineKey = ref(0)

const addRecordVisible = ref(false)
const markDeadVisible = ref(false)
const convertVisible = ref(false)
const converting = ref(false)
const convertFormRef = ref(null)
const convertForm = reactive({ customerName: '', industry: '', level: 'C', phone: '' })
const convertRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
}

// ---------- 状态枚举 ----------
const STATUS = { 1: '未跟进', 2: '跟进中', 3: '已转客户', 4: '已死线索' }
const STATUS_TAG = { 1: 'info', 2: 'primary', 3: 'success', 4: 'danger' }
const statusText = (s) => STATUS[s] || '-'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

// ---------- 权限计算 ----------
const isOwner = computed(() => lead.value && Number(userStore.userId) === Number(lead.value.ownerUserId))
const canEdit = computed(() => lead.value && lead.value.status !== 3 && lead.value.status !== 4 && isOwner.value)
const canMarkDead = computed(() => lead.value && [1, 2].includes(lead.value.status) && isOwner.value)
const canConvert = computed(() => lead.value && [1, 2].includes(lead.value.status) && isOwner.value)

// ---------- 工具 ----------
const formatDate = (s) => s ? s.substring(0, 16).replace('T', ' ') : '-'
const ownerChar = computed(() => (lead.value?.ownerName || '·').charAt(0))

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
const daysSinceLastFollow = computed(() => {
  if (!lastRecord.value) return 0
  return dayjs().diff(dayjs(lastRecord.value.createTime), 'day')
})
const nextFollowTime = computed(() => lastRecord.value?.nextFollowTime || null)
const isNextOverdue = computed(() => nextFollowTime.value && dayjs(nextFollowTime.value).isBefore(dayjs()))
const formatNextShort = (t) => {
  const d = dayjs(t)
  const now = dayjs()
  if (d.isSame(now, 'day')) return '今天 ' + d.format('HH:mm')
  if (d.isSame(now.add(1, 'day'), 'day')) return '明天 ' + d.format('HH:mm')
  if (d.year() === now.year()) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD HH:mm')
}

// ---------- 数据加载 ----------
async function loadLead() {
  const id = route.params.id
  loading.value = true
  try {
    const { data } = await getLead(id)
    lead.value = data
  } catch (e) {
    lead.value = null
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  if (!lead.value?.id) {
    records.value = []
    recordCount.value = 0
    hasAnyRecord.value = false
    return
  }
  try {
    const { data } = await getTimeline({ relatedType: 'lead', relatedId: lead.value.id })
    records.value = data || []
    recordCount.value = records.value.length
    hasAnyRecord.value = recordCount.value > 0
  } catch (e) {
    records.value = []
    recordCount.value = 0
    hasAnyRecord.value = false
  }
}

function loadAll() {
  loadLead()
  loadRecords()
}

// ---------- 弹窗 ----------
const openAddRecord = () => { addRecordVisible.value = true }
const openMarkDead = () => { markDeadVisible.value = true }
const openConvert = () => {
  convertForm.customerName = lead.value.leadName
  convertForm.phone = lead.value.phone
  convertVisible.value = true
}

const onRecordSaved = async () => {
  await loadLead()
  await loadRecords()
  timelineKey.value++
}

const onMarked = async () => {
  await loadLead()
  ElMessage.success('已标记为死线索')
}

const onConvert = async () => {
  await convertFormRef.value.validate()
  converting.value = true
  try {
    const { data: customerId } = await convertLead(lead.value.id, convertForm)
    ElMessage.success(`转客户成功,新客户 ID=${customerId}`)
    router.push(`/customer/${customerId}`)
  } finally {
    converting.value = false
  }
}

// ---------- 跳转 ----------
const goLeadList = () => router.push('/lead/list')

// ---------- 生命周期 ----------
watch(() => route.params.id, loadAll)
onMounted(() => {
  loadAll()
  if (route.query.action === 'addRecord') addRecordVisible.value = true
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
.detail-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--ink);
}
.dead-reason { margin-left: 4px; }
.detail-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  strong { color: var(--ink); font-weight: 500; }
  .dot { color: var(--subtle); }
}
.detail-actions {
  display: flex;
  gap: 8px;
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
.meta-value.owner { display: flex; align-items: center; gap: 6px; }
.owner-avatar {
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--accent); color: white;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10.5px; font-weight: 600;
  flex-shrink: 0;
}

/* Banner */
.readonly-banner {
  margin-bottom: 16px;
  border-radius: var(--radius);
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
.info-item.full { grid-column: span 3; }
@media (max-width: 900px) { .info-item.full { grid-column: span 2; } }
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
.info-value.remark {
  font-weight: 400;
  color: var(--ink-soft);
  line-height: 1.6;
  white-space: pre-wrap;
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

/* Side cards */
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

.sug-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  font-size: 12.5px;
  line-height: 1.5;
}
.sug-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  &.warn { background: var(--warn-soft); color: var(--warn); }
  &.danger { background: var(--danger-soft); color: var(--danger); }
}
.sug-content { flex: 1; }
.sug-title { font-weight: 500; color: var(--ink); }
.sug-desc { color: var(--muted); margin-top: 2px; font-size: 11.5px; }
.sug-action {
  margin-top: 6px;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
</style>
