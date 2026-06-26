<template>
  <div class="page">
    <el-card>
      <el-table :data="list" border>
        <el-table-column prop="name" label="商机名称" />
        <el-table-column prop="customerName" label="所属客户" />
        <el-table-column prop="expectedAmount" label="预计金额" />
        <el-table-column prop="stage" label="阶段">
          <template #default>
            <el-tag type="warning">方案报价</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" />
        <el-table-column label="操作" width="180">
          <template #default>
            <el-button link type="primary">详情</el-button>
            <el-button link type="primary">推进阶段</el-button>
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
