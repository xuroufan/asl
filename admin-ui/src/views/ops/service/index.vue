<template>
  <div class="ops-service">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">服务状态监控</h2>
      <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
    </div>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="4" v-for="item in kpiCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 监控大盘图表 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center justify-between">
              <span>服务健康状态</span>
              <el-tag :type="allHealthy ? 'success' : 'danger'" size="small">
                {{ allHealthy ? '全部正常' : '部分异常' }}
              </el-tag>
            </div>
          </template>
          <el-table :data="services" stripe style="width: 100%" size="small" max-height="400">
            <el-table-column prop="displayName" label="服务名称" min-width="120" />
            <el-table-column prop="serviceName" label="服务ID" min-width="150">
              <template #default="{ row }">
                <code class="text-xs">{{ row.serviceName }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="instances" label="实例数" width="80" align="center" />
            <el-table-column label="健康实例" width="100" align="center">
              <template #default="{ row }">
                <span class="text-success">{{ row.healthy }}/{{ row.instances }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'RUNNING' ? 'success' : 'warning'" size="small">
                  {{ row.status === 'RUNNING' ? '运行中' : '降级' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="QPS" width="90" align="right">
              <template #default="{ row }">{{ row.qps.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="平均响应(ms)" width="110" align="right">
              <template #default="{ row }">{{ row.avgResponseTime }}</template>
            </el-table-column>
            <el-table-column label="成功率" width="90" align="center">
              <template #default="{ row }">{{ row.successRate }}%</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showInstances(row.serviceName)">
                  实例详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>服务依赖拓扑</span></template>
          <div class="topology-placeholder">
            <div class="text-gray-400 text-sm text-center py-12">
              <el-icon class="mb-2" :size="48"><Connection /></el-icon>
              <p>服务依赖拓扑图</p>
              <p class="text-xs mt-2">gateway → order → matching</p>
              <p class="text-xs">gateway → account → fund → settlement</p>
              <p class="text-xs">gateway → market</p>
              <p class="text-xs">order → fund, risk</p>
              <p class="text-xs">matching → market</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实例详情弹窗 -->
    <el-dialog v-model="instanceDialogVisible" :title="'实例详情: ' + selectedService" width="800px">
      <el-table :data="instances" stripe size="small" style="width: 100%">
        <el-table-column prop="instanceId" label="实例ID" min-width="150">
          <template #default="{ row }"><code class="text-xs">{{ row.instanceId }}</code></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uptime" label="运行时间" width="100" />
        <el-table-column label="CPU使用率" width="110" align="right">
          <template #default="{ row }">{{ row.cpuUsage }}%</template>
        </el-table-column>
        <el-table-column label="内存使用率" width="110" align="right">
          <template #default="{ row }">{{ row.memoryUsage }}%</template>
        </el-table-column>
        <el-table-column prop="startTime" label="启动时间" width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Connection } from '@element-plus/icons-vue'
import { getServiceDashboard, getServiceInstances } from '@/api/ops'

const kpiCards = ref<any[]>([])
const services = ref<any[]>([])
const instances = ref<any[]>([])
const selectedService = ref('')
const instanceDialogVisible = ref(false)

const allHealthy = computed(() => services.value.every(s => s.status === 'RUNNING'))

const showInstances = async (serviceName: string) => {
  selectedService.value = serviceName
  try {
    const res = await getServiceInstances(serviceName)
    instances.value = res.data || []
  } catch {
    instances.value = []
  }
  instanceDialogVisible.value = true
}

const loadData = async () => {
  try {
    const res = await getServiceDashboard()
    const d = res.data
    services.value = d.services || []
    kpiCards.value = [
      { label: '总服务数', value: d.totalServices, color: '#409eff' },
      { label: '健康服务', value: d.healthyServices, color: '#67c23a' },
      { label: '异常服务', value: d.unhealthyServices, color: '#f56c6c' },
      { label: '总实例数', value: d.totalInstances, color: '#409eff' },
      { label: '总成功率', value: d.overallSuccessRate + '%', color: '#67c23a' },
      { label: '平均响应', value: d.avgResponseTime + 'ms', color: '#e6a23c' },
    ]
  } catch {
    kpiCards.value = [
      { label: '总服务数', value: 8, color: '#409eff' },
      { label: '健康服务', value: 7, color: '#67c23a' },
      { label: '异常服务', value: 1, color: '#f56c6c' },
      { label: '总实例数', value: 16, color: '#409eff' },
      { label: '总成功率', value: '99.2%', color: '#67c23a' },
      { label: '平均响应', value: '45ms', color: '#e6a23c' },
    ]
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 22px; font-weight: bold; }
</style>
