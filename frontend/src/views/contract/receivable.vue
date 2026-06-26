<template>
  <div class="page">
    <el-card>
      <el-table :data="list" border>
        <el-table-column prop="receivableNum" label="回款编号" />
        <el-table-column prop="contractNum" label="合同编号" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="actualAmount" label="回款金额" />
        <el-table-column prop="returnDate" label="回款日期" />
        <el-table-column prop="paymentMethod" label="支付方式" />
        <el-table-column prop="recorder" label="录入人" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
