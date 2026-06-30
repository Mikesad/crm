<template>
  <div class="report-center">
    <!-- 面包屑条(替代了原 48px 顶栏,只 40px) -->
    <div class="crumb-bar">
      <div class="crumbs">可视化</div>
      <div class="crumb-right">
        <span class="updated">数据更新于 {{ updatedLabel }}</span>
        <button class="btn-link" @click="onClearCache" :disabled="clearing">↻ 刷新缓存</button>
      </div>
    </div>

    <div class="main">
      <!-- Page header(阶段八 commit 7,对齐其他模块的 page-title 醒目标题风格) -->
      <div class="page-header">
        <div>
          <div class="page-title">报表中心</div>
          <div class="page-sub">{{ nowLabel }} · 销售漏斗 + 客户分布 + 跟进与转化率 + 回款/财务</div>
        </div>
      </div>

      <!-- Filterbar(P20:去掉"全部销售"下拉,只保留时间范围 + 部门) -->
      <div class="filterbar">
        <div class="range-tabs">
          <button
            v-for="r in rangeOptions"
            :key="r.value"
            class="range-tab"
            :class="{ active: filters.range === r.value }"
            @click="onRangeChange(r.value)"
          >{{ r.label }}</button>
        </div>
        <select v-model="filters.deptId" class="filter-select" @change="onFilterChange">
          <option :value="null">全部部门</option>
          <option v-for="d in depts" :key="d.id" :value="d.id">{{ d.name }}</option>
        </select>
        <div class="filter-spacer"></div>
        <div class="meta-info">下次刷新 <span class="mono">{{ nextRefreshLabel }}</span></div>
      </div>

      <!-- 4 Tab chip 切换 -->
      <div class="report-tabs">
        <div
          v-for="t in tabs"
          :key="t.key"
          class="report-tab"
          :class="{ active: activeTab === t.key }"
          @click="onTabChange(t.key)"
        >{{ t.label }}</div>
      </div>

      <!-- Tab 内容:动态挂载 + 加载状态 -->
      <div v-loading="loading" class="tab-pane">
        <ReportFunnel v-if="activeTab === 'funnel'" :data="tabData" />
        <ReportCustomer v-else-if="activeTab === 'customer'" :data="tabData" :dim="filters.dim" />
        <ReportConversion v-else-if="activeTab === 'conversion'" :data="tabData" />
        <ReportFinance v-else-if="activeTab === 'finance'" :data="tabData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useReportStore } from '@/store/report'
import { getFilterDepts, getFilterUsers } from '@/api/report'
import ReportFunnel from './tabs/ReportFunnel.vue'
import ReportCustomer from './tabs/ReportCustomer.vue'
import ReportConversion from './tabs/ReportConversion.vue'
import ReportFinance from './tabs/ReportFinance.vue'

const store = useReportStore()

const activeTab = ref(store.activeTab || 'funnel')
const filters = reactive({
  range: store.filters.range || 'month',
  deptId: store.filters.deptId ?? null,
  userId: store.filters.userId ?? null
})

const tabs = [
  { key: 'funnel',     label: '销售漏斗 + 业绩' },
  { key: 'customer',   label: '客户分布' },
  { key: 'conversion', label: '跟进与转化率' },
  { key: 'finance',    label: '回款 / 财务' }
]

const rangeOptions = [
  { value: 'today',   label: '今日' },
  { value: 'week',    label: '本周' },
  { value: 'month',   label: '本月' },
  { value: 'quarter', label: '本季' },
  { value: 'year',    label: '本年' }
]

const depts = ref([])
const users = ref([])
const tabData = ref({})
const loading = ref(false)
const clearing = ref(false)
const lastUpdated = ref(0)
const now = ref(new Date())
let timer = null

const updatedLabel = computed(() => {
  if (!lastUpdated.value) return '—'
  const d = new Date(lastUpdated.value)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

const nowLabel = computed(() => now.value.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }))

const nextRefreshLabel = computed(() => {
  if (!lastUpdated.value) return '—'
  const next = new Date(lastUpdated.value + 5 * 60 * 1000)
  return next.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

async function loadTabData() {
  loading.value = true
  try {
    const data = await store.fetchReport(activeTab.value, {})
    tabData.value = data || {}
    lastUpdated.value = Date.now()
  } catch (e) {
    console.error('[ReportCenter] loadTabData failed', e)
    tabData.value = {}
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  try {
    const { data: d1 } = await getFilterDepts()
    depts.value = d1 || []
  } catch { depts.value = [] }
  try {
    if (filters.deptId) {
      const { data: d2 } = await getFilterUsers({ deptId: filters.deptId })
      users.value = d2 || []
    }
  } catch { users.value = [] }
}

function onRangeChange(range) {
  filters.range = range
  onFilterChange()
}

function onFilterChange() {
  store.setFilters({ range: filters.range, deptId: filters.deptId, userId: filters.userId })
  loadTabData()
  loadFilters()  // deptId 切换后刷新 users
}

function onTabChange(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  store.setActiveTab(key)
  loadTabData()
}

async function onClearCache() {
  clearing.value = true
  try {
    await store.clearAll()
    await loadTabData()
  } finally {
    clearing.value = false
  }
}

onMounted(async () => {
  // 从 query 读初始 tab(如 /report?tab=finance)
  const params = new URLSearchParams(window.location.hash.split('?')[1] || '')
  const tab = params.get('tab')
  if (tab && tabs.some(t => t.key === tab)) {
    activeTab.value = tab
  }
  await loadFilters()
  await loadTabData()
  // 时钟 + 自动刷新(每 60s 拉一次数据;缓存命中则不重算)
  timer = setInterval(() => { now.value = new Date() }, 60 * 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.report-center { width: 100%; }

/* 40px 极简面包屑条(替代了原 48px 顶栏) */
.crumb-bar {
  display: flex;
  align-items: center;
  height: 40px;
  background: var(--surface);
  border-bottom: 1px solid var(--hairline);
  padding: 0 24px;
  position: sticky; top: 0; z-index: 10;
}

.crumbs {
  font-size: 12px;
  color: var(--subtle);
  letter-spacing: 0.02em;

  .sep { margin: 0 6px; }
  .current { color: var(--ink-soft); font-weight: 500; }
}

.crumb-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;

  .updated {
    font-size: 11.5px;
    color: var(--muted);
  }

  .btn-link {
    background: none;
    border: none;
    color: var(--accent);
    font-size: 12px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: var(--radius);

    &:hover:not(:disabled) { background: var(--accent-pale); }
    &:disabled { color: var(--subtle); cursor: not-allowed; }
  }
}

.main { padding: 18px 24px 40px; }

.page-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-title { font-size: 16px; font-weight: 600; color: var(--ink); }
.page-sub { font-size: 12px; color: var(--muted); margin-top: 2px; }
.page-meta { font-size: 12px; color: var(--muted); }

.filterbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.range-tabs { display: flex; gap: 0; }
.range-tab {
  padding: 5px 12px;
  font-size: 12px;
  border: 1px solid var(--hairline);
  background: var(--surface);
  cursor: pointer;
  color: var(--muted);
  font-family: inherit;
  transition: all 0.12s;

  &:first-child { border-radius: var(--radius) 0 0 var(--radius); }
  &:last-child  { border-radius: 0 var(--radius) var(--radius) 0; }
  & + & { border-left: none; }
  &:hover:not(.active) { color: var(--ink); }
  &.active { background: var(--ink); color: white; border-color: var(--ink); }
}

.filter-select {
  height: 28px;
  padding: 0 10px;
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  font-size: 12px;
  color: var(--ink-soft);
  font-family: inherit;
  cursor: pointer;
}

.filter-spacer { flex: 1; }

.meta-info {
  font-size: 11.5px;
  color: var(--muted);
  .mono { color: var(--ink-soft); }
}

.report-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--hairline);
}

.report-tab {
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  border-radius: var(--radius) var(--radius) 0 0;
  transition: all 0.12s;

  &:hover { color: var(--ink); background: var(--bg); }
  &.active {
    color: var(--ink);
    border-bottom-color: var(--accent);
    background: var(--surface);
    font-weight: 600;
  }
}

.tab-pane { min-height: 400px; }
</style>
