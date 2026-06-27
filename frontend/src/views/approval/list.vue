<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">审批中心</div>
        <div class="page-sub">合同折扣低于 8.5 折时需总监审批 · 共 {{ pendingCount }} 条待审</div>
      </div>
    </div>

    <el-card class="table-card" v-loading="loading">
      <div class="filter-bar">
        <el-radio-group v-model="filter.status" @change="loadList">
          <el-radio-button :value="0">待审 ({{ pendingCount }})</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="list" stripe empty-text="暂无审批记录">
        <el-table-column prop="contractNum" label="合同编号" width="180">
          <template #default="{ row }">
            <a class="mono accent link" @click="$router.push(`/contract/${row.contractId}`)">{{ row.contractNum }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="220" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="customerName" label="客户" width="160" />
        <el-table-column label="合同金额" width="130" align="right">
          <template #default="{ row }">
            <span class="price">¥ {{ Number(row.contractTotalAmount).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最低折扣" width="100" align="center">
          <template #default="{ row }">
            <span style="color: #ef4444; font-weight: 600;">{{ Number(row.minDiscount).toFixed(1) }} 折</span>
          </template>
        </el-table-column>
        <el-table-column prop="triggerReason" label="触发原因" min-width="200" />
        <el-table-column prop="createTime" label="申请时间" width="160">
          <template #default="{ row }"><span class="text-muted">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link class="action-link" @click="openApprove(row, true)">通过</el-button>
              <el-button link class="action-link danger" @click="openApprove(row, false)">驳回</el-button>
            </template>
            <span v-else class="text-muted">
              <span v-if="row.status === 1" class="zen-status ok">已通过</span>
              <span v-else class="zen-status gray">已驳回</span>
            </span>
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

    <!-- 通过/驳回共用弹窗 -->
    <el-dialog v-model="modal.show" :title="modal.action === 'approve' ? '审批通过' : '驳回合同'" width="500px">
      <div v-if="modal.row" class="modal-summary">
        <div class="text-muted micro">合同</div>
        <div class="modal-name">{{ modal.row.contractName }}</div>
        <div class="text-muted micro" style="margin-top: 4px;">{{ modal.row.contractNum }} · 最低折扣 {{ Number(modal.row.minDiscount).toFixed(1) }} 折</div>
      </div>
      <el-input
        v-model="modal.comment"
        type="textarea"
        :rows="3"
        :placeholder="modal.action === 'approve' ? '审批意见 (选填)' : '请填写驳回原因'"
      />
      <template #footer>
        <el-button @click="modal.show = false">取消</el-button>
        <el-button v-if="modal.action === 'approve'" class="btn-zen-primary" :loading="modal.loading" @click="confirmAction">确认通过</el-button>
        <el-button v-else type="danger" :loading="modal.loading" @click="confirmAction">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { pageApproval, approveApproval, rejectApproval } from '@/api/approval'

defineOptions({ name: 'ApprovalList' })

const filter = reactive({ status: 0 })
const query = reactive({ pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

const pendingCount = computed(() => list.value.filter(a => a.status === 0).length)

async function loadList() {
  loading.value = true
  try {
    const res = await pageApproval({
      status: filter.status ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally { loading.value = false }
}

const modal = reactive({ show: false, row: null, action: 'approve', comment: '', loading: false })
function openApprove(row, ok) {
  modal.row = row
  modal.action = ok ? 'approve' : 'reject'
  modal.comment = ''
  modal.show = true
}
async function confirmAction() {
  if (modal.action === 'reject' && !modal.comment.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  modal.loading = true
  try {
    const payload = { id: modal.row.id, comment: modal.comment || undefined }
    if (modal.action === 'approve') {
      await approveApproval(payload)
      ElMessage.success('已通过,合同进入执行中')
    } else {
      await rejectApproval({ id: modal.row.id, comment: modal.comment })
      ElMessage.success('已驳回')
    }
    modal.show = false
    loadList()
  } finally { modal.loading = false }
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { margin-top: 4px; font-size: 13.5px; color: var(--muted); }

.filter-bar { margin-bottom: 16px; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.mono.accent.link { cursor: pointer; }
.mono.accent.link:hover { text-decoration: underline; }
.price { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; font-weight: 600; color: var(--ink); }
.text-muted { color: var(--subtle); }
.text-muted.micro { font-size: 12px; }
.action-link { padding: 0 6px; }
.action-link.danger { color: var(--danger); }

.zen-status { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.zen-status::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; }
.zen-status.gray { background: #f3f4f6; color: #6b7280; } .zen-status.gray::before { background: #9ca3af; }
.zen-status.ok { background: #d1fae5; color: #065f46; } .zen-status.ok::before { background: #10b981; }

.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

.modal-summary { margin-bottom: 16px; }
.modal-name { font-weight: 500; }
</style>
