<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="appStore.sidebarCollapsed"
    background-color="#001529"
    text-color="#c9d1d9"
    active-text-color="#fff"
    router
    class="sidebar-menu"
  >
    <template v-for="route in menuRoutes" :key="route.path">
      <SidebarItem :item="route" />
    </template>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SidebarItem from './SidebarItem.vue'
import { useAppStore } from '@/store/app'

const route = useRoute()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)

// 简单菜单元数据，可后续改为从后端拉取
const menuRoutes = [
  { path: '/dashboard', meta: { title: '首页', icon: 'HomeFilled' } },
  {
    path: '/lead',
    meta: { title: '线索管理', icon: 'Aim' },
    children: [{ path: '/lead/list', meta: { title: '线索列表' } }]
  },
  {
    path: '/customer',
    meta: { title: '客户管理', icon: 'User' },
    children: [
      { path: '/customer/list', meta: { title: '客户列表' } },
      { path: '/customer/public', meta: { title: '公海池' } }
    ]
  },
  {
    path: '/business',
    meta: { title: '商机管理', icon: 'TrendCharts' },
    children: [
      { path: '/business/list', meta: { title: '商机列表' } },
      { path: '/business/funnel', meta: { title: '销售漏斗' } }
    ]
  },
  {
    path: '/contract',
    meta: { title: '合同与回款', icon: 'Document' },
    children: [
      { path: '/contract/list', meta: { title: '合同列表' } },
      { path: '/contract/receivable', meta: { title: '回款管理' } }
    ]
  },
  {
    path: '/system',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      { path: '/system/user', meta: { title: '用户管理' } },
      { path: '/system/role', meta: { title: '角色管理' } },
      { path: '/system/menu', meta: { title: '菜单权限' } },
      { path: '/system/dept', meta: { title: '部门管理' } }
    ]
  }
]
</script>

<style lang="scss" scoped>
.sidebar-menu {
  border-right: none;
  height: calc(100vh - 56px);
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 48px;
    line-height: 48px;
  }
  :deep(.el-menu-item.is-active) {
    background: #1890ff !important;
  }
}
</style>
