<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item label="客户名称">
          <el-input v-model="query.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户来源">
          <el-select v-model="query.source" placeholder="全部" clearable>
            <el-option label="官网注册" value="官网" />
            <el-option label="电话咨询" value="电话" />
            <el-option label="客户介绍" value="介绍" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Refresh">重置</el-button>
          <el-button type="success" :icon="Plus">新增客户</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" border>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="客户名称" />
        <el-table-column prop="contact" label="联系人" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="source" label="来源" />
        <el-table-column prop="ownerName" label="负责人" />
        <el-table-column prop="lastFollowTime" label="最后跟进时间" />
        <el-table-column prop="isPublic" label="状态">
          <template #default>
            <el-tag type="success">私海</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default>
            <el-button link type="primary">详情</el-button>
            <el-button link type="primary">跟进</el-button>
            <el-button link type="primary">共享</el-button>
            <el-button link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        style="margin-top: 12px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'

const query = reactive({ name: '', source: '', pageNum: 1, pageSize: 10 })
const total = ref(0)
const list = ref([])
</script>
