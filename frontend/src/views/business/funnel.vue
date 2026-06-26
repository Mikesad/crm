<template>
  <div class="page">
    <el-card header="销售漏斗">
      <div ref="chartRef" style="height: 480px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref()
onMounted(() => {
  echarts.init(chartRef.value).setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      sort: 'descending',
      gap: 2,
      label: { show: true, position: 'inside' },
      data: [
        { value: 100, name: '需求分析' },
        { value: 80, name: '方案报价' },
        { value: 60, name: '商务谈判' },
        { value: 40, name: '赢单' }
      ]
    }]
  })
})
</script>
