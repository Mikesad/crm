<template>
  <div class="tab-content">
    <!-- 4 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 2x2 图表:主分布 / 活跃度 / 等级 / 行业 -->
    <ChartGrid2x2>
      <ChartCard title="主分布" :meta="`按 ${dimLabel} · ${data.distribution?.length || 0} 项`">
        <ChartBar
          :data="(data.distribution || []).map(d => ({ name: d.key, value: d.count }))"
          unit=""
          :height="280"
        />
      </ChartCard>
      <ChartCard title="活跃度" meta="活跃 / 沉睡 / 公海 / 总数">
        <div class="activity-block">
          <div class="activity-row" v-for="r in activityRows" :key="r.label">
            <span class="dot" :class="r.cls"></span>
            <span class="label">{{ r.label }}</span>
            <span class="value mono">{{ r.value }}</span>
            <span class="percent">{{ r.percent }}</span>
          </div>
        </div>
      </ChartCard>
      <ChartCard title="客户等级分布" meta="A / B / C">
        <ChartDonut
          :data="(data.levelDistribution || []).map(d => ({ name: d.key, value: d.count }))"
          :height="240"
        />
      </ChartCard>
      <ChartCard title="行业分布" meta="行业 TOP 10">
        <ChartDonut
          :data="(data.regionDistribution || []).map(d => ({ name: d.key, value: d.count }))"
          :height="240"
        />
      </ChartCard>
    </ChartGrid2x2>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartGrid2x2 from '@/components/report/ChartGrid2x2.vue'
import ChartBar from '@/components/report/charts/ChartBar.vue'
import ChartDonut from '@/components/report/charts/ChartDonut.vue'

const props = defineProps({
  data: { type: Object, required: true },
  dim:  { type: String, default: 'industry' }
})

const dimLabel = computed(() => {
  return { industry: '行业', level: '等级', source: '来源' }[props.dim] || props.dim
})

const activityRows = computed(() => {
  const a = props.data.activity || {}
  return [
    { label: '总数',   value: a.total || 0,  percent: '',        cls: 'total' },
    { label: '活跃',   value: a.active || 0, percent: a.activePercent || '', cls: 'active' },
    { label: '沉睡',   value: a.dormant || 0, percent: a.dormantPercent || '', cls: 'dormant' },
    { label: '公海',   value: a.publicPool || 0, percent: a.publicPercent || '', cls: 'public' }
  ]
})
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }

.activity-block {
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-row {
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;

  .dot {
    width: 10px; height: 10px; border-radius: 50%;
    background: var(--muted);
  }
  .dot.active   { background: var(--accent); }
  .dot.dormant  { background: var(--warn); }
  .dot.public   { background: var(--info); }
  .dot.total    { background: var(--ink); }

  .label { color: var(--ink-soft); }
  .value { font-weight: 600; font-size: 15px; }
  .percent {
    color: var(--muted); font-size: 11px;
    font-variant-numeric: tabular-nums;
    min-width: 48px; text-align: right;
  }
}
</style>
