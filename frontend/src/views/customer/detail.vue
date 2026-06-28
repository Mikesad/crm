<template>
  <div class="page" v-loading="loading">
    <div v-if="customer.id">
      <!-- Header -->
      <div class="detail-header">
        <div class="detail-title">
          <div class="detail-name">{{ customer.customerName }}</div>
          <div class="detail-meta">
            <el-tag v-if="customer.level === 'A'" type="success" effect="light">A 重要客户</el-tag>
            <el-tag v-else-if="customer.level === 'B'" type="primary" effect="light">B 普通客户</el-tag>
            <el-tag v-else type="info" effect="plain">C 意向客户</el-tag>
            <span class="sep">·</span>
            <span>{{ customer.industry || '未分类' }}</span>
            <span class="sep">·</span>
            <span>归属 {{ customer.ownerName || '-' }}</span>
            <span v-if="isReadOnly" class="sep">·</span>
            <span v-if="isReadOnly" class="readonly-tag">🔒 只读共享</span>
            <span class="sep">·</span>
            <span>最后跟进 <span class="mono" :class="followClass(customer.lastFollowTime)">{{ followText(customer.lastFollowTime) }}</span></span>
          </div>
        </div>
        <div class="detail-actions">
          <el-button :icon="Plus" :disabled="isReadOnly" @click="handleAddBusiness">新建商机</el-button>
          <el-button v-if="isOwner" :icon="ShareIcon" @click="openShareDialog">共享</el-button>
          <el-button :disabled="isReadOnly" @click="handleEdit">编辑</el-button>
        </div>
      </div>

      <!-- 阶段四:只读模式顶部警示条 -->
      <el-alert
        v-if="isReadOnly"
        type="warning"
        show-icon
        :closable="false"
        class="readonly-banner"
        title="你对该客户只有只读权限"
        description="由主销售共享给你(只读);如需编辑请让主销售改为「读写」权限,或前往公海池认领。"
      />

      <!-- Tabs -->
      <div class="tabs">
        <div class="tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">基本信息</div>
        <div class="tab" :class="{ active: activeTab === 'timeline' }" @click="activeTab = 'timeline'">
          跟进记录
        </div>
      </div>

      <div class="layout">
        <!-- 主区 -->
        <div class="layout-main">
          <!-- 基本信息 tab:含客户信息 + 联系人 + 商机三段 -->
          <div v-if="activeTab === 'basic'" class="basic-stack">
            <!-- 1. 客户基础信息 -->
            <div class="panel">
              <div class="section-head">
                <div class="section-title">客户信息</div>
              </div>
              <div class="info-grid">
                <div class="info-item"><div class="info-label">客户名称</div><div class="info-value">{{ customer.customerName }}</div></div>
                <div class="info-item"><div class="info-label">客户级别</div><div class="info-value">{{ levelText(customer.level) }}</div></div>
                <div class="info-item"><div class="info-label">所属行业</div><div class="info-value">{{ customer.industry || '未分类' }}</div></div>
                <div class="info-item"><div class="info-label">归属销售</div><div class="info-value">{{ customer.ownerName || '-' }}</div></div>
                <div class="info-item"><div class="info-label">最后跟进</div><div class="info-value mono" :class="followClass(customer.lastFollowTime)">{{ followText(customer.lastFollowTime) }}</div></div>
                <div class="info-item"><div class="info-label">商机金额</div><div class="info-value mono">¥ {{ formatAmount(businessAmount) }}</div></div>
                <div class="info-item"><div class="info-label">建立于</div><div class="info-value mono">{{ formatDate(customer.createTime) }}</div></div>
                <div class="info-item"><div class="info-label">客户 ID</div><div class="info-value mono">#{{ customer.id }}</div></div>
              </div>
            </div>

            <!-- 2. 联系人 -->
            <div class="panel">
              <div class="section-head">
                <div class="section-title">联系人 ({{ contacts.length }})</div>
                <el-button class="btn-zen-primary" :icon="Plus" size="small" :disabled="isReadOnly" @click="handleAddContact">新建联系人</el-button>
              </div>
              <el-card class="list-card" v-loading="loadingContact">
                <el-table :data="contacts" stripe>
                  <el-table-column label="姓名" min-width="140">
                    <template #default="{ row }">
                      <span class="name">{{ row.contactName }}</span>
                      <el-tag v-if="row.isMaster === 1" type="success" size="small" effect="light" style="margin-left: 6px">主</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="post" label="职务" min-width="120" />
                  <el-table-column prop="phone" label="手机" min-width="140">
                    <template #default="{ row }">
                      <span class="mono">{{ row.phone || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="决策权重" width="100">
                    <template #default="{ row }">
                      <el-tag :type="weightTagType(row.decisionWeight)" effect="light" size="small">
                        {{ weightText(row.decisionWeight) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="140" fixed="right">
                    <template #default="{ row }">
                      <el-button link class="action-link" :disabled="isReadOnly" @click.stop="handleEditContact(row)">编辑</el-button>
                      <el-button link class="action-link danger" :disabled="isReadOnly" @click.stop="handleDeleteContact(row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>

            <!-- 3. 商机 -->
            <div class="panel">
              <div class="section-head">
                <div class="section-title">商机 ({{ businesses.length }})</div>
                <el-button class="btn-zen-primary" :icon="Plus" size="small" :disabled="isReadOnly" @click="handleAddBusiness">新建商机</el-button>
              </div>
              <el-card class="list-card" v-loading="loadingBusiness">
                <el-table :data="businesses" stripe>
                  <el-table-column prop="businessName" label="商机名称" min-width="200">
                    <template #default="{ row }">
                      <span class="name">{{ row.businessName }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="阶段" width="130">
                    <template #default="{ row }">
                      <el-tag :type="stageTagType(row.stage)" effect="light" size="small">{{ row.stage }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="预计金额" width="140" align="right">
                    <template #default="{ row }">
                      <span class="mono">¥ {{ formatAmount(row.expectedAmount) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="ownerName" label="负责人" width="100" />
                  <el-table-column label="预计结单" width="120">
                    <template #default="{ row }">
                      <span class="mono">{{ row.expectedDealDate || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="160" fixed="right">
                    <template #default="{ row }">
                      <el-button link class="action-link" @click.stop="goBusinessDetail(row.id)">详情</el-button>
                      <el-button link class="action-link" :disabled="isReadOnly" @click.stop="handleStage(row)">推进阶段</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>

          <!-- 跟进记录 tab -->
          <div v-else-if="activeTab === 'timeline'">
            <div class="timeline-card">
              <div class="timeline-head">
                <div class="timeline-title">
                  📋 跟进记录
                  <span class="count">· 共 {{ recordCount }} 条</span>
                </div>
                <el-button
                  class="btn-zen-primary"
                  size="small"
                  @click="handleAddRecord"
                >✚ 新建跟进</el-button>
              </div>
              <div class="timeline-body">
                <RecordTimeline :key="timelineKey" related-type="customer" :related-id="customer.id" />
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧辅助面板 -->
        <aside class="layout-side">
          <!-- 客户摘要 -->
          <div class="side-panel">
            <div class="side-panel-title">客户摘要</div>
            <div class="cust-card-row"><span class="label">客户级别</span><span class="value">{{ levelText(customer.level) }}</span></div>
            <div class="cust-card-row"><span class="label">所属行业</span><span class="value">{{ customer.industry || '未分类' }}</span></div>
            <div class="cust-card-row"><span class="label">归属销售</span><span class="value">{{ customer.ownerName || '-' }}</span></div>
            <div class="cust-card-row"><span class="label">最后跟进</span><span class="value" :class="followClass(customer.lastFollowTime)">{{ followText(customer.lastFollowTime) }}</span></div>
            <div class="cust-card-row"><span class="label">商机金额</span><span class="value mono">¥ {{ formatAmount(businessAmount) }}</span></div>
            <div class="cust-card-row"><span class="label">建立于</span><span class="value mono">{{ formatDate(customer.createTime) }}</span></div>
          </div>

          <!-- 关键联系人 -->
          <div class="side-panel">
            <div class="side-panel-title">
              关键联系人 <span class="more" @click="activeTab = 'contact'">管理 →</span>
            </div>
            <div v-if="keyContacts.length === 0" class="empty-mini">暂无联系人</div>
            <div v-for="c in keyContacts" :key="c.id" class="kc-item">
              <div class="kc-avatar">{{ c.contactName?.charAt(0) }}</div>
              <div class="kc-meta">
                <div class="kc-name">
                  {{ c.contactName }}
                  <el-tag v-if="c.isMaster === 1" type="success" size="small" effect="light">主</el-tag>
                </div>
                <div class="kc-post">{{ c.post || '-' }} · {{ weightText(c.decisionWeight) }}</div>
              </div>
            </div>
          </div>

          <!-- 智能建议（mock，阶段三接入业务规则） -->
          <div class="side-panel">
            <div class="side-panel-title">智能建议</div>
            <div class="sug-item">
              <div class="sug-icon" :class="suggestions.overdue ? 'warn' : 'accent'">
                {{ suggestions.overdue ? '!' : '✓' }}
              </div>
              <div class="sug-content">
                <div class="sug-title">{{ suggestions.overdue ? `客户 ${daysSinceLastFollow()} 天未跟进` : '跟进状态良好' }}</div>
                <div class="sug-desc">{{ suggestions.overdue ? '建议立即电话沟通，推动主商机' : '继续保持每周一次的跟进节奏' }}</div>
                <div v-if="suggestions.overdue" class="sug-action" @click="handleAddRecord">立即新建跟进 →</div>
              </div>
            </div>
            <div v-if="suggestions.advancingBusiness" class="sug-item">
              <div class="sug-icon accent">↑</div>
              <div class="sug-content">
                <div class="sug-title">商机待推进</div>
                <div class="sug-desc">{{ suggestions.advancingBusiness }}</div>
                <div class="sug-action" @click="activeTab = 'business'">前往商机 →</div>
              </div>
            </div>
            <div v-if="!suggestions.overdue && !suggestions.advancingBusiness" class="sug-item">
              <div class="sug-icon accent">★</div>
              <div class="sug-content">
                <div class="sug-title">暂无紧急事项</div>
                <div class="sug-desc">建议每周检查一次商机漏斗，保持节奏</div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 联系人弹窗 -->
    <el-dialog v-model="contactVisible" :title="contactForm.id ? '编辑联系人' : '新建联系人'" width="480px" @closed="resetContactForm">
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-position="top">
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" />
        </el-form-item>
        <el-form-item label="职务">
          <el-input v-model="contactForm.post" placeholder="如：采购总监" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="contactForm.phone" placeholder="如：138 0013 8000" />
        </el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="contactForm.isMasterBool" @change="onMasterChange" />
        </el-form-item>
        <el-form-item label="决策权重">
          <el-radio-group v-model="contactForm.decisionWeight">
            <el-radio :value="1">1 核心决策者</el-radio>
            <el-radio :value="2">2 弱影响者</el-radio>
            <el-radio :value="3">3 普通职员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="savingContact" @click="handleSaveContact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 商机弹窗 -->
    <el-dialog v-model="businessVisible" title="新建商机" width="480px" @closed="resetBusinessForm">
      <el-form ref="businessFormRef" :model="businessForm" :rules="businessRules" label-position="top">
        <el-form-item label="商机名称" prop="businessName">
          <el-input v-model="businessForm.businessName" placeholder="如：全集团 CRM 部署" />
        </el-form-item>
        <el-form-item label="预计金额">
          <el-input v-model="businessForm.expectedAmount" placeholder="如：2400000.00" />
        </el-form-item>
        <el-form-item label="预计结单日期">
          <el-date-picker v-model="businessForm.expectedDealDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="businessVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="savingBusiness" @click="handleSaveBusiness">保存</el-button>
      </template>
    </el-dialog>

    <!-- 阶段推进弹窗 -->
    <el-dialog v-model="stageVisible" :title="`推进商机阶段 - ${stagingRow?.businessName || ''}`" width="460px">
      <div class="stage-current">
        当前阶段：<el-tag :type="stageTagType(stagingRow?.stage)" effect="light">{{ stagingRow?.stage }}</el-tag>
      </div>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-position="top" style="margin-top: 16px;">
        <el-form-item label="目标阶段" prop="stage">
          <el-select v-model="stageForm.stage" placeholder="选择目标阶段" style="width: 100%">
            <el-option v-for="s in nextStageOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="stageForm.followContent" type="textarea" :rows="3" placeholder="说明本次推进的背景与客户反馈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageVisible = false">取消</el-button>
        <el-button class="btn-zen-primary" :loading="savingStage" @click="handleSaveStage">确认推进</el-button>
      </template>
    </el-dialog>

    <!-- 写跟进弹窗(阶段五:抽成通用 AddRecordDialog) -->
    <AddRecordDialog
      v-if="customer.id"
      v-model:visible="recordVisible"
      related-type="customer"
      :related-id="customer.id"
      :related-name="customer.customerName"
      @saved="onRecordSaved"
    />

    <!-- 阶段四:共享对话框 -->
    <CustomerShareDialog
      v-model:visible="shareDialogVisible"
      :customer="customer"
      @shared="onSharedOk"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Share as ShareIcon } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'
import { getCustomer, updateCustomer } from '@/api/customer'
import { listContact, addContact, updateContact, deleteContact } from '@/api/contact'
import { pageBusiness, addBusiness, updateBusinessStage } from '@/api/business'
import RecordTimeline from '@/components/RecordTimeline.vue'
import AddRecordDialog from '@/components/AddRecordDialog.vue'
import CustomerShareDialog from './components/ShareDialog.vue'
import { getTimeline } from '@/api/record'

defineOptions({ name: 'CustomerDetail' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---------- 状态 ----------
const customer = ref({})
const contacts = ref([])
const businesses = ref([])
const loading = ref(false)
const loadingContact = ref(false)
const loadingBusiness = ref(false)
const activeTab = ref('basic')   // 合并后默认展示"基本信息" Tab(含客户信息/联系人/商机三段)
const recordCount = ref(0)
const timelineKey = ref(0)

// ---------- 阶段四:写权限判定 ----------
const myUserId = computed(() => userStore.userId)
const isAdmin = computed(() => Array.isArray(userStore.roleKeys) && userStore.roleKeys.includes('admin'))
const isOwner = computed(() => {
  if (!myUserId.value || !customer.value.ownerUserId) return false
  return Number(customer.value.ownerUserId) === Number(myUserId.value)
})
const isPublicCustomer = computed(() => customer.value.isPublic === 1)
// 只读 = 非 owner + 非 admin + 非公海
// (admin 有全量权限,任何客户都可写;owner 可写;公海客户允许认领即编辑)
const isReadOnly = computed(() => {
  if (!customer.value.id) return false   // 客户未加载完,不展示警示
  if (isOwner.value) return false
  if (isAdmin.value) return false
  if (isPublicCustomer.value) return false
  return true                            // 被共享给我(读写未知)或其他人 → 只读
})

// 共享对话框
const shareDialogVisible = ref(false)
function openShareDialog() {
  if (!isOwner.value) return
  shareDialogVisible.value = true
}
function onSharedOk() {
  // 共享操作不影响当前 owner 的可见性,无需 reload
}

// ---------- 工具函数 ----------
const followText = (t) => {
  if (!t) return '从未跟进'
  const days = dayjs().diff(dayjs(t), 'day')
  if (days < 1) return '今天'
  if (days < 7) return days + ' 天前'
  return dayjs(t).format('MM-DD')
}
const followClass = (t) => {
  if (!t) return ''
  const days = dayjs().diff(dayjs(t), 'day')
  if (days > 7) return 'danger'
  if (days > 3) return 'warn'
  return ''
}
const daysSinceLastFollow = () => {
  if (!customer.value.lastFollowTime) return 0
  return dayjs().diff(dayjs(customer.value.lastFollowTime), 'day')
}
const formatDate = (t) => t ? t.substring(0, 10) : '-'
const formatAmount = (a) => {
  if (a == null) return '0'
  return Number(a).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}
const levelText = (l) => ({ A: 'A 重要', B: 'B 普通', C: 'C 意向' }[l] || '-')
const weightText = (w) => ({ 1: '核心决策', 2: '弱影响', 3: '普通职员' }[w] || '-')
const weightTagType = (w) => ({ 1: 'success', 2: 'primary', 3: 'info' }[w] || 'info')
const stageColor = (s) => ({ '需求分析': '#a1a1aa', '方案报价': '#b45309', '商务谈判': '#1e40af', '赢单': '#166534', '输单': '#b91c1c' }[s] || '#a1a1aa')
const stageTagType = (s) => ({ '需求分析': 'info', '方案报价': 'warning', '商务谈判': 'primary', '赢单': 'success', '输单': 'danger' }[s] || 'info')

const businessAmount = computed(() => businesses.value.reduce((sum, b) => sum + (Number(b.expectedAmount) || 0), 0))
const keyContacts = computed(() => contacts.value.slice(0, 3))

const suggestions = computed(() => {
  const days = daysSinceLastFollow()
  const advancing = businesses.value.find(b => b.stage === '商务谈判')
  return {
    overdue: days >= 5,
    advancingBusiness: advancing ? `「${advancing.businessName}」已在商务谈判 ${days} 天` : null
  }
})

// ---------- 数据加载 ----------
async function loadCustomer() {
  loading.value = true
  try {
    const res = await getCustomer(route.params.id)
    customer.value = res.data || {}
  } catch (e) {
    customer.value = {}
  } finally {
    loading.value = false
  }
}

async function loadContacts() {
  loadingContact.value = true
  try {
    const res = await listContact({ customerId: route.params.id })
    contacts.value = res.data || []
  } catch (e) {
    contacts.value = []
  } finally {
    loadingContact.value = false
  }
}

async function loadBusinesses() {
  loadingBusiness.value = true
  try {
    const res = await pageBusiness({ customerId: route.params.id, pageNum: 1, pageSize: 100 })
    businesses.value = res.data?.records || []
  } catch (e) {
    businesses.value = []
  } finally {
    loadingBusiness.value = false
  }
}

async function loadAll() {
  await loadCustomer()
  await Promise.all([loadContacts(), loadBusinesses(), loadRecordCount()])
}

// ---------- 客户编辑 ----------
function handleEdit() {
  ElMessageBox.prompt('修改客户名称', '编辑客户', {
    inputValue: customer.value.customerName,
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  }).then(async ({ value }) => {
    if (!value) return
    await updateCustomer({ id: customer.value.id, customerName: value })
    ElMessage.success('已更新')
    loadCustomer()
  }).catch(() => {})
}

// ---------- 跳转 ----------
function goBusinessDetail(id) {
  if (id) router.push(`/business/${id}`)
}

// ---------- 联系人 CRUD ----------
const contactVisible = ref(false)
const contactFormRef = ref(null)
const savingContact = ref(false)
const contactForm = reactive({ id: null, customerId: null, contactName: '', post: '', phone: '', isMasterBool: false, decisionWeight: 3 })
// 中国手机号校验：空值可选；非空时先剥离空格/中划线/括号再匹配 11 位 1[3-9] 开头
const phoneRule = {
  validator: (rule, value, callback) => {
    if (!value) return callback()
    const normalized = String(value).replace(/[\s\-()]/g, '')
    if (!/^1[3-9]\d{9}$/.test(normalized)) {
      return callback(new Error('请输入正确的 11 位手机号'))
    }
    callback()
  },
  trigger: 'blur'
}
const contactRules = {
  contactName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [phoneRule]
}

function resetContactForm() {
  contactForm.id = null
  contactForm.contactName = ''
  contactForm.post = ''
  contactForm.phone = ''
  contactForm.isMasterBool = false
  contactForm.decisionWeight = 3
}

function handleAddContact() {
  resetContactForm()
  contactForm.customerId = customer.value.id
  contactVisible.value = true
}

function handleEditContact(row) {
  contactForm.id = row.id
  contactForm.customerId = row.customerId
  contactForm.contactName = row.contactName
  contactForm.post = row.post
  contactForm.phone = row.phone
  contactForm.isMasterBool = row.isMaster === 1
  contactForm.decisionWeight = row.decisionWeight
  contactVisible.value = true
}

function onMasterChange(v) {
  contactForm.isMaster = v ? 1 : 0
}

async function handleSaveContact() {
  await contactFormRef.value.validate()
  contactForm.isMaster = contactForm.isMasterBool ? 1 : 0
  savingContact.value = true
  try {
    if (contactForm.id) {
      await updateContact(contactForm)
    } else {
      await addContact(contactForm)
    }
    ElMessage.success('已保存')
    contactVisible.value = false
    loadContacts()
  } finally {
    savingContact.value = false
  }
}

async function handleDeleteContact(row) {
  try {
    await ElMessageBox.confirm(`确认删除联系人「${row.contactName}」？`, '提示', { type: 'warning', confirmButtonClass: 'btn-zen-primary', customClass: 'msgbox-zen-confirm' })
    await deleteContact(row.id)
    ElMessage.success('已删除')
    loadContacts()
  } catch (e) {}
}

// ---------- 商机 CRUD ----------
const businessVisible = ref(false)
const businessFormRef = ref(null)
const savingBusiness = ref(false)
const businessForm = reactive({ customerId: null, businessName: '', expectedAmount: '', expectedDealDate: '' })
const businessRules = { businessName: [{ required: true, message: '请输入商机名称', trigger: 'blur' }] }

function resetBusinessForm() {
  businessForm.customerId = null
  businessForm.businessName = ''
  businessForm.expectedAmount = ''
  businessForm.expectedDealDate = ''
}

function handleAddBusiness() {
  resetBusinessForm()
  businessForm.customerId = customer.value.id
  businessVisible.value = true
}

async function handleSaveBusiness() {
  await businessFormRef.value.validate()
  savingBusiness.value = true
  try {
    await addBusiness(businessForm)
    ElMessage.success('已创建')
    businessVisible.value = false
    loadBusinesses()
  } finally {
    savingBusiness.value = false
  }
}

// ---------- 阶段推进 ----------
const stageVisible = ref(false)
const stageFormRef = ref(null)
const savingStage = ref(false)
const stagingRow = ref(null)
const stageForm = reactive({ stage: '', followContent: '' })
const stageRules = { stage: [{ required: true, message: '请选择目标阶段', trigger: 'change' }] }

// 阶段顺序（与后端 STAGE_ORDER 一致）
const STAGES = ['需求分析', '方案报价', '商务谈判', '赢单', '输单']
const nextStageOptions = computed(() => {
  if (!stagingRow.value) return STAGES
  const cur = stagingRow.value.stage
  if (cur === '赢单' || cur === '输单') return []
  const idx = STAGES.indexOf(cur)
  const result = []
  // 严格 +1
  if (idx >= 0 && idx < 3) result.push(STAGES[idx + 1])
  // 任意阶段可转输单
  result.push('输单')
  return result
})

function handleStage(row) {
  stagingRow.value = row
  stageForm.stage = ''
  stageForm.followContent = ''
  stageVisible.value = true
}

async function handleSaveStage() {
  await stageFormRef.value.validate()
  savingStage.value = true
  try {
    await updateBusinessStage(stagingRow.value.id, stageForm)
    ElMessage.success('阶段已推进')
    stageVisible.value = false
    loadBusinesses()
  } catch (e) {
    // 错误已在响应拦截器弹 ElMessage，这里不再重复
  } finally {
    savingStage.value = false
  }
}

// ---------- 写跟进(阶段五:通用 AddRecordDialog) ----------
const recordVisible = ref(false)

function handleAddRecord() {
  recordVisible.value = true
}

async function onRecordSaved() {
  await loadCustomer()
  await loadRecordCount()
  timelineKey.value++  // 强制 RecordTimeline 重新加载
}

// ---------- 跟进记录数(用于 timeline 卡片头部计数) ----------
async function loadRecordCount() {
  // 从 route 直接拿 id,不依赖 customer.value(loadCustomer 失败时也能正确)
  const id = route.params.id
  if (!id) {
    recordCount.value = 0
    return
  }
  try {
    const { data } = await getTimeline({ relatedType: 'customer', relatedId: id })
    recordCount.value = (data || []).length
  } catch (e) {
    recordCount.value = 0
  }
}

watch(() => route.params.id, loadAll)
onMounted(loadAll)
</script>

<style lang="scss" scoped>
.page { padding: 32px 32px 48px; min-height: 100%; }

/* header */
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--hairline);
}
.readonly-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 8px;
  font-size: 11.5px;
  background: var(--warn-soft);
  color: var(--warn);
  border-radius: 3px;
  font-weight: 500;
}
.readonly-banner {
  margin: 16px 0;
  border-radius: var(--radius);
}
.detail-title { display: flex; flex-direction: column; gap: 8px; }
.detail-name { font-size: 24px; font-weight: 600; letter-spacing: -0.015em; color: var(--ink); }
.detail-meta { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--muted); flex-wrap: wrap; }
.detail-meta .sep { color: var(--subtle); }
.detail-actions { display: flex; gap: 8px; }
.mono { font-family: var(--font-mono); font-feature-settings: 'tnum' 1; }
.mono.warn, .value.warn { color: var(--warn); }
.mono.danger, .value.danger { color: var(--danger); }

/* tabs */
.tabs { display: flex; border-bottom: 1px solid var(--hairline); margin-top: 18px; }
.tab {
  padding: 10px 16px; font-size: 14px; color: var(--muted);
  cursor: pointer; border-bottom: 2px solid transparent;
  margin-bottom: -1px; transition: all 0.12s;
  display: flex; align-items: center; gap: 6px;
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

/* layout */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
  margin-top: 20px;
}
@media (max-width: 1280px) { .layout { grid-template-columns: 1fr; } }
.layout-main { min-width: 0; }
.layout-side { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 20px; }
@media (max-width: 1280px) { .layout-side { position: static; } }

/* panel */
.panel, .list-card { background: var(--bg-warm); border: 1px solid var(--hairline); border-radius: var(--radius); }
.list-card :deep(.el-card__body) { padding: 0; }
.info-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 20px 40px;
  padding: 24px;
}
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: var(--muted); }
.info-value { font-size: 14px; color: var(--ink); }

.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-title { font-size: 14px; font-weight: 500; color: var(--ink); }

.name { font-weight: 500; color: var(--ink); }
.dot { display: inline-block; width: 5px; height: 5px; border-radius: 50%; margin-right: 4px; vertical-align: 1px; }

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

.cust-card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 12.5px;
  .label { color: var(--muted); }
  .value { color: var(--ink); font-weight: 500; }
}

.kc-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--hairline-soft); }
.kc-item:last-child { border-bottom: none; padding-bottom: 0; }
.kc-item:first-child { padding-top: 0; }
.kc-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--info-soft); color: var(--info);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; flex-shrink: 0;
}
.kc-meta { flex: 1; min-width: 0; }
.kc-name { font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 4px; }
.kc-post { font-size: 11.5px; color: var(--muted); margin-top: 1px; }

.sug-item { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--hairline-soft); font-size: 12.5px; line-height: 1.5; }
.sug-item:last-child { border-bottom: none; padding-bottom: 0; }
.sug-item:first-child { padding-top: 0; }
.sug-icon {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--warn-soft); color: var(--warn);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; flex-shrink: 0;
  &.accent { background: var(--accent-soft); color: var(--accent); }
}
.sug-content { flex: 1; }
.sug-title { font-weight: 500; color: var(--ink); }
.sug-desc { color: var(--muted); margin-top: 2px; }
.sug-action { margin-top: 6px; font-size: 12px; color: var(--accent); cursor: pointer; }
.sug-action:hover { text-decoration: underline; }

.empty-mini { font-size: 12.5px; color: var(--muted); padding: 8px 0; text-align: center; }

.stage-current { font-size: 13.5px; color: var(--ink-soft); }

/* 基本信息 Tab 内多 section 堆叠 */
.basic-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.basic-stack .panel {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 20px 20px;
}

/* 跟进记录卡片(阶段五:与商机/线索统一) */
.timeline-card {
  background: var(--surface);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
}
.timeline-head {
  padding: 14px 18px;
  border-bottom: 1px solid var(--hairline-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.timeline-title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  .count {
    font-size: 12px;
    color: var(--muted);
    font-weight: 400;
  }
}
.timeline-body { padding: 8px 18px 18px; }
</style>
