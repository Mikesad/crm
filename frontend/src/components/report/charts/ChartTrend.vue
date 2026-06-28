<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 趋势折线/面积图(支持单 series / 多 series)
 *
 * <p>data 格式: [{ date, value, seriesKey? }] 或多 series 时 [{ date, values: { k1: v1, k2: v2 } }]</p>
 * <p>多 series 模式:每行代表一个时间点(粒度月/日),values 包含多个 key</p>
 */
const props = defineProps({
  /** 单 series 模式:数组;多 series 模式:由 caller 自己 setOption,本组件不处理 */
  data: { type: Array, default: () => [] },
  /** 多 series 配置(可选),[{ key, name, color }] */
  series: { type: Array, default: () => [] },
  height: { type: Number, default: 280 },
  /** y 轴单位(用于 formatter,默认 ¥ K) */
  unit: { type: String, default: '¥' }
})

const chartRef = ref(null)
let chart = null

const AXIS_LABEL = { color: 'var(--muted)', fontSize: 10.5, fontFamily: 'Inter' }
const SPLIT_LINE = { lineStyle: { color: 'var(--hairline)', type: 'dashed' } }

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  if (props.series.length === 0) {
    // 单 series
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 8, top: 16, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: props.data.map(d => d.date),
        axisLine: { lineStyle: { color: 'var(--hairline)' } },
        axisLabel: AXIS_LABEL,
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { ...AXIS_LABEL, formatter: v => props.unit + (v / 1000) + 'K' },
        splitLine: SPLIT_LINE
      },
      series: [{
        type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, color: 'var(--accent)' },
        itemStyle: { color: 'var(--accent)', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: 'rgba(22,101,52,0.2)' },
            { offset: 1, color: 'rgba(22,101,52,0)' }
          ]}
        },
        data: props.data.map(d => d.value)
      }]
    }, true)
  } else {
    // 多 series(按 seriesKey 分组)
    const byKey = {}
    for (const d of props.data) {
      const sk = d.seriesKey || 'default'
      if (!byKey[sk]) byKey[sk] = []
      byKey[sk].push(d)
    }
    const seriesArr = props.series.map(s => ({
      name: s.name,
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { width: 2, color: s.color || 'var(--accent)' },
      itemStyle: { color: s.color || 'var(--accent)' },
      data: (byKey[s.key] || []).map(d => d.value)
    }))
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, textStyle: AXIS_LABEL, icon: 'circle' },
      grid: { left: 8, right: 8, top: 16, bottom: 32, containLabel: true },
      xAxis: {
        type: 'category',
        data: [...new Set(props.data.map(d => d.date))],
        axisLine: { lineStyle: { color: 'var(--hairline)' } },
        axisLabel: AXIS_LABEL,
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { ...AXIS_LABEL, formatter: v => props.unit + (v / 1000) + 'K' },
        splitLine: SPLIT_LINE
      },
      series: seriesArr
    }, true)
  }
}

onMounted(() => nextTick(render))
watch(() => [props.data, props.series], render, { deep: true })
onBeforeUnmount(() => { chart?.dispose(); chart = null })
window.addEventListener('resize', () => chart?.resize())
</script>

<style lang="scss" scoped>
.chart { width: 100%; }
</style>
