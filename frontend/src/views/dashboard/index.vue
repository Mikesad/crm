<template>
  <div class="dashboard">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { FunnelChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'

use([CanvasRenderer, FunnelChart, LineChart, TitleComponent, TooltipComponent, LegendComponent])

const cards = [
  { label: '今日新增线索', value: 12, icon: 'Aim', color: '#409eff' },
  { label: '跟进中客户', value: 86, icon: 'User', color: '#67c23a' },
  { label: '本月签约金额', value: '¥328,000', icon: 'Document', color: '#e6a23c' },
  { label: '待回款金额', value: '¥152,000', icon: 'Wallet', color: '#f56c6c' }
]

const funnelRef = ref()
const trendRef = ref()

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
</style>
