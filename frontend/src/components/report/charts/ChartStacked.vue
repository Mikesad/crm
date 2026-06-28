<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 堆叠柱状图(回款/计划堆叠)
 *
 * <p>categories: x 轴标签数组 (e.g. ['2026-01', '2026-02', ...])</p>
 * <p>series: [{ name, data: [v1, v2, ...] }] — 多个 series 累加堆叠</p>
 */
const props = defineProps({
  categories: { type: Array, required: true },
  series:     { type: Array, required: true },   // [{ name, data, color? }]
  height:     { type: Number, default: 240 }
})

const chartRef = ref(null)
let chart = null

const COLORS = ['#166534', '#4ade80', '#86efac', '#bbf7d0']
const AXIS_LABEL = { color: 'var(--muted)', fontSize: 10.5, fontFamily: 'Inter' }
const SPLIT_LINE = { lineStyle: { color: 'var(--hairline)', type: 'dashed' } }

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, textStyle: AXIS_LABEL, icon: 'circle' },
    grid: { left: 8, right: 8, top: 8, bottom: 32, containLabel: true },
    xAxis: {
      type: 'category',
      data: props.categories,
      axisLine: { lineStyle: { color: 'var(--hairline)' } },
      axisLabel: AXIS_LABEL,
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, formatter: v => '¥' + (v / 1000) + 'K' },
      splitLine: SPLIT_LINE
    },
    series: props.series.map((s, i) => ({
      name: s.name,
      type: 'bar',
      stack: 'total',
      barWidth: 18,
      itemStyle: { color: s.color || COLORS[i % COLORS.length] },
      data: s.data
    }))
  }, true)
}

onMounted(() => nextTick(render))
watch(() => [props.categories, props.series], render, { deep: true })
onBeforeUnmount(() => { chart?.dispose(); chart = null })
window.addEventListener('resize', () => chart?.resize())
</script>

<style lang="scss" scoped>
.chart { width: 100%; }
</style>
