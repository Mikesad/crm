<template>
  <div class="app-layout">
    <!-- 左侧栏：白底 + 森林绿激活态 -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <div class="wordmark">
          <div class="wordmark-glyph" />
          <span>ZenCRM</span>
        </div>
      </div>
      <SidebarMenu class="sidebar-menu" />
      <div class="user-card">
        <div class="user-info">
          <div class="user-name">{{ userStore.nickname || userStore.username || '' }}</div>
          <div class="user-role">{{ roleLabel }}</div>
        </div>
      </div>
    </aside>

    <!-- 右侧主区 -->
    <div class="main-wrap">
      <HeaderBar />
      <main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import SidebarMenu from './components/SidebarMenu.vue'
import HeaderBar from './components/HeaderBar.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const roleLabel = computed(() => {
  const keys = userStore.roleKeys || []
  if (keys.length === 0) return ''
  // 简化映射：取第一个 role key 翻译成中文
  const map = {
    admin: '系统管理员',
    sales_director: '销售总监',
    sales_lead: '销售主管',
    sales: '销售',
    finance: '财务'
  }
  return map[keys[0]] || keys[0]
})
</script>

<style lang="scss" scoped>
.app-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: var(--bg);
}

.sidebar {
  background: var(--bg-warm);
  border-right: 1px solid var(--hairline);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.sidebar-top {
  padding: 20px 20px 16px;
  flex-shrink: 0;
}

.wordmark {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: var(--ink);
}
.wordmark-glyph {
  width: 18px;
  height: 18px;
  background: var(--accent);
  border-radius: 4px;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
  min-height: 0;
}

.user-card {
  margin: 0 12px 16px;
  padding: 10px 12px;
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-info {
  flex: 1;
  line-height: 1.3;
  min-width: 0;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.user-role {
  font-size: 11.5px;
  color: var(--muted);
}

.main-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.main {
  flex: 1;
  overflow: auto;
  padding: 0;
  background: var(--bg);
}

/* 页面切换过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
