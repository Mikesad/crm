<template>
  <div class="tab-content">
    <!-- 6 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:漏斗 / 趋势 / 部门 / 个人榜 -->
    <ChartGrid2x2>
      <ChartCard title="销售漏斗" :meta="`按阶段 · ${data.funnel?.length || 0} 阶段`">
        <ChartFunnel :data="(data.funnel || []).map(s => ({ name: s.stageName, value: s.count }))" :height="280" />
      </ChartCard>

      <ChartCard title="近 6 月销售趋势">
        <!-- 阶段八 P6:chip tab 切换"合同业绩 / 实际回款" -->
        <template #header-right>
          <div class="mode-tabs">
            <button :class="{ active: trendMode === 'contract' }" @click="trendMode = 'contract'">合同业绩</button>
            <button :class="{ active: trendMode === 'received' }" @click="trendMode = 'received'">实际回款</button>
          </div>
        </template>
        <ChartTrend :data="trendData" :height="280" />
      </ChartCard>

      <ChartCard title="部门业绩">
        <!-- 阶段八 C2-D4:chip tab 切换"合同业绩 / 实际回款" -->
        <template #header-right>
          <div class="mode-tabs">
            <button :class="{ active: deptMode === 'contract' }" @click="deptMode = 'contract'">合同业绩</button>
            <button :class="{ active: deptMode === 'received' }" @click="deptMode = 'received'">实际回款</button>
          </div>
        </template>
        <ChartBar
          :data="deptChartData"
          :height="240"
        />
        <!-- 头部最大部门占比(随 chip tab 切换) -->
        <div class="dept-foot mono">
          <template v-if="topDept">
            <span class="foot-name">{{ topDept.deptName }}</span>
            <span class="foot-pct">{{ topDeptPct }}</span>
          </template>
          <template v-else>—</template>
        </div>
      </ChartCard>

      <ReportDataTable
        title="销售个人榜 · TOP N"
        :columns="performerColumns"
        :rows="performerRows"
      >
        <template #cell-rank="{ row }">
          <span class="rank" :class="rankClass(row.rank)">{{ row.rank }}</span>
        </template>
        <template #cell-name="{ row }">
          <span class="performer-name">{{ row.name }}</span>
        </template>
        <template #cell-count="{ row }">
          <span class="num-cell">{{ row.count ?? 0 }}</span>
        </template>
        <template #cell-amount="{ row }">
          <span class="num-cell">¥ {{ formatAmount(performerAmount(row)) }}</span>
        </template>
      </ReportDataTable>
    </ChartGrid2x2>

    <!-- 销售个人榜 chip tab(放在表格下方,跟部门业绩一致风格) -->
    <div class="performer-mode">
      <div class="mode-tabs mode-tabs-lg">
        <button :class="{ active: performerMode === 'contract' }" @click="performerMode = 'contract'">合同业绩</button>
        <button :class="{ active: performerMode === 'received' }" @click="performerMode = 'received'">实际回款</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartGrid2x2 from '@/components/report/ChartGrid2x2.vue'
import ReportDataTable from '@/components/report/ReportDataTable.vue'
import ChartFunnel from '@/components/report/charts/ChartFunnel.vue'
import ChartTrend from '@/components/report/charts/ChartTrend.vue'
import ChartBar from '@/components/report/charts/ChartBar.vue'

const props = defineProps({
  data: { type: Object, required: true }
})

/**
 * 阶段八 chip tab 三处独立(部门业绩 / 销售个人榜 / 6月趋势),默认全部合同业绩口径
 * - deptMode: 部门业绩卡片(影响柱状图 + 头部最大占比)
 * - performerMode: 销售个人榜表格(影响 count + amount 字段)
 * - trendMode: 6 月趋势卡片(影响 ChartTrend 的 data 源)
 */
const deptMode = ref('contract')
const performerMode = ref('contract')
const trendMode = ref('contract')

/* ============== P1 部门业绩 ============== */
const deptChartData = computed(() => {
  const rows = props.data.departmentPerformers || []
  return rows.map(d => ({
    name: d.deptName,
    value: Number(deptMode.value === 'received' ? d.receivedAmount : d.amount)
  }))
})
const topDept = computed(() => {
  const rows = props.data.departmentPerformers || []
  return rows.length > 0 ? rows[0] : null
})
const topDeptPct = computed(() => {
  if (!topDept.value) return '0%'
  return deptMode.value === 'received'
    ? (topDept.value.receivedPercent || '0%')
    : (topDept.value.percent || '0%')
})

/* ============== P6 6 月趋势 ============== */
const trendData = computed(() => {
  const arr = trendMode.value === 'received'
    ? (props.data.trendReceived || [])
    : (props.data.trend || [])
  return arr.map(t => ({ date: t.date, value: Number(t.value) }))
})

/* ============== P3 销售个人榜 ============== */
const performerColumns = computed(() => [
  { key: 'rank',   title: '排名',     align: 'left'  },
  { key: 'name',   title: '销售',     align: 'left'  },
  { key: 'count',  title: '单数',     align: 'right' },
  { key: 'amount', title: performerMode.value === 'received' ? '实际回款' : '销售总额', align: 'right' }
])
const performerRows = computed(() =>
  performerMode.value === 'received'
    ? (props.data.topPerformersReceived || [])
    : (props.data.topPerformers || [])
)
const performerAmount = (row) =>
  performerMode.value === 'received'
    ? (row.receivedAmount || row.amount)
    : (row.amount || row.receivedAmount)

/* ============== 辅助 ============== */
/** 排名 1/2/3 用金/银/铜高亮(沿用 ReportDataTable 默认 rank 样式) */
function rankClass(rank) {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}

/** 金额格式化:980000 → "980,000" */
function formatAmount(v) {
  if (!v) return '0'
  return Number(v).toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
.tab-content {
  width: 100%;
}

.mode-tabs {
  display: flex;
  gap: 0;
  border: 1px solid var(--hairline);
  border-radius: 4px;
  overflow: hidden;

  button {
    padding: 3px 10px;
    font-size: 11.5px;
    border: none;
    background: var(--surface);
    color: var(--muted);
    cursor: pointer;
    font-family: inherit;
    transition: all 0.12s;

    &:hover:not(.active) { color: var(--ink-soft); background: var(--bg); }
    &.active {
      background: var(--ink);
      color: white;
      font-weight: 500;
    }
    & + button { border-left: 1px solid var(--hairline); }
  }
}

/* 销售个人榜 chip tab 加大尺寸(独立行,跟表格分离) */
.performer-mode {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
.mode-tabs-lg button {
  padding: 5px 14px;
  font-size: 12px;
}

.dept-foot {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 11px;
  color: var(--muted);

  .foot-name { color: var(--ink-soft); font-weight: 500; }
  .foot-pct { color: var(--accent); font-weight: 600; }
}

/* P2 数字右对齐 + tabular-nums(销售个人榜"单数"和"金额"列) */
:deep(.num-cell) {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
  display: inline-block;
  width: 100%;
  text-align: right;
}
:deep(.performer-name) {
  font-weight: 500;
  color: var(--ink);
}
:deep(.rank) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--bg);
  color: var(--muted);
  font-size: 10.5px;
  font-weight: 600;
  margin-right: 6px;
  font-family: var(--font-mono);
}
:deep(.rank.gold)   { background: #fef3c7; color: #92400e; }
:deep(.rank.silver) { background: #f1f5f9; color: #475569; }
:deep(.rank.bronze) { background: #fce7f3; color: #9d174d; }
</style>