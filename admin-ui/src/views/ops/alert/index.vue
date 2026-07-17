<template>
  <div class="ops-alert">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">告警管理</h2>
      <div>
        <el-select v-model="filters.level" size="small" style="width: 120px; margin-right: 8px" clearable placeholder="告警级别">
          <el-option label="全部" value="" />
          <el-option label="CRITICAL" value="CRITICAL" />
          <el-option label="WARNING" value="WARNING" />
          <el-option label="INFO" value="INFO" />
        </el-select>
        <el-select v-model="filters.status" size="small" style="width: 130px; margin-right: 8px" clearable placeholder="处理状态">
          <el-option label="全部" value="" />
          <el-option label="已触发" value="TRIGGERED" />
          <el-option label="已认领" value="ACKNOWLEDGED" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
      </div>
    </div>

    <!-- 告警统计 KPI -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="4" v-for="item in alertStatsCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 告警列表 -->
    <el-card shadow="hover">
      <el-table :data="alerts" stripe size="small" style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="text-sm p-2">
              <p><b>告警信息：</b>{{ row.message }}</p>
              <p class="mt-1" v-if="row.resolution"><b>处理方式：</b>{{ row.resolution }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="alertName" label="告警名称" min-width="160" />
        <el-table-column label="级别" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="alertLevelTag(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务" width="140">
          <template #default="{ row }">{{ row.serviceName?.replace('futures-', '') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="alertStatusTag(row.status)" size="small">
              {{ { TRIGGERED: '已触发', ACKNOWLEDGED: '已认领', RESOLVED: '已解决' }[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="claimedBy" label="处理人" width="80" align="center" />
        <el-table-column prop="triggerTime" label="触发时间" width="160" />
        <el-table-column prop="duration" label="持续时长" width="90" align="center" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'TRIGGERED'" type="primary" link size="small"
              @click="handleClaim(row.alertId)">认领</el-button>
            <el-button v-if="row.status !== 'RESOLVED'" type="success" link size="small"
              @click="handleResolve(row.alertId)">解决</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlertList, getAlertStats, claimAlert, resolveAlert } from '@/api/ops'

const filters = ref({ level: '', status: '', serviceName: '' })
const alerts = ref<any[]>([])
const alertStats = ref<any>({})

const alertLevelTag = (level: string) => {
  const map: Record<string, string> = { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}
const alertStatusTag = (status: string) => {
  const map: Record<string, string> = { TRIGGERED: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success' }
  return map[status] || 'info'
}

const alertStatsCards = computed(() => [
  { label: '告警总数', value: alertStats.value.totalAlerts || 128, color: '#409eff' },
  { label: '待处理', value: alertStats.value.triggeredAlerts || 5, color: '#f56c6c' },
  { label: '处理中', value: alertStats.value.acknowledgedAlerts || 8, color: '#e6a23c' },
  { label: '已解决', value: alertStats.value.resolvedAlerts || 115, color: '#67c23a' },
  { label: '严重告警', value: alertStats.value.criticalAlerts || 12, color: '#f56c6c' },
  { label: '平均解决', value: (alertStats.value.avgResolutionTime || 25.5) + 'min', color: '#909399' },
])

const loadAlerts = async () => {
  try {
    const res = await getAlertList({ ...filters.value, page: 1, size: 50 })
    alerts.value = res.data || []
  } catch {
    alerts.value = []
  }
}

const loadStats = async () => {
  try {
    const res = await getAlertStats()
    alertStats.value = res.data || {}
  } catch { /* ignore */ }
}

const loadData = async () => {
  await Promise.all([loadAlerts(), loadStats()])
}

const handleClaim = async (alertId: string) => {
  try {
    const res = await claimAlert(alertId)
    ElMessage.success(res.data || '已认领')
    await loadData()
  } catch { /* ignore */ }
}

const handleResolve = async (alertId: string) => {
  try {
    await ElMessageBox.prompt('请输入处理方式', '解决告警', { inputValue: '已修复' })
    const res = await resolveAlert(alertId, 'FIXED', '已处理')
    ElMessage.success(res.data || '已解决')
    await loadData()
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
