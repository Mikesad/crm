<template>
  <div class="tab-content">
    <!-- 4 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:转化漏斗 / 高频跟进人 / 团队 vs 全公司 / 6 月频次 -->
    <ChartGrid2x2>
      <ChartCard title="阶段转化漏斗" :meta="`5 阶段 · ${data.stageFunnel?.length || 0}`">
        <ChartFunnel
          :data="(data.stageFunnel || []).map(s => ({ name: s.stageName, value: s.count }))"
          :height="280"
        />
      </ChartCard>
      <ReportDataTable
        title="高频跟进人 · TOP N"
        meta="按跟进条数"
        :columns="[
          { key: 'rank',  title: '排名' },
          { key: 'name',  title: '跟进人' },
          { key: 'count', title: '条数', align: 'right' }
        ]"
        :rows="data.topPerformers || []"
      />
      <ChartCard title="团队 vs 全公司" meta="5 阶段转化率对比">
        <div class="compare-table">
          <div class="compare-row compare-head">
            <span class="label">阶段</span>
            <span class="v">本团队</span>
            <span class="v">全公司</span>
          </div>
          <div v-for="(stage, i) in stages" :key="i" class="compare-row">
            <span class="label">{{ stage }}</span>
            <span class="v mono">{{ getCompareVal('team', i) }}</span>
            <span class="v mono">{{ getCompareVal('company', i) }}</span>
          </div>
        </div>
      </ChartCard>
      <ChartCard title="6 月跟进频次" meta="按月统计">
        <ChartTrend
          :data="(data.trend || []).map(t => ({ date: t.date, value: Number(t.value) }))"
          unit=""
          :height="240"
        />
      </ChartCard>
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

const props = defineProps({
  data: { type: Object, required: true }
})

const stages = ['新建线索', '需求分析', '方案报价', '商务谈判', '赢单']
const stageFields = ['stage1Lead', 'stage2Analysis', 'stage3Quote', 'stage4Negotiate', 'stage5Win']

function getCompareVal(group, idx) {
  const compare = (props.data || {}).teamVsCompany || []
  const row = compare.find(r => r.group === group)
  if (!row) return '-'
  return row[stageFields[idx]] || '-'
}
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }

.compare-table {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 4px 0;
  font-size: 12px;
}

.compare-row {
  display: grid;
  grid-template-columns: 1fr 80px 80px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--hairline-soft);

  &:last-child { border-bottom: none; }

  &.compare-head {
    font-size: 10.5px;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .label { color: var(--ink-soft); }
  .v { text-align: right; font-variant-numeric: tabular-nums; color: var(--ink); }
}
</style>
