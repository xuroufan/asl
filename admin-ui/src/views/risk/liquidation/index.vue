<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 class="text-lg font-medium">强平记录管理</h2>
      <div>
        <el-input
          v-model="searchUserId"
          placeholder="用户ID"
          style="width: 160px; margin-right: 8px"
          clearable
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </div>

    <el-table :data="records" border stripe v-loading="loading" max-height="600">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="symbol" label="合约" width="80" />
      <el-table-column prop="direction" label="方向" width="70" />
      <el-table-column prop="volume" label="手数" width="70" align="right" />
      <el-table-column prop="price" label="强平价格" width="120" align="right">
        <template #default="{ row }">{{ formatNumber(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="riskRatioBefore" label="强平前风险度(%)" width="140" align="right">
        <template #default="{ row }">{{ formatNumber(row.riskRatioBefore) }}%</template>
      </el-table-column>
      <el-table-column prop="riskRatioAfter" label="强平后风险度(%)" width="140" align="right">
        <template #default="{ row }">{{ formatNumber(row.riskRatioAfter) }}%</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'EXECUTED' ? 'danger' : row.status === 'PENDING' ? 'warning' : 'info'" size="small">
            {{ row.status === 'EXECUTED' ? '已执行' : row.status === 'PENDING' ? '待执行' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="触发时间" width="170" />
      <el-table-column prop="executedTime" label="执行时间" width="170" />
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
import { getLiquidationRecords } from '@/api/risk'

const loading = ref(false)
const records = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const searchUserId = ref('')

const formatNumber = (val: any) => {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (searchUserId.value) params.userId = searchUserId.value
    const res = await getLiquidationRecords(params)
    if (res.data) {
      records.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch {
    // 模拟数据
    records.value = [
      { id: 1, userId: 1001, symbol: 'HSI', direction: 'SELL', volume: 5, price: 18520.50, riskRatioBefore: 125.3, riskRatioAfter: 95.2, reason: '风控度超过120%阈值', status: 'EXECUTED', createTime: '2024-03-15 14:32:10', executedTime: '2024-03-15 14:32:15' },
      { id: 2, userId: 1002, symbol: 'ES', direction: 'SELL', volume: 3, price: 4520.75, riskRatioBefore: 131.8, riskRatioAfter: 98.5, reason: '风控度超过120%阈值', status: 'EXECUTED', createTime: '2024-03-15 14:35:22', executedTime: '2024-03-15 14:35:28' },
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.flex { display: flex; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.items-center { align-items: center; }
</style>
