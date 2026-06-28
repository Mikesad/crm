<template>
  <div class="kpi-grid">
    <div v-for="kpi in kpis" :key="kpi.key" class="kpi-cell">
      <div class="kpi-label">{{ kpi.label }}</div>
      <div class="kpi-value mono">
        <span v-if="kpi.unit === '¥'">¥ </span>{{ formatValue(kpi.value) }}<span v-if="kpi.unit && kpi.unit !== '¥'" class="unit"> {{ kpi.unit }}</span>
      </div>
      <div v-if="kpi.delta" class="kpi-delta" :class="kpi.deltaDir">
        {{ kpi.delta }}
        <span v-if="kpi.footnote" class="footnote"> {{ kpi.footnote }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 报表 KPI 密集条(阶段五 commit 2)
 *
 * <p>沿用 phase5-report-variant-b-cockpit.html 第 257-289 行:
 * 6 KPI 横向密集排布,无内部 border-radius,KPI 之间 border-right hairline 分隔。
 * 数字用 JetBrains Mono + tabular-nums,等宽不抖。</p>
 *
 * <p>4 Tab 复用:funnel 6 个 / customer 4 个 / conversion 4 个 / finance 4 个,
 * kpis.length 自适应。</p>
 */
defineProps({
  kpis: {
    type: Array,
    required: true
    /** { key, label, value, unit, delta, deltaDir, footnote }[] */
  }
})

/** 简单格式化:value 已是 toPlainString 后的字符串,直接展示 */
function formatValue(value) {
  if (value == null || value === '') return '0'
  return value
}
</script>

<style lang="scss" scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(v-bind('kpis.length'), 1fr);
  gap: 0;
  margin-bottom: 14px;
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  overflow: hidden;
}

.kpi-cell {
  padding: 14px 16px;
  border-right: 1px solid var(--hairline-soft);

  &:last-child {
    border-right: none;
  }
}

.kpi-label {
  font-size: 11px;
  color: var(--muted);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.02em;

  .unit {
    font-size: 13px;
    color: var(--muted);
    font-weight: 500;
  }
}

.kpi-delta {
  font-size: 10.5px;
  margin-top: 4px;
  color: var(--muted);

  &.up   { color: var(--accent); }
  &.down { color: var(--danger); }

  .footnote {
    color: var(--subtle);
    margin-left: 4px;
  }
}
</style>
