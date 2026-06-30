<template>
  <div class="tab-content">
    <!-- 4 KPI 密集条 -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- 1 卡:实际回款 vs 理应回款 饼图 + 完成率 -->
    <ChartCard title="实际回款 vs 理应回款" :meta="`完成率 ${data.receivableCompare?.completionRate || '0%'}`">
      <div class="donut-wrap">
        <ChartDonut :data="donutData" :height="260" :show-legend="false" />
        <div class="donut-center">
          <div class="donut-rate">{{ data.receivableCompare?.completionRate || '0%' }}</div>
          <div class="donut-rate-sub">完成率</div>
        </div>
      </div>
      <div class="donut-legend">
        <div class="legend-item">
          <span class="dot dot-actual"></span>
          <span class="legend-label">实际回款</span>
          <span class="legend-val">¥ {{ formatAmount(data.receivableCompare?.actualAmount) }}</span>
        </div>
        <div class="legend-item">
          <span class="dot dot-gap"></span>
          <span class="legend-label">应回未回</span>
          <span class="legend-val">¥ {{ formatAmount(data.receivableCompare?.gapAmount) }}</span>
        </div>
        <div class="legend-item">
          <span class="dot dot-planned"></span>
          <span class="legend-label">理应回款</span>
          <span class="legend-val">¥ {{ formatAmount(data.receivableCompare?.plannedAmount) }}</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartDonut from '@/components/report/charts/ChartDonut.vue'

const props = defineProps({
  data: { type: Object, required: true }
})

/** 饼图两块:实际回款(深绿) + 应回未回(琥珀);理应回款仅展示在底部 legend */
const COLOR_ACTUAL  = '#166534' // 森林绿
const COLOR_GAP     = '#d97706' // 琥珀

const donutData = computed(() => {
  const c = props.data.receivableCompare || {}
  const actual = Number(c.actualAmount || 0)
  const gap = Math.max(0, Number(c.gapAmount || 0))
  // 任一为 0 时仍渲染(避免空饼图)
  if (actual === 0 && gap === 0) {
    return [{ name: '暂无数据', value: 1, color: '#e5e7eb' }]
  }
  return [
    { name: '实际回款', value: actual, color: COLOR_ACTUAL },
    { name: '应回未回', value: gap,    color: COLOR_GAP }
  ].filter(d => d.value > 0)
})

/** 金额格式化:1200000 → "1,200,000" */
function formatAmount(v) {
  const n = Number(v || 0)
  return n.toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }

/* 饼图 + 中心完成率叠加 */
.donut-wrap {
  position: relative;
  width: 100%;
}

.donut-center {
  position: absolute;
  top: 36%;
  left: 0;
  right: 0;
  text-align: center;
  pointer-events: none;
  transform: translateY(-50%);

  .donut-rate {
    font-size: 24px;
    font-weight: 600;
    color: var(--ink);
    font-family: var(--font-mono);
    line-height: 1.2;
  }
  .donut-rate-sub {
    font-size: 11px;
    color: var(--muted);
    margin-top: 2px;
    letter-spacing: 0.04em;
  }
}

/* 底部 3 行 legend(实际 / 应回未回 / 理应) */
.donut-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--hairline-soft);

  .legend-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
  }
  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  .dot-actual  { background: #166534; }
  .dot-gap     { background: #d97706; }
  .dot-planned { background: #6b7280; }
  .legend-label {
    color: var(--ink-soft);
    flex: 1;
  }
  .legend-val {
    color: var(--ink);
    font-weight: 500;
    font-family: var(--font-mono);
    font-variant-numeric: tabular-nums;
  }
}
</style>