<template>
  <div class="tab-content">
    <!-- 4 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:回款趋势 / 月度堆叠 / 账龄 / 应收 TopN -->
    <ChartGrid2x2>
      <ChartCard title="回款趋势" meta="合同 / 已回款 / 预测(3 series)">
        <ChartTrend
          :data="data.trend || []"
          :series="trendSeries"
          unit="¥"
          :height="280"
        />
      </ChartCard>
      <ChartCard title="月度回款堆叠" meta="已回款 / 计划未回款">
        <ChartStacked
          :categories="stackedCategories"
          :series="stackedSeries"
          :height="280"
        />
      </ChartCard>
      <ChartCard title="账龄分布" meta="0-30 / 31-60 / 61-90 / 90+ 天">
        <ChartBarH
          :data="(data.agingBuckets || []).map(b => ({
            name: b.label,
            value: b.count
          }))"
          :height="240"
        />
      </ChartCard>
      <ReportDataTable
        title="应收 Top N 客户"
        meta="按未回款金额"
        :columns="[
          { key: 'rank',   title: '排名' },
          { key: 'name',   title: '客户' },
          { key: 'count',  title: '合同数', align: 'right' },
          { key: 'amount', title: '未回款金额', align: 'right' }
        ]"
        :rows="data.topDebtors || []"
      >
        <template #cell-amount="{ row }">
          <span class="mono">¥ {{ formatAmount(row.amount) }}</span>
        </template>
      </ReportDataTable>
    </ChartGrid2x2>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartGrid2x2 from '@/components/report/ChartGrid2x2.vue'
import ReportDataTable from '@/components/report/ReportDataTable.vue'
import ChartTrend from '@/components/report/charts/ChartTrend.vue'
import ChartStacked from '@/components/report/charts/ChartStacked.vue'
import ChartBarH from '@/components/report/charts/ChartBarH.vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const TREND_COLORS = { contract: '#166534', received: '#4ade80', predicted: '#bbf7d0' }
const TREND_NAMES  = { contract: '合同', received: '已回款', predicted: '预测' }

const trendSeries = computed(() => {
  const keys = [...new Set((props.data.trend || []).map(d => d.seriesKey).filter(Boolean))]
  if (keys.length === 0) keys.push('contract')
  return keys.map(k => ({
    key: k,
    name: TREND_NAMES[k] || k,
    color: TREND_COLORS[k] || '#166534'
  }))
})

const stackedCategories = computed(() => {
  const months = [...new Set((props.data.trend || []).map(d => d.date))]
  return months
})

const stackedSeries = computed(() => {
  const months = stackedCategories.value
  const seriesMap = {}
  for (const d of props.data.trend || []) {
    if (!seriesMap[d.seriesKey]) seriesMap[d.seriesKey] = new Array(months.length).fill(0)
    const idx = months.indexOf(d.date)
    if (idx >= 0) seriesMap[d.seriesKey][idx] = Number(d.value) || 0
  }
  return Object.entries(seriesMap).map(([k, data]) => ({
    name: TREND_NAMES[k] || k,
    data,
    color: TREND_COLORS[k] || '#166534'
  }))
})

function formatAmount(v) {
  if (!v) return '0'
  return Number(v).toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }
</style>
