<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(v) => emit('update:visible', v)"
    title="新建跟进"
    width="560px"
    :close-on-click-modal="false"
    @keydown.esc="emit('update:visible', false)"
  >
    <!-- 实体类型 chip(仅 selectable 模式,合同不展示,图标与侧栏一致) -->
    <div v-if="isSelectableMode" class="form-row">
      <label class="form-label">关联实体类型 <span class="req">*</span></label>
      <div class="type-row">
        <div
          v-for="t in types"
          :key="t.value"
          class="type-chip"
          :class="{ active: form.relatedType === t.value }"
          @click="onTypePick(t.value)"
        >
          <el-icon class="ico"><component :is="t.icon" /></el-icon>
          <div class="name">{{ t.name }}</div>
          <div class="desc">{{ t.desc }}</div>
        </div>
      </div>
    </div>

    <!-- 关联实体选择器(仅 selectable 模式) -->
    <div v-if="isSelectableMode" class="form-row">
      <label class="form-label">选择 {{ typeLabel }} <span class="req">*</span></label>
      <el-select
        v-model="form.relatedId"
        filterable
        remote
        :remote-method="searchEntities"
        :loading="searchLoading"
        placeholder="输入关键字搜索实体名称…"
        style="width: 100%"
      >
        <el-option
          v-for="o in candidates"
          :key="o.id"
          :value="o.id"
          :label="o.name"
        >
          <div class="option-row">
            <span class="opt-name">{{ o.name }}</span>
            <span class="opt-meta">{{ o.meta }}</span>
          </div>
        </el-option>
      </el-select>
    </div>

    <!-- 固定模式:已知实体,仅一行紧凑徽章 -->
    <div v-else class="entity-badge" :class="`is-${form.relatedType}`">
      <el-icon class="ico"><component :is="entityIcon(form.relatedType)" /></el-icon>
      <span class="type-tag">{{ typeLabel }}</span>
      <span class="divider">·</span>
      <span class="name">{{ relatedName || `#${form.relatedId}` }}</span>
    </div>

    <!-- 跟进方式(纯文字,无彩色图标) -->
    <div class="form-row">
      <label class="form-label">跟进方式 <span class="req">*</span></label>
      <div class="way-row">
        <div
          v-for="w in ways"
          :key="w.value"
          class="way-chip"
          :class="{ active: form.followType === w.value }"
          @click="form.followType = w.value"
        >
          <span class="name">{{ w.name }}</span>
        </div>
      </div>
    </div>

    <!-- 跟进内容 -->
    <div class="form-row">
      <label class="form-label">跟进内容 <span class="req">*</span></label>
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="4"
        placeholder="沟通要点、客户反馈、下一步动作"
        maxlength="500"
        show-word-limit
      />
    </div>

    <!-- 下次跟进时间 -->
    <div class="form-row">
      <div class="switch-row">
        <span class="label-text">📅 <strong>安排下次跟进</strong> · 到期时会在右上角铃铛提醒</span>
        <label class="toggle">
          <input type="checkbox" v-model="nextEnabled" />
          <span></span>
        </label>
      </div>
      <div v-if="nextEnabled" class="datetime-row">
        <el-date-picker
          v-model="form.nextFollowTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择日期时间"
          style="flex: 1"
        />
        <div class="quick-buttons">
          <button class="qbtn" @click="quickAdd(1)">+1 天</button>
          <button class="qbtn" @click="quickAdd(3)">+3 天</button>
          <button class="qbtn" @click="quickAdd(7)">下周一</button>
        </div>
      </div>
    </div>

    <!-- 阶段联动提示已移除 -->

    <template #footer>
      <div class="foot-actions">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button class="btn-zen-primary" :loading="saving" :disabled="!canSubmit" @click="onSave(true)">
          保存跟进
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Aim, User, TrendCharts, Document } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { addRecord } from '@/api/record'
import { pageLead } from '@/api/lead'
import { pageCustomer } from '@/api/customer'
import { pageBusiness } from '@/api/business'
import { useUserStore } from '@/store/user'

const props = defineProps({
  visible: { type: Boolean, default: false },
  // 模式 A:固定实体(详情页时间轴"+")
  relatedType: { type: String, default: '' },
  relatedId: { type: [Number, String], default: null },
  relatedName: { type: String, default: '' },  // 固定模式展示用
  // 模式 B:可选实体(跟进中心"快速新建")
  selectable: { type: Boolean, default: false },
  defaultType: { type: String, default: 'customer' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:visible', 'saved'])

const userStore = useUserStore()
const currentUser = computed(() => userStore.nickname || userStore.username || '当前用户')

// 实体类型 chip(阶段五 v2:图标与侧栏导航一致 Element Plus Icons)
const types = [
  { value: 'customer', icon: User, name: '客户', desc: '已转化' },
  { value: 'lead', icon: Aim, name: '线索', desc: '初步接触' },
  { value: 'business', icon: TrendCharts, name: '商机', desc: '销售过程' },
  { value: 'contract', icon: Document, name: '合同', desc: '已签约' },
]
// 跟进方式(纯文字 chip,不使用 emoji 彩色图标)
const ways = [
  { value: '电话', name: '电话' },
  { value: '上门拜访', name: '上门' },
  { value: '微信', name: '微信' },
  { value: '邮件', name: '邮件' },
]

const isSelectableMode = computed(() => props.selectable || !props.relatedType)

const form = reactive({
  relatedType: '',
  relatedId: null,
  content: '',
  followType: '电话',
  nextFollowTime: null,
})
const nextEnabled = ref(true)
const saving = ref(false)

const typeLabel = computed(() => types.find(t => t.value === form.relatedType)?.name || '')
const canSubmit = computed(() => form.relatedId && form.content.trim().length > 0 && !props.disabled)

// 实体搜索(selectable 模式)
const candidates = ref([])
const searchLoading = ref(false)

const searchEntities = async (kw) => {
  searchLoading.value = true
  try {
    const params = { keyword: kw || '', pageNum: 1, pageSize: 20 }
    let list = []
    if (form.relatedType === 'lead') {
      const { data } = await pageLead(params)
      list = (data.records || []).map(r => ({
        id: r.id, name: r.leadName,
        meta: `${r.contactName || ''}${r.phone ? ' · ' + r.phone : ''}`,
      }))
    } else if (form.relatedType === 'customer') {
      const { data } = await pageCustomer(params)
      list = (data.records || []).map(r => ({
        id: r.id, name: r.customerName,
        meta: `${r.industry || '-'}${r.level ? ' · Lv ' + r.level : ''}`,
      }))
    } else if (form.relatedType === 'business') {
      const { data } = await pageBusiness(params)
      list = (data.records || []).map(r => ({
        id: r.id, name: r.businessName,
        meta: `${r.stage || '-'}${r.expectedAmount ? ' · ¥' + Number(r.expectedAmount).toLocaleString() : ''}`,
      }))
    } else {
      // contract 不在 selectable chip 列表中(本期不展示)
      list = []
    }
    candidates.value = list
  } finally {
    searchLoading.value = false
  }
}

const onTypePick = (v) => {
  form.relatedType = v
  form.relatedId = null
  candidates.value = []
  searchEntities('')
}

const quickAdd = (days) => {
  const base = form.nextFollowTime ? dayjs(form.nextFollowTime) : dayjs()
  form.nextFollowTime = base.add(days, 'day').format('YYYY-MM-DDTHH:mm:ss')
}

// 固定模式实体预览的图标(与侧栏一致)
const entityIcon = (t) => {
  const map = { customer: User, lead: Aim, business: TrendCharts, contract: Document }
  return map[t] || null
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      if (props.relatedType && props.relatedId) {
        form.relatedType = props.relatedType
        form.relatedId = props.relatedId
      } else {
        form.relatedType = props.defaultType || 'customer'
        form.relatedId = null
        candidates.value = []
        searchEntities('')
      }
      form.content = ''
      form.followType = '电话'
      form.nextFollowTime = null
      nextEnabled.value = true
    }
  },
)

const onSave = async () => {
  if (!canSubmit.value) return
  saving.value = true
  try {
    await addRecord({
      relatedType: form.relatedType,
      relatedId: form.relatedId,
      content: form.content.trim(),
      followType: form.followType,
      nextFollowTime: nextEnabled.value ? (form.nextFollowTime || null) : null,
    })
    ElMessage.success('已记录')
    emit('saved')
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.form-row { margin-bottom: 18px; }
.form-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-soft);
  margin-bottom: 6px;
  .req { color: var(--danger); font-weight: 600; }
  .hint-light { font-weight: 400; color: var(--subtle); }
}

// 实体类型 chip
.type-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.type-chip {
  padding: 12px 8px;
  background: var(--surface);
  border: 1.5px solid var(--hairline);
  border-radius: var(--radius);
  cursor: pointer;
  text-align: center;
  transition: all 0.12s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  &:hover { border-color: var(--muted); }
  &.active {
    background: var(--accent-pale);
    border-color: var(--accent);
    .name { color: var(--accent); font-weight: 600; }
  }
  .ico { font-size: 18px; color: var(--ink-soft); }
  &.active .ico { color: var(--accent); }
  .name { font-size: 12.5px; font-weight: 500; color: var(--ink-soft); }
  .desc { font-size: 10.5px; color: var(--subtle); }
}

// 跟进方式(纯文字 chip,无彩色 emoji)
.way-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.way-chip {
  padding: 10px 8px;
  background: var(--surface);
  border: 1.5px solid var(--hairline);
  border-radius: var(--radius);
  cursor: pointer;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  color: var(--ink-soft);
  &:hover { border-color: var(--muted); }
  &.active {
    background: var(--accent-soft);
    border-color: var(--accent);
    color: var(--accent);
    font-weight: 600;
  }
}

// 下次跟进
.switch-row {
  padding: 10px 12px;
  background: var(--bg);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  .label-text { font-size: 12.5px; color: var(--ink-soft); strong { color: var(--ink); } }
}
.toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  input {
    appearance: none;
    width: 28px; height: 16px;
    background: var(--hairline);
    border-radius: 8px;
    position: relative;
    cursor: pointer;
    transition: all 0.15s;
    &::before {
      content: '';
      position: absolute; left: 2px; top: 2px;
      width: 12px; height: 12px;
      background: white; border-radius: 50%;
      transition: all 0.15s;
    }
    &:checked { background: var(--accent); }
    &:checked::before { left: 14px; }
  }
}
.datetime-row { display: flex; align-items: center; gap: 8px; }
.quick-buttons { display: flex; gap: 4px; }
.qbtn {
  padding: 4px 8px;
  font-size: 11.5px;
  background: transparent;
  border: 1px solid var(--hairline);
  border-radius: 3px;
  color: var(--ink-soft);
  cursor: pointer;
  &:hover { background: var(--accent-pale); color: var(--accent); border-color: var(--accent-soft); }
}

// 实体徽章(固定模式:已知实体,一行展示)
.entity-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 6px;
  margin-bottom: 16px;
  background: var(--bg);
  border: 1px solid var(--hairline);
  border-radius: 4px;
  font-size: 12px;
  color: var(--ink-soft);
  max-width: 100%;
  .ico {
    width: 18px; height: 18px;
    border-radius: 4px;
    background: var(--accent-soft); color: var(--accent);
    display: inline-flex; align-items: center; justify-content: center;
    font-size: 11px;
    flex-shrink: 0;
  }
  &.is-business .ico { background: var(--info-soft); color: var(--info); }
  &.is-lead .ico { background: #ede9fe; color: #6d28d9; }
  &.is-contract .ico { background: var(--warn-soft); color: var(--warn); }
  .type-tag {
    padding: 1px 6px;
    background: var(--surface);
    border: 1px solid var(--hairline);
    border-radius: 3px;
    font-size: 11px;
    color: var(--muted);
    flex-shrink: 0;
  }
  .divider { color: var(--subtle); }
  .name {
    font-weight: 500;
    color: var(--ink);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 0;
  }
}

// 上传(已移除,占位保留)

// 阶段提示
.note {
  padding: 10px 12px;
  background: var(--accent-pale);
  border: 1px solid var(--accent-soft);
  border-radius: var(--radius);
  font-size: 12.5px;
  color: var(--ink-soft);
  display: flex;
  gap: 8px;
  line-height: 1.55;
  .ico { color: var(--accent); flex-shrink: 0; }
}

// 底部
.foot-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

// el-select option 自定义行
.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  .opt-name { font-weight: 500; }
  .opt-meta { font-size: 11.5px; color: var(--muted); }
}
</style>