<template>
  <div class="page">
    <el-card>
      <el-table :data="list" border>
        <el-table-column prop="contractNum" label="合同编号" />
        <el-table-column prop="name" label="合同名称" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="totalAmount" label="金额" />
        <el-table-column prop="status" label="状态">
          <template #default>
            <el-tag type="success">执行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="signDate" label="签约日期" />
        <el-table-column label="操作">
          <el-button link type="primary">详情</el-button>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
