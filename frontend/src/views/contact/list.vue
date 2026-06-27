<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">联系人</div>
        <div class="page-sub">联系人跟随客户维护，强烈建议从客户详情 → 联系人 tab 进入</div>
      </div>
    </div>

    <div class="empty-state">
      <div class="empty-icon">⚠</div>
      <div class="empty-title">联系人属于客户</div>
      <div class="empty-sub">请到 <a @click="$router.push('/customer/list')">客户列表</a> 打开任一客户，在「联系人」tab 中维护</div>
      <el-button class="btn-zen-primary" @click="$router.push('/customer/list')">前往客户列表</el-button>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'ContactList' })
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { margin-bottom: 32px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

.empty-state {
  background: var(--bg-warm);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 60px 20px;
  text-align: center;
  max-width: 480px;
  margin: 0 auto;
}
.empty-icon { font-size: 36px; color: var(--warn); margin-bottom: 12px; }
.empty-title { font-size: 16px; font-weight: 500; color: var(--ink); margin-bottom: 6px; }
.empty-sub {
  font-size: 13px; color: var(--muted); margin-bottom: 16px;
  a { color: var(--accent); cursor: pointer; text-decoration: underline; }
}
</style>
