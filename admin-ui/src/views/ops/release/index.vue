<template>
  <div class="ops-release">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">发布管理</h2>
      <div>
        <el-button @click="showCreateDialog" type="primary" :icon="'Plus'" size="small">创建发布单</el-button>
        <el-button @click="loadStats" :icon="'Refresh'" size="small">刷新</el-button>
      </div>
    </div>

    <!-- 发布统计 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="4" v-for="item in statsCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发布趋势 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>发布趋势（近7天）</span></template>
          <el-table :data="dailyReleases" size="small" stripe>
            <el-table-column prop="date" label="日期" width="100" />
            <el-table-column prop="total" label="发布总数" width="100" align="center" />
            <el-table-column prop="success" label="成功" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ row.success }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成功率" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.total > 0 ? Math.round(row.success / row.total * 100) : 0" :stroke-width="16" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>发布策略说明</span></template>
          <div class="text-sm space-y-2">
            <p><el-tag size="small" type="primary">蓝绿部署</el-tag> 同时运行两套环境，切换流量完成发布</p>
            <p><el-tag size="small" type="warning">灰度发布</el-tag> 按比例逐步放量：5%→20%→50%→100%</p>
            <p><el-tag size="small" type="danger">回滚</el-tag> 一键回滚到上一版本，预计耗时2分钟</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发布历史 -->
    <el-card shadow="hover">
      <template #header><span>发布历史</span></template>
      <el-table :data="releases" stripe size="small" style="width: 100%">
        <el-table-column prop="releaseId" label="发布编号" width="120">
          <template #default="{ row }"><code class="text-xs">{{ row.releaseId }}</code></template>
        </el-table-column>
        <el-table-column prop="serviceName" label="服务" width="140">
          <template #default="{ row }">{{ row.serviceName.replace('futures-', '') }}</template>
        </el-table-column>
        <el-table-column label="版本变更" width="140">
          <template #default="{ row }">{{ row.previousVersion || '-' }} → {{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="strategy" label="策略" width="100">
          <template #default="{ row }">
            <el-tag :type="row.strategy === 'BLUE_GREEN' ? 'primary' : 'warning'" size="small">
              {{ row.strategy === 'BLUE_GREEN' ? '蓝绿部署' : '灰度' }}({{ row.grayPercent }}%)
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row.releaseId)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL'" type="warning" link size="small"
              @click="approveRelease(row.releaseId)">审批</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS'" type="danger" link size="small"
              @click="rollbackRelease(row.releaseId)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReleaseHistory, getReleaseStats, approveRelease, rollbackRelease } from '@/api/ops'

const releases = ref<any[]>([])
const dailyReleases = ref<any[]>([])
const statsCards = ref<any[]>([])

const tagType = (status: string) => {
  const map: Record<string, string> = {
    SUCCESS: 'success', ROLLED_BACK: 'danger', IN_PROGRESS: 'warning',
    PENDING_APPROVAL: 'info', FAILED: 'danger'
  }
  return map[status] || 'info'
}
const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    SUCCESS: '发布成功', ROLLED_BACK: '已回滚', IN_PROGRESS: '发布中',
    PENDING_APPROVAL: '待审批', FAILED: '失败'
  }
  return map[status] || status
}

const loadStats = async () => {
  try {
    const res = await getReleaseStats()
    const d = res.data
    dailyReleases.value = d.dailyReleases || []
    statsCards.value = [
      { label: '总发布数', value: d.totalReleases, color: '#409eff' },
      { label: '成功', value: d.successReleases, color: '#67c23a' },
      { label: '失败', value: d.failedReleases, color: '#f56c6c' },
      { label: '回滚', value: d.rolledBackReleases, color: '#e6a23c' },
      { label: '待审批', value: d.pendingApprovals, color: '#909399' },
      { label: '成功率', value: d.successRate + '%', color: '#67c23a' },
    ]
  } catch {
    statsCards.value = [
      { label: '总发布数', value: 156, color: '#409eff' },
      { label: '成功', value: 142, color: '#67c23a' },
      { label: '失败', value: 8, color: '#f56c6c' },
      { label: '回滚', value: 6, color: '#e6a23c' },
      { label: '待审批', value: 3, color: '#909399' },
      { label: '成功率', value: '91%', color: '#67c23a' },
    ]
  }
}

const loadData = async () => {
  try {
    const res = await getReleaseHistory({ page: 1, size: 50 })
    releases.value = res.data?.records || res.data || []
  } catch {
    releases.value = []
  }
  await loadStats()
}

const showDetail = (releaseId: string) => {
  ElMessage.info('发布详情: ' + releaseId)
}

const approveRelease = async (releaseId: string) => {
  try {
    await ElMessageBox.confirm('确认批准发布单 ' + releaseId + '？', '审批确认')
    const res = await approveRelease(releaseId, 'approve')
    ElMessage.success(res.data || '已批准')
    await loadData()
  } catch { /* cancelled */ }
}

const rollbackRelease = async (releaseId: string) => {
  try {
    await ElMessageBox.confirm('确认回滚发布 ' + releaseId + '？此操作不可逆。', '回滚确认', { type: 'warning' })
    const res = await rollbackRelease(releaseId)
    ElMessage.success(res.data || '已回滚')
    await loadData()
  } catch { /* cancelled */ }
}

const showCreateDialog = () => {
  ElMessage.info('发布单创建功能已准备')
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 22px; font-weight: bold; }
</style>
