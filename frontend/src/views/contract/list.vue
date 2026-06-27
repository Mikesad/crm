<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">合同管理</div>
        <div class="page-sub">共 {{ total }} 份合同 · 执行中 {{ activeCount }} · 审批中 {{ pendingCount }} · 已结束 {{ doneCount }}</div>
      </div>
      <div v-if="hasPerm('crm:contract:edit')">
        <el-button :icon="Plus" class="btn-zen-primary" @click="goSubmit">新建合同</el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索合同编号 / 合同名称" class="search" clearable @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="全部状态" class="filter" clearable @change="handleSearch">
        <el-option label="审批中" :value="0" />
        <el-option label="执行中" :value="1" />
        <el-option label="已结束" :value="2" />
        <el-option label="已作废" :value="3" />
      </el-select>
      <div class="spacer" />
      <el-button :icon="Search" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="contractNum" label="合同编号" width="180">
          <template #default="{ row }">
            <a class="mono accent link" @click="goDetail(row)">{{ row.contractNum }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="200" />
        <el-table-column prop="customerName" label="客户" width="160" />
        <el-table-column label="合同金额" width="140" align="right">
          <template #default="{ row }">
            <span class="price">¥ {{ Number(row.totalAmount).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 0" class="zen-status warn">审批中</span>
            <span v-else-if="row.status === 1" class="zen-status blue">执行中</span>
            <span v-else-if="row.status === 2" class="zen-status ok">已结束</span>
            <span v-else class="zen-status gray">已作废</span>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="签约人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link class="action-link" @click="goDetail(row)">详情</el-button>
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
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { pageContract } from '@/api/contract'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'ContractList' })

const router = useRouter()
const { hasPerm } = useAuth()

const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

const pendingCount = computed(() => list.value.filter(c => c.status === 0).length)
const activeCount = computed(() => list.value.filter(c => c.status === 1).length)
const doneCount = computed(() => list.value.filter(c => c.status === 2).length)

async function loadList() {
  loading.value = true
  try {
    const res = await pageContract({
      keyword: query.keyword || undefined,
      status: query.status ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.pageNum = 1; loadList() }
function handleReset() { query.keyword = ''; query.status = null; query.pageNum = 1; loadList() }
function goSubmit() { router.push('/contract/submit') }
function goDetail(row) { router.push(`/contract/${row.id}`) }

onMounted(loadList)
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 280px; }
.filter { width: 140px; }
.spacer { flex: 1; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.mono.accent.link { cursor: pointer; }
.mono.accent.link:hover { text-decoration: underline; }
.price { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; font-weight: 600; color: var(--ink); }
.text-muted { color: var(--subtle); }
.action-link { padding: 0 6px; }

.zen-status {
  display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 4px;
  font-size: 12px; font-weight: 500;
  &::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; }
}
.zen-status.gray { background: #f3f4f6; color: #6b7280; } .zen-status.gray::before { background: #9ca3af; }
.zen-status.warn { background: #fef3c7; color: #92400e; } .zen-status.warn::before { background: #f59e0b; }
.zen-status.ok { background: #d1fae5; color: #065f46; } .zen-status.ok::before { background: #10b981; }
.zen-status.blue { background: #dbeafe; color: #1e40af; } .zen-status.blue::before { background: #3b82f6; }

.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }
</style>
