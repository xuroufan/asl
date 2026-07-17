<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>角色管理</span>
          <el-button type="primary" size="small" @click="openDialog()">新增角色</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="roleId" label="ID" width="60" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleKey" label="权限字符串" />
        <el-table-column prop="roleSort" label="排序" width="60" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">{{ scope.row.status === 0 ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > 0" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" style="margin-top: 15px; justify-content: flex-end" @current-change="fetchData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改角色' : '新增角色'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="权限字符串">
          <el-input v-model="form.roleKey" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.roleSort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="id" :props="{ label: 'menuName', children: 'children' }" :default-checked-keys="form.menuIds" check-strictly />
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
import { getRoleList, getRoleById, addRole, editRole, delRole, getMenuList } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const menuTree = ref<any[]>([])
const menuTreeRef = ref()

const query = reactive({ page: 1, size: 20 })
const form = reactive<any>({ roleName: '', roleKey: '', roleSort: 0, status: 0, menuIds: [] })

onMounted(() => { fetchData(); loadMenuTree() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getRoleList(query)
    if (res.data) { list.value = res.data.records; total.value = res.data.total }
  } catch {} finally { loading.value = false }
}

async function loadMenuTree() {
  try { const res = await getMenuList(); if (res.data) menuTree.value = res.data } catch {}
}

async function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) {
    form.roleId = row.roleId; form.roleName = row.roleName; form.roleKey = row.roleKey; form.roleSort = row.roleSort; form.status = row.status
    const res = await getRoleById(row.roleId)
    if (res.data) form.menuIds = res.data.menuIds || []
  } else {
    form.roleId = undefined; form.roleName = ''; form.roleKey = ''; form.roleSort = 0; form.status = 0; form.menuIds = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  form.menuIds = menuTreeRef.value?.getCheckedKeys() || []
  try {
    if (isEdit.value) { await editRole(form); ElMessage.success('修改成功') }
    else { await addRole(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {}
}

async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除角色 ' + row.roleName + '？', '提示').then(async () => {
    await delRole(row.roleId); ElMessage.success('删除成功'); fetchData()
  }).catch(() => {})
}
</script>