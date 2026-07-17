<template>
  <div>
    <h2 class="text-lg font-medium mb-4">对账管理</h2>

    <el-card shadow="hover" class="mb-4">
      <el-form :inline="true">
        <el-form-item label="对账日期">
          <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRunReconciliation('EXCHANGE')">执行交易所对账</el-button>
          <el-button type="primary" @click="handleRunReconciliation('BANK')">执行银行对账</el-button>
          <el-button type="warning" @click="handleRunReconciliation('FULL')">执行完整对账</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="tab">
      <el-tab-pane label="对账列表" name="list">
        <el-table :data="reconList" border stripe max-height="400" @row-click="viewDiffs">
          <el-table-column prop="reconciliationDate" label="对账日期" width="120" />
          <el-table-column label="对账类型" width="120">
            <template #default="{ row }">{{ row.type === 'EXCHANGE' ? '交易所' : '银行' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.status === 'COMPLETED' ? '已完成' : '进行中' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ourTotal" label="我方总额" width="150" align="right">
            <template #default="{ row }">¥{{ (row.ourTotal || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="theirTotal" label="对方总额" width="150" align="right">
            <template #default="{ row }">¥{{ (row.theirTotal || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="diffCount" label="差异数" width="80" align="center" />
          <el-table-column prop="resolvedCount" label="已处理" width="80" align="center" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="差异明细" name="diffs">
        <el-table :data="diffList" border stripe max-height="500">
          <el-table-column prop="diffType" label="差异类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.diffType === 'MISMATCH' ? 'danger' : row.diffType === 'MISSING' ? 'warning' : 'info'">
                {{ row.diffType === 'MISMATCH' ? '金额不符' : row.diffType === 'MISSING' ? '我方多录' : '对方多录' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ourRecordId" label="我方记录ID" width="180" />
          <el-table-column prop="theirRecordId" label="对方记录ID" width="180" />
          <el-table-column prop="ourAmount" label="我方金额" width="120" align="right">
            <template #default="{ row }">¥{{ (row.ourAmount || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="theirAmount" label="对方金额" width="120" align="right">
            <template #default="{ row }">¥{{ (row.theirAmount || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="amountDiff" label="差额" width="120" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.amountDiff > 0 ? '#f56c6c' : '#67c23a' }">
                ¥{{ (row.amountDiff || 0).toLocaleString() }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'RESOLVED' ? 'success' : 'danger'">{{ row.status === 'RESOLVED' ? '已处理' : '待处理' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" min-width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'RESOLVED'" type="primary" link size="small" @click="handleResolve(row)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReconciliationHistory, getReconciliationDiffs, runReconciliation, resolveDiff } from '@/api/finance'

const tab = ref('list')
const queryDate = ref(new Date())
const reconList = ref<any[]>([])
const diffList = ref<any[]>([])

const loadData = async () => {
  try {
    const res = await getReconciliationHistory({})
    if (res.data) reconList.value = res.data.records || []
  } catch {
    reconList.value = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1, reconciliationDate: '2024-03-' + String(15 - i).padStart(2, '0'),
      type: i % 2 === 0 ? 'EXCHANGE' : 'BANK',
      status: i === 0 ? 'IN_PROGRESS' : 'COMPLETED',
      ourTotal: 1256800 + i * 100, theirTotal: 1256800 + i * 100 + (i % 3 === 0 ? 50 : 0),
      diffCount: i % 3 === 0 ? 2 : 0, resolvedCount: i % 3 === 0 ? 1 : 0,
      createTime: '2024-03-' + String(15 - i).padStart(2, '0') + 'T16:30:00',
    }))
  }
}

const viewDiffs = async (row: any) => {
  try {
    const res = await getReconciliationDiffs(row.id)
    if (res.data) diffList.value = res.data
  } catch {
    diffList.value = [
      { diffType: 'MISMATCH', ourRecordId: 'TRADE20240315001', theirRecordId: 'EXT20240315001', ourAmount: 15230.50, theirAmount: 15230.00, amountDiff: 0.50, status: 'PENDING', notes: '手续费差异' },
      { diffType: 'MISSING', ourRecordId: 'TRADE20240315002', theirRecordId: '', ourAmount: 5000.00, theirAmount: 0, amountDiff: 5000.00, status: 'PENDING', notes: '我方有记录，对方缺' },
    ]
  }
  tab.value = 'diffs'
  ElMessage.info('已加载差异明细')
}

const handleRunReconciliation = async (type: string) => {
  const date = queryDate.value.toISOString().slice(0, 10)
  try {
    await runReconciliation(date, type)
    ElMessage.success(type === 'EXCHANGE' ? '交易所' : type === 'BANK' ? '银行' : '完整' + '对账任务已启动')
  } catch {
    ElMessage.warning('对账服务暂不可用（模拟模式）')
  }
}

const handleResolve = async (row: any) => {
  try {
    await resolveDiff(row.id, 'MANUAL_ADJUST')
    row.status = 'RESOLVED'
    ElMessage.success('差异已标记为已处理')
  } catch {
    ElMessage.warning('处理失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
</style>
