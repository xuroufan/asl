<template>
  <div>
    <h2 style="margin-bottom:16px">Nacos 服务注册与健康</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-card shadow="never">
      <template #header><span>已注册服务</span></template>
      <el-table :data="services" size="small" stripe>
        <el-table-column prop="name" label="服务名" min-width="180" />
        <el-table-column prop="instances" label="实例数" width="80" />
        <el-table-column label="健康状态" width="100">
          <template #default="{row}"><el-tag :type="row.healthy === row.instances ? 'success' : row.healthy > 0 ? 'warning' : 'danger'" size="small">{{row.healthy}}/{{row.instances}}</el-tag></template>
        </el-table-column>
        <el-table-column prop="group" label="分组" width="120" />
        <el-table-column prop="cluster" label="集群" width="100" />
        <el-table-column label="操作" width="120">
          <template #default><el-button link size="small" type="primary">查看实例</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import statistic from '@/components/Statistic.vue'

const cards = ref([
  { label: '注册服务数', value: '8', color: '#409eff' },
  { label: '总实例数', value: '8', color: '#67c23a' },
  { label: '健康实例', value: '8', color: '#67c23a' },
  { label: '配置数', value: '24', color: '#e6a23c' },
])
const services = ref([
  { name: 'futures-gateway', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-account', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-order', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-matching', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-market', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-fund', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-risk', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
  { name: 'futures-settlement', instances: 1, healthy: 1, group: 'DEFAULT_GROUP', cluster: 'DEFAULT' },
])
</script>
