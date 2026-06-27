<template>
  <div class="timeline" v-loading="loading">
    <div v-if="records.length === 0 && !loading" class="empty">
      暂无跟进记录
    </div>
    <div v-for="r in records" :key="r.id" class="timeline-item">
      <div class="timeline-time mono">{{ formatTime(r.createTime) }}</div>
      <div class="timeline-body">
        <div class="title">{{ r.content }}</div>
        <div class="meta">
          <span class="type-tag" :class="{ system: r.followType === '系统' }">{{ r.followType || '电话' }}</span>
          <span>·</span>
          <span>{{ r.createBy || '系统' }}</span>
          <template v-if="r.nextFollowTime">
            <span>·</span>
            <span class="next">下次跟进 {{ dayjs(r.nextFollowTime).format('MM-DD HH:mm') }}</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import { getTimeline } from '@/api/record'

const props = defineProps({
  relatedType: { type: String, required: true },  // lead / customer / business
  relatedId: { type: [Number, String], required: true }
})

const records = ref([])
const loading = ref(false)

function formatTime(t) {
  if (!t) return '-'
  const d = dayjs(t)
  const now = dayjs()
  if (d.isSame(now, 'day')) return d.format('HH:mm')
  if (d.isSame(now.subtract(1, 'day'), 'day')) return '昨日 ' + d.format('HH:mm')
  if (d.year() === now.year()) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD HH:mm')
}

async function load() {
  if (!props.relatedId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    const res = await getTimeline({ relatedType: props.relatedType, relatedId: props.relatedId })
    records.value = res.data || []
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

watch(() => [props.relatedType, props.relatedId], load, { immediate: true })
</script>

<style lang="scss" scoped>
.timeline {
  background: var(--bg-warm);
  border: 1px solid var(--hairline);
  border-radius: var(--radius);
  padding: 16px 20px;
  min-height: 80px;
}

.empty {
  text-align: center;
  font-size: 12.5px;
  color: var(--muted);
  padding: 20px 0;
}

.timeline-item {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--hairline-soft);
  &:last-child { border-bottom: none; padding-bottom: 4px; }
  &:first-child { padding-top: 4px; }
}

.timeline-time {
  font-size: 12px;
  color: var(--muted);
  font-family: var(--font-mono);
  font-feature-settings: 'tnum' 1;
  padding-top: 2px;
}

.timeline-body .title {
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--ink);
  white-space: pre-wrap;
}

.timeline-body .meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.type-tag {
  padding: 1px 6px;
  background: var(--hairline-soft);
  color: var(--muted);
  border-radius: 3px;
  font-size: 11px;

  &.system {
    background: var(--accent-soft);
    color: var(--accent);
  }
}

.next {
  color: var(--warn);
  font-weight: 500;
}
</style>
