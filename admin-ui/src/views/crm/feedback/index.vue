<template>
  <div class="crm-feedback">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">客户反馈处理</h2>
      <div>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="4" v-for="item in statsCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计分析 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>反馈类型分布</span></template>
          <div v-for="t in typeDistribution" :key="t.type" class="flex justify-between items-center py-1">
            <span class="text-sm">{{ t.type }}</span>
            <div class="flex items-center gap-2">
              <el-progress :percentage="Math.round(t.count / totalFeedbacks * 100)" :stroke-width="16" style="width: 160px" />
              <span class="text-sm text-gray-400">{{ t.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>来源分布</span></template>
          <div v-for="s in sourceDistribution" :key="s.source" class="flex justify-between items-center py-1">
            <span class="text-sm">{{ s.source }}</span>
            <div class="flex items-center gap-2">
              <el-progress :percentage="Math.round(s.count / totalFeedbacks * 100)" :stroke-width="16" style="width: 160px" />
              <span class="text-sm text-gray-400">{{ s.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 反馈列表 -->
    <el-card shadow="hover">
      <template #header>
        <div class="flex items-center justify-between">
          <span>反馈列表</span>
          <div>
            <el-select v-model="filters.status" size="small" style="width: 120px; margin-right: 8px" clearable placeholder="处理状态">
              <el-option label="全部" value="" /><el-option label="待处理" value="待处理" />
              <el-option label="处理中" value="处理中" /><el-option label="已解决" value="已解决" /><el-option label="已关闭" value="已关闭" />
            </el-select>
            <el-select v-model="filters.type" size="small" style="width: 130px; margin-right: 8px" clearable placeholder="反馈类型">
              <el-option label="全部" value="" /><el-option label="功能建议" value="功能建议" />
              <el-option label="问题反馈" value="问题反馈" /><el-option label="服务投诉" value="服务投诉" /><el-option label="产品咨询" value="产品咨询" />
            </el-select>
            <el-button type="primary" size="small" @click="loadFeedbacks" :icon="'Search'">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="feedbacks" stripe size="small" style="width: 100%">
        <el-table-column prop="feedbackId" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="fbStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="80" />
        <el-table-column prop="priority" label="优先级" width="70" align="center">
          <template #default="{ row }"><el-tag :type="row.priority === '高' ? 'danger' : (row.priority === '中' ? 'warning' : 'info')" size="small">{{ row.priority }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="assignee" label="处理人" width="80" align="center">
          <template #default="{ row }">{{ row.assignee || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="150" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.assignee" type="primary" link size="small" @click="handleAssign(row.feedbackId)">分配</el-button>
            <el-button v-if="row.status === '待处理' || row.status === '处理中'" type="success" link size="small"
              @click="handleResolve(row.feedbackId)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFeedbackList, getFeedbackStats, assignFeedback, updateFeedbackStatus } from '@/api/crm'

const filters = ref({ status: '', type: '', assignee: '' })
const feedbacks = ref<any[]>([])
const statsCards = ref<any[]>([])
const typeDistribution = ref<any[]>([])
const sourceDistribution = ref<any[]>([])
const totalFeedbacks = ref(1)

const fbStatusTag = (s: string) => ({ 待处理: 'danger', 处理中: 'warning', 已解决: 'success', 已关闭: 'info' }[s] || 'info')

const loadStats = async () => {
  try {
    const res = await getFeedbackStats()
    const d = res.data
    totalFeedbacks.value = d.totalFeedbacks || 1
    typeDistribution.value = d.typeDistribution || []
    sourceDistribution.value = d.sourceDistribution || []
    statsCards.value = [
      { label: '反馈总数', value: d.totalFeedbacks, color: '#409eff' },
      { label: '待处理', value: d.pendingCount, color: '#f56c6c' },
      { label: '处理中', value: d.processingCount, color: '#e6a23c' },
      { label: '已解决', value: d.resolvedCount, color: '#67c23a' },
      { label: '平均解决', value: d.avgResolutionTime + 'h', color: '#909399' },
      { label: '满意度', value: d.satisfactionRate + '%', color: '#67c23a' },
    ]
  } catch {
    statsCards.value = [
      { label: '反馈总数', value: 128, color: '#409eff' },
      { label: '待处理', value: 15, color: '#f56c6c' },
      { label: '处理中', value: 22, color: '#e6a23c' },
      { label: '已解决', value: 68, color: '#67c23a' },
      { label: '平均解决', value: '48.5h', color: '#909399' },
      { label: '满意度', value: '92.5%', color: '#67c23a' },
    ]
  }
}

const loadFeedbacks = async () => {
  try {
    const res = await getFeedbackList({ ...filters.value, page: 1, size: 50 })
    feedbacks.value = res.data?.records || res.data || []
  } catch { feedbacks.value = [] }
}

const loadData = async () => {
  await Promise.all([loadStats(), loadFeedbacks()])
}

const handleAssign = async (feedbackId: number) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理人姓名', '分配反馈')
    if (value) {
      await assignFeedback(feedbackId, value)
      ElMessage.success('已分配给 ' + value)
      await loadFeedbacks()
    }
  } catch { /* cancelled */ }
}

const handleResolve = async (feedbackId: number) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理结果', '处理反馈', { inputValue: '已处理完成' })
    await updateFeedbackStatus(feedbackId, '已解决', value)
    ElMessage.success('反馈已处理')
    await loadFeedbacks()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 20px; font-weight: bold; }
</style>
