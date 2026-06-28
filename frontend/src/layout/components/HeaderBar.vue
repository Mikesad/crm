<template>
  <header class="header-bar">
    <div class="left">
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="i">
          <router-link v-if="crumb.to && i < breadcrumbs.length - 1" :to="crumb.to">{{ crumb.title }}</router-link>
          <span v-else>{{ crumb.title }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right">
      <NotificationBell />
      <el-dropdown @command="handleCommand" trigger="click">
        <span class="user-info">
          <span class="nickname">{{ userStore.nickname || userStore.username || '' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <div class="dropdown-header">
                <div class="name">{{ userStore.nickname || userStore.username || '' }}</div>
                <div class="role">{{ roleLabel }}</div>
              </div>
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import NotificationBell from '@/components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => {
  const n = userStore.nickname || userStore.username || 'U'
  return n.charAt(0)
})

const roleLabel = computed(() => {
  const keys = userStore.roleKeys || []
  if (keys.length === 0) return ''
  const map = {
    admin: '系统管理员',
    sales_director: '销售总监',
    sales_lead: '销售主管',
    sales: '销售',
    finance: '财务'
  }
  return map[keys[0]] || keys[0]
})

// 面包屑：根据路由 meta.title + 父路径生成
const breadcrumbs = computed(() => {
  const list = []
  const segments = route.path.split('/').filter(Boolean)
  let p = ''
  segments.forEach((seg, i) => {
    p += '/' + seg
    const matched = router.getRoutes().find(r => r.path === p)
    if (matched && matched.meta?.title) {
      list.push({ title: matched.meta.title, to: p === route.path ? null : p })
    } else {
      list.push({ title: seg })
    }
  })
  return list.length > 0 ? list : [{ title: '工作台' }]
})

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (e) { /* 用户取消 */ }
  }
}
</script>

<style lang="scss" scoped>
.header-bar {
  height: 48px;
  background: var(--bg-warm);
  border-bottom: 1px solid var(--hairline);
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.left {
  flex: 0 0 auto;
}
.breadcrumb {
  font-size: 13px;
  :deep(.el-breadcrumb__item__inner) {
    color: var(--muted);
    font-weight: normal;
  }
  :deep(a) {
    color: var(--muted);
    text-decoration: none;
    &:hover { color: var(--accent); }
  }
  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__item__inner) {
    color: var(--ink);
    font-weight: 500;
  }
}

.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 4px;
  &:hover { color: var(--accent); }
}

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.nickname {
  font-size: 13px;
  color: var(--ink-soft);
}

.dropdown-header {
  padding: 4px 0;
  .name {
    font-size: 13px;
    font-weight: 500;
    color: var(--ink);
  }
  .role {
    font-size: 11.5px;
    color: var(--muted);
    margin-top: 2px;
  }
}
</style>
