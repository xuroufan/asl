<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between">
        <span>参数配置</span>
        <el-button type="primary" size="small" @click="openDialog()">新增参数</el-button>
      </div>
    </template>
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="configId" label="ID" width="60" />
      <el-table-column prop="configName" label="参数名称" />
      <el-table-column prop="configKey" label="参数键名" />
      <el-table-column prop="configValue" label="参数键值" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="configType" label="类型" width="80">
        <template #default="scope"><el-tag :type="scope.row.configType === 0 ? 'primary' : 'success'" size="small">{{ scope.row.configType === 0 ? '内置' : '自定义' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="openDialog(scope.row)">修改</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" style="margin-top: 15px; justify-content: flex-end" @current-change="fetchData" />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '修改参数' : '新增参数'" width="500px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="参数名称"><el-input v-model="form.configName" /></el-form-item>
      <el-form-item label="参数键名"><el-input v-model="form.configKey" :disabled="isEdit" /></el-form-item>
      <el-form-item label="参数键值"><el-input v-model="form.configValue" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="类型">
        <el-radio-group v-model="form.configType">
          <el-radio :value="0">系统内置</el-radio>
          <el-radio :value="1">自定义</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getConfigList, addConfig, editConfig, delConfig } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const dialogVisible = ref(false); const isEdit = ref(false)
const query = reactive({ page: 1, size: 20 })
const form = reactive<any>({ configName: '', configKey: '', configValue: '', remark: '', configType: 1 })

onMounted(() => fetchData())
async function fetchData() {
  loading.value = true
  try { const res = await getConfigList(query); if (res.data) { list.value = res.data.records; total.value = res.data.total } } catch {} finally { loading.value = false }
}
async function openDialog(row?: any) {
  isEdit.value = !!row; Object.assign(form, row || { configName: '', configKey: '', configValue: '', remark: '', configType: 1 }); dialogVisible.value = true
}
async function handleSubmit() {
  try { isEdit.value ? await editConfig(form) : await addConfig(form); ElMessage.success('操作成功'); dialogVisible.value = false; fetchData() } catch {}
}
async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除？', '提示').then(async () => { await delConfig(row.configId); ElMessage.success('删除成功'); fetchData() }).catch(() => {})
}
</script>