<template>
  <nav class="nav">
    <div v-for="group in menuGroups" :key="group.label || group.path" class="nav-group">
      <div v-if="group.label" class="nav-label">{{ group.label }}</div>

      <!-- 单级菜单项 -->
      <router-link
        v-if="!group.children && hasPerm(group)"
        :to="group.path"
        class="nav-item"
        :class="{ active: isActive(group.path) }"
      >
        <el-icon v-if="group.icon" class="nav-icon"><component :is="group.icon" /></el-icon>
        <span class="nav-text">{{ group.title }}</span>
        <span v-if="group.badge" class="nav-badge">{{ group.badge }}</span>
      </router-link>

      <!-- 分组菜单（含子项） -->
      <template v-else-if="group.children">
        <router-link
          v-for="child in group.children.filter(hasPerm)"
          :key="child.path"
          :to="child.path"
          class="nav-item"
          :class="{ active: isActive(child.path) }"
        >
          <el-icon v-if="child.icon" class="nav-icon"><component :is="child.icon" /></el-icon>
          <span class="nav-text">{{ child.title }}</span>
          <span v-if="child.badge" class="nav-badge">{{ child.badge }}</span>
        </router-link>
      </template>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

/**
 * 菜单定义（顺序：工作区 → 交易 → 协作）
 * icon 使用 Element Plus Icons（已在 main.js 全局注册）
 * perm 缺失则默认全部角色可见；存在则校验用户 permissions
 * badge 由业务模块通过 store 注入（阶段二先硬编码）
 */
const menuGroups = [
  {
    label: '工作区',
    children: [
      { path: '/dashboard', title: '仪表盘', icon: 'HomeFilled' },
      { path: '/lead/list', title: '线索', icon: 'Aim', badge: 28 },
      { path: '/customer/list', title: '客户', icon: 'User', badge: 156 },
      { path: '/business/list', title: '商机', icon: 'TrendCharts', badge: 42 }
    ]
  },
  {
    label: '交易',
    children: [
      { path: '/contract/list', title: '合同', icon: 'Document' },
      { path: '/contract/receivable', title: '回款', icon: 'Money' }
    ]
  },
  {
    label: '协作',
    children: [
      { path: '/record/list', title: '跟进记录', icon: 'ChatLineRound' },
      { path: '/report', title: '报表', icon: 'DataLine' }
    ]
  }
]

const activePath = computed(() => route.path)

function isActive(path) {
  return activePath.value === path || activePath.value.startsWith(path + '/')
}

function hasPerm(item) {
  if (!item.perm) return true
  const perms = userStore.userInfo?.permissions || []
  return perms.includes(item.perm)
}
</script>

<style lang="scss" scoped>
.nav {
  padding: 0;
  flex: 1;
}

.nav-group {
  margin-bottom: 16px;
}

.nav-label {
  padding: 6px 8px 4px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--subtle);
  font-weight: 500;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: var(--radius);
  color: var(--ink-soft);
  font-size: 13.5px;
  text-decoration: none;
  transition: background 0.12s, color 0.12s;

  &:hover {
    background: var(--bg);
    color: var(--ink);
  }

  &.active {
    background: var(--accent-ring);
    color: var(--accent);
    font-weight: 500;
  }
}

.nav-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.nav-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-badge {
  font-size: 11px;
  padding: 1px 6px;
  background: var(--hairline);
  color: var(--muted);
  border-radius: 8px;
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
  flex-shrink: 0;
}

.nav-item.active .nav-badge {
  background: var(--accent);
  color: #fff;
}
</style>
