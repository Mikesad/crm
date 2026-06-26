<template>
  <template v-if="!item.children || item.children.length === 0">
    <el-menu-item :index="item.path">
      <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
      <template #title>{{ item.meta?.title }}</template>
    </el-menu-item>
  </template>
  <template v-else>
    <el-sub-menu :index="item.path">
      <template #title>
        <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>
      <SidebarItem
        v-for="child in item.children"
        :key="child.path"
        :item="child"
      />
    </el-sub-menu>
  </template>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})
</script>
