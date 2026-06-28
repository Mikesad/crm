<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">客户管理</div>
        <div class="page-sub">共 {{ grandTotal }} 个客户 · 私海 {{ myTotal }} · 公海 {{ publicTotal }} · 共享给我 {{ sharedTotal }}</div>
      </div>
      <div>
        <el-button :icon="Plus" class="btn-zen-primary" @click="handleCreate">新建客户</el-button>
      </div>
    </div>

    <!-- Tabs: 私海 / 公海 / 被共享给我的 -->
    <div class="tabs">
      <div class="tab" :class="{ active: currentTab === 'mine' }" @click="switchTab('mine')">
        私海 <span class="count">{{ myTotal }}</span>
      </div>
      <div class="tab" :class="{ active: currentTab === 'public' }" @click="switchTab('public')">
        公海池 <span class="count">{{ publicTotal }}</span>
      </div>
      <div class="tab" :class="{ active: currentTab === 'shared' }" @click="switchTab('shared')">
        被共享给我的 <span class="count">{{ sharedTotal }}</span>
      </div>
      <div class="tab-meta">数据权限:仅本人 + 共享命中 + 公海</div>
    </div>

    <div class="layout">
      <!-- 主区 -->
      <div class="layout-main">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索客户名称"
            class="search"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.level" placeholder="全部级别" class="filter" clearable @change="handleSearch">
            <el-option label="A 重要" value="A" />
            <el-option label="B 普通" value="B" />
            <el-option label="C 意向" value="C" />
          </el-select>
          <el-select v-model="query.industry" placeholder="全部行业" class="filter" clearable @change="handleSearch">
            <el-option v-for="i in industryOptions" :key="i" :label="i" :value="i" />
          </el-select>
          <div class="spacer" />
          <el-button :icon="Search" @click="handleSearch">查询</el-button>
        </div>

        <el-card class="table-card" v-loading="loading">
          <el-table :data="list" stripe @row-dblclick="handleRowDblClick">
            <el-table-column prop="customerName" label="客户名称" min-width="180">
              <template #default="{ row }">
                <div class="name-block">
                  <a class="name customer-link" @click.stop="handleView(row)">{{ row.customerName }}</a>
                  <span class="sub">客户 ID #{{ row.id }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.level === 'A'" type="success" effect="light">重要</el-tag>
                <el-tag v-else-if="row.level === 'B'" type="primary" effect="light">普通</el-tag>
                <el-tag v-else type="info" effect="plain">意向</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="归属 / 共享" min-width="160">
              <template #default="{ row }">
                <span class="owner-badge" :class="ownerBadgeClass(row)">
                  <span class="dot" />
                  {{ ownerBadgeText(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="lastFollowTime" label="最后跟进" width="110">
              <template #default="{ row }">
                <span class="mono" :class="followTimeClass(row.lastFollowTime)">{{ followTimeText(row.lastFollowTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="商机 / 联系人" width="140">
              <template #default="{ row }">
                <span class="stats">
                  <span class="stats-num mono">{{ statsCache[row.id]?.business ?? '-' }}</span> 商机
                  <span style="margin: 0 6px; color: var(--hairline)">|</span>
                  <span class="stats-num mono">{{ statsCache[row.id]?.contact ?? '-' }}</span> 联系人
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link class="action-link" @click.stop="handleView(row)">详情</el-button>
                <el-button
                  link
                  class="action-link"
                  :disabled="isReadOnlyOnRow(row)"
                  @click.stop="handleEdit(row)"
                >编辑</el-button>
                <el-button
                  v-if="isOwnerOnRow(row)"
                  link
                  class="action-link share-link"
                  @click.stop="openShareDialog(row)"
                >↗ 共享</el-button>
                <el-button
                  v-if="currentTab === 'public'"
                  link
                  class="action-link claim-link"
                  @click.stop="handleClaim(row)"
                >认领</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            class="pagination"
            @current-change="loadList"
            @size-change="loadList"
          />
        </el-card>
      </div>

      <!-- 右侧辅助面板 -->
      <aside class="layout-side">
        <div class="side-panel">
          <div class="side-panel-title">客户级别分布</div>
          <div class="dist-row">
            <div class="dist-label">重要</div>
            <div class="dist-track"><div class="dist-fill" style="width: 7.7%"></div></div>
            <div class="dist-num">12</div>
          </div>
          <div class="dist-row">
            <div class="dist-label">普通</div>
            <div class="dist-track"><div class="dist-fill info" style="width: 30.7%"></div></div>
            <div class="dist-num">48</div>
          </div>
          <div class="dist-row">
            <div class="dist-label">意向</div>
            <div class="dist-track"><div class="dist-fill muted" style="width: 61.5%"></div></div>
            <div class="dist-num">96</div>
          </div>
        </div>

        <div class="side-panel" v-if="currentTab === 'mine'">
          <div class="side-panel-title">
            公海池 <span class="more" @click="switchTab('public')">查看 →</span>
          </div>
          <div class="pool-banner">
            <strong>{{ publicTotal }}</strong> 个公海客户待认领，超过 15 天未跟进的客户会被自动回收到这里
          </div>
          <div class="pool-link" @click="switchTab('public')">
            <span>前往公海池挑选</span>
            <span>→</span>
          </div>
        </div>

        <div class="side-panel" v-if="currentTab === 'shared'">
          <div class="side-panel-title">协作说明</div>
          <div class="rule-list">
            <div>只读共享:仅查看,不能编辑</div>
            <div>读写共享:可编辑,等同 owner 权限</div>
            <div>如需取消请让 owner 撤销</div>
          </div>
        </div>

        <div class="side-panel">
          <div class="side-panel-title">
            待跟进客户 <span class="more">查看全部 →</span>
          </div>
          <div v-if="followList.length === 0" class="empty-mini">暂无待跟进客户</div>
          <div
            v-for="c in followList"
            :key="c.id"
            class="follow-item"
          >
            <div class="follow-meta">
              <div class="follow-name">{{ c.customerName }}</div>
              <div class="follow-time" :class="followTimeClass(c.lastFollowTime)">
                {{ followTimeText(c.lastFollowTime) }} · 已逾期
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 新建/编辑客户弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editing.id ? '编辑客户' : '新建客户'"
      width="480px"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top">
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="editing.customerName" placeholder="如：华为集团" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select v-model="editing.industry" placeholder="选择行业" clearable allow-create filterable>
            <el-option v-for="i in industryOptions" :key="i" :label="i" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户级别">
          <el-radio-group v-model="editing.level">
            <el-radio value="A">A 重要</el-radio>
            <el-radio value="B">B 普通</el-radio>
            <el-radio value="C">C 意向</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 阶段四:共享对话框 -->
    <CustomerShareDialog
      v-model:visible="shareDialogVisible"
      :customer="shareTarget"
      @shared="onSharedOk"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'
import { pageCustomer, addCustomer, updateCustomer, claimCustomer } from '@/api/customer'
import CustomerShareDialog from './components/ShareDialog.vue'

defineOptions({ name: 'CustomerList' })

const router = useRouter()
const userStore = useUserStore()

// ---------- 状态 ----------
// 'mine' 私海 / 'public' 公海池 / 'shared' 被共享给我的
const currentTab = ref('mine')
const query = reactive({ keyword: '', level: '', industry: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const myTotal = ref(0)
const publicTotal = ref(0)
const sharedTotal = ref(0)
const loading = ref(false)
const statsCache = reactive({})

const industryOptions = ['通信', '互联网', '云计算', 'AI', '智能硬件', '金融科技', '本地生活', '零售', '教育', '医疗']

// 共享对话框
const shareDialogVisible = ref(false)
const shareTarget = ref(null)

// ---------- 时间工具 ----------
const followTimeText = (t) => {
  if (!t) return '从未跟进'
  const days = dayjs().diff(dayjs(t), 'day')
  if (days < 1) return '今天'
  if (days < 7) return days + ' 天前'
  return dayjs(t).format('MM-DD')
}
const followTimeClass = (t) => {
  if (!t) return ''
  const days = dayjs().diff(dayjs(t), 'day')
  if (days > 7) return 'danger'
  if (days > 3) return 'warn'
  return ''
}

// ---------- 行内 owner/共享 判定 ----------
const myUserId = computed(() => userStore.userId)

const ownerBadgeText = (row) => {
  if (row.isPublic === 1) return '公海池'
  if (row.ownerUserId === myUserId.value) return `${row.ownerName || '我'} (我)`
  return `${row.ownerName || '其他'} 共享给我`
}
const ownerBadgeClass = (row) => {
  if (row.isPublic === 1) return 'owner-badge public'
  if (row.ownerUserId === myUserId.value) return 'owner-badge'
  return 'owner-badge shared'
}
const isOwnerOnRow = (row) => row.ownerUserId === myUserId.value
const isReadOnlyOnRow = (row) => {
  // 公海客户:可编辑元数据(行业/级别)
  if (row.isPublic === 1) return false
  // owner:可编辑
  if (row.ownerUserId === myUserId.value) return false
  // 共享给我:暂视为只读(共享写权限 V2 再细化)
  return true
}

// ---------- 列表加载 ----------
async function loadList() {
  loading.value = true
  try {
    const params = {
      keyword: query.keyword || undefined,
      level: query.level || undefined,
      industry: query.industry || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (currentTab.value === 'public') {
      params.isPublic = 1
    } else if (currentTab.value === 'shared') {
      params.sharedToMeOnly = 1
    } else {
      params.isPublic = 0
    }
    const res = await pageCustomer(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
    list.value.forEach((c) => {
      if (!statsCache[c.id]) {
        statsCache[c.id] = { business: Math.floor(Math.random() * 4), contact: 1 + Math.floor(Math.random() * 4) }
      }
    })
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadTotals() {
  try {
    const [r1, r2, r3] = await Promise.all([
      pageCustomer({ isPublic: 0, pageNum: 1, pageSize: 1 }),
      pageCustomer({ isPublic: 1, pageNum: 1, pageSize: 1 }),
      pageCustomer({ sharedToMeOnly: 1, pageNum: 1, pageSize: 1 })
    ])
    myTotal.value = r1.data?.total || 0
    publicTotal.value = r2.data?.total || 0
    sharedTotal.value = r3.data?.total || 0
  } catch (e) { /* ignore */ }
}

const grandTotal = computed(() => myTotal.value + publicTotal.value + sharedTotal.value)
const followList = computed(() => list.value.slice(0, 4))

function switchTab(tab) {
  currentTab.value = tab
  query.pageNum = 1
  loadList()
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleRowDblClick(row) {
  handleView(row)
}

function handleView(row) {
  router.push(`/customer/${row.id}`)
}

function handleEdit(row) {
  if (isReadOnlyOnRow(row)) {
    ElMessage.warning('你对该客户只有只读权限')
    return
  }
  Object.assign(editing, row)
  editVisible.value = true
}

function handleCreate() {
  resetEditForm()
  editVisible.value = true
}

function openShareDialog(row) {
  shareTarget.value = row
  shareDialogVisible.value = true
}

function onSharedOk() {
  // 共享创建/撤销后无需刷列表(owner 不变),但 tab 计数可能微变
  loadTotals()
}

async function handleClaim(row) {
  try {
    await ElMessageBox.confirm(
      `确认认领公海客户「${row.customerName}」?认领后归属将变为你。`,
      '公海认领',
      { type: 'success', confirmButtonText: '认领', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await claimCustomer(row.id)
    ElMessage.success('认领成功,客户已转为你的私海')
    loadList()
    loadTotals()
  } catch (e) { /* 错误已全局提示 */ }
}

// ---------- 新建/编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, customerName: '', industry: '', level: 'C' })
const editRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

function resetEditForm() {
  editing.id = null
  editing.customerName = ''
  editing.industry = ''
  editing.level = 'C'
}

async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await updateCustomer(editing)
      ElMessage.success('已更新')
    } else {
      await addCustomer(editing)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    loadList()
    loadTotals()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadList()
  loadTotals()
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

/* tabs */
.tabs {
  display: flex;
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 16px;
  align-items: center;
}
.tab {
  padding: 10px 16px;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.12s;
  display: flex;
  align-items: center;
  gap: 6px;
  &:hover { color: var(--ink); }
  &.active { color: var(--ink); font-weight: 500; border-bottom-color: var(--accent); }
  .count {
    font-size: 11.5px; padding: 1px 7px;
    background: var(--hairline-soft); color: var(--muted);
    border-radius: 8px;
    font-family: var(--font-mono); font-feature-settings: 'tnum' 1;
  }
  &.active .count { background: var(--accent-soft); color: var(--accent); }
}
.tab-meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  padding: 0 8px;
}

/* layout */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }
.layout-main { min-width: 0; }
.layout-side { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 20px; }
@media (max-width: 1280px) { .layout-side { position: static; } }

/* toolbar */
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 280px; }
.filter { width: 140px; }
.spacer { flex: 1; }

/* table */
.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name-block { display: flex; flex-direction: column; gap: 2px; }
.name-block .name { font-weight: 500; color: var(--ink); }
.name-block .sub { font-size: 11.5px; color: var(--muted); font-weight: normal; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; color: var(--ink-soft); }
.mono.warn { color: var(--warn); }
.mono.danger { color: var(--danger); }

.stats { font-size: 12.5px; color: var(--muted); display: inline-flex; align-items: center; }
.stats-num { color: var(--ink); font-weight: 500; }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

/* owner / share cell */
.owner-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: var(--muted);
  .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); flex-shrink: 0; }
  &.shared .dot { background: var(--info); }
  &.public .dot { background: var(--warn); }
}

/* row actions */
.action-link { font-size: 12.5px; padding: 0 4px; }
.share-link { color: var(--accent); }
.claim-link { color: var(--accent); font-weight: 500; }
.action-link.is-disabled { color: var(--subtle); cursor: not-allowed; }

/* side panel */
.side-panel {
  background: var(--bg-warm);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 18px;
}
.side-panel-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.more { font-size: 11.5px; color: var(--muted); font-weight: normal; cursor: pointer; }
.more:hover { color: var(--accent); }

.dist-row {
  display: grid;
  grid-template-columns: 56px 1fr 32px;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 12.5px;
  &:last-child { margin-bottom: 0; }
}
.dist-label { color: var(--muted); }
.dist-track { height: 8px; background: var(--hairline-soft); border-radius: 4px; position: relative; overflow: hidden; }
.dist-fill { position: absolute; left: 0; top: 0; bottom: 0; background: var(--accent); border-radius: 4px; }
.dist-fill.info { background: var(--info); }
.dist-fill.muted { background: var(--subtle); }
.dist-num { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; text-align: right; color: var(--ink); font-weight: 500; }

.pool-banner {
  padding: 10px 12px;
  background: var(--warn-soft);
  border-radius: var(--radius);
  color: var(--warn);
  font-size: 12.5px;
  line-height: 1.5;
  strong { font-weight: 600; }
}
.pool-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0 0;
  font-size: 12.5px;
  color: var(--accent);
  cursor: pointer;
  margin-top: 8px;
}

.rule-list { font-size: 12.5px; color: var(--ink-soft); line-height: 1.7; }
.rule-list div::before { content: '· '; color: var(--accent); }

.follow-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--hairline-soft);
  &:last-child { border-bottom: none; padding-bottom: 0; }
  &:first-child { padding-top: 0; }
}
.follow-meta { flex: 1; min-width: 0; }
.follow-name { font-size: 13px; font-weight: 500; }
.follow-time { font-size: 11.5px; color: var(--muted); margin-top: 1px; &.warn { color: var(--warn); } &.danger { color: var(--danger); } }

.empty-mini { font-size: 12.5px; color: var(--muted); padding: 8px 0; text-align: center; }
</style>
