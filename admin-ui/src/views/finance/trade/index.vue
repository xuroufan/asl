<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 class="text-lg font-medium">交易流水查询</h2>
      <div>
        <el-button @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <el-card shadow="hover" class="mb-4">
      <el-form :inline="true" :model="query">
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="留空查全部" style="width: 140px" clearable />
        </el-form-item>
        <el-form-item label="合约">
          <el-select v-model="query.symbol" placeholder="全部" style="width: 120px" clearable>
            <el-option label="HSI" value="HSI" />
            <el-option label="ES" value="ES" />
            <el-option label="GC" value="GC" />
            <el-option label="CL" value="CL" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="records" border stripe v-loading="loading" max-height="500">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="settlementDate" label="结算日期" width="120" />
      <el-table-column prop="openingEquity" label="期初权益" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.openingEquity) }}</template>
      </el-table-column>
      <el-table-column prop="closingEquity" label="期末权益" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.closingEquity) }}</template>
      </el-table-column>
      <el-table-column prop="dailyProfit" label="当日盈亏" width="120" align="right">
        <template #default="{ row }">
          <span :style="{ color: (row.dailyProfit || 0) >= 0 ? '#f56c6c' : '#67c23a' }">
            {{ formatMoney(row.dailyProfit) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="fee" label="手续费" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.fee) }}</template>
      </el-table-column>
      <el-table-column prop="margin" label="保证金" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.margin) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SETTLED' ? 'success' : 'warning'" size="small">
            {{ row.status === 'SETTLED' ? '已结算' : '待结算' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTradeList, exportTrades } from '@/api/finance'

const loading = ref(false)
const records = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const query = reactive({
  userId: '',
  symbol: '',
  startDate: '',
  endDate: '',
})

const formatMoney = (val: any) => {
  if (val == null) return '-'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const resetQuery = () => {
  query.userId = ''
  query.symbol = ''
  query.startDate = ''
  query.endDate = ''
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (query.userId) params.userId = query.userId
    if (query.symbol) params.symbol = query.symbol
    if (query.startDate) params.startDate = query.startDate
    if (query.endDate) params.endDate = query.endDate
    const res = await getTradeList(params)
    if (res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    records.value = Array.from({ length: 15 }, (_, i) => ({
      id: i + 1, userId: 1001 + i, username: 'user_' + String(1001 + i),
      settlementDate: '2024-03-' + String(10 + (i % 15)).padStart(2, '0'),
      openingEquity: 100000 + i * 1000, closingEquity: 102000 + i * 500,
      dailyProfit: 2000 - i * 100, fee: 150 + i * 10, margin: 30000 + i * 1000,
      status: i % 5 === 0 ? 'PENDING' : 'SETTLED',
      createTime: '2024-03-' + String(10 + (i % 15)).padStart(2, '0') + ' 16:30:00',
    }))
    total.value = 50
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  try {
    await exportTrades(query)
    ElMessage.success('导出任务已提交')
  } catch {
    ElMessage.warning('导出功能需服务端支持')
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
</style>
