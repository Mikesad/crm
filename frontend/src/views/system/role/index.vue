<template>
  <div class="page">
    <!-- 角色管理 顶部 title + tabs -->
    <div class="page-header">
      <div>
        <div class="page-title">角色管理</div>
        <div class="page-sub">
          用户 {{ userCount }} 人 ·
          角色 {{ roleCount }} 个 ·
          <span class="text-muted">仅 admin / 销售总监 可访问</span>
        </div>
      </div>
    </div>

    <div class="tabs">
      <div class="tab" :class="{ active: tab === 'users' }" @click="switchTab('users')">
        用户管理
      </div>
      <div class="tab" :class="{ active: tab === 'roles' }" @click="switchTab('roles')">
        权限管理
      </div>
      <div class="spacer" />
      <div class="right-action">
        <a v-if="tab === 'roles' && hasPerm('sys:role:edit')" class="action-link" @click.prevent="switchTab('roles')">
          <el-icon><Plus /></el-icon> 新建角色
        </a>
      </div>
    </div>

    <!-- 用户 tab -->
    <UserPanel v-if="tab === 'users'" @count="userCount = $event" />

    <!-- 权限 tab -->
    <RolePanel v-else @count="roleCount = $event" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { pageRole } from '@/api/sys-role'
import { pageUser } from '@/api/sys-user'
import { useAuth } from '@/composables/useAuth'
import UserPanel from './UserPanel.vue'
import RolePanel from './RolePanel.vue'

defineOptions({ name: 'SystemRoleHome' })

const route = useRoute()
const router = useRouter()
const { hasPerm } = useAuth()

const tab = ref(route.query.tab === 'roles' ? 'roles' : 'users')
const userCount = ref(0)
const roleCount = ref(0)

function switchTab(t) {
  tab.value = t
  router.replace({ query: { ...route.query, tab: t } })
  // 顶 tab 切换时刷新计数(子组件初次挂载后会 emit count,但用户切回时也要重新拉)
  fetchCounts()
}

async function fetchCounts() {
  try {
    const [u, r] = await Promise.all([
      pageUser({ pageNum: 1, pageSize: 1 }),
      pageRole({ pageNum: 1, pageSize: 1 })
    ])
    userCount.value = u.data?.total || 0
    roleCount.value = r.data?.total || 0
  } catch (e) { /* ignore */ }
}

onMounted(fetchCounts)
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

.tabs { display: flex; gap: 0; border-bottom: 1px solid var(--hairline); margin-bottom: 24px; align-items: center; }
.tab { padding: 10px 16px; font-size: 14px; color: var(--muted); cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; transition: all 0.12s; display: flex; align-items: center; gap: 6px; }
.tab:hover { color: var(--ink-soft); }
.tab.active { color: var(--accent); border-bottom-color: var(--accent); font-weight: 500; }
.spacer { flex: 1; }
.right-action { padding: 0 0 8px; }
.action-link { background: none; border: none; padding: 4px 10px; color: var(--accent); cursor: pointer; font-size: 13px; display: inline-flex; align-items: center; gap: 4px; }
.action-link:hover { text-decoration: underline; }

.text-muted { color: var(--subtle); }
</style>
