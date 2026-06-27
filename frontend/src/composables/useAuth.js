import { computed } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * 权限 composable
 *
 * 包装 userStore.permissions，提供 hasPerm / hasAnyPerm / hasRole / isAdmin 等响应式方法。
 * 后端 @SaCheckPermission 兜底,前端用来条件渲染按钮（v-if="hasPerm('crm:contract:edit')"）。
 */
export function useAuth() {
  const userStore = useUserStore()

  const perms = computed(() => userStore.permissions || [])
  const roleKeys = computed(() => userStore.roleKeys || [])

  function hasPerm(perm) {
    return perms.value.includes(perm)
  }
  function hasAnyPerm(list) {
    return list.some((p) => perms.value.includes(p))
  }
  function hasAllPerms(list) {
    return list.every((p) => perms.value.includes(p))
  }
  function hasRole(role) {
    return roleKeys.value.includes(role)
  }
  const isAdmin = computed(() => roleKeys.value.includes('admin'))
  const isSales = computed(() => hasRole('sales') || hasRole('sales_lead') || hasRole('sales_director'))
  const isDirector = computed(() => hasRole('sales_director') || hasRole('admin'))
  const isFinance = computed(() => hasRole('finance'))

  return {
    perms,
    roleKeys,
    hasPerm,
    hasAnyPerm,
    hasAllPerms,
    hasRole,
    isAdmin,
    isSales,
    isDirector,
    isFinance
  }
}
