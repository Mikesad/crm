<template>
  <div class="dashboard">
    <!-- 原有 4 KPI 卡(销售/跟进概览) -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <el-card class="stat-card">
          <div class="stat">
            <div class="value">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
          <el-icon :size="32" :color="card.color"><component :is="card.icon" /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 原有 2 图表(销售漏斗 + 趋势) -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card header="销售漏斗">
          <div ref="funnelRef" style="height: 360px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card header="本月业绩趋势">
          <div ref="trendRef" style="height: 360px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 阶段五 commit 2:4 张报表入口卡(点击跳 /report?tab=xxx&range=month) -->
    <div class="section-eyebrow">报表中心 · 4 Tab 入口</div>
    <h2 class="section-title">数据可视化</h2>
    <p class="section-desc">点击下方任一卡片进入报表中心,筛选条件(本月)会随链接一起带过去。</p>
    <el-row :gutter="16" class="report-entry-row">
      <el-col v-for="r in reportEntries" :key="r.tab" :span="6">
        <div class="report-entry" @click="goReport(r.tab)">
          <div class="report-entry-head">
            <el-icon :size="18" :color="r.color"><component :is="r.icon" /></el-icon>
            <span class="report-entry-title">{{ r.title }}</span>
          </div>
          <div class="report-entry-desc">{{ r.desc }}</div>
          <div class="report-entry-foot">
            <span v-for="s in r.stats" :key="s.label" class="stat">
              <span class="stat-label">{{ s.label }}</span>
              <span class="stat-value mono">{{ s.value }}</span>
            </span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { FunnelChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import * as echarts from 'echarts'

use([CanvasRenderer, FunnelChart, LineChart, TitleComponent, TooltipComponent, LegendComponent])

const router = useRouter()

const cards = [
  { label: '今日新增线索', value: 12, icon: 'Aim', color: '#409eff' },
  { label: '跟进中客户', value: 86, icon: 'User', color: '#67c23a' },
  { label: '本月签约金额', value: '¥328,000', icon: 'Document', color: '#e6a23c' },
  { label: '待回款金额', value: '¥152,000', icon: 'Wallet', color: '#f56c6c' }
]

// 阶段五 commit 2:4 张报表入口卡(对应 4 Tab)
const reportEntries = [
  {
    tab: 'funnel',
    title: '销售漏斗 + 业绩',
    icon: 'TrendCharts', color: '#166534',
    desc: '5 阶段转化 + 6 月趋势 + 部门业绩 + 销售榜',
    stats: [
      { label: '本月销售', value: '¥2.47M' },
      { label: '新签合同', value: '11 单' }
    ]
  },
  {
    tab: 'customer',
    title: '客户分布',
    icon: 'PieChart', color: '#4ade80',
    desc: '行业 / 等级 / 来源 + 活跃沉睡公海',
    stats: [
      { label: '客户总数', value: '386' },
      { label: '公海占比', value: '14%' }
    ]
  },
  {
    tab: 'conversion',
    title: '跟进与转化率',
    icon: 'DataAnalysis', color: '#22c55e',
    desc: '5 阶段漏斗 + 跟进方式 + 高频榜',
    stats: [
      { label: '跟进总数', value: '428' },
      { label: '转化率', value: '17.8%' }
    ]
  },
  {
    tab: 'finance',
    title: '回款 / 财务',
    icon: 'Money', color: '#b45309',
    desc: '合同 / 已回款 / 账龄 4 桶 + 应收 TopN',
    stats: [
      { label: '合同总额', value: '¥8.2M' },
      { label: '已回款', value: '¥5.6M' }
    ]
  }
]

const funnelRef = ref()
const trendRef = ref()

function goReport(tab) {
  router.push({ path: '/report', query: { tab, range: 'month' } })
}

const renderCharts = () => {
  funnelRef.value && echarts.init(funnelRef.value).setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      left: '10%',
      width: '80%',
      data: [
        { value: 100, name: '需求分析' },
        { value: 80, name: '方案报价' },
        { value: 60, name: '商务谈判' },
        { value: 40, name: '赢单' }
      ]
    }]
  })
  trendRef.value && echarts.init(trendRef.value).setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      smooth: true,
      data: [120, 200, 150, 80, 70, 110],
      areaStyle: { opacity: 0.3 }
    }]
  })
}

onMounted(renderCharts)
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-card {
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .value {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
    .label {
      color: #888;
      font-size: 13px;
      margin-top: 4px;
    }
  }
}

.section-eyebrow {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin: 32px 0 4px;
}
.section-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; color: var(--ink); }
.section-desc  { font-size: 12.5px; color: var(--muted); margin: 0 0 14px; }

.report-entry-row { margin-top: 0; }

.report-entry {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.15s;
  height: 100%;

  &:hover {
    border-color: var(--accent);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(22, 101, 52, 0.06);
  }
}

.report-entry-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.report-entry-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.report-entry-desc {
  font-size: 11.5px;
  color: var(--muted);
  line-height: 1.55;
  margin-bottom: 12px;
  min-height: 36px;
}

.report-entry-foot {
  display: flex;
  gap: 16px;
  padding-top: 10px;
  border-top: 1px solid var(--hairline-soft);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 10px;
  color: var(--subtle);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
</style>
