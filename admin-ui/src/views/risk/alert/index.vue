<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 class="text-lg font-medium">异常交易报警</h2>
      <div>
        <el-select v-model="filterLevel" placeholder="报警级别" style="width: 140px; margin-right: 8px" clearable @change="loadData">
          <el-option label="严重" value="CRITICAL" />
          <el-option label="警告" value="WARN" />
        </el-select>
        <el-select v-model="filterType" placeholder="报警类型" style="width: 160px; margin-right: 8px" clearable @change="loadData">
          <el-option label="追保" value="MARGIN_CALL" />
          <el-option label="强平" value="FORCE_LIQUIDATION" />
          <el-option label="日内亏损超限" value="DAILY_LOSS" />
          <el-option label="持仓超限" value="POSITION_LIMIT" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </div>

    <el-table :data="alerts" border stripe v-loading="loading" max-height="600">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column label="报警级别" width="90">
        <template #default="{ row }">
          <el-tag :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">
            {{ row.alertLevel === 'CRITICAL' ? '严重' : '警告' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报警类型" width="140">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ alertTypeMap[row.alertType] || row.alertType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="报警消息" min-width="300" show-overflow-tooltip />
      <el-table-column prop="riskRatio" label="触发风险度(%)" width="130" align="right">
        <template #default="{ row }">{{ row.riskRatio != null ? row.riskRatio + '%' : '-' }}</template>
      </el-table-column>
      <el-table-column label="已处理" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.resolved ? 'success' : 'danger'" size="small">{{ row.resolved ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="报警时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.resolved" type="primary" link size="small" @click="handleResolve(row)">标记处理</el-button>
          <span v-else style="color: #999;">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="mt-4 flex justify-center">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRiskAlertList } from '@/api/risk'

const loading = ref(false)
const alerts = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterLevel = ref('')
const filterType = ref('')

const alertTypeMap: Record<string, string> = {
  MARGIN_CALL: '追保',
  FORCE_LIQUIDATION: '强平',
  DAILY_LOSS: '日内亏损超限',
  POSITION_LIMIT: '持仓超限',
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (filterLevel.value) params.alertLevel = filterLevel.value
    if (filterType.value) params.alertType = filterType.value
    const res = await getRiskAlertList(params)
    if (res.data) {
      alerts.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch {
    // 模拟数据
    alerts.value = [
      { id: 1, userId: 1001, alertLevel: 'CRITICAL', alertType: 'MARGIN_CALL', message: '用户 1001 保证金不足，当前权益 50000，需补缴 15000', riskRatio: 115.3, resolved: false, createTime: '2024-03-15 14:30:00' },
      { id: 2, userId: 1002, alertLevel: 'WARN', alertType: 'POSITION_LIMIT', message: '用户 1002 HSI 持仓 5200 手，超限 200 手', riskRatio: 85.6, resolved: false, createTime: '2024-03-15 14:25:00' },
      { id: 3, userId: 1003, alertLevel: 'CRITICAL', alertType: 'FORCE_LIQUIDATION', message: '用户 1003 风险度 131.8%，触发自动强平', riskRatio: 131.8, resolved: true, createTime: '2024-03-15 14:20:00' },
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

const handleResolve = (row: any) => {
  row.resolved = true
  ElMessage.success('已标记为已处理')
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
</style>
