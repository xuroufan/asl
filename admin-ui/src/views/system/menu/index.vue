<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>菜单管理</span>
          <el-button type="primary" size="small" @click="openDialog()">新增菜单</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading" row-key="menuId" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" />
        <el-table-column prop="icon" label="图标" width="60"><template #default="scope"><el-icon><component :is="scope.row.icon" /></el-icon></template></el-table-column>
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="scope"><el-tag :type="scope.row.menuType === 0 ? '' : scope.row.menuType === 1 ? 'primary' : 'warning'" size="small">{{ ['目录','菜单','按钮'][scope.row.menuType] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" />
        <el-table-column prop="perms" label="权限标识" />
        <el-table-column prop="orderNum" label="排序" width="60" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">{{ scope.row.status === 0 ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改菜单' : '新增菜单'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="list" :props="{ label: 'menuName', value: 'menuId' }" placeholder="顶级菜单" clearable allow-create filterable style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="路由地址" v-if="form.menuType < 2"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="组件路径" v-if="form.menuType === 1"><el-input v-model="form.component" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perms" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">显示</el-radio>
            <el-radio :value="1">隐藏</el-radio>
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
import { getMenuList, getMenuById, addMenu, editMenu, delMenu } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive<any>({ parentId: 0, menuName: '', menuType: 0, path: '', component: '', perms: '', icon: '', orderNum: 0, status: 0 })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try { const res = await getMenuList(); if (res.data) list.value = res.data } catch {} finally { loading.value = false }
}

async function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) { Object.assign(form, row) }
  else { Object.assign(form, { parentId: 0, menuName: '', menuType: 0, path: '', component: '', perms: '', icon: '', orderNum: 0, status: 0 }) }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) { await editMenu(form); ElMessage.success('修改成功') }
    else { await addMenu(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchData()
  } catch {}
}

async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除菜单 ' + row.menuName + '？', '提示').then(async () => {
    await delMenu(row.menuId); ElMessage.success('删除成功'); fetchData()
  }).catch(() => {})
}
</script>