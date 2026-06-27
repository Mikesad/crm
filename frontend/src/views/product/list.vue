<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">产品库</div>
        <div class="page-sub">共 {{ total }} 个产品 · 上架 {{ onShelfCount }} · 下架 {{ offShelfCount }}</div>
      </div>
      <div v-if="hasPerm('crm:product:edit')">
        <el-button :icon="Plus" class="btn-zen-primary" @click="handleCreate">新建产品</el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索产品编码 / 名称"
        class="search"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="全部状态" class="filter" clearable @change="handleSearch">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <div class="spacer" />
      <el-button :icon="Search" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="productCode" label="产品编码" width="150">
          <template #default="{ row }">
            <span class="mono accent">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="200">
          <template #default="{ row }">
            <div class="name-block">
              <span class="name">{{ row.productName }}</span>
              <span v-if="row.spec" class="sub">{{ row.spec }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标准售价" width="140" align="right">
          <template #default="{ row }">
            <span class="price">¥ {{ Number(row.price).toLocaleString() }}</span>
            <span v-if="row.unit" class="text-muted"> / {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light">上架</el-tag>
            <el-tag v-else effect="plain">下架</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="hasPerm('crm:product:edit')" label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link class="action-link" @click="handleEdit(row)">编辑</el-button>
            <el-button link class="action-link danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑产品弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editing.id ? '编辑产品' : '新建产品'"
      width="520px"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="editing.productCode" placeholder="如: P-CRM-001" :disabled="!!editing.id" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="editing.productName" placeholder="如: ZenCRM 企业版" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="editing.spec" placeholder="如: 50 用户 / 1 年" />
        </el-form-item>
        <el-form-item label="标准售价" prop="price">
          <el-input-number
            v-model="editing.price"
            :min="0"
            :precision="2"
            :step="1000"
            controls-position="right"
            style="width: 200px;"
          />
          <span class="text-muted" style="margin-left: 8px;">元</span>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="editing.unit" placeholder="套/个/人天" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editing.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { pageProduct, addProduct, updateProduct, deleteProduct } from '@/api/product'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'ProductList' })

const { hasPerm } = useAuth()

// ---------- 状态 ----------
const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const onShelfCount = computed(() => list.value.filter(p => p.status === 1).length)
const offShelfCount = computed(() => list.value.filter(p => p.status === 0).length)

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

// ---------- 加载 ----------
async function loadList() {
  loading.value = true
  try {
    const res = await pageProduct({
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

function handleSearch() {
  query.pageNum = 1
  loadList()
}
function handleReset() {
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  loadList()
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, productCode: '', productName: '', spec: '', price: 0, unit: '个', status: 1 })
const editRules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入标准售价', trigger: 'blur' }]
}

function resetEditForm() {
  editing.id = null
  editing.productCode = ''
  editing.productName = ''
  editing.spec = ''
  editing.price = 0
  editing.unit = '个'
  editing.status = 1
}

function handleCreate() {
  resetEditForm()
  editVisible.value = true
}
function handleEdit(row) {
  Object.assign(editing, row)
  editVisible.value = true
}
async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await updateProduct(editing)
      ElMessage.success('已更新')
    } else {
      await addProduct(editing)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除产品「${row.productName}」?此操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' }
  )
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
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
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.accent { color: var(--accent); }
.price { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; font-weight: 600; color: var(--ink); }
.text-muted { color: var(--subtle); }
.action-link { padding: 0 6px; }
.action-link.danger { color: var(--danger); }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }
</style>
