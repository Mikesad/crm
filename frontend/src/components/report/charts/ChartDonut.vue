<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 环形饼图(部门业绩占比 / 跟进方式 / 客户等级)
 *
 * <p>data: [{ name, value, color? }] — 颜色默认森林绿阶</p>
 */
const props = defineProps({
  data: { type: Array, required: true },
  height: { type: Number, default: 240 },
  showLegend: { type: Boolean, default: true }
})

const chartRef = ref(null)
let chart = null

const COLORS = ['#166534', '#4ade80', '#86efac', '#bbf7d0', '#22c55e', '#16a34a', '#15803d']

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: props.showLegend ? {
      bottom: 0, left: 'center',
      textStyle: { color: 'var(--muted)', fontSize: 11, fontFamily: 'Inter' },
      icon: 'circle'
    } : { show: false },
    series: [{
      type: 'pie',
      radius: ['52%', '76%'],
      center: ['50%', props.showLegend ? '42%' : '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      labelLine: { show: false },
      data: props.data.map((d, i) => ({
        ...d,
        itemStyle: { color: d.color || COLORS[i % COLORS.length] }
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
