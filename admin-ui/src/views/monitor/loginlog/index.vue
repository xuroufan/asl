<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between">
        <span>登录日志</span>
        <div>
          <el-button type="danger" size="small" @click="handleClean">清空</el-button>
        </div>
      </div>
    </template>
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="infoId" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="status" label="结果" width="70">
        <template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">{{ scope.row.status === 0 ? '成功' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="location" label="登录地点" />
      <el-table-column prop="msg" label="消息" />
      <el-table-column prop="browser" label="浏览器" />
      <el-table-column prop="os" label="操作系统" />
      <el-table-column prop="loginTime" label="登录时间" width="170" />
      <el-table-column label="操作" width="80">
        <template #default="scope"><el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" style="margin-top: 15px; justify-content: flex-end" @current-change="fetchData" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getLoginLogList, delLoginLog, cleanLoginLog } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const query = reactive({ page: 1, size: 20 })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try { const res = await getLoginLogList(query); if (res.data) { list.value = res.data.records; total.value = res.data.total } } catch {} finally { loading.value = false }
}

async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除？', '提示').then(async () => { await delLoginLog(row.infoId); ElMessage.success('删除成功'); fetchData() }).catch(() => {})
}

async function handleClean() {
  ElMessageBox.confirm('确认清空所有登录日志？', '警告').then(async () => { await cleanLoginLog(); ElMessage.success('清空成功'); fetchData() }).catch(() => {})
}
</script>