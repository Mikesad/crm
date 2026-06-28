<template>
  <div class="chart-grid">
    <slot />
  </div>
</template>

<script setup>
/**
 * 2x2 图表网格(阶段五 commit 2)
 *
 * <p>沿用 phase5-report-variant-b-cockpit.html 第 322-352 行:
 * 2 列等宽,14px gap,内含 4 个 ChartCard。也可只放 1-3 个 ChartCard,会自动占位。</p>
 *
 * <p>也可用作"2 张表平铺"容器,把 ReportDataTable 塞进 slot 即可。</p>
 */
</script>

<style lang="scss" scoped>
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}
</style>
