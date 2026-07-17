<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>字典类型</span>
          <el-button type="primary" size="small" @click="openTypeDialog()">新增字典</el-button>
        </div>
      </template>
      <el-table :data="typeList" border stripe v-loading="loading">
        <el-table-column prop="dictId" label="ID" width="60" />
        <el-table-column prop="dictName" label="字典名称" />
        <el-table-column prop="dictType" label="字典类型" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">{{ scope.row.status === 0 ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDataPanel(scope.row)">数据管理</el-button>
            <el-button link type="primary" size="small" @click="openTypeDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelType(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="typeTotal > 0" v-model:current-page="typeQuery.page" v-model:page-size="typeQuery.size" :total="typeTotal" layout="total, prev, pager, next" style="margin-top: 15px; justify-content: flex-end" @current-change="fetchTypes" />
    </el-card>

    <!-- 字典数据子面板 -->
    <el-card v-if="selectedType">
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>字典数据 - {{ selectedType.dictName }}</span>
          <el-button type="primary" size="small" @click="openDataDialog()">新增数据</el-button>
        </div>
      </template>
      <el-table :data="dataList" border stripe>
        <el-table-column prop="dictCode" label="ID" width="60" />
        <el-table-column prop="dictLabel" label="标签" />
        <el-table-column prop="dictValue" label="键值" />
        <el-table-column prop="dictSort" label="排序" width="60" />
        <el-table-column prop="cssClass" label="样式" />
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDataDialog(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" @click="handleDelData(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="isEditType ? '修改字典' : '新增字典'" width="450px">
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="字典名称"><el-input v-model="typeForm.dictName" /></el-form-item>
        <el-form-item label="字典类型"><el-input v-model="typeForm.dictType" :disabled="isEditType" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="0">正常</el-radio><el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitType">确认</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="isEditData ? '修改数据' : '新增数据'" width="450px">
      <el-form :model="dataForm" label-width="100px">
        <el-form-item label="标签"><el-input v-model="dataForm.dictLabel" /></el-form-item>
        <el-form-item label="键值"><el-input v-model="dataForm.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dataForm.dictSort" :min="0" /></el-form-item>
        <el-form-item label="样式"><el-input v-model="dataForm.cssClass" placeholder="primary/warning/danger/info" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="0">正常</el-radio><el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitData">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDictTypeList, addDictType, editDictType, delDictType, getDictDataList, addDictData, editDictData, delDictData } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const typeList = ref<any[]>([])
const typeTotal = ref(0)
const typeQuery = reactive({ page: 1, size: 20 })
const typeDialogVisible = ref(false)
const isEditType = ref(false)
const typeForm = reactive<any>({ dictName: '', dictType: '', status: 0 })

const selectedType = ref<any>(null)
const dataList = ref<any[]>([])
const dataDialogVisible = ref(false)
const isEditData = ref(false)
const dataForm = reactive<any>({ dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 0 })

onMounted(() => fetchTypes())

async function fetchTypes() {
  loading.value = true
  try { const res = await getDictTypeList(typeQuery); if (res.data) { typeList.value = res.data.records; typeTotal.value = res.data.total } } catch {} finally { loading.value = false }
}

async function openTypeDialog(row?: any) {
  isEditType.value = !!row; Object.assign(typeForm, row || { dictName: '', dictType: '', status: 0 }); typeDialogVisible.value = true
}
async function handleSubmitType() {
  try { isEditType.value ? await editDictType(typeForm) : await addDictType(typeForm); ElMessage.success(isEditType.value ? '修改成功' : '新增成功'); typeDialogVisible.value = false; fetchTypes() } catch {}
}
async function handleDelType(row: any) {
  ElMessageBox.confirm('确认删除？', '提示').then(async () => { await delDictType(row.dictId); ElMessage.success('删除成功'); fetchTypes() }).catch(() => {})
}

async function openDataPanel(row: any) {
  selectedType.value = row; dataForm.dictType = row.dictType; fetchData()
}
async function fetchData() {
  if (!selectedType.value) return
  try { const res = await getDictDataList({ page: 1, size: 200, dictType: selectedType.value.dictType }); if (res.data) dataList.value = res.data.records } catch {}
}
async function openDataDialog(row?: any) {
  isEditData.value = !!row; Object.assign(dataForm, row || { dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 0, dictType: selectedType.value?.dictType }); dataDialogVisible.value = true
}
async function handleSubmitData() {
  try { isEditData.value ? await editDictData(dataForm) : await addDictData(dataForm); ElMessage.success('操作成功'); dataDialogVisible.value = false; fetchData() } catch {}
}
async function handleDelData(row: any) {
  ElMessageBox.confirm('确认删除？', '提示').then(async () => { await delDictData(row.dictCode); ElMessage.success('删除成功'); fetchData() }).catch(() => {})
}
</script>