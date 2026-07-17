<template>
  <div class="crm-communication">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">客户沟通记录</h2>
      <div>
        <el-input v-model="searchCustomerId" placeholder="输入客户ID" size="small" style="width: 150px; margin-right: 8px" />
        <el-button type="primary" @click="loadCommunications" :icon="'Search'" size="small">查询</el-button>
        <el-button @click="showCreateDialog" type="primary" :icon="'Plus'" size="small">新增记录</el-button>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><span>沟通列表</span></template>
          <el-table :data="communications" stripe size="small" style="width: 100%">
            <el-table-column label="时间" width="150">
              <template #default="{ row }"><span class="text-xs">{{ row.contactTime }}</span></template>
            </el-table-column>
            <el-table-column label="方式" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="methodTag(row.method)" size="small">{{ row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="沟通内容" min-width="300" show-overflow-tooltip />
            <el-table-column prop="staff" label="客服" width="80" align="center" />
            <el-table-column label="满意度" width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.satisfaction">
                  <el-rate :model-value="row.satisfaction" disabled size="small" />
                </span>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="mb-4">
          <template #header><span>跟进提醒</span></template>
          <div v-if="followUps.length === 0" class="text-gray-400 text-sm text-center py-8">暂无跟进提醒</div>
          <div v-for="f in followUps" :key="f.followUpId" class="follow-up-item">
            <div class="flex justify-between items-center">
              <span class="text-sm font-medium">{{ f.customerName }}</span>
              <el-tag :type="f.priority === '高' ? 'danger' : (f.priority === '中' ? 'warning' : 'info')" size="small">{{ f.priority }}</el-tag>
            </div>
            <div class="text-xs text-gray-400 mt-1">{{ f.content }}</div>
            <div class="text-xs mt-1">跟进日期: {{ f.followUpDate }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增沟通记录弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增沟通记录" width="550px">
      <el-form :model="newComm" label-width="80px" size="small">
        <el-form-item label="客户ID">
          <el-input-number v-model="newComm.customerId" :min="1000" :max="9999" />
        </el-form-item>
        <el-form-item label="沟通方式">
          <el-select v-model="newComm.method" style="width: 100%">
            <el-option label="电话" value="电话" />
            <el-option label="在线聊天" value="在线聊天" />
            <el-option label="邮件" value="邮件" />
            <el-option label="面谈" value="面谈" />
            <el-option label="微信" value="微信" />
          </el-select>
        </el-form-item>
        <el-form-item label="沟通内容">
          <el-input v-model="newComm.content" type="textarea" :rows="4" placeholder="请输入沟通内容" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="newComm.nextFollowUp" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false" size="small">取消</el-button>
        <el-button type="primary" @click="handleCreate" size="small">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommunicationList, createCommunication, getFollowUpList } from '@/api/crm'

const searchCustomerId = ref('1001')
const communications = ref<any[]>([])
const followUps = ref<any[]>([])
const createDialogVisible = ref(false)
const newComm = ref({ customerId: 1001, method: '电话', content: '', nextFollowUp: null })

const methodTag = (m: string) => ({ 电话: 'primary', '在线聊天': 'success', 邮件: 'info', 面谈: 'warning', 微信: 'default' }[m] || 'info')

const loadCommunications = async () => {
  try {
    const cid = parseInt(searchCustomerId.value) || 1001
    const res = await getCommunicationList({ customerId: cid, page: 1, size: 50 })
    communications.value = res.data?.records || res.data || []
  } catch { communications.value = [] }
}

const loadFollowUps = async () => {
  try {
    const res = await getFollowUpList({ page: 1, size: 10 })
    followUps.value = res.data || []
  } catch { followUps.value = [] }
}

const loadData = async () => {
  await Promise.all([loadCommunications(), loadFollowUps()])
}

const showCreateDialog = () => {
  newComm.value = { customerId: 1001, method: '电话', content: '', nextFollowUp: null }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!newComm.value.content) { ElMessage.warning('请输入沟通内容'); return }
  await createCommunication(newComm.value)
  ElMessage.success('沟通记录已保存')
  createDialogVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.follow-up-item { border-bottom: 1px solid #ebeef5; padding: 8px 0; }
.follow-up-item:last-child { border-bottom: none; }
</style>
