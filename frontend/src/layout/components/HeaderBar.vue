<template>
  <div class="header-bar">
    <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
      <Fold v-if="!appStore.sidebarCollapsed" />
      <Expand v-else />
    </el-icon>
    <div class="right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="28" :icon="UserFilled" />
          <span class="nickname">{{ userStore.userInfo?.nickname || '未登录' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox, ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      type: 'warning'
    }).catch(() => null)
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'profile') {
    ElMessage.info('个人中心 - 待实现')
  }
}
</script>

<style lang="scss" scoped>
.header-bar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #555;
  }
  .right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .user-info {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: #333;
    .nickname {
      font-size: 14px;
    }
  }
}
</style>
