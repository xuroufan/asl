<template>
  <div class="crm-tag">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">客户标签管理</h2>
      <div>
        <el-select v-model="categoryFilter" size="small" style="width: 130px; margin-right: 8px" clearable placeholder="标签分类">
          <el-option label="全部" value="" />
          <el-option label="等级" value="等级" />
          <el-option label="行为" value="行为" />
          <el-option label="类型" value="类型" />
          <el-option label="风险" value="风险" />
          <el-option label="阶段" value="阶段" />
          <el-option label="自定义" value="自定义" />
        </el-select>
        <el-button type="primary" @click="showCreateDialog" :icon="'Plus'" size="small">新增标签</el-button>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><span>标签列表</span></template>
          <el-table :data="tags" stripe size="small" style="width: 100%">
            <el-table-column prop="tagId" label="ID" width="60" />
            <el-table-column label="标签" width="150">
              <template #default="{ row }">
                <el-tag :color="row.color" effect="dark" size="small">{{ row.tagName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="80" align="center" />
            <el-table-column prop="customerCount" label="客户数" width="80" align="center" />
            <el-table-column prop="createdAt" label="创建时间" width="100" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleDelete(row.tagId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center justify-between">
              <span>标签分配</span>
              <el-input v-model="assignCustomerId" placeholder="客户ID" size="small" style="width: 120px" />
            </div>
          </template>

          <el-tag v-for="tag in tags" :key="tag.tagId" :color="tag.color" effect="plain"
            class="tag-item" closable @close="handleRemoveTag(tag.tagId)">
            {{ tag.tagName }}
          </el-tag>
          <div class="mt-4">
            <el-select v-model="selectedTagId" placeholder="选择标签" size="small" style="width: 100%">
              <el-option v-for="tag in tags" :key="tag.tagId" :label="tag.tagName" :value="tag.tagId" />
            </el-select>
            <el-button type="primary" size="small" class="mt-2 w-full" @click="handleAssignTag">添加标签</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建标签弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增标签" width="450px">
      <el-form :model="newTag" label-width="80px" size="small">
        <el-form-item label="标签名称">
          <el-input v-model="newTag.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newTag.category" style="width: 100%">
            <el-option label="等级" value="等级" />
            <el-option label="行为" value="行为" />
            <el-option label="类型" value="类型" />
            <el-option label="风险" value="风险" />
            <el-option label="阶段" value="阶段" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签颜色">
          <el-color-picker v-model="newTag.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false" size="small">取消</el-button>
        <el-button type="primary" @click="handleCreate" size="small">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTagList, createTag, deleteTag, assignTag, removeTag } from '@/api/crm'

const tags = ref<any[]>([])
const categoryFilter = ref('')
const createDialogVisible = ref(false)
const newTag = ref({ tagName: '', category: '自定义', color: '#409EFF' })
const assignCustomerId = ref('1001')
const selectedTagId = ref(null)

const loadData = async () => {
  try {
    const res = await getTagList(categoryFilter.value || undefined)
    tags.value = res.data || []
  } catch { tags.value = MOCK_TAGS }
}

const MOCK_TAGS = [
  { tagId: 1, tagName: '高频交易者', category: 'TRADING_STYLE', color: '#409EFF', customerCount: 45 },
  { tagId: 2, tagName: '长线持仓', category: 'TRADING_STYLE', color: '#67C23A', customerCount: 32 },
  { tagId: 3, tagName: '高风险偏好', category: 'RISK', color: '#F56C6C', customerCount: 28 },
  { tagId: 4, tagName: '需重点关注', category: 'RISK', color: '#E6A23C', customerCount: 15 },
  { tagId: 5, tagName: 'VIP客户', category: 'LEVEL', color: '#8E5CD8', customerCount: 12 },
  { tagId: 6, tagName: '新注册用户', category: 'STAGE', color: '#909399', customerCount: 67 },
  { tagId: 7, tagName: 'API交易者', category: 'TRADING_STYLE', color: '#409EFF', customerCount: 23 },
]

const showCreateDialog = () => {
  newTag.value = { tagName: '', category: '自定义', color: '#409EFF' }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!newTag.value.tagName) { ElMessage.warning('请输入标签名称'); return }
  await createTag(newTag.value)
  ElMessage.success('标签创建成功')
  createDialogVisible.value = false
  await loadData()
}

const handleDelete = async (tagId: number) => {
  try {
    await ElMessageBox.confirm('确认删除此标签？', '确认')
    await deleteTag(tagId)
    ElMessage.success('标签已删除')
    await loadData()
  } catch { /* cancelled */ }
}

const handleAssignTag = async () => {
  const cid = parseInt(assignCustomerId.value)
  if (!cid || !selectedTagId.value) { ElMessage.warning('请填写客户ID并选择标签'); return }
  await assignTag(cid, selectedTagId.value)
  ElMessage.success('标签已添加')
}

const handleRemoveTag = async (tagId: number) => {
  const cid = parseInt(assignCustomerId.value)
  if (!cid) { ElMessage.warning('请填写客户ID'); return }
  await removeTag(cid, tagId)
  ElMessage.success('标签已移除')
}

watch(categoryFilter, loadData)
onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.tag-item { margin: 4px; cursor: pointer; }
.mt-2 { margin-top: 8px; }
.w-full { width: 100%; }
</style>
