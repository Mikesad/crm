<template>
  <div class="table-section">
    <div class="table-head">
      <div class="table-title">{{ title }}</div>
      <div class="table-meta">{{ meta }}</div>
    </div>
    <table class="data-table">
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" :class="{ num: col.align === 'right' }">
            {{ col.title }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in rows" :key="row[rankKey] ?? idx">
          <td v-for="col in columns" :key="col.key" :class="{ num: col.align === 'right' }">
            <template v-if="col.key === 'rank'">
              <span class="rank" :class="rankClass(row[rankKey])">{{ row[rankKey] }}</span>
              <slot :name="`cell-${col.key}`" :row="row">{{ row[col.key] }}</slot>
            </template>
            <slot v-else :name="`cell-${col.key}`" :row="row">{{ row[col.key] }}</slot>
          </td>
        </tr>
        <tr v-if="!rows || rows.length === 0">
          <td :colspan="columns.length" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
/**
 * 报表数据表(阶段五 commit 2)
 *
 * <p>沿用 phase5-report-variant-b-cockpit.html 第 355-388 行:
 * 标题 + meta + 表格,无卡片装饰(密度 8 模式),hairline 分割,
 * 排名 1/2/3 用金/银/铜高亮,4+ 用普通灰色。</p>
 */
defineProps({
  title:    { type: String, required: true },
  meta:     { type: String, default: '' },
  columns:  { type: Array, required: true },   // [{ key, title, align? }]
  rows:     { type: Array, required: true },   // 原始数据行
  rankKey:  { type: String, default: 'rank' }  // 排名字段名
})

function rankClass(rank) {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}
</script>

<style lang="scss" scoped>
.table-section {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--hairline);
}

.table-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.table-meta {
  font-size: 10.5px;
  color: var(--muted);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  color: var(--ink-soft);

  th {
    text-align: left;
    font-weight: 500;
    color: var(--muted);
    padding: 8px 16px;
    background: var(--bg);
    font-size: 10.5px;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    border-bottom: 1px solid var(--hairline);
  }

  td {
    padding: 8px 16px;
    border-bottom: 1px solid var(--hairline-soft);

    &.num { text-align: right; font-variant-numeric: tabular-nums; font-family: var(--font-mono); }
  }

  tr:last-child td { border-bottom: none; }
  tr:hover td { background: var(--bg); }

  .rank {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--bg);
    color: var(--muted);
    font-size: 10.5px;
    font-weight: 600;
    margin-right: 6px;
  }
  .rank.gold   { background: #fef3c7; color: #92400e; }
  .rank.silver { background: #f1f5f9; color: #475569; }
  .rank.bronze { background: #fce7f3; color: #9d174d; }

  .empty {
    text-align: center;
    color: var(--subtle);
    padding: 24px 0;
    font-style: italic;
  }
}
</style>
