<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 销售漏斗图(5 阶段)
 * <p>沿用阶段二 Dashboard 的森林绿色阶(浅 → 深):#bbf7d0 / #86efac / #4ade80 / #22c55e / #166534</p>
 */
const props = defineProps({
  data: { type: Array, required: true },  // [{ name, value }]
  height: { type: Number, default: 280 }
})

const chartRef = ref(null)
let chart = null

const COLORS = ['#bbf7d0', '#86efac', '#4ade80', '#22c55e', '#166534']

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}<br/>占比 {d}%' },
    series: [{
      type: 'funnel',
      left: '10%', right: '10%', top: 8, bottom: 8,
      sort: 'descending', gap: 4,
      label: { show: true, position: 'inside', color: '#fff', fontWeight: 500, fontSize: 12 },
      labelLine: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: props.data.map((d, i) => ({
        ...d,
        itemStyle: { color: COLORS[Math.min(i, COLORS.length - 1)] }
      }))
    }]
  }, true)
}

onMounted(() => nextTick(render))
watch(() => props.data, render, { deep: true })
onBeforeUnmount(() => { chart?.dispose(); chart = null })
window.addEventListener('resize', () => chart?.resize())
</script>

<style lang="scss" scoped>
.chart { width: 100%; }
</style>
