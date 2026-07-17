<template>
  <div class="ops-log">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">日志查询</h2>
      <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="mb-4">
      <el-form :model="filters" inline size="small">
        <el-form-item label="服务">
          <el-select v-model="filters.serviceName" style="width: 150px" clearable>
            <el-option v-for="s in services" :key="s" :label="s.replace('futures-', '')" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" style="width: 100px" clearable>
            <el-option label="全部" value="" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
            <el-option label="DEBUG" value="DEBUG" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" placeholder="搜索日志内容" style="width: 200px" clearable />
        </el-form-item>
        <el-form-item label="TraceID">
          <el-input v-model="filters.traceId" placeholder="输入traceId" style="width: 220px" clearable />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间"
            end-placeholder="结束时间" style="width: 320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchLogs" :icon="'Search'">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="mb-4">
      <el-col :span="14">
        <!-- 日志结果 -->
        <el-card shadow="hover">
          <template #header><span>日志列表</span></template>
          <el-table :data="logs" stripe size="small" style="width: 100%" max-height="500">
            <el-table-column label="时间" width="160">
              <template #default="{ row }"><span class="text-xs">{{ row.timestamp }}</span></template>
            </el-table-column>
            <el-table-column label="级别" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="logLevelTag(row.level)" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="服务" width="130">
              <template #default="{ row }">{{ row.serviceName?.replace('futures-', '') }}</template>
            </el-table-column>
            <el-table-column label="日志内容" min-width="300">
              <template #default="{ row }">
                <span class="text-xs">{{ row.message }}</span>
              </template>
            </el-table-column>
            <el-table-column label="TraceID" width="130">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewTrace(row.traceId)">
                  <code class="text-xs">{{ row.traceId?.slice(0, 16) }}...</code>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="mb-4">
          <template #header><span>日志统计</span></template>
          <div class="flex items-center gap-4 mb-2">
            <div><span class="text-xs text-gray-400">总日志</span><div class="text-lg font-bold">{{ logStats.totalLogs?.toLocaleString() }}</div></div>
            <div><span class="text-xs text-gray-400">ERROR</span><div class="text-lg font-bold text-danger">{{ logStats.errorCount }}</div></div>
            <div><span class="text-xs text-gray-400">WARN</span><div class="text-lg font-bold text-warning">{{ logStats.warnCount }}</div></div>
            <div><span class="text-xs text-gray-400">错误率</span><div class="text-lg font-bold">{{ (logStats.errorRate * 100).toFixed(2) }}%</div></div>
          </div>
        </el-card>
        <el-card shadow="hover">
          <template #header><span>Top 错误类型</span></template>
          <div v-for="e in logStats.topErrors" :key="e.type" class="flex justify-between text-sm py-1">
            <span>{{ e.type }}</span>
            <el-tag size="small">{{ e.count }} 次</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchLogs, getLogStats, getLogContext } from '@/api/ops'

const services = [
  'futures-gateway', 'futures-order', 'futures-matching', 'futures-account',
  'futures-fund', 'futures-risk', 'futures-market', 'futures-settlement'
]

const filters = ref({ serviceName: 'futures-order', level: '', keyword: '', traceId: '' })
const timeRange = ref(null)
const logs = ref<any[]>([])
const logStats = ref<any>({ totalLogs: 0, errorCount: 0, warnCount: 0, errorRate: 0, topErrors: [] })

const logLevelTag = (level: string) => {
  const map: Record<string, string> = { INFO: 'info', WARN: 'warning', ERROR: 'danger', DEBUG: 'default' }
  return map[level] || 'info'
}

const searchLogs = async () => {
  try {
    const res = await searchLogs({
      ...filters.value,
      startTime: timeRange.value?.[0]?.toISOString(),
      endTime: timeRange.value?.[1]?.toISOString(),
      page: 1, size: 50
    })
    logs.value = res.data || []
  } catch {
    logs.value = []
  }
}

const viewTrace = async (traceId: string) => {
  try {
    const res = await getLogContext(traceId)
    const ctx = res.data || []
    ElMessageBox.alert(
      ctx.map((c: any) => `<div class="mb-1 text-xs">
        <span class="text-gray-400">${c.timestamp}</span>
        <el-tag size="mini" :type="c.level === 'ERROR' ? 'danger' : 'info'" class="ml-1">${c.level}</el-tag>
        <span class="ml-1">${c.message}</span>
      </div>`).join(''),
      { title: 'TraceID: ' + traceId, dangerouslyUseHTMLString: true, width: '800px' }
    )
  } catch { /* ignore */ }
}

const loadData = async () => {
  try {
    const res = await getLogStats({ serviceName: filters.value.serviceName })
    logStats.value = res.data || logStats.value
  } catch { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
</style>
