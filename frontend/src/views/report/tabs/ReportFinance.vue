<template>
  <div class="tab-content">
    <!-- 2 KPI 密集条(phase8 commit1:删了 未回款 + 逾期率) -->
    <KpiStrip :kpis="data.kpis || []" />

    <!-- phase8 commit1:2 张图 (回款率 + 合同状态分布) -->
    <el-row :gutter="14" class="charts-row">
      <el-col :xs="24" :md="12">
        <ChartCard title="回款率" :meta="`${rangeLabel}完成率 ${data.receivableCompare?.completionRate || '0%'}`">
          <div class="donut-wrap">
            <ChartDonut :data="donutData" :height="260" :show-legend="false" />
            <div class="donut-center">
              <div class="donut-rate">{{ data.receivableCompare?.completionRate || '0%' }}</div>
              <div class="donut-rate-sub">回款率 = 已回款 / 合同总额</div>
            </div>
          </div>
          <div class="mini-legend">
            <div class="ml-item">
              <span class="dot dot-actual"></span>
              <span class="ml-label">已回款(分子)</span>
              <span class="ml-val">¥ {{ formatAmount(data.receivableCompare?.actualAmount) }}</span>
            </div>
            <div class="ml-item">
              <span class="dot dot-planned"></span>
              <span class="ml-label">合同总额(分母)</span>
              <span class="ml-val">¥ {{ formatAmount(data.receivableCompare?.plannedAmount) }}</span>
            </div>
            <div class="ml-item">
              <span class="dot dot-gap"></span>
              <span class="ml-label">未回款(合同-已回)</span>
              <span class="ml-val">¥ {{ formatAmount(data.receivableCompare?.gapAmount) }}</span>
            </div>
          </div>
        </ChartCard>
      </el-col>

      <el-col :xs="24" :md="12">
        <ChartCard title="合同状态分布" :meta="`全量合同(status 0/1/2/3)`">
          <div class="bar-wrap">
            <ChartBarH :data="statusBar" :height="260" />
          </div>
          <div class="mini-legend">
            <div v-for="(s, i) in statusBar" :key="s.name" class="ml-item">
              <span class="dot" :style="{ background: STATUS_COLORS[i] }"></span>
              <span class="ml-label">{{ s.name }}</span>
              <span class="ml-val">{{ s.count }} 份 · ¥ {{ formatAmount(s.amount) }}</span>
            </div>
          </div>
        </ChartCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import KpiStrip from '@/components/report/KpiStrip.vue'
import ChartCard from '@/components/report/ChartCard.vue'
import ChartDonut from '@/components/report/charts/ChartDonut.vue'
import ChartBarH from '@/components/report/charts/ChartBarH.vue'

const props = defineProps({
  data: { type: Object, required: true }
})

/** 当前 range 标签(从 kpi.footnote 推断,缺省 '本月') */
const rangeLabel = computed(() => {
  const kpi0 = (props.data.kpis || [])[0]
  return kpi0?.footnote || '本月'
})

/* ======== 图 1:回款率 donut(中央大数字 + 环) ======== */
const COLOR_ACTUAL  = '#166534' // 森林绿 - 已回款
const COLOR_GAP     = '#d97706' // 琥珀 - 未回款

const donutData = computed(() => {
  const c = props.data.receivableCompare || {}
  const actual = Number(c.actualAmount || 0)
  const gap = Math.max(0, Number(c.gapAmount || 0))
  if (actual === 0 && gap === 0) {
    return [{ name: '暂无数据', value: 1, color: '#e5e7eb' }]
  }
  return [
    { name: '已回款', value: actual, color: COLOR_ACTUAL },
    { name: '未回款', value: gap,    color: COLOR_GAP }
  ].filter(d => d.value > 0)
})

/* ======== 图 2:合同状态分布(横向柱图 + 4 状态) ======== */
const STATUS_COLORS = ['#fbbf24', '#166534', '#9ca3af', '#dc2626'] // 审批中(琥珀) / 执行中(绿) / 已结束(灰) / 已作废(红)
const statusBar = computed(() => {
  const list = props.data.contractStatusDistribution || []
  return list.map((s, i) => ({
    name: s.key,
    value: Number(s.count || 0),
    count: s.count,
    amount: s.amount,
    color: STATUS_COLORS[i] || '#9ca3af'
  }))
})

/** 金额格式化:1200000 → "1,200,000" */
function formatAmount(v) {
  const n = Number(v || 0)
  return n.toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
.tab-content { width: 100%; }

/* 2 列布局 */
.charts-row { margin-top: 14px; }
.charts-row > [class*="el-col"] { margin-bottom: 14px; }

/* 饼图 + 中心完成率叠加(图 1) */
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
    font-size: 22px;
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

/* 通用 legend(图 1 和图 3 共用) */
.mini-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--hairline-soft);

  .ml-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 11.5px;
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
  .ml-label {
    color: var(--ink-soft);
    flex: 1;
  }
  .ml-val {
    color: var(--ink);
    font-weight: 500;
    font-family: var(--font-mono);
    font-variant-numeric: tabular-nums;
  }
}

/* 图 3 柱图 */
.bar-wrap {
  width: 100%;
  padding: 4px 0;
}
</style>