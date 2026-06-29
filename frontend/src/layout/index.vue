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
import SidebarMenu from './components/SidebarMenu.vue'
import HeaderBar from './components/HeaderBar.vue'
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
