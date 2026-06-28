<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 横向柱状图(回款方式 / 账龄 / 客户来源 等)
 *
 * <p>data: [{ name, value, color? }]</p>
 */
const props = defineProps({
  data: { type: Array, required: true },
  height: { type: Number, default: 240 }
})

const chartRef = ref(null)
let chart = null

const COLORS = ['#166534', '#4ade80', '#86efac', '#bbf7d0', '#22c55e']

const AXIS_LABEL = { color: 'var(--muted)', fontSize: 10.5, fontFamily: 'Inter' }
const SPLIT_LINE = { lineStyle: { color: 'var(--hairline)', type: 'dashed' } }

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const sorted = [...props.data].sort((a, b) => Number(a.value) - Number(b.value))
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 8, top: 8, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: AXIS_LABEL,
      splitLine: SPLIT_LINE
    },
    yAxis: {
      type: 'category',
      data: sorted.map(d => d.name),
      axisLine: { lineStyle: { color: 'var(--hairline)' } },
      axisTick: { show: false },
      axisLabel: { color: 'var(--ink)', fontSize: 11, fontFamily: 'Inter' }
    },
    series: [{
      type: 'bar',
      barWidth: 12,
      data: sorted.map((d, i) => ({
        value: d.value,
        itemStyle: { color: d.color || COLORS[i % COLORS.length], borderRadius: [0, 6, 6, 0] }
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
