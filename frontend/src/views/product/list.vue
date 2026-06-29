<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">产品库</div>
        <div class="page-sub" v-if="activeTab === 'product'">
          共 <span class="accent">{{ total }}</span> 个产品 ·
          上架 <span class="accent">{{ onShelfCount }}</span> ·
          下架 <span class="muted">{{ offShelfCount }}</span>
        </div>
        <div class="page-sub" v-else>
          共 <span class="accent">{{ categoryTotal }}</span> 个分类 ·
          顶级 <span class="muted">{{ rootCount }}</span> ·
          已用 <span class="pro">{{ usedCount }}</span> / {{ categoryTotal }} (含产品)
        </div>
      </div>
      <div v-if="activeTab === 'product' && hasPerm('crm:product:edit')">
        <el-button :icon="Plus" class="btn-zen-primary" @click="handleCreate">新建产品</el-button>
      </div>
      <div v-else-if="hasPerm('crm:product:category:edit')">
        <el-button :icon="Plus" class="btn-zen-primary" @click="handleCreateCategory">新建分类</el-button>
      </div>
    </div>

    <!-- ===== Tabs(v0.5:D7 修订,产品分类作为产品库 Tab 存在) ===== -->
    <div class="tabs">
      <div :class="['tab', { active: activeTab === 'product' }]" @click="switchTab('product')">
        产品
      </div>
      <div :class="['tab', { active: activeTab === 'category' }]" @click="switchTab('category')">
        产品分类
      </div>
    </div>

    <!-- ===== 产品 Tab ===== -->
    <template v-if="activeTab === 'product'">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索产品编码 / 名称" class="search" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.categoryId" placeholder="全部分类" class="filter" clearable @change="handleSearch">
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
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
          <el-table-column prop="productName" label="产品名称" min-width="220">
            <template #default="{ row }">
              <div class="name-block">
                <span class="name">{{ row.productName }}</span>
                <span v-if="row.spec" class="sub">{{ row.spec }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.categoryName" type="info" effect="plain" size="small">{{ row.categoryName }}</el-tag>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="标准售价" width="140" align="right">
            <template #default="{ row }">
              <span class="price">¥ {{ Number(row.price).toLocaleString() }}</span>
              <span v-if="row.unit" class="text-muted"> / {{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" effect="light" size="small">上架</el-tag>
              <el-tag v-else effect="plain" size="small">下架</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="140">
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

      <!-- 新建/编辑产品弹窗(v0.6 对齐 frontend-design/UI/新建产品.png) -->
      <el-dialog v-model="editVisible" :title="editing.id ? '编辑产品' : '新建产品'" width="640px" @closed="resetEditForm">
        <el-form ref="editFormRef" :model="editing" :rules="editRules" label-position="top" class="product-form">
          <!-- Row 1: 编码 + 名称 -->
          <el-form-item label="产品编码" prop="productCode">
            <el-input v-model="editing.productCode" placeholder="如: P-CRM-BASE-02" :disabled="!!editing.id" />
            <div class="form-help">全局唯一 / SKU,创建后不可修改</div>
          </el-form-item>
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="editing.productName" placeholder="如: ZenCRM 基础版" />
          </el-form-item>

          <!-- Row 2: 分类 + 规格 -->
          <el-form-item label="产品分类" prop="categoryId">
            <el-select v-model="editing.categoryId" placeholder="请选择分类" clearable style="width: 100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
            </el-select>
            <div class="form-help">
              分类用于合同选择时归类;
              <a href="javascript:;" class="form-link" @click="goCategoryTab">去维护分类 →</a>
            </div>
          </el-form-item>
          <el-form-item label="规格型号">
            <el-input v-model="editing.spec" placeholder="如: 50 用户 / 1 年" />
          </el-form-item>

          <!-- Row 3: 价格 + 单位 -->
          <el-form-item label="标准售价" prop="price">
            <div class="price-input">
              <span class="price-prefix">¥</span>
              <el-input-number v-model="editing.price" :min="0" :precision="2" :step="1000" controls-position="right" class="price-number" />
              <span class="price-suffix">元</span>
            </div>
          </el-form-item>
          <el-form-item label="单位">
            <el-input v-model="editing.unit" placeholder="套/个/人天" />
          </el-form-item>

          <!-- Row 4: 状态(整行,segmented 风格) -->
          <el-form-item label="状态" class="form-row-full">
            <div class="status-seg">
              <div :class="['status-seg-item', { on: editing.status === 1 }]" @click="editing.status = 1">上架</div>
              <div :class="['status-seg-item', { on: editing.status === 0 }]" @click="editing.status = 0">下架</div>
            </div>
            <div class="form-help">下架后,合同新建时不可再选;已签订合同不受影响</div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editVisible = false">取消</el-button>
          <el-button class="btn-zen-primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- ===== 分类 Tab ===== -->
    <template v-else>
      <div class="banner">
        <el-icon><InfoFilled /></el-icon>
        <span>
          分类用于合同新建时的产品归类与产品下拉过滤;
          <strong>删除分类前需先迁移该分类下所有产品</strong>,否则产品将变成"未分类"。
        </span>
      </div>

      <div class="toolbar">
        <el-input v-model="catQuery.keyword" placeholder="搜索分类名称" class="search" clearable @keyup.enter="handleCatSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="catQuery.hasProduct" placeholder="使用状态" class="filter" clearable @change="handleCatSearch">
          <el-option label="已使用" :value="1" />
          <el-option label="未使用" :value="0" />
        </el-select>
        <div class="spacer" />
        <el-button :icon="Search" @click="handleCatSearch">查询</el-button>
        <el-button @click="handleCatReset">重置</el-button>
      </div>

      <el-card class="table-card" v-loading="catLoading">
        <el-table :data="catList" stripe>
          <el-table-column prop="id" label="序号" width="80">
            <template #default="{ row }">
              <span class="mono text-muted">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类名称" min-width="220">
            <template #default="{ row }">
              <div class="name-block">
                <span class="name">{{ row.categoryName }}</span>
                <span class="sub">顶级分类</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="parentId" label="层级" width="120">
            <template #default="{ row }">
              <el-tag v-if="!row.parentId || row.parentId === 0" type="success" effect="plain" size="small">顶级</el-tag>
              <el-tag v-else size="small">子级</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="productCount" label="关联产品数" width="140" align="center">
            <template #default="{ row }">
              <span class="chip chip-count">{{ row.productCount ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              <span class="text-muted">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="hasPerm('crm:product:category:edit')" label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link class="action-link" @click="handleEditCategory(row)">编辑</el-button>
              <el-button link class="action-link danger" :disabled="(row.productCount ?? 0) > 0" @click="handleDeleteCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="catQuery.pageNum"
          v-model:page-size="catQuery.pageSize"
          :total="categoryTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @current-change="loadCatList"
          @size-change="loadCatList"
        />
      </el-card>

      <!-- 新建/编辑分类弹窗 -->
      <el-dialog v-model="catEditVisible" :title="catEditing.id ? '编辑分类' : '新建分类'" width="480px" @closed="resetCatEditForm">
        <el-form ref="catEditFormRef" :model="catEditing" :rules="catEditRules" label-position="top">
          <el-form-item label="父分类">
            <el-select v-model="catEditing.parentId" placeholder="顶级分类" style="width: 100%">
              <el-option label="— 顶级分类 —" :value="0" />
              <el-option v-for="c in otherCategories" :key="c.id" :label="c.categoryName" :value="c.id" />
            </el-select>
            <div class="help">V1 暂不开放多级分类;选择顶级即作为一级分类</div>
          </el-form-item>
          <el-form-item label="分类名称" prop="categoryName">
            <el-input v-model="catEditing.categoryName" placeholder="如 高级支持服务" />
            <div class="help">同一父分类下不可重复,建议 2-8 个字</div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="catEditVisible = false">取消</el-button>
          <el-button class="btn-zen-primary" :loading="catSaving" @click="handleSaveCategory">保存</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, InfoFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { pageProduct, addProduct, updateProduct, deleteProduct } from '@/api/product'
import { pageCategory, listCategory, addCategory, updateCategory, deleteCategory } from '@/api/product-category'
import { useAuth } from '@/composables/useAuth'

defineOptions({ name: 'ProductList' })

const { hasPerm } = useAuth()

// ---------- Tabs(v0.5:D7 修订;v0.9:Tab 不显示数字 badge) ----------
const activeTab = ref('product')

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'product' && list.value.length === 0) loadList()
  if (tab === 'category' && catList.value.length === 0) loadCatList()
}

/**
 * 产品表单里的"去维护分类→"链接触发:关闭弹窗 + 切到分类 Tab
 * (v0.6 对齐 UI 图新建产品.png,分类下方快捷入口)
 */
function goCategoryTab() {
  editVisible.value = false
  switchTab('category')
}

// ====================== 产品 Tab 状态(v0.7 移除套餐线/计费周期) ======================
const query = reactive({ keyword: '', categoryId: null, status: null, pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])

const onShelfCount = computed(() => list.value.filter(p => p.status === 1).length)
const offShelfCount = computed(() => list.value.filter(p => p.status === 0).length)

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

async function loadList() {
  loading.value = true
  try {
    const res = await pageProduct({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId ?? undefined,
      status: query.status ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []; total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await listCategory()
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

function handleSearch() { query.pageNum = 1; loadList() }
function handleReset() {
  query.keyword = ''; query.categoryId = null; query.status = null
  query.pageNum = 1; loadList()
}

// 编辑
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editing = reactive({ id: null, productCode: '', productName: '', categoryId: null, spec: '', price: 0, unit: '个', status: 1 })
const editRules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入标准售价', trigger: 'blur' }]
}

function resetEditForm() {
  editing.id = null; editing.productCode = ''; editing.productName = ''
  editing.categoryId = null; editing.spec = ''; editing.price = 0; editing.unit = '个'
  editing.status = 1
}

function handleCreate() { resetEditForm(); editVisible.value = true }
function handleEdit(row) {
  Object.assign(editing, {
    id: row.id, productCode: row.productCode, productName: row.productName,
    categoryId: row.categoryId, spec: row.spec || '', price: row.price || 0,
    unit: row.unit || '个', status: row.status
  })
  editVisible.value = true
}
async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    if (editing.id) { await updateProduct(editing); ElMessage.success('已更新') }
    else { await addProduct(editing); ElMessage.success('已创建') }
    editVisible.value = false; loadList()
  } finally { saving.value = false }
}
async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除产品「${row.productName}」?此操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' }
  )
  await deleteProduct(row.id); ElMessage.success('已删除'); loadList()
}

// ====================== 分类 Tab 状态 ======================
const catQuery = reactive({ keyword: '', hasProduct: null, pageNum: 1, pageSize: 10 })
const catList = ref([])
const categoryTotal = ref(0)
const catLoading = ref(false)

const rootCount = computed(() => catList.value.filter(c => !c.parentId || c.parentId === 0).length)
const usedCount = computed(() => catList.value.filter(c => (c.productCount ?? 0) > 0).length)
const otherCategories = computed(() => catList.value.filter(c => c.id !== catEditing.id))

async function loadCatList() {
  catLoading.value = true
  try {
    const res = await pageCategory({
      keyword: catQuery.keyword || undefined,
      pageNum: catQuery.pageNum,
      pageSize: catQuery.pageSize
    })
    let records = res.data?.records || []
    if (catQuery.hasProduct != null) {
      records = records.filter(r => {
        const c = r.productCount ?? 0
        return catQuery.hasProduct === 1 ? c > 0 : c === 0
      })
    }
    catList.value = records
    categoryTotal.value = res.data?.total || 0
  } catch (e) {
    catList.value = []; categoryTotal.value = 0
  } finally {
    catLoading.value = false
  }
}

function handleCatSearch() { catQuery.pageNum = 1; loadCatList() }
function handleCatReset() { catQuery.keyword = ''; catQuery.hasProduct = null; catQuery.pageNum = 1; loadCatList() }

const catEditVisible = ref(false)
const catSaving = ref(false)
const catEditFormRef = ref(null)
const catEditing = reactive({ id: null, parentId: 0, categoryName: '' })
const catEditRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

function resetCatEditForm() { catEditing.id = null; catEditing.parentId = 0; catEditing.categoryName = '' }
function handleCreateCategory() { resetCatEditForm(); catEditVisible.value = true }
function handleEditCategory(row) {
  Object.assign(catEditing, { id: row.id, parentId: row.parentId || 0, categoryName: row.categoryName })
  catEditVisible.value = true
}
async function handleSaveCategory() {
  await catEditFormRef.value.validate()
  catSaving.value = true
  try {
    if (catEditing.id) { await updateCategory(catEditing); ElMessage.success('已更新') }
    else { await addCategory(catEditing); ElMessage.success('已创建') }
    catEditVisible.value = false
    loadCatList()
    // 同时刷新产品 tab 的分类下拉
    loadCategories()
  } finally { catSaving.value = false }
}
async function handleDeleteCategory(row) {
  await ElMessageBox.confirm(
    `确定删除分类「${row.categoryName}」?此操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' }
  )
  await deleteCategory(row.id); ElMessage.success('已删除'); loadCatList(); loadCategories()
}

onMounted(() => {
  loadCategories()
  loadList()
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
.page-sub .accent { color: var(--accent); font-weight: 500; }
.page-sub .muted { color: var(--muted); }
.page-sub .pro { color: var(--info); font-weight: 500; }

/* ===== Tabs(v0.5,复用 phase6-role-permission 风格) ===== */
.tabs { display: flex; gap: 0; border-bottom: 1px solid var(--hairline); margin-bottom: 24px; align-items: center; }
.tab { padding: 10px 16px; font-size: 14px; color: var(--muted); cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; transition: all 0.12s; display: flex; align-items: center; gap: 6px; }
.tab:hover { color: var(--ink-soft); }
.tab.active { color: var(--accent); border-bottom-color: var(--accent); font-weight: 500; }
.tab .badge { font-size: 11px; padding: 1px 6px; background: var(--hairline); color: var(--muted); border-radius: 8px; font-family: var(--font-mono); }
.tab.active .badge { background: var(--accent-ring); color: var(--accent); }

/* toolbar */
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search { width: 240px; }
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
.action-link.is-disabled { color: var(--subtle); cursor: not-allowed; }
.pagination { padding: 12px 16px; display: flex; justify-content: flex-end; }

/* chips(分类 Tab 用) */
.chip { display: inline-flex; align-items: center; padding: 1px 8px; border-radius: 10px; font-size: 11.5px; font-weight: 500; line-height: 18px; white-space: nowrap; }
.chip-count { background: var(--info-soft); color: var(--info); font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }

/* ===== 产品表单(v0.6 对齐新建产品.png) ===== */
.product-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}
.product-form :deep(.form-row-full) { grid-column: 1 / -1; }
.product-form :deep(.el-form-item) { margin-bottom: 16px; }
.product-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  padding: 0 0 4px;
  line-height: 1.4;
}

.form-help {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 4px;
  line-height: 1.4;
}
.form-link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
}
.form-link:hover { text-decoration: underline; }

/* 标准售价 ¥ + input + 元 */
.price-input {
  display: flex;
  align-items: center;
  gap: 8px;
}
.price-prefix {
  font-size: 13px;
  color: var(--muted);
  font-family: var(--font-mono);
}
.price-suffix {
  font-size: 12.5px;
  color: var(--muted);
}
.price-number { flex: 1; }
.price-number :deep(.el-input-number__decrease),
.price-number :deep(.el-input-number__increase) {
  background: var(--bg);
}

/* 状态 segmented */
.status-seg {
  display: inline-flex;
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  overflow: hidden;
}
.status-seg-item {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--ink-soft);
  cursor: pointer;
  border-right: 1px solid var(--hairline);
  background: var(--surface);
  transition: all 0.12s;
  user-select: none;
}
.status-seg-item:last-child { border-right: 0; }
.status-seg-item:hover:not(.on) { background: var(--bg); }
.status-seg-item.on {
  background: var(--accent-pale);
  color: var(--accent);
  font-weight: 500;
}

/* banner(分类 Tab 提示) */
.banner {
  background: var(--info-soft);
  border: 1px solid #bfdbfe;
  border-radius: var(--radius);
  padding: 10px 14px;
  font-size: 12.5px;
  color: var(--info);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.banner strong { color: var(--ink); }

.help { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
</style>