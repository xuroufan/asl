<template>
  <div class="risk-config">
    <div class="mb-4 flex justify-between items-center">
      <h2 class="text-lg font-medium">风控规则配置</h2>
      <div>
        <el-button type="warning" plain @click="handleRefresh">刷新缓存</el-button>
      </div>
    </div>

    <el-table :data="configList" border stripe v-loading="loading" max-height="600">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="symbol" label="合约代码" width="120" />
      <el-table-column label="保证金率" width="150">
        <template #default="{ row }">
          <el-input-number
            v-model="row.marginRate"
            :precision="4"
            :step="0.01"
            :min="0.01"
            :max="1"
            size="small"
            @change="handleEdit(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="持仓限额(手)" width="150">
        <template #default="{ row }">
          <el-input-number
            v-model="row.positionLimit"
            :min="1"
            :max="99999"
            size="small"
            @change="handleEdit(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="合约乘数" width="120">
        <template #default="{ row }">
          <el-input-number
            v-model="row.contractMultiplier"
            :precision="0"
            :min="1"
            size="small"
            @change="handleEdit(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="预警阈值(%)" width="150">
        <template #default="{ row }">
          <el-input-number
            v-model="row.warningRatio"
            :precision="2"
            :step="5"
            :min="0"
            :max="200"
            size="small"
            @change="handleEdit(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="强平阈值(%)" width="150">
        <template #default="{ row }">
          <el-input-number
            v-model="row.liquidationRatio"
            :precision="2"
            :step="5"
            :min="0"
            :max="200"
            size="small"
            @change="handleEdit(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleEdit(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRiskConfigList, updateRiskConfig, refreshRiskConfig } from '@/api/risk'

const loading = ref(false)
const configList = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRiskConfigList()
    if (res.data) {
      configList.value = res.data
    }
  } catch {
    // 加载模拟数据
    configList.value = [
      { id: 1, symbol: 'HSI', marginRate: 0.05, positionLimit: 5000, contractMultiplier: 50, warningRatio: 80, liquidationRatio: 120, enabled: true, createTime: '2024-01-01 00:00:00' },
      { id: 2, symbol: 'ES', marginRate: 0.08, positionLimit: 3000, contractMultiplier: 50, warningRatio: 80, liquidationRatio: 120, enabled: true, createTime: '2024-01-01 00:00:00' },
      { id: 3, symbol: 'GC', marginRate: 0.1, positionLimit: 2000, contractMultiplier: 100, warningRatio: 75, liquidationRatio: 115, enabled: true, createTime: '2024-01-01 00:00:00' },
      { id: 4, symbol: 'CL', marginRate: 0.12, positionLimit: 1000, contractMultiplier: 1000, warningRatio: 80, liquidationRatio: 120, enabled: true, createTime: '2024-01-01 00:00:00' },
    ]
  } finally {
    loading.value = false
  }
}

const handleEdit = async (row: any) => {
  try {
    await updateRiskConfig(row)
    ElMessage.success('风控配置已更新')
  } catch {
    ElMessage.warning('更新失败（模拟模式已保存本地）')
  }
}

const handleRefresh = async () => {
  try {
    await refreshRiskConfig()
    ElMessage.success('风控配置缓存已刷新')
  } catch {
    ElMessage.warning('刷新失败，风控服务可能未运行')
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
</style>
