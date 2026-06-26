<template>
  <el-container class="layout-container">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon size="22" color="#fff"><Avatar /></el-icon>
        <span v-if="!appStore.sidebarCollapsed" class="title">智能 CRM</span>
      </div>
      <SidebarMenu />
    </el-aside>
    <el-container>
      <el-header class="header">
        <HeaderBar />
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import SidebarMenu from './components/SidebarMenu.vue'
import HeaderBar from './components/HeaderBar.vue'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}
.sidebar {
  background: #001529;
  transition: width 0.28s;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #fff;
  gap: 8px;
  .title {
    font-size: 18px;
    font-weight: 600;
    white-space: nowrap;
  }
}
.header {
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 0;
}
.main {
  background: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.28s ease;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
