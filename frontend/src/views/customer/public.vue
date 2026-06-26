<template>
  <div class="page">
    <el-alert
      title="公海池说明：超过 15 天未跟进的客户将被系统自动回收至此，任何销售可领取。"
      type="info"
      show-icon
      :closable="false"
    />
    <el-card style="margin-top: 12px">
      <el-table :data="list" border>
        <el-table-column prop="name" label="客户名称" />
        <el-table-column prop="source" label="来源" />
        <el-table-column prop="lastOwner" label="原负责人" />
        <el-table-column prop="releaseTime" label="释放时间" />
        <el-table-column label="操作" width="120">
          <template #default>
            <el-button link type="primary">领取</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
