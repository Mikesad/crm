<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(v) => emit('update:visible', v)"
    :title="title"
    width="560px"
    :close-on-click-modal="false"
    @open="onOpen"
    @closed="onClosed"
    align-center
  >
    <!-- 客户信息卡 -->
    <div v-if="customer" class="info-card">
      <div class="info-icon">{{ customerInitial }}</div>
      <div class="info-body">
        <div class="info-name">
          {{ customer.customerName }}
          <el-tag
            v-if="customer.level === 'A'"
            type="success"
            effect="light"
            size="small"
            style="margin-left: 6px"
          >A 重要</el-tag>
          <el-tag
            v-else-if="customer.level === 'B'"
            type="primary"
            effect="light"
            size="small"
            style="margin-left: 6px"
          >B 普通</el-tag>
          <el-tag v-else type="info" effect="plain" size="small" style="margin-left: 6px">C 意向</el-tag>
        </div>
        <div class="info-meta">
          {{ customer.industry || '未分类' }}<span class="sep">·</span>
          owner: {{ customer.ownerName || customer.ownerUserId }}
        </div>
      </div>
    </div>

    <!-- 新增共享表单 -->
    <div class="form-section">
      <div class="form-label">
        被共享人<span class="req">*</span>
        <span class="hint">从下方下拉选择同事</span>
      </div>
      <el-select
        v-model="form.userId"
        filterable
        remote
        :remote-method="searchUsers"
        :loading="usersLoading"
        placeholder="请输入姓名 / 用户名搜索"
        style="width: 100%"
      >
        <el-option
          v-for="u in userOptions"
          :key="u.id"
          :label="`${u.nickname} (${u.username})`"
          :value="u.id"
        >
          <span style="float:left;">{{ u.nickname }}</span>
          <span style="float:right; font-size: 11px; color: var(--muted);">{{ u.username }}</span>
        </el-option>
      </el-select>
    </div>

    <div class="form-section">
      <div class="form-label">权限类型<span class="req">*</span></div>
      <el-radio-group v-model="form.authType" style="display: flex; gap: 10px; width: 100%">
        <div
          class="radio-card"
          :class="{ selected: form.authType === 1 }"
          @click="form.authType = 1"
        >
          <div class="radio-card-head">
            <div class="radio-card-dot" />
            <div class="radio-card-title">只读</div>
          </div>
          <div class="radio-card-desc">可查看客户资料、联系人、跟进记录,不能编辑</div>
        </div>
        <div
          class="radio-card"
          :class="{ selected: form.authType === 2 }"
          @click="form.authType = 2"
        >
          <div class="radio-card-head">
            <div class="radio-card-dot" />
            <div class="radio-card-title">读写</div>
          </div>
          <div class="radio-card-desc">可编辑客户、改联系人、加跟进,等同协作 owner</div>
        </div>
      </el-radio-group>
    </div>

    <!-- 已有共享名单 -->
    <div class="share-section">
      <div class="share-section-head">
        <div class="share-section-title">已有共享</div>
        <div class="share-section-count">{{ shares.length }} 人</div>
      </div>
      <div v-if="loadingShares" class="loading-mini">加载中...</div>
      <div v-else-if="shares.length === 0" class="empty-mini">该客户尚未共享给其他同事</div>
      <div v-else class="share-list">
        <div v-for="s in shares" :key="s.id" class="share-item">
          <div class="share-avatar">{{ nicknameInitial(s.userNickname) }}</div>
          <div class="share-info">
            <div class="share-name">{{ s.userNickname || s.userId }}</div>
            <div class="share-dept">{{ s.userDeptName || '—' }} · 由 {{ s.createBy }} 发起</div>
          </div>
          <span class="share-perm" :class="s.authType === 2 ? 'share-perm-rw' : 'share-perm-ro'">
            {{ s.authType === 2 ? '↗ 读写' : '⊘ 只读' }}
          </span>
          <el-button link type="danger" size="small" @click="handleRevoke(s)">撤销</el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button
        class="btn-zen-primary"
        :loading="saving"
        :disabled="!form.userId || !form.authType"
        @click="handleSave"
      >保存共享</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { shareCustomer, listShares, revokeShare } from '@/api/customer'
import { listUsers } from '@/api/sys'

const props = defineProps({
  visible: { type: Boolean, default: false },
  customer: { type: Object, default: null }
})
const emit = defineEmits(['update:visible', 'shared'])

const userStore = useUserStore()
const myUserId = computed(() => userStore.userId)

const title = computed(() => {
  if (!props.customer) return '共享客户'
  return `共享客户「${props.customer.customerName}」`
})
const customerInitial = computed(() => {
  if (!props.customer?.customerName) return '?'
  return props.customer.customerName.charAt(0)
})

const form = reactive({ userId: null, authType: 2 })
const userOptions = ref([])
const usersLoading = ref(false)
const shares = ref([])
const loadingShares = ref(false)
const saving = ref(false)

function resetForm() {
  form.userId = null
  form.authType = 2
  userOptions.value = []
  shares.value = []
}

function onUserChange() { /* no-op */ }

async function searchUsers(keyword) {
  usersLoading.value = true
  try {
    const { data } = await listUsers({ keyword: keyword || '' })
    // 阶段四:过滤掉自己(共享给当前登录用户没意义)
    const all = (data || []).filter((u) => u.id !== myUserId.value)
    userOptions.value = all
  } finally {
    usersLoading.value = false
  }
}

async function loadShares() {
  if (!props.customer?.id) return
  loadingShares.value = true
  try {
    const { data } = await listShares(props.customer.id)
    shares.value = data || []
  } catch (e) {
    shares.value = []
  } finally {
    loadingShares.value = false
  }
}

function onOpen() {
  resetForm()
  searchUsers('')
  loadShares()
}

function onClosed() {
  resetForm()
}

async function handleSave() {
  if (!props.customer?.id || !form.userId || !form.authType) return
  saving.value = true
  try {
    await shareCustomer({
      customerId: props.customer.id,
      userId: form.userId,
      authType: form.authType
    })
    ElMessage.success('共享成功')
    form.userId = null
    await loadShares()
    emit('shared')
  } finally {
    saving.value = false
  }
}

async function handleRevoke(s) {
  try {
    await ElMessageBox.confirm(
      `确认撤销对「${s.userNickname || s.userId}」的共享?`,
      '撤销共享',
      { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await revokeShare(s.id)
    ElMessage.success('已撤销')
    await loadShares()
    emit('shared')
  } catch (e) { /* 错误已全局提示 */ }
}

function nicknameInitial(name) {
  if (!name) return '?'
  return name.charAt(0)
}
</script>

<style lang="scss" scoped>
.info-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--bg);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  margin-bottom: 18px;
}
.info-icon {
  width: 36px;
  height: 36px;
  background: var(--accent-soft);
  color: var(--accent);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.info-body { flex: 1; line-height: 1.4; min-width: 0; }
.info-name { font-size: 14px; font-weight: 600; }
.info-meta { font-size: 12px; color: var(--muted); margin-top: 2px; }
.info-meta .sep { margin: 0 6px; color: var(--subtle); }

.form-section + .form-section { margin-top: 16px; }
.form-label {
  display: block;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-soft);
  margin-bottom: 6px;
}
.form-label .req { color: var(--danger); margin-left: 2px; }
.form-label .hint { font-weight: normal; color: var(--muted); margin-left: 6px; }

.radio-card {
  flex: 1;
  position: relative;
  padding: 10px 12px;
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.12s;
  background: var(--surface);
}
.radio-card:hover { border-color: var(--muted); }
.radio-card.selected {
  border-color: var(--accent);
  background: var(--accent-pale);
  box-shadow: 0 0 0 1px var(--accent);
}
.radio-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.radio-card-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 4px solid var(--hairline);
  background: var(--surface);
  flex-shrink: 0;
  transition: all 0.12s;
}
.radio-card.selected .radio-card-dot { border-color: var(--accent); }
.radio-card-title { font-size: 13px; font-weight: 600; }
.radio-card-desc { font-size: 11.5px; color: var(--muted); line-height: 1.45; padding-left: 20px; }

.share-section { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--hairline); }
.share-section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.share-section-title { font-size: 12.5px; font-weight: 600; color: var(--ink); }
.share-section-count { font-size: 11.5px; color: var(--muted); }
.share-list { display: flex; flex-direction: column; gap: 6px; max-height: 200px; overflow-y: auto; }
.share-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--bg);
  border: 1px solid var(--hairline-soft);
  border-radius: var(--radius);
}
.share-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.share-info { flex: 1; line-height: 1.3; min-width: 0; }
.share-name { font-size: 13px; font-weight: 500; }
.share-dept { font-size: 11.5px; color: var(--muted); margin-top: 1px; }
.share-perm {
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: 3px;
  font-weight: 500;
}
.share-perm-rw { background: var(--accent-soft); color: var(--accent); }
.share-perm-ro { background: var(--hairline-soft); color: var(--muted); }

.loading-mini, .empty-mini {
  font-size: 12.5px;
  color: var(--muted);
  padding: 12px 0;
  text-align: center;
}
</style>
