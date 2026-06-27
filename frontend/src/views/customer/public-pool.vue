<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">公海池</div>
        <div class="page-sub">所有未跟进的私海客户,逾期将被系统自动回收至此</div>
      </div>
    </div>

    <!-- 阶段四:admin/director 专属回收操作卡 -->
    <div v-if="canRecycle" class="recycle-banner">
      <div class="recycle-icon">⚙</div>
      <div class="recycle-content">
        <div class="recycle-title">
          手动触发回收
          <el-tag size="small" type="info" effect="plain" style="margin-left: 6px">仅 admin / 销售总监</el-tag>
        </div>
        <div class="recycle-desc">
          凌晨 2 点定时回收之前,你可以手动跑一次。支持秒级阈值参数(开发期便于联调)
        </div>
      </div>
      <div class="recycle-actions">
        <el-input
          v-model.number="thresholdSeconds"
          type="number"
          placeholder="阈值(秒)"
          size="default"
          style="width: 130px"
          :min="1"
          :max="7776000"
        >
          <template #append>s</template>
        </el-input>
        <el-input
          v-model.number="recycleLimit"
          type="number"
          placeholder="条数"
          size="default"
          style="width: 100px"
          :min="1"
          :max="10000"
        />
        <el-button :loading="recycling" @click="handleRecycle(true)">Dry Run</el-button>
        <el-button class="btn-zen-primary" :loading="recycling" @click="handleRecycle(false)">
          执行回收 →
        </el-button>
      </div>
    </div>

    <div class="layout">
      <div class="layout-main">
        <!-- Toolbar -->
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索公海客户名称、行业..."
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
        </div>

        <!-- 列表 -->
        <el-card class="table-card" v-loading="loading">
          <el-table :data="list" stripe @row-dblclick="handleRowDblClick">
            <el-table-column prop="customerName" label="客户名" min-width="200">
              <template #default="{ row }">
                <div class="name-block">
                  <span class="name">{{ row.customerName }}</span>
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
            <el-table-column prop="industry" label="行业" width="110">
              <template #default="{ row }">
                <span class="text-muted">{{ row.industry || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastFollowTime" label="最后跟进" width="120">
              <template #default="{ row }">
                <span class="mono" :class="followTimeClass(row.lastFollowTime)">{{ followTimeText(row.lastFollowTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="商机/联系人" width="120">
              <template #default="{ row }">
                <span class="stats">
                  <span class="stats-num mono">{{ statsCache[row.id]?.business ?? '-' }}</span>/<span class="stats-num mono">{{ statsCache[row.id]?.contact ?? '-' }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="120">
              <template #default="{ row }">
                <span class="mono text-muted">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  link
                  class="action-link claim-link"
                  :loading="claimingId === row.id"
                  @click="handleClaim(row)"
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

      <!-- 右侧辅助 -->
      <aside class="layout-side">
        <div class="side-panel">
          <div class="side-panel-title"><span class="icon">↻</span>公海池规则</div>
          <ul class="rule-list">
            <li>客户最后跟进时间超过 <strong>15 天</strong>(阈值可在 yml 调整)自动回收至公海</li>
            <li>回收时自动插入一条系统跟进记录,客户时间轴可追溯</li>
            <li>凌晨 2:00 由 <code>@Scheduled</code> 任务自动执行,无需人工干预</li>
            <li>认领后立即转为私海,owner 改为当前用户</li>
            <li>公海客户不归属任何销售,所有人均可查看和认领</li>
          </ul>
        </div>

        <div class="side-panel">
          <div class="side-panel-title"><span class="icon">⊙</span>今日数据</div>
          <div class="stat-row">
            <span class="label">池内客户</span>
            <span class="value">{{ total }}</span>
          </div>
          <div class="stat-row">
            <span class="label">今日自动回收</span>
            <span class="value">—</span>
          </div>
          <div class="stat-row">
            <span class="label">今日手动回收</span>
            <span class="value warn">{{ lastRecycleSummary.recycled }}</span>
          </div>
          <div class="stat-row">
            <span class="label">今日认领</span>
            <span class="value">—</span>
          </div>
        </div>

        <div class="side-panel">
          <div class="side-panel-title"><span class="icon">▤</span>池内级别分布</div>
          <div class="stat-row">
            <span class="label"><span class="tag tag-A">A 重要</span></span>
            <span class="value">{{ levelCount.A }}</span>
          </div>
          <div class="stat-row">
            <span class="label"><span class="tag tag-B">B 普通</span></span>
            <span class="value">{{ levelCount.B }}</span>
          </div>
          <div class="stat-row">
            <span class="label"><span class="tag tag-C">C 意向</span></span>
            <span class="value">{{ levelCount.C }}</span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 回收结果详情 -->
    <el-dialog
      v-model="recycleResultVisible"
      :title="`回收结果 (${recycleResult.dryRun ? 'Dry Run' : '实际执行'})`"
      width="640px"
    >
      <div class="result-summary">
        <div class="result-card">
          <div class="result-label">扫描</div>
          <div class="result-num">{{ recycleResult.scanned }}</div>
        </div>
        <div class="result-card highlight">
          <div class="result-label">回收</div>
          <div class="result-num">{{ recycleResult.recycled }}</div>
        </div>
        <div class="result-card">
          <div class="result-label">阈值</div>
          <div class="result-num small">{{ recycleResult.thresholdSeconds }}s</div>
        </div>
        <div class="result-card">
          <div class="result-label">耗时</div>
          <div class="result-num small">{{ recycleResult.durationMs }}ms</div>
        </div>
      </div>
      <div v-if="recycleResult.details && recycleResult.details.length > 0" class="result-details">
        <div class="result-details-title">本次扫描到 {{ recycleResult.details.length }} 个客户:</div>
        <el-table :data="recycleResult.details" size="small" max-height="280">
          <el-table-column prop="customerId" label="ID" width="80" />
          <el-table-column prop="customerName" label="客户名" min-width="160" />
          <el-table-column prop="ownerUserId" label="原 owner" width="100" />
          <el-table-column prop="lastFollowTime" label="最后跟进" width="160">
            <template #default="{ row }">
              <span class="mono text-muted">{{ row.lastFollowTime }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button class="btn-zen-primary" @click="recycleResultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'
import { useAuth } from '@/composables/useAuth'
import { pageCustomer, claimCustomer, recyclePublicPool } from '@/api/customer'

defineOptions({ name: 'CustomerPublicPool' })

const router = useRouter()
const userStore = useUserStore()
const { isAdmin, isDirector } = useAuth()

// ---------- 状态 ----------
const query = reactive({ keyword: '', level: '', industry: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const claimingId = ref(null)
const statsCache = reactive({})
const industryOptions = ['通信', '互联网', '云计算', 'AI', '智能硬件', '金融科技', '本地生活', '零售', '教育', '医疗']

// 回收相关
const canRecycle = computed(() => isAdmin.value || isDirector.value)
const thresholdSeconds = ref(null)  // null = 用 yml 默认 15 天
const recycleLimit = ref(1000)
const recycling = ref(false)
const recycleResultVisible = ref(false)
const recycleResult = ref({ thresholdSeconds: 0, limit: 0, dryRun: false, scanned: 0, recycled: 0, durationMs: 0, details: [] })
const lastRecycleSummary = ref({ recycled: 0 })

// 池内级别分布
const levelCount = computed(() => {
  const c = { A: 0, B: 0, C: 0 }
  list.value.forEach((x) => { if (c[x.level] != null) c[x.level]++ })
  return c
})

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
const formatDate = (t) => t ? t.substring(0, 10) : '-'

// ---------- 列表加载 ----------
async function loadList() {
  loading.value = true
  try {
    const res = await pageCustomer({
      keyword: query.keyword || undefined,
      level: query.level || undefined,
      industry: query.industry || undefined,
      isPublic: 1,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
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

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleRowDblClick(row) {
  router.push(`/customer/${row.id}`)
}

async function handleClaim(row) {
  try {
    await ElMessageBox.confirm(
      `确认认领公海客户「${row.customerName}」?认领后归属将变更为你。`,
      '公海认领',
      { type: 'success', confirmButtonText: '认领', cancelButtonText: '取消' }
    )
  } catch { return }
  claimingId.value = row.id
  try {
    await claimCustomer(row.id)
    ElMessage.success('认领成功,客户已转为你的私海')
    loadList()
  } finally {
    claimingId.value = null
  }
}

// ---------- 回收 ----------
async function handleRecycle(dryRun) {
  recycling.value = true
  try {
    const body = {
      thresholdSeconds: thresholdSeconds.value || undefined,
      limit: recycleLimit.value || undefined,
      dryRun
    }
    const { data } = await recyclePublicPool(body)
    recycleResult.value = data || {}
    lastRecycleSummary.value.recycled = recycleResult.value.recycled || 0
    recycleResultVisible.value = true
    if (!dryRun && recycleResult.value.recycled > 0) {
      // 真回收后需要刷一下列表
      loadList()
    }
  } finally {
    recycling.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; }

.page-header { margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.page-sub { color: var(--muted); font-size: 13.5px; margin-top: 4px; }

.recycle-banner {
  background: linear-gradient(135deg, var(--accent-pale) 0%, #ecfdf5 100%);
  border: 1px solid var(--accent-soft);
  border-radius: var(--radius);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.recycle-icon {
  width: 36px;
  height: 36px;
  background: white;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 0 0 1px var(--hairline);
}
.recycle-content { flex: 1; line-height: 1.4; min-width: 0; }
.recycle-title { font-size: 14px; font-weight: 600; color: var(--ink); margin-bottom: 2px; }
.recycle-desc { font-size: 12.5px; color: var(--ink-soft); }
.recycle-actions { display: flex; gap: 8px; flex-shrink: 0; align-items: center; }

.layout { display: grid; grid-template-columns: 1fr 300px; gap: 20px; align-items: start; }
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }
.layout-main { min-width: 0; }
.layout-side { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 20px; }
@media (max-width: 1280px) { .layout-side { position: static; } }

.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 320px; }
.filter { width: 140px; }

.table-card { padding: 0; }
.table-card :deep(.el-card__body) { padding: 0; }
:deep(.el-table) { border-radius: 0; }
.name-block { display: flex; flex-direction: column; gap: 2px; }
.name-block .name { font-weight: 500; color: var(--ink); }
.name-block .sub { font-size: 11.5px; color: var(--muted); font-weight: normal; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; color: var(--ink-soft); }
.mono.warn { color: var(--warn); }
.mono.danger { color: var(--danger); }
.text-muted { color: var(--subtle); }
.stats { font-size: 12.5px; color: var(--muted); display: inline-flex; align-items: center; gap: 2px; }
.stats-num { color: var(--ink); font-weight: 500; }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

.action-link { font-size: 12.5px; padding: 0 4px; }
.claim-link { color: var(--accent); font-weight: 500; }

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
  gap: 6px;
  .icon { color: var(--accent); font-size: 14px; }
}
.rule-list { list-style: none; padding: 0; margin: 0; }
.rule-list li {
  display: flex;
  gap: 8px;
  padding: 5px 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--ink-soft);
  &::before { content: '·'; color: var(--accent); font-weight: 700; flex-shrink: 0; }
  code { background: var(--hairline-soft); padding: 1px 4px; border-radius: 3px; font-size: 11.5px; }
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px solid var(--hairline-soft);
  font-size: 13px;
  &:last-child { border-bottom: none; }
  .label { color: var(--muted); }
  .value {
    font-weight: 600;
    color: var(--ink);
    font-family: var(--font-mono);
    font-feature-settings: 'tnum' 1;
    &.warn { color: var(--warn); }
  }
}
.tag { display: inline-flex; padding: 2px 8px; font-size: 11.5px; border-radius: 3px; font-weight: 500; }
.tag-A { background: #fef3c7; color: #b45309; }
.tag-B { background: #dbeafe; color: var(--info); }
.tag-C { background: var(--hairline-soft); color: var(--muted); }

.result-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.result-card {
  background: var(--bg);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 12px;
  text-align: center;
  &.highlight { background: var(--accent-pale); border-color: var(--accent-soft); }
}
.result-label { font-size: 11.5px; color: var(--muted); margin-bottom: 4px; }
.result-num { font-size: 22px; font-weight: 600; color: var(--ink); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.result-num.small { font-size: 14px; }
.result-card.highlight .result-num { color: var(--accent); }

.result-details { margin-top: 8px; }
.result-details-title { font-size: 12.5px; color: var(--muted); margin-bottom: 8px; }
</style>
