<template>
  <div class="page">
    <el-card>
      <div class="toolbar">
        <el-button type="success" :icon="Plus">新增线索</el-button>
        <el-button type="warning" :icon="Refresh">批量导入</el-button>
      </div>
      <el-table :data="list" border>
        <el-table-column prop="name" label="线索名称" />
        <el-table-column prop="contact" label="联系人" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="source" label="来源" />
        <el-table-column prop="status" label="状态">
          <template #default>
            <el-tag>未跟进</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default>
            <el-button link type="primary">跟进</el-button>
            <el-button link type="success">转为客户</el-button>
            <el-button link type="danger">放弃</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
const list = ref([])
</script>
