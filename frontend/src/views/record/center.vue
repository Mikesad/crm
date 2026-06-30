<template>
  <div class="record-center">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">跟进中心</h1>
        <div class="page-sub">个人工作台 · 查看今日 / 本周待跟进与历史记录</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary" @click="quickCreateVisible = true">
          ✚ 快速新建跟进
        </button>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi-row">
      <div class="kpi">
        <div class="kpi-label">📌 今日待跟进</div>
        <div class="kpi-value mono">{{ counts.today }}</div>
      </div>
      <div class="kpi">
        <div class="kpi-label">📅 本周待跟进</div>
        <div class="kpi-value mono">{{ counts.week }}</div>
      </div>
      <div class="kpi is-danger">
        <div class="kpi-label">⚠ 逾期未跟进</div>
        <div class="kpi-value mono">{{ counts.overdue }}</div>
        <div class="kpi-foot">需尽快处理,避免进入公海</div>
      </div>
      <div class="kpi">
        <div class="kpi-label">✓ 本月已写跟进</div>
        <div class="kpi-value mono">{{ counts.monthWritten || 0 }}</div>
      </div>
    </div>

    <!-- Tabs(阶段五 v3:顺序 = 我的跟进历史(上) → 本周待跟进 → 今日待跟进(下)) -->
    <div class="tabs-row">
      <div class="tab" :class="{ active: activeTab === 'mine' }" @click="switchTab('mine')">
        我的跟进历史 <span class="count">{{ mineTotal }}</span>
      </div>
      <div class="tab" :class="{ active: activeTab === 'week' }" @click="switchTab('week')">
        本周待跟进 <span class="count">{{ counts.week }}</span>
        <span v-if="counts.overdue > 0" class="count danger">逾期 {{ counts.overdue }}</span>
      </div>
      <div class="tab" :class="{ active: activeTab === 'today' }" @click="switchTab('today')">
        今日待跟进 <span class="count">{{ counts.today }}</span>
      </div>
      <div class="tab-meta">
        排序: 逾期优先,再按下次跟进时间 ↑
      </div>
    </div>

    <!-- 实体类型过滤栏(阶段五 v3:仅在 time-scope tab 下显示) -->
    <div v-if="activeTab === 'mine'" class="filter-bar search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索跟进内容 / 跟进方式(如「电话」「邮件」「报价」)"
        class="search-input"
        clearable
        @keyup.enter="onSearch"
        @clear="onSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" class="btn-zen-primary" :icon="Search" @click="onSearch">搜索</el-button>
      <span v-if="keyword" class="filter-clear" @click="onClearSearch">↻ 清空</span>
      <div class="search-hint">{{ keyword ? `「${keyword}」匹配结果` : '按 Enter 搜索' }}</div>
    </div>

    <div v-if="activeTab !== 'mine'" class="filter-bar">
      <span
        class="filter-chip"
        :class="{ active: selectedType === 'all' }"
        @click="onTypePick('all')"
      >
        全部 <span class="num">{{ currentTypeTotal }}</span>
      </span>
      <span
        v-for="t in entityTypes"
        :key="t.value"
        class="filter-chip"
        :class="{ active: selectedType === t.value }"
        @click="onTypePick(t.value)"
      >
        <el-icon style="font-size:13px"><component :is="t.icon" /></el-icon>
        {{ t.name }} <span class="num">{{ byTypeNum(t.value) }}</span>
      </span>
      <span v-if="selectedType !== 'all'" class="filter-clear" @click="onTypePick('all')">↻ 重置</span>
    </div>

    <!-- 双栏布局 -->
    <div class="layout">
      <!-- 主内容 -->
      <div>
        <div class="section-meta">
          <span>展示 <strong>{{ filteredRows.length }}</strong> 条</span>
          <span class="dot-sep">·</span>
          <span class="legend"><span class="dot d-danger"></span> 逾期 ({{ counts.overdue }})</span>
          <span style="color:var(--hairline)">·</span>
          <span style="color:var(--muted)">今日到期 ({{ counts.today }})</span>
          <span style="color:var(--hairline)">·</span>
          <span style="color:var(--muted)">本周 ({{ counts.week - counts.today }})</span>
          <el-select
            v-model="sortMode"
            size="default"
            class="sort-select"
          >
            <template #prefix>
              <span class="sort-prefix">排序</span>
            </template>
            <el-option
              v-for="o in sortOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </div>

        <div v-if="loading" class="empty">
          <div class="text">加载中…</div>
        </div>
        <div v-else-if="rows.length === 0" class="empty">
          <div class="ico">📭</div>
          <div class="text">{{ emptyText }}</div>
        </div>
        <div v-else class="record-grid">
          <div
            v-for="row in filteredRows"
            :key="`${row.relatedType}-${row.recordId}`"
            class="record-card"
            :class="{
              'is-overdue': row.overdue,
              'is-dead': row.leadStatus === 4,
            }"
          >
            <div class="card-head">
              <span class="entity-chip" :class="`chip-${row.relatedType}`">
                <el-icon class="chip-icon"><component :is="entityIcon(row.relatedType)" /></el-icon>
                <span>{{ entityName(row.relatedType) }}</span>
              </span>
              <span v-if="row.overdue" class="overdue-tag">⚠ 逾期</span>
              <span v-if="row.subjectStatusText" class="pill">{{ row.subjectStatusText }}</span>
              <div v-if="row.subjectAmount" class="card-amount">
                <span class="currency">¥</span>{{ formatAmount(row.subjectAmount) }}
              </div>
            </div>

            <div class="card-title">{{ row.subjectName || '(主体已删除)' }}</div>
            <div class="card-sub">{{ cardSubText(row) }}</div>

            <div v-if="row.content" class="last-follow">
              <div class="last-follow-head">
                <span class="follow-type" :class="`t-${followTypeClass(row.followType)}`">
                  {{ followTypeIco(row.followType) }} {{ row.followType }}
                </span>
                <span class="last-follow-meta">{{ formatRel(row.createTime) }} · {{ row.createBy }}</span>
              </div>
              <div class="last-follow-body">{{ row.content }}</div>
            </div>

            <div class="card-foot">
              <div class="next-time">
                <div class="next-time-label">{{ row.overdue ? '下次跟进(逾期)' : '下次跟进' }}</div>
                <div
                  class="next-time-val"
                  :class="row.overdue ? 'danger' : (isToday(row.nextFollowTime) ? 'warn' : '')"
                >
                  {{ formatDate(row.nextFollowTime) || '未安排' }}
                </div>
              </div>
              <div class="card-actions">
                <button class="btn btn-ghost" @click.stop="goDetail(row)">查看详情</button>
                <button class="btn btn-primary" @click.stop="goDetail(row, true)">立即跟进</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧辅助栏 -->
      <aside class="layout-side">
        <div class="side-card">
          <div class="side-card-title">
            📊 本周跟进方式分布
            <span class="more">查看明细</span>
          </div>
          <div class="bar-row">
            <span class="label">📞 电话</span>
            <div class="track"><div class="fill b-phone" :style="{ width: pct('电话') + '%' }"></div></div>
            <span class="val">{{ followTypeCount('电话') }}</span>
          </div>
          <div class="bar-row">
            <span class="label">🚗 上门</span>
            <div class="track"><div class="fill b-visit" :style="{ width: pct('上门拜访') + '%' }"></div></div>
            <span class="val">{{ followTypeCount('上门拜访') }}</span>
          </div>
          <div class="bar-row">
            <span class="label">💬 微信</span>
            <div class="track"><div class="fill b-wechat" :style="{ width: pct('微信') + '%' }"></div></div>
            <span class="val">{{ followTypeCount('微信') }}</span>
          </div>
          <div class="bar-row">
            <span class="label">📧 邮件</span>
            <div class="track"><div class="fill b-email" :style="{ width: pct('邮件') + '%' }"></div></div>
            <span class="val">{{ followTypeCount('邮件') }}</span>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">
            📈 近 7 日跟进频次
            <span class="more">查看趋势</span>
          </div>
          <div class="sparkline">
            <div
              v-for="(b, i) in sparkBars"
              :key="i"
              class="spark-bar"
              :class="{ 'is-peak': b.isPeak }"
              :style="{ height: b.height + '%' }"
              :data-tip="b.tip"
            ></div>
          </div>
          <div class="spark-labels">
            <span>6/22</span>
            <span>6/28</span>
          </div>
        </div>

        <div class="tip">
          <span class="ico">💡</span>
          <span>建议每天至少跟进 <strong>3-5 个客户</strong>,保持销售节奏。</span>
        </div>
      </aside>
    </div>

    <!-- 写跟进(快速新建模式,自带实体选择器) -->
    <AddRecordDialog
      v-model:visible="quickCreateVisible"
      selectable
      :default-type="'customer'"
      @saved="onSaved"
      @keep-adding="onKeepAdding"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { todoCount, todoList, myRecords, last7Days as fetchLast7Days } from '@/api/record'
import { useTodoStore } from '@/store/todo'
import AddRecordDialog from '@/components/AddRecordDialog.vue'
import { Aim, User, TrendCharts, Document, Search } from '@element-plus/icons-vue'

const router = useRouter()
const todoStore = useTodoStore()

const activeTab = ref('today')
const counts = reactive({
  today: 0, week: 0, overdue: 0, total: 0, monthWritten: 0,
  byType: { today: { lead: 0, customer: 0, business: 0, contract: 0 }, week: { lead: 0, customer: 0, business: 0, contract: 0 } },
})
const rows = ref([])
const mineTotal = ref(0)
/** 阶段八 commit 10:跟进历史搜索关键词 */
const keyword = ref('')
const loading = ref(false)
const quickCreateVisible = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)

// 近 7 日跟进频次 [{ date: 'YYYY-MM-DD', weekday: '周一', count: N }, ...] 长度固定 7
const last7Days = ref([])

// 实体类型过滤(阶段五 v2)
const selectedType = ref('all')
// 合同实体跟进接入较晚,过滤栏暂不展示(后端保留 contract 枚举)
// 用 Element Plus Icons 与侧边栏导航保持视觉一致
// 合同实体跟进接入较晚,过滤栏暂不展示(后端保留 contract 枚举)
// 用 Element Plus Icons 与侧边栏导航保持视觉一致
const entityTypes = [
  { value: 'lead', icon: Aim, name: '线索' },
  { value: 'customer', icon: User, name: '客户' },
  { value: 'business', icon: TrendCharts, name: '商机' },
  { value: 'contract', icon: Document, name: '合同' },
]
const entityName = (t) => ({ lead: '线索', customer: '客户', business: '商机', contract: '合同' }[t] || t)
const entityIcon = (t) => ({ lead: Aim, customer: User, business: TrendCharts, contract: 'Document' }[t] || null)

// 排序(阶段五 v2):默认"逾期优先 + 下次跟进升序",只保留箭头符号
const sortMode = ref('overdue-priority')
const sortOptions = [
  { value: 'overdue-priority', label: '逾期优先' },
  { value: 'next-follow-asc', label: '下次跟进 ↑' },
  { value: 'next-follow-desc', label: '下次跟进 ↓' },
  { value: 'create-time-desc', label: '创建时间 ↓' },
]
const sortLabel = computed(() => sortOptions.find(o => o.value === sortMode.value)?.label || '逾期优先')

// 当前 Tab 对应的 byType 子 Map(切 Tab 时联动刷新 chip 数字)
const currentByType = computed(() => {
  if (activeTab.value === 'mine') return counts.byType.week // 历史 tab 也显示 week 数字(参考)
  return counts.byType[activeTab.value] || counts.byType.week
})

// 客户端过滤:切换 chip 立即生效,无需重新请求
const filteredRows = computed(() => {
  let list = selectedType.value === 'all' ? rows.value : rows.value.filter(r => r.relatedType === selectedType.value)
  // 客户端排序
  return [...list].sort((a, b) => {
    switch (sortMode.value) {
      case 'next-follow-asc':
        return new Date(a.nextFollowTime || 0) - new Date(b.nextFollowTime || 0)
      case 'next-follow-desc':
        return new Date(b.nextFollowTime || 0) - new Date(a.nextFollowTime || 0)
      case 'create-time-desc':
        return new Date(b.createTime || 0) - new Date(a.createTime || 0)
      case 'overdue-priority':
      default: {
        // 逾期优先 → next_follow_time 升序
        const aOver = a.overdue ? 0 : 1
        const bOver = b.overdue ? 0 : 1
        if (aOver !== bOver) return aOver - bOver
        return new Date(a.nextFollowTime || 0) - new Date(b.nextFollowTime || 0)
      }
    }
  })
})

const byTypeNum = (t) => (currentByType.value && currentByType.value[t]) || 0

// 当前 Tab 范围下 4 个实体类型的总待跟进数(供"全部" chip 显示)
// = lead + customer + business + contract 之和
const currentTypeTotal = computed(() => {
  const m = currentByType.value || {}
  return (m.lead || 0) + (m.customer || 0) + (m.business || 0) + (m.contract || 0)
})

const onTypePick = (t) => {
  selectedType.value = t
  // 客户端过滤已生效(filteredRows),无需重新请求
}

const emptyText = computed(() => activeTab.value === 'mine' ? '还没写过跟进,去写一条吧' : '今天没有待跟进,继续保持!')

onMounted(async () => {
  await refreshCounts()
  await loadData()
  await refreshLast7Days()
  await refreshMineCount()    // 首次进入也要把「我的跟进历史」条数拉回来
})

const refreshLast7Days = async () => {
  try {
    const { data } = await fetchLast7Days()
    last7Days.value = Array.isArray(data) ? data : []
  } catch (e) {
    last7Days.value = []
  }
}

// 仅拉总数(用 pageSize=1 走最小查询),不取列表内容
const refreshMineCount = async () => {
  try {
    const { data } = await myRecords({ pageNum: 1, pageSize: 1 })
    mineTotal.value = data?.total || 0
  } catch (e) {
    mineTotal.value = 0
  }
}

const refreshCounts = async () => {
  try {
    const { data } = await todoCount()
    const emptyByType = { today: { lead: 0, customer: 0, business: 0, contract: 0 }, week: { lead: 0, customer: 0, business: 0, contract: 0 } }
    Object.assign(counts, {
      today: data?.today || 0,
      week: data?.week || 0,
      overdue: data?.overdue || 0,
      total: data?.total || 0,
      monthWritten: data?.monthWritten || 0,
      byType: data?.byType || emptyByType,
    })
    todoStore.counts = data || counts
  } catch (e) { /* ignore */ }
}

const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  // 切到「我的跟进历史」时,清掉实体类型过滤(否则筛选语义不明)
  if (tab === 'mine') selectedType.value = 'all'
  pageNum.value = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'mine') {
      const kw = keyword.value?.trim() || ''
      const { data } = await myRecords({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: kw || undefined
      })
      rows.value = data.records || []
      mineTotal.value = data.total || 0
    } else {
      const { data } = await todoList({
        range: activeTab.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      rows.value = data.records || []
    }
  } finally {
    loading.value = false
  }
}

const onSaved = async () => {
  await refreshCounts()
  await loadData()
}
const onKeepAdding = async () => {
  await refreshCounts()
  // 不重新 load,让用户继续在同一主体上写
}

/** 阶段八 commit 10:跟进历史搜索(关键词命中后回到第 1 页) */
const onSearch = async () => {
  pageNum.value = 1
  await loadData()
}
const onClearSearch = async () => {
  keyword.value = ''
  pageNum.value = 1
  await loadData()
}

const goDetail = (row, withRecord) => {
  const map = { lead: '/lead', customer: '/customer', business: '/business', contract: '/contract' }
  const base = map[row.relatedType] || '/dashboard'
  router.push(`${base}/${row.relatedId}${withRecord ? '?action=addRecord' : ''}`)
}

// helpers
const entityLabel = (t) => ({ lead: '📥 线索', customer: '👥 客户', business: '📈 商机', contract: '📄 合同' }[t] || t)
const cardSubText = (row) => {
  if (row.relatedType === 'lead') return `线索人 · ID: ${row.relatedId}`
  if (row.relatedType === 'customer') return `客户 · ID: ${row.relatedId}`
  if (row.relatedType === 'business') return `商机 · ID: ${row.relatedId}`
  if (row.relatedType === 'contract') return `合同 · ID: ${row.relatedId}`
  return ''
}
const followTypeClass = (t) => ({ '电话': 'phone', '微信': 'wechat', '上门拜访': 'visit', '邮件': 'email', '系统': 'system' }[t] || 'system')
const followTypeIco = (t) => ({ '电话': '📞', '微信': '💬', '上门拜访': '🚗', '邮件': '📧', '系统': '⚙' }[t] || '📌')

const followTypeCount = (t) => rows.value.filter(r => r.followType === t).length
const pct = (t) => {
  const total = rows.value.length
  if (total === 0) return 0
  return Math.round((followTypeCount(t) / total) * 100)
}

const isToday = (s) => s && dayjs(s).isSame(dayjs(), 'day')

const formatDate = (s) => s ? dayjs(s).format('MM-DD HH:mm') : ''
const formatRel = (s) => {
  if (!s) return ''
  const d = dayjs(s)
  const now = dayjs()
  const diffMin = now.diff(d, 'minute')
  if (diffMin < 60) return `${diffMin} 分钟前`
  if (diffMin < 24 * 60) return `${Math.floor(diffMin / 60)} 小时前`
  const diffDay = now.diff(d, 'day')
  if (diffDay < 7) return `${diffDay} 天前`
  return d.format('MM-DD')
}
const formatAmount = (s) => {
  const n = Number(s)
  if (isNaN(n)) return s
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// 7 日 sparkline (演示数据,接入后端后用真实数据)
const sparkBars = computed(() => {
  if (!last7Days.value.length) {
    return Array.from({ length: 7 }, () => ({ height: 6, isPeak: false, tip: '暂无数据' }))
  }
  const max = Math.max(1, ...last7Days.value.map(d => d.count))
  const maxIdx = last7Days.value.findIndex(d => d.count === max)
  return last7Days.value.map((d, i) => {
    const heightPct = Math.max(6, Math.round((d.count / max) * 100))
    const label = d.date.slice(5)
    return { height: heightPct, isPeak: i === maxIdx, tip: `${label} ${d.weekday} · ${d.count} 条` }
  })
})
</script>

<style lang="scss" scoped>
@import '@/styles/tokens.scss';

/* 与 phase5-record-center.html 原型一致:整体页面留白 */
.record-center { padding: 32px 32px 48px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}
.page-title {
  font-size: 26px; font-weight: 600; letter-spacing: -0.015em;
  margin: 0 0 6px;
}
.page-sub { font-size: 13.5px; color: var(--muted); }
.page-actions { display: flex; gap: 8px; }

.btn {
  padding: 8px 16px;
  font-size: 13px;
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  cursor: pointer;
  color: var(--ink);
  font-family: inherit;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  &:hover { border-color: var(--muted); }
  &.btn-primary {
    background: var(--accent);
    color: white;
    border-color: var(--accent);
    &:hover { background: #14532d; border-color: #14532d; }
  }
  &.btn-ghost {
    background: transparent;
    border-color: transparent;
    color: var(--ink-soft);
    padding: 5px 10px;
    font-size: 12.5px;
    &:hover { background: var(--bg); color: var(--accent); }
  }
}

// KPI
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
.kpi {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 20px;
}
.kpi.is-danger {
  border-color: var(--danger);
  background: linear-gradient(180deg, var(--danger-soft) 0%, var(--surface) 60%);
  .kpi-value { color: var(--danger); }
}
.kpi-label {
  font-size: 12.5px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}
.kpi-value {
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 4px;
}
.kpi-foot {
  font-size: 12px;
  color: var(--muted);
}

// Tabs
.tabs-row {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 18px;
}
.tab {
  padding: 11px 18px;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
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
    font-feature-settings: 'tnum' 1;
  }
  &.active .count { background: var(--accent-soft); color: var(--accent); }
  .count.danger { background: var(--danger-soft); color: var(--danger); }
}
.tab-meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  padding: 0 8px;
}

// 双栏
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  align-items: start;
}
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }

.layout-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 76px;
}
@media (max-width: 1280px) { .layout-side { position: static; } }

// Section meta
.section-meta {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  .dot-sep { color: var(--hairline); }
  .legend { display: inline-flex; align-items: center; gap: 4px; }
  .dot { width: 6px; height: 6px; border-radius: 50%; }
  .dot.d-danger { background: var(--danger); }
  .dot.d-warn { background: var(--warn); }
  .dot.d-accent { background: var(--accent); }
}
// 排序下拉:el-select,放大尺寸,带 "排序" 前缀标签
.sort-select {
  margin-left: auto;
  width: 200px;                /* 加大宽度,从 12px 文字触发器升级为标准 el-select */
  :deep(.el-input__wrapper) {
    background: var(--surface);
    box-shadow: 0 0 0 1px var(--hairline) inset;
    padding: 1px 11px;
    &:hover { box-shadow: 0 0 0 1px var(--muted) inset; }
    &.is-focus { box-shadow: 0 0 0 1px var(--accent) inset; }
  }
  :deep(.el-input__inner) {
    font-size: 13px;
    color: var(--ink);
  }
  .sort-prefix {
    color: var(--muted);
    font-size: 12.5px;
    margin-right: 6px;
    font-weight: 500;
  }
}

// Empty
.empty {
  padding: 48px 24px;
  text-align: center;
  color: var(--muted);
  border: 1px dashed var(--hairline);
  border-radius: var(--radius);
  background: var(--surface);
  .ico { font-size: 32px; margin-bottom: 8px; opacity: 0.4; }
  .text { font-size: 13px; }
}

// 跟进卡片网格
.record-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
@media (max-width: 1100px) { .record-grid { grid-template-columns: 1fr; } }

.record-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 18px;
  transition: all 0.12s;
  &:hover { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-ring); }
  /* 逾期不再用红边,改用卡片内的红色 tag(放在主体名正上方) */
  &.is-dead { border-left: 3px solid var(--subtle); opacity: 0.75; }
}
/* 逾期 tag:与 entity-chip 同行(flex 自动排列),不再是独立行 */
.overdue-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 10.5px;
  font-weight: 600;
  background: var(--danger-soft);
  color: var(--danger);
  border-radius: 3px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.entity-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 7px;
  font-size: 10.5px;
  font-weight: 500;
  border-radius: 3px;
  line-height: 1.6;
  background: var(--hairline-soft);
  color: var(--muted);
}
.chip-lead { background: #ede9fe; color: #6d28d9; }
.chip-customer { background: var(--accent-soft); color: var(--accent); }
.chip-business { background: var(--info-soft); color: var(--info); }
.chip-contract { background: var(--warn-soft); color: var(--warn); }

.pill {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 10px;
  background: var(--hairline-soft);
  color: var(--muted);
}
.card-amount {
  margin-left: auto;
  font-size: 13px;
  color: var(--ink);
  font-weight: 600;
  font-feature-settings: 'tnum' 1;
  .currency { color: var(--muted); font-size: 11.5px; font-weight: 400; margin-right: 2px; }
}
.card-title {
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin-bottom: 6px;
  line-height: 1.4;
}
.card-sub {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 12px;
  line-height: 1.5;
}
.last-follow {
  padding: 10px 12px;
  background: var(--bg);
  border-radius: var(--radius);
  margin-bottom: 14px;
  font-size: 12.5px;
  line-height: 1.55;
}
.last-follow-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 5px;
}
.follow-type {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  color: var(--ink-soft);
  &::before {
    content: ''; width: 6px; height: 6px; border-radius: 50%;
    background: var(--accent);
  }
  &.t-phone::before { background: var(--info); }
  &.t-wechat::before { background: #16a34a; }
  &.t-visit::before { background: var(--warn); }
  &.t-email::before { background: #6d28d9; }
  &.t-system::before { background: var(--subtle); }
}
.last-follow-meta {
  color: var(--subtle);
  font-size: 11px;
  margin-left: auto;
}
.last-follow-body {
  color: var(--ink-soft);
  line-height: 1.55;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--hairline-soft);
}
.next-time { font-size: 12.5px; }
.next-time-label {
  color: var(--muted);
  font-size: 11px;
  margin-bottom: 2px;
}
.next-time-val {
  font-weight: 600;
  color: var(--ink);
  font-feature-settings: 'tnum' 1;
  &.danger { color: var(--danger); }
  &.warn { color: var(--warn); }
}
.card-actions { display: flex; gap: 6px; }

/* 实体类型过滤栏 */
.search-bar { display: flex; align-items: center; gap: 10px; }

.search-input { flex: 1; max-width: 460px; }

.search-input :deep(.el-input__wrapper) {
  background: var(--surface);
  border-radius: 6px;
  padding: 2px 12px;
}

.search-hint {
  font-size: 11.5px;
  color: var(--muted);
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  margin-bottom: 16px;
}
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 12.5px;
  background: var(--bg);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  color: var(--ink-soft);
  cursor: pointer;
  transition: all 0.12s;
  font-weight: 500;
  font-family: inherit;
  &:hover {
    border-color: var(--muted);
    color: var(--ink);
  }
  &.active {
    background: var(--accent-pale);
    border-color: var(--accent);
    color: var(--accent);
    font-weight: 600;
  }
  .num {
    font-size: 11px;
    padding: 1px 6px;
    background: var(--hairline-soft);
    color: var(--muted);
    border-radius: 8px;
    font-feature-settings: 'tnum' 1;
    font-weight: 400;
  }
  &.active .num {
    background: var(--accent-soft);
    color: var(--accent);
  }
}
.filter-clear {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  cursor: pointer;
  padding: 4px 8px;
  user-select: none;
  &:hover { color: var(--accent); }
}

// 右侧辅助
.side-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 18px;
}
.side-card-title {
  font-size: 13.5px;
  font-weight: 600;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  .more {
    margin-left: auto;
    font-size: 11.5px;
    color: var(--muted);
    font-weight: 400;
    cursor: pointer;
    &:hover { color: var(--accent); }
  }
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 12.5px;
  .label { width: 64px; color: var(--ink-soft); }
  .track {
    flex: 1;
    height: 6px;
    background: var(--hairline-soft);
    border-radius: 3px;
    overflow: hidden;
  }
  .fill { height: 100%; background: var(--accent); border-radius: 3px; }
  .fill.b-phone { background: var(--info); }
  .fill.b-wechat { background: #16a34a; }
  .fill.b-visit { background: var(--warn); }
  .fill.b-email { background: #6d28d9; }
  .val { font-size: 12px; color: var(--muted); font-feature-settings: 'tnum' 1; min-width: 24px; text-align: right; }
}

// Sparkline
.sparkline {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 70px;
  padding: 6px 0;
}
.spark-bar {
  flex: 1;
  background: var(--accent-soft);
  border-radius: 3px 3px 0 0;
  position: relative;
  &.is-peak { background: var(--accent); }
  &:hover::after {
    content: attr(data-tip);
    position: absolute;
    bottom: 100%; left: 50%;
    transform: translate(-50%, -4px);
    background: var(--ink); color: white;
    font-size: 10.5px; padding: 2px 6px;
    border-radius: 3px;
    white-space: nowrap;
  }
}
.spark-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--subtle);
  margin-top: 6px;
  font-feature-settings: 'tnum' 1;
}

.tip {
  padding: 12px 14px;
  background: var(--accent-pale);
  border-radius: var(--radius);
  font-size: 12.5px;
  color: var(--ink-soft);
  line-height: 1.6;
  display: flex;
  gap: 10px;
  .ico { flex-shrink: 0; color: var(--accent); font-size: 14px; line-height: 1.4; }
}
</style>