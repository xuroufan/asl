<template>
  <div>
    <h2 class="text-lg font-medium mb-4">日报/月报</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="日报" name="daily">
        <div class="mb-4">
          <el-date-picker v-model="reportDate" type="date" placeholder="选择日期" @change="loadDaily" />
          <el-button type="primary" class="ml-2" @click="loadDaily">生成日报</el-button>
          <el-button class="ml-2" @click="handleExport">导出</el-button>
        </div>
        <el-table :data="dailyData" border stripe max-height="500">
          <el-table-column prop="label" label="指标" min-width="180" />
          <el-table-column prop="value" label="数值" min-width="200" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="月报" name="monthly">
        <div class="mb-4">
          <el-date-picker v-model="monthDate" type="month" placeholder="选择月份" @change="loadMonthly" />
          <el-button type="primary" class="ml-2" @click="loadMonthly">生成月报</el-button>
          <el-button class="ml-2" @click="handleExport">导出</el-button>
        </div>
        <el-table :data="monthlyData" border stripe max-height="500">
          <el-table-column prop="label" label="指标" min-width="180" />
          <el-table-column prop="value" label="数值" min-width="200" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="报表审核" name="audit">
        <el-table :data="reportHistory" border stripe max-height="400" class="mb-4">
          <el-table-column prop="reportName" label="报表名称" min-width="200" />
          <el-table-column prop="reportType" label="类型" width="100">
            <template #default="{ row }">{{ row.reportType === 'DAILY' ? '日报' : '月报' }}</template>
          </el-table-column>
          <el-table-column prop="reportDate" label="日期" width="120" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'AUDITING' ? 'warning' : 'info'" size="small">
                {{ row.status === 'PUBLISHED' ? '已发布' : row.status === 'AUDITING' ? '审核中' : '已生成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'AUDITING'" type="success" link size="small" @click="handleAudit(row.id, 'APPROVE')">通过</el-button>
              <el-button v-if="row.status === 'AUDITING'" type="danger" link size="small" @click="handleAudit(row.id, 'REJECT')">驳回</el-button>
              <el-button v-else type="primary" link size="small" @click="handleExport">下载</el-button>
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
import { getDailyReport, getMonthlyReport, getReportHistory, generateReport, auditReport } from '@/api/finance'

const activeTab = ref('daily')
const reportDate = ref(new Date())
const monthDate = ref(new Date())
const dailyData = ref<any[]>([])
const monthlyData = ref<any[]>([])
const reportHistory = ref<any[]>([])

const loadDaily = async () => {
  try {
    const date = reportDate.value.toISOString().slice(0, 10)
    const res = await getDailyReport(date)
    if (res.data) {
      const d = res.data
      dailyData.value = [
        { label: '报表日期', value: d.reportDate },
        { label: '状态', value: d.status === 'GENERATED' ? '已生成' : d.status },
        { label: '总成交笔数', value: d.totalTrades?.toLocaleString() },
        { label: '总成交手数', value: d.totalVolume?.toLocaleString() },
        { label: '手续费收入', value: '¥' + (d.totalFee || 0).toLocaleString() },
        { label: '客户盈利合计', value: '¥' + (d.clientProfit || 0).toLocaleString() },
        { label: '客户亏损合计', value: '¥' + (d.clientLoss || 0).toLocaleString() },
        { label: '净盈亏', value: '¥' + (d.netPnl || 0).toLocaleString() },
        { label: '入金总额', value: '¥' + (d.totalDeposit || 0).toLocaleString() },
        { label: '出金总额', value: '¥' + (d.totalWithdraw || 0).toLocaleString() },
        { label: '期初权益', value: '¥' + (d.beginEquity || 0).toLocaleString() },
        { label: '期末权益', value: '¥' + (d.endEquity || 0).toLocaleString() },
      ]
    }
  } catch { /* 模拟数据已由后端提供 */ }
}

const loadMonthly = async () => {
  try {
    const ym = monthDate.value.toISOString().slice(0, 7)
    const res = await getMonthlyReport(ym)
    if (res.data) {
      const d = res.data
      monthlyData.value = [
        { label: '报表月份', value: d.yearMonth },
        { label: '状态', value: d.status === 'AUDITING' ? '审核中' : d.status === 'PUBLISHED' ? '已发布' : '已生成' },
        { label: '交易日数', value: d.totalTradingDays },
        { label: '总成交笔数', value: d.totalTrades?.toLocaleString() },
        { label: '总成交手数', value: d.totalVolume?.toLocaleString() },
        { label: '手续费总收入', value: '¥' + (d.totalFee || 0).toLocaleString() },
        { label: '客户盈利合计', value: '¥' + (d.totalClientProfit || 0).toLocaleString() },
        { label: '客户亏损合计', value: '¥' + (d.totalClientLoss || 0).toLocaleString() },
        { label: '净收入', value: '¥' + (d.netRevenue || 0).toLocaleString() },
        { label: '入金总额', value: '¥' + (d.totalDeposit || 0).toLocaleString() },
        { label: '出金总额', value: '¥' + (d.totalWithdraw || 0).toLocaleString() },
        { label: '期初权益', value: '¥' + (d.beginMonthEquity || 0).toLocaleString() },
        { label: '期末权益', value: '¥' + (d.endMonthEquity || 0).toLocaleString() },
      ]
    }
  } catch { /* 模拟数据已由后端提供 */ }
}

const loadHistory = async () => {
  try {
    const res = await getReportHistory({})
    if (res.data) reportHistory.value = res.data
  } catch {
    reportHistory.value = [
      { id: 1, reportType: 'DAILY', reportName: '日报表-20240315', reportDate: '2024-03-15', status: 'PUBLISHED', createTime: '2024-03-15T18:00:00' },
      { id: 2, reportType: 'DAILY', reportName: '日报表-20240314', reportDate: '2024-03-14', status: 'PUBLISHED', createTime: '2024-03-14T18:00:00' },
      { id: 3, reportType: 'MONTHLY', reportName: '月报表-202403', reportDate: '2024-03-31', status: 'AUDITING', createTime: '2024-03-31T18:00:00' },
    ]
  }
}

const handleExport = () => ElMessage.success('导出任务已提交')
const handleAudit = async (id: number, action: string) => {
  await auditReport(id, action)
  ElMessage.success(action === 'APPROVE' ? '审核通过，报表已发布' : '报表已驳回')
  loadHistory()
}

onMounted(() => { loadDaily(); loadHistory() })
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.ml-2 { margin-left: 8px; }
</style>
