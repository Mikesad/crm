<template>
  <div class="tab-content">
    <!-- 6 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:漏斗 / 趋势 / 部门 / 个人榜 -->
    <ChartGrid2x2>
      <ChartCard title="销售漏斗" :meta="`按阶段 · ${data.funnel?.length || 0} 阶段`">
        <ChartFunnel :data="(data.funnel || []).map(s => ({ name: s.stageName, value: s.count }))" :height="280" />
      </ChartCard>
      <ChartCard title="近 6 月销售趋势" meta="2026-01 ~ 06 · 月度">
        <ChartTrend :data="(data.trend || []).map(t => ({ date: t.date, value: Number(t.value) }))" :height="280" />
      </ChartCard>
      <ChartCard title="部门业绩" meta="按销售总额">
        <ChartBar
          :data="(data.departmentPerformers || []).map(d => ({ name: d.deptName, value: Number(d.amount) }))"
          :height="240"
        />
      </ChartCard>
      <ReportDataTable
        title="销售个人榜 · TOP N"
        :meta="`按销售总额`"
        :columns="[
          { key: 'rank',   title: '排名' },
          { key: 'name',   title: '销售' },
          { key: 'count',  title: '单数',  align: 'right' },
          { key: 'amount', title: '销售总额', align: 'right' }
        ]"
        :rows="data.topPerformers || []"
      >
        <template #cell-amount="{ row }">
          <span class="mono">¥ {{ formatAmount(row.amount) }}</span>
        </template>
      </ReportDataTable>
    </ChartGrid2x2>
  </div>
</template>

<script setup>
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartGrid2x2 from '@/components/report/ChartGrid2x2.vue'
import ReportDataTable from '@/components/report/ReportDataTable.vue'
import ChartFunnel from '@/components/report/charts/ChartFunnel.vue'
import ChartTrend from '@/components/report/charts/ChartTrend.vue'
import ChartBar from '@/components/report/charts/ChartBar.vue'

defineProps({
  data: { type: Object, required: true }
})

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
</style>
