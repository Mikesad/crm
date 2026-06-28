<template>
  <div class="timeline" v-loading="loading">
    <div v-if="records.length === 0 && !loading" class="empty">
      暂无跟进记录 · <span class="empty-hint">点上方"新建跟进"开始记录</span>
    </div>

    <div v-for="r in records" :key="r.id" class="timeline-item">
      <!-- 时间 -->
      <div class="tl-time">
        <div class="tl-time-day mono">{{ formatDay(r.createTime) }}</div>
        <div class="tl-time-hm mono">{{ formatHm(r.createTime) }}</div>
        <div v-if="relativeText(r.createTime)" class="tl-time-rel">{{ relativeText(r.createTime) }}</div>
      </div>

      <!-- 垂直时间轴 rail + dot -->
      <div class="tl-rail">
        <div class="tl-dot" :class="dotClass(r)"></div>
      </div>

      <!-- 内容 -->
      <div class="tl-body">
        <div class="tl-head">
          <span class="tl-author">
            <span class="tl-avatar" :class="{ 'is-system': isSystem(r) }">{{ avatarChar(r) }}</span>
            <span class="tl-author-name" :class="{ 'is-system': isSystem(r) }">{{ r.createBy || '系统' }}</span>
          </span>
          <span class="follow-type" :class="typeClass(r)">{{ typeText(r) }}</span>
        </div>

        <!-- 阶段变更特殊卡片 -->
        <div v-if="isStageChange(r)" class="stage-change">
          <span class="from">{{ stageChangeFrom(r) }}</span>
          <span class="arrow">→</span>
          <strong class="to">{{ stageChangeTo(r) }}</strong>
          <span v-if="!stageChangeHasContent(r)" class="auto-tip">自动记录</span>
        </div>

        <!-- 普通跟进内容 -->
        <div v-if="!isStageChange(r) || stageChangeHasContent(r)" class="tl-content">
          <span v-if="isStageChange(r)" class="content-prefix">跟进内容:</span>
          {{ stripStageChangePrefix(r.content) }}
        </div>

        <!-- 下次跟进 -->
        <div v-if="r.nextFollowTime && !isSystem(r)" class="tl-next">
          <el-icon class="ico"><Calendar /></el-icon>
          下次跟进:<strong class="mono">{{ formatNext(r.nextFollowTime) }}</strong>
          <span v-if="isOverdue(r.nextFollowTime)" class="overdue-tag">已逾期</span>
        </div>
      </div>
    </div>

    <div v-if="records.length > 0 && hasMore" class="load-more" @click="loadMore">
      展开更早的 {{ remaining }} 条记录 ↓
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import { Calendar } from '@element-plus/icons-vue'
import { getTimeline } from '@/api/record'

const props = defineProps({
  relatedType: { type: String, required: true },  // lead / customer / business
  relatedId: { type: [Number, String], required: true },
  pageSize: { type: Number, default: 30 },
})

const records = ref([])
const loading = ref(false)
const showAll = ref(false)

const PAGE_SIZE = props.pageSize

const visibleRecords = computed(() => {
  if (showAll.value || records.value.length <= PAGE_SIZE) return records.value
  return records.value.slice(0, PAGE_SIZE)
})
const hasMore = computed(() => records.value.length > PAGE_SIZE && !showAll.value)
const remaining = computed(() => records.value.length - PAGE_SIZE)

const dotClass = (r) => {
  if (isSystem(r)) return 't-system'
  const t = (r.followType || '').toLowerCase()
  if (t.includes('电话')) return 't-phone'
  if (t.includes('微信')) return 't-wechat'
  if (t.includes('上门')) return 't-visit'
  if (t.includes('邮件')) return 't-email'
  return 't-phone'  // default
}

const typeClass = (r) => {
  if (isStageChange(r)) return 't-stage'
  if (isSystem(r)) return 't-system'
  const t = (r.followType || '').toLowerCase()
  if (t.includes('电话')) return 't-phone'
  if (t.includes('微信')) return 't-wechat'
  if (t.includes('上门')) return 't-visit'
  if (t.includes('邮件')) return 't-email'
  return 't-phone'
}

const typeText = (r) => {
  if (isStageChange(r)) return '⇄ 阶段变更'
  if (isSystem(r)) return '⚙ 系统记录'
  const t = r.followType || '电话'
  if (t === '电话') return '📞 电话'
  if (t === '上门拜访') return '🚗 上门拜访'
  if (t === '微信') return '💬 微信'
  if (t === '邮件') return '📧 邮件'
  return t
}

const isSystem = (r) => (r.followType || '') === '系统'

// 阶段变更:followType=系统 且 content 含「阶段从」或「阶段推进」
const isStageChange = (r) => {
  if (!isSystem(r)) return false
  const c = r.content || ''
  return c.includes('阶段从') || (c.includes('阶段') && c.includes('推进'))
}

// 解析 "阶段从「A」推进到「B」" 中的 A / B
const stageChangeFrom = (r) => {
  const m = (r.content || '').match(/阶段从「([^」]+)」/)
  return m ? m[1] : ''
}
const stageChangeTo = (r) => {
  const m = (r.content || '').match(/推进到「([^」]+)」/)
  return m ? m[1] : ''
}

// 阶段变更时是否带了 followContent(有 "\n跟进内容:" 表示带内容)
const stageChangeHasContent = (r) => (r.content || '').includes('跟进内容:')
const stripStageChangePrefix = (s) => {
  if (!s) return ''
  return s.replace(/^.*?跟进内容：/, '').replace(/^.*?跟进内容:/, '')
}

const isOverdue = (t) => dayjs(t).isBefore(dayjs())

const avatarChar = (r) => {
  if (isSystem(r)) return 'SYS'
  const name = r.createBy || ''
  return name ? name.charAt(0).toUpperCase() : '·'
}

const formatDay = (t) => {
  if (!t) return '-'
  const d = dayjs(t)
  const now = dayjs()
  if (d.year() === now.year()) return d.format('MM-DD')
  return d.format('YYYY-MM-DD')
}
const formatHm = (t) => (t ? dayjs(t).format('HH:mm') : '-')
const formatNext = (t) => dayjs(t).format('YYYY-MM-DD HH:mm')
const relativeText = (t) => {
  if (!t) return ''
  const diff = dayjs().diff(dayjs(t), 'day')
  if (diff < 1) return ''
  if (diff < 7) return diff + ' 天前'
  return ''
}

function load() {
  if (!props.relatedId) {
    records.value = []
    return
  }
  loading.value = true
  showAll.value = false
  getTimeline({ relatedType: props.relatedType, relatedId: props.relatedId })
    .then((res) => {
      records.value = res.data || []
    })
    .catch(() => {
      records.value = []
    })
    .finally(() => {
      loading.value = false
    })
}

function loadMore() {
  showAll.value = true
}

watch(() => [props.relatedType, props.relatedId], load, { immediate: true })
</script>

<style lang="scss" scoped>
.timeline {
  background: transparent;
  padding: 0;
  min-height: 80px;
}

.empty {
  text-align: center;
  font-size: 12.5px;
  color: var(--muted);
  padding: 32px 0;
  .empty-hint { color: var(--subtle); margin-left: 4px; }
}

.timeline-item {
  display: grid;
  grid-template-columns: 72px 24px 1fr;
  gap: 12px;
  padding: 12px 0;
  position: relative;
}

.tl-time {
  font-size: 12px;
  color: var(--muted);
  text-align: right;
  line-height: 1.4;
  padding-top: 2px;
}
.tl-time-day {
  font-size: 13px;
  color: var(--ink);
  font-weight: 500;
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
}
.tl-time-hm {
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
}
.tl-time-rel {
  font-size: 11px;
  color: var(--subtle);
  margin-top: 1px;
}

.tl-rail {
  position: relative;
  &::before {
    content: '';
    position: absolute;
    left: 50%;
    top: 0;
    bottom: 0;
    width: 1px;
    background: var(--hairline);
    transform: translateX(-50%);
  }
}
.timeline-item:last-child .tl-rail::before { display: none; }
.timeline-item:first-child .tl-rail::before { top: 8px; }

.tl-dot {
  position: absolute;
  left: 50%;
  top: 8px;
  transform: translateX(-50%);
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--surface);
  border: 2px solid var(--accent);
  z-index: 1;

  &.t-phone { border-color: var(--info); }
  &.t-wechat { border-color: #16a34a; }
  &.t-visit { border-color: var(--warn); }
  &.t-email { border-color: #6d28d9; }
  &.t-stage { border-color: var(--accent); background: var(--accent); }
  &.t-system { border-color: var(--subtle); background: var(--subtle); }
}

.tl-body { padding-top: 2px; min-width: 0; }
.tl-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.tl-author {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tl-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10.5px;
  font-weight: 600;
  font-family: var(--font-mono);
  flex-shrink: 0;
  &.is-system { background: var(--subtle); }
}
.tl-author-name {
  font-size: 12.5px;
  font-weight: 500;
  &.is-system { color: var(--muted); }
}

.follow-type {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 3px;
  background: var(--hairline-soft);
  color: var(--muted);

  &.t-phone { background: var(--info-soft); color: var(--info); }
  &.t-wechat { background: #dcfce7; color: #16a34a; }
  &.t-visit { background: var(--warn-soft); color: var(--warn); }
  &.t-email { background: #ede9fe; color: #6d28d9; }
  &.t-stage { background: var(--accent-soft); color: var(--accent); }
  &.t-system { background: var(--hairline); color: var(--muted); }
}

.tl-content {
  font-size: 13px;
  color: var(--ink-soft);
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  .content-prefix {
    color: var(--muted);
    margin-right: 4px;
  }
}

.stage-change {
  background: var(--accent-pale);
  border: 1px solid var(--accent-soft);
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 6px;
  font-size: 12.5px;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  .arrow { color: var(--muted); }
  .from { font-weight: 500; }
  .to { font-weight: 600; }
  .auto-tip {
    margin-left: auto;
    font-size: 11px;
    color: var(--muted);
  }
}

.tl-next {
  margin-top: 6px;
  padding: 6px 10px;
  background: var(--bg);
  border-radius: 4px;
  font-size: 12px;
  color: var(--ink-soft);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  .ico { color: var(--accent); }
  .overdue-tag {
    margin-left: auto;
    font-size: 10.5px;
    padding: 1px 6px;
    background: var(--danger-soft);
    color: var(--danger);
    border-radius: 3px;
    font-weight: 500;
  }
}

.load-more {
  text-align: center;
  padding: 12px 0;
  font-size: 12.5px;
  color: var(--muted);
  cursor: pointer;
  &:hover { color: var(--accent); }
}
</style>