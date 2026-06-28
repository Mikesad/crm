<template>
  <el-badge
    :value="todoStore.badgeNumber"
    :max="99"
    :hidden="!todoStore.hasTodo"
    class="bell-badge"
  >
    <el-tooltip content="待跟进提醒" placement="bottom">
      <button class="bell-btn" @click="goCenter" aria-label="待跟进">
        <el-icon class="bell-icon"><BellFilled /></el-icon>
      </button>
    </el-tooltip>
  </el-badge>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { BellFilled } from '@element-plus/icons-vue'
import { useTodoStore } from '@/store/todo'

const router = useRouter()
const todoStore = useTodoStore()

let timer = null

onMounted(async () => {
  // 立即拉一次(恢复 store 持久化的数字后,再校验是否 stale)
  await todoStore.fetchCount()
  // 每 5 分钟轮询
  timer = setInterval(() => todoStore.fetchCount(), 5 * 60 * 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const goCenter = () => {
  router.push('/record/center')
}
</script>

<style lang="scss" scoped>
.bell-badge {
  display: inline-flex;
  align-items: center;
}
.bell-btn {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  color: var(--ink-soft);
  transition: all 0.12s;
  padding: 0;
  &:hover {
    background: var(--bg);
    color: var(--accent);
  }
}
.bell-icon {
  font-size: 17px;
}
</style>