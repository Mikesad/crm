<template>
  <div ref="chartRef" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * 纵向柱状图(部门业绩 / 行业分布等)
 *
 * <p>data: [{ name, value, color? }] — color 可选,缺省用森林绿阶</p>
 * <p>P16 新增 format prop:'k'(默认, 除 1000 + K, 适合金额)/ 'raw'(原值显示, 适合 count)</p>
 */
const props = defineProps({
  data: { type: Array, required: true },
  height: { type: Number, default: 240 },
  /** y 轴单位(用于 formatter);仅 format='k' 时生效 */
  unit: { type: String, default: '¥' },
  /** 数据展示格式:'k' = 除 1000 + 'K'  |  'raw' = 原值显示(P16) */
  format: { type: String, default: 'k' },
  /** 是否按 value 降序排列 */
  sortDesc: { type: Boolean, default: true }
})

const chartRef = ref(null)
let chart = null

// 调色板:森林绿为主色 + 互补色相(青/琥珀/紫/粉/蓝/红)
// 与 ChartDonut 保持一致,类别多了也能区分
const COLORS = [
  '#166534', // 森林绿(品牌主色)
  '#0891b2', // 青
  '#d97706', // 琥珀
  '#7c3aed', // 紫
  '#db2777', // 粉
  '#2563eb', // 蓝
  '#65a30d', // 草绿
  '#dc2626', // 红
  '#0e7490', // 深青
  '#a16207'  // 暗黄
]

const AXIS_LABEL = { color: 'var(--muted)', fontSize: 10.5, fontFamily: 'Inter' }
const SPLIT_LINE = { lineStyle: { color: 'var(--hairline)', type: 'dashed' } }

/**
 * y 轴 / tooltip / 顶部数字 通用格式化(P16)
 * <p>'k' 模式: 除以 1000 + 'K'(默认,适合金额)</p>
 * <p>'raw' 模式: 原值显示(适合 count / 数量)</p>
 */
function fmtVal(v) {
  const n = Number(v)
  if (props.format === 'raw') return props.unit + n.toLocaleString('en-US')
  return props.unit + (n / 1000) + 'K'
}

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const sorted = props.sortDesc
    ? [...props.data].sort((a, b) => Number(b.value) - Number(a.value))
    : props.data
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 8, top: 32, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: sorted.map(d => d.name),
      axisLine: { lineStyle: { color: 'var(--hairline)' } },
      axisLabel: AXIS_LABEL,
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, formatter: v => fmtVal(v) },
      splitLine: SPLIT_LINE
    },
    series: [{
      type: 'bar',
      barWidth: 24,
      data: sorted.map((d, i) => ({
        value: d.value,
        itemStyle: { color: d.color || COLORS[i % COLORS.length], borderRadius: [3, 3, 0, 0] }
      })),
      label: {
        show: true, position: 'top', color: 'var(--ink)', fontSize: 10.5, fontWeight: 500,
        formatter: p => fmtVal(p.value)
      }
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