<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>部门管理</span>
          <el-button type="primary" size="small" @click="openDialog()">新增部门</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading" row-key="deptId" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="deptName" label="部门名称" />
        <el-table-column prop="orderNum" label="排序" width="60" />
        <el-table-column prop="leader" label="负责人" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">{{ scope.row.status === 0 ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改部门' : '新增部门'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="list" :props="{ label: 'deptName', value: 'deptId' }" placeholder="顶级部门" clearable style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门名称"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="显示排序"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDeptList, getDeptById, addDept, editDept, delDept } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive<any>({ parentId: 0, deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 0 })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try { const res = await getDeptList(); if (res.data) list.value = res.data } catch {} finally { loading.value = false }
}

async function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) { Object.assign(form, row) }
  else { Object.assign(form, { parentId: 0, deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 0 }) }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) { await editDept(form); ElMessage.success('修改成功') }
    else { await addDept(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {}
}

async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除部门 ' + row.deptName + '？', '提示').then(async () => {
    await delDept(row.deptId); ElMessage.success('删除成功'); fetchData()
  }).catch(() => {})
}
</script>