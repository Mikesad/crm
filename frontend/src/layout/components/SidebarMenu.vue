<template>
  <nav class="nav">
    <template v-for="group in menuGroups" :key="group.label || group.path">
      <!-- v0.4:整组不再按 requiresRole 过滤,改为 per-item 过滤;
           group 始终显示 label,空子项组(无任何可见子项)整个隐藏 -->
      <div v-if="hasVisibleChildren(group)" class="nav-group">
        <div v-if="group.label" class="nav-label">{{ group.label }}</div>

        <!-- 单级菜单项 -->
        <router-link
          v-if="!group.children && hasItemAccess(group)"
          :to="group.path"
          class="nav-item"
          :class="{ active: isActive(group.path) }"
        >
          <el-icon v-if="group.icon" class="nav-icon"><component :is="group.icon" /></el-icon>
          <span class="nav-text">{{ group.title }}</span>
          <span v-if="group.badge" class="nav-badge">{{ group.badge }}</span>
        </router-link>

        <!-- 分组菜单（含子项,按 perm + requiresRole per-item 过滤） -->
        <template v-else-if="group.children">
          <router-link
            v-for="child in group.children.filter(hasItemAccess)"
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
    </template>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

/**
 * 菜单定义（顺序：工作台 → 业务 → 交易 → 可视化 → 系统设置）
 *
 * v0.4 修订(D7):
 *   - "系统设置"组收纳 4 项系统设置(当前 v0.3 只保留"角色管理" 1 项) + 产品 + 产品分类
 *   - 可见性 **per-item**:
 *       角色管理:admin + sales_director(D3 v0.2 锁定)
 *       产品 / 产品分类:全员 5 角色(D7 v0.4)
 *   - group 不再设 requiresRole,空子项组整组隐藏(hasVisibleChildren 实现)
 *
 * icon 使用 Element Plus Icons（已在 main.js 全局注册）
 * perm 缺失则默认全部角色可见；存在则校验用户 permissions
 * badge 由业务模块通过 store 注入
 */
const menuGroups = [
  {
    label: '工作台',
    children: [
      { path: '/dashboard', title: '仪表盘', icon: 'HomeFilled' },
      { path: '/record/center', title: '跟进中心', icon: 'BellFilled', perm: 'crm:record:center' }
    ]
  },
  {
    label: '业务',
    children: [
      { path: '/lead/list', title: '线索', icon: 'Aim' },
      { path: '/customer/list', title: '客户', icon: 'User' },
      { path: '/business/list', title: '商机', icon: 'TrendCharts' }
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
    label: '可视化',
    children: [
      { path: '/report', title: '报表', icon: 'DataLine' }
    ]
  },
  {
    label: '系统设置',
    children: [
      // 角色管理 - admin + 销售总监(D3 v0.2)
      { path: '/system/role', title: '角色管理', icon: 'Avatar', perm: 'sys:user:list', requiresRole: ['admin', 'sales_director'] },
      // 阶段七 commit:部门管理 - admin + 销售总监(左侧部门树 + 右侧详情卡)
      { path: '/system/dept', title: '部门管理', icon: 'OfficeBuilding', perm: 'sys:dept:list', requiresRole: ['admin', 'sales_director'] },
      // 产品 - 全员(D7 v0.4 全员可见)
      // v0.5:产品分类已整合为产品库 Tab,不再单独是菜单项
      // v0.7:产品图标从 Goods 改成 Box(包装盒,SaaS 产品更直观)
      { path: '/product/list', title: '产品', icon: 'Box', perm: 'crm:product:list' }
    ]
  }
]

const activePath = computed(() => route.path)

function isActive(path) {
  return activePath.value === path || activePath.value.startsWith(path + '/')
}

function hasPerm(item) {
  if (!item.perm) return true
  const perms = userStore.permissions || []
  return perms.includes(item.perm)
}

function hasRole(item) {
  if (!item.requiresRole || item.requiresRole.length === 0) return true
  const keys = userStore.roleKeys || []
  return item.requiresRole.some((r) => keys.includes(r))
}

/**
 * per-item 可见性(v0.4):
 *   - perm 命中 + requiresRole 命中(若配置) → 可见
 */
function hasItemAccess(item) {
  return hasPerm(item) && hasRole(item)
}

/**
 * 整组可见性(v0.4):
 *   - 无 children:不显示分组(交给单级 router-link)
 *   - 有 children:至少 1 个子项 visible 才显示整组,空子项组不显示
 */
function hasVisibleChildren(group) {
  if (!group.children) return true
  return group.children.some(hasItemAccess)
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
