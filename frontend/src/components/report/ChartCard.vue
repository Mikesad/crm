<template>
  <div class="chart-card">
    <div class="chart-card-head">
      <div class="chart-card-title">{{ title }}</div>
      <div class="chart-card-meta">{{ meta }}</div>
    </div>
    <div class="chart-slot">
      <slot />
    </div>
  </div>
</template>

<script setup>
/**
 * 报表图表卡 wrapper(阶段五 commit 2)
 *
 * <p>沿用 phase5-report-variant-b-cockpit.html 第 323-352 行:
 * 标题(粗体 12.5px) + meta(灰色 10.5px) + chart 区域(默认 240px 高,可通过 class 调整)。</p>
 */
defineProps({
  title: { type: String, required: true },
  meta:  { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
.chart-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
}

.chart-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.chart-card-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.chart-card-meta {
  font-size: 10.5px;
  color: var(--muted);
}

.chart-slot {
  flex: 1;
  min-height: 0;
  width: 100%;
  // 默认高度,Chart 组件内部用 style 或 prop 调整
  min-height: 240px;
}
</style>
