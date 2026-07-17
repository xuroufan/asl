<template>
  <div class="ops-config">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">配置管理</h2>
      <div>
        <el-select v-model="selectedEnv" size="small" style="width: 120px; margin-right: 8px">
          <el-option label="开发环境" value="dev" />
          <el-option label="测试环境" value="test" />
          <el-option label="生产环境" value="prod" />
        </el-select>
        <el-select v-model="selectedService" size="small" style="width: 150px; margin-right: 8px" filterable>
          <el-option v-for="s in services" :key="s" :label="s.replace('futures-', '')" :value="s" />
        </el-select>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
        <el-button @click="showCompare" type="primary" size="small" :icon="'DataAnalysis'">环境对比</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="配置列表" name="list">
        <el-card shadow="hover">
          <el-table :data="configs" stripe size="small" style="width: 100%">
            <el-table-column prop="configName" label="配置名称" min-width="200" />
            <el-table-column prop="configKey" label="配置键" min-width="260">
              <template #default="{ row }"><code class="text-xs">{{ row.configKey }}</code></template>
            </el-table-column>
            <el-table-column prop="configValue" label="配置值" min-width="300">
              <template #default="{ row }">
                <el-input :model-value="row.configValue" readonly size="small" class="config-value-input" />
              </template>
            </el-table-column>
            <el-table-column label="版本" width="60" align="center">
              <template #default="{ row }">v{{ row.version }}</template>
            </el-table-column>
            <el-table-column prop="lastModified" label="最后修改" width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="showHistory(row.configId)">历史</el-button>
                <el-button type="warning" link size="small">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="环境对比" name="compare">
        <el-card shadow="hover">
          <div class="mb-4 flex items-center gap-2">
            <span class="text-sm">对比环境：</span>
            <el-select v-model="compareEnvA" size="small" style="width: 130px">
              <el-option label="开发环境" value="dev" />
              <el-option label="测试环境" value="test" />
              <el-option label="生产环境" value="prod" />
            </el-select>
            <span>vs</span>
            <el-select v-model="compareEnvB" size="small" style="width: 130px">
              <el-option label="开发环境" value="dev" />
              <el-option label="测试环境" value="test" />
              <el-option label="生产环境" value="prod" />
            </el-select>
            <el-button type="primary" size="small" @click="loadCompare">对比</el-button>
          </div>
          <el-table :data="configDiffs" stripe size="small" style="width: 100%">
            <el-table-column prop="configKey" label="配置键" min-width="240">
              <template #default="{ row }"><code class="text-xs">{{ row.configKey }}</code></template>
            </el-table-column>
            <el-table-column label="环境A" min-width="280">
              <template #default="{ row }"><code class="text-xs">{{ row.envAValue }}</code></template>
            </el-table-column>
            <el-table-column label="环境B" min-width="280">
              <template #default="{ row }"><code class="text-xs">{{ row.envBValue }}</code></template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '相同' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getConfigList, getConfigChangeHistory, compareConfigs } from '@/api/ops'

const activeTab = ref('list')
const selectedService = ref('futures-order')
const selectedEnv = ref('dev')
const compareEnvA = ref('dev')
const compareEnvB = ref('prod')
const configs = ref<any[]>([])
const configDiffs = ref<any[]>([])

const services = [
  'futures-gateway', 'futures-order', 'futures-matching', 'futures-account',
  'futures-fund', 'futures-risk', 'futures-market', 'futures-settlement'
]

const loadData = async () => {
  try {
    const res = await getConfigList({ serviceName: selectedService.value, env: selectedEnv.value })
    configs.value = res.data || []
  } catch {
    configs.value = []
  }
}

const loadCompare = async () => {
  try {
    const res = await compareConfigs(selectedService.value, compareEnvA.value, compareEnvB.value)
    configDiffs.value = res.data || []
  } catch {
    configDiffs.value = []
  }
}

const showHistory = async (configId: string) => {
  try {
    const res = await getConfigChangeHistory(configId)
    const history = res.data || []
    ElMessageBox.alert(
      history.map((h: any) => `<div class="mb-2 text-sm">
        <div><b>v${h.version}</b> by ${h.operator} at ${h.changeTime}</div>
        <div class="text-xs text-gray-400">原因: ${h.reason}</div>
        <div class="text-xs"><span class="text-danger">旧值:</span> ${h.oldValue}</div>
        <div class="text-xs"><span class="text-success">新值:</span> ${h.newValue}</div>
        <div>状态: <el-tag size="small" :type="h.status === 'APPLIED' ? 'success' : 'danger'">${h.status === 'APPLIED' ? '已生效' : '已回滚'}</el-tag></div>
      </div>`).join('<el-divider />'),
      { title: '配置变更历史', dangerouslyUseHTMLString: true, width: '700px' }
    )
  } catch { /* ignore */ }
}

const showCompare = () => { activeTab.value = 'compare' }

watch([selectedService, selectedEnv], loadData)
onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.config-value-input :deep(.el-input__inner) {
  font-family: monospace; font-size: 12px;
}
</style>
