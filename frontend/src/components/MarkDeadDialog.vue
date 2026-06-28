<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(v) => emit('update:visible', v)"
    title="标记为死线索"
    width="460px"
    :close-on-click-modal="false"
  >
    <p class="hint">
      将标记 <strong>{{ lead?.leadName }}</strong> 为死线索，标记后该线索不可再写跟进（时间轴保留可查）。
    </p>
    <el-form ref="formRef" :model="form" label-position="top">
      <el-form-item label="死因(可选)">
        <el-input
          v-model="form.deadReason"
          type="textarea"
          :rows="3"
          placeholder="例：客户长期不回复 / 联系方式错误 / 已被竞品签下"
          maxlength="500"
          show-word-limit
        />
        <span class="form-hint">建议填写死因，便于后续报表统计与复盘</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button class="btn-zen-primary" :loading="saving" @click="onConfirm">
        确认标记
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { markDead } from '@/api/lead'

const props = defineProps({
  visible: { type: Boolean, default: false },
  lead: { type: Object, default: null },
})
const emit = defineEmits(['update:visible', 'marked'])

const formRef = ref(null)
const saving = ref(false)
const form = reactive({ deadReason: '' })

watch(
  () => props.visible,
  (v) => {
    if (v) form.deadReason = ''
  },
)

const onConfirm = async () => {
  saving.value = true
  try {
    await markDead(props.lead.id, {
      deadReason: form.deadReason.trim() || null,
    })
    ElMessage.success('已标记为死线索')
    emit('marked')
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.hint {
  font-size: 13px;
  color: var(--ink-soft);
  margin: 0 0 16px;
  line-height: 1.6;
  strong {
    color: var(--ink);
    font-weight: 600;
  }
}
.form-hint {
  font-size: 11.5px;
  color: var(--subtle);
}
</style>