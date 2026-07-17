<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="openDialog()">新增用户</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="userId" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
            <el-button link type="warning" size="small" @click="handleResetPwd(scope.row)">重置</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > 0" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" style="margin-top: 15px; justify-content: flex-end" @current-change="fetchData" />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserList, getUserById, addUser, editUser, delUser, resetUserPwd } from '@/api/system'
import { getAllRoles } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const roles = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)

const query = reactive({ page: 1, size: 20 })
const form = reactive<any>({ username: '', nickname: '', password: '', phone: '', email: '', status: 0, roleIds: [] })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(() => { fetchData(); loadRoles() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList(query)
    if (res.data) { list.value = res.data.records; total.value = res.data.total }
  } catch {} finally { loading.value = false }
}

async function loadRoles() {
  try {
    const res = await getAllRoles()
    if (res.data) roles.value = res.data
  } catch {}
}

async function openDialog(row?: any) {
  isEdit.value = !!row
  if (row) {
    form.userId = row.userId
    form.username = row.username
    form.nickname = row.nickname
    form.phone = row.phone
    form.email = row.email
    form.status = row.status
    const res = await getUserById(row.userId)
    if (res.data) form.roleIds = res.data.roleIds || []
  } else {
    form.userId = undefined
    form.username = ''
    form.nickname = ''
    form.password = ''
    form.phone = ''
    form.email = ''
    form.status = 0
    form.roleIds = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await editUser(form)
      ElMessage.success('修改成功')
    } else {
      await addUser(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally { submitLoading.value = false }
}

async function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除用户 ' + row.username + '？', '提示').then(async () => {
    await delUser(row.userId)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

async function handleResetPwd(row: any) {
  ElMessageBox.confirm('确认重置密码为 admin123？', '提示').then(async () => {
    await resetUserPwd(row.userId)
    ElMessage.success('密码已重置')
  }).catch(() => {})
}
</script>