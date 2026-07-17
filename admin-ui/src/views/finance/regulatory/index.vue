<template>
  <div>
    <h2 class="text-lg font-medium mb-4">监管报表</h2>

    <el-card shadow="hover" class="mb-4">
      <el-form :inline="true">
        <el-form-item label="报表类型">
          <el-select v-model="reportType" style="width: 160px">
            <el-option label="客户资金报表" value="CLIENT_FUNDS" />
            <el-option label="交易汇总报表" value="TRADE_SUMMARY" />
            <el-option label="持仓报表" value="POSITION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleGenerate">生成报表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="reportList" border stripe max-height="500">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="reportName" label="报表名称" min-width="220" />
      <el-table-column prop="reportType" label="报表类型" width="140">
        <template #default="{ row }">
          <el-tag>{{ row.reportType === 'DAILY' ? '日报' : row.reportType === 'MONTHLY' ? '月报' : row.reportType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reportDate" label="报表日期" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
            {{ row.status === 'PUBLISHED' ? '已发布' : '审核中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="生成时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleExport(row, 'PDF')">导出 PDF</el-button>
          <el-button type="primary" link size="small" @click="handleExport(row, 'EXCEL')">导出 Excel</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRegulatoryReportList, generateRegulatoryReport, exportRegulatoryReport } from '@/api/finance'

const reportType = ref('CLIENT_FUNDS')
const reportList = ref<any[]>([])

const loadData = async () => {
  try {
    const res = await getRegulatoryReportList({})
    if (res.data) reportList.value = res.data
  } catch {
    reportList.value = [
      { id: 1, reportType: 'DAILY', reportName: 'SFC客户资金报表-20240315', reportDate: '2024-03-15', status: 'PUBLISHED', createTime: '2024-03-15T18:00:00' },
      { id: 2, reportType: 'DAILY', reportName: 'SFC交易汇总报表-20240315', reportDate: '2024-03-15', status: 'PUBLISHED', createTime: '2024-03-15T18:00:00' },
      { id: 3, reportType: 'DAILY', reportName: 'SFC持仓报表-20240315', reportDate: '2024-03-15', status: 'PUBLISHED', createTime: '2024-03-15T18:00:00' },
      { id: 4, reportType: 'MONTHLY', reportName: 'SFC月报表-202403', reportDate: '2024-03-31', status: 'AUDITING', createTime: '2024-03-31T18:00:00' },
    ]
  }
}

const handleGenerate = async () => {
  try {
    await generateRegulatoryReport(reportType.value)
    ElMessage.success('监管报表已生成')
    loadData()
  } catch {
    ElMessage.warning('生成失败（模拟模式）')
  }
}

const handleExport = async (row: any, format: string) => {
  try {
    await exportRegulatoryReport(row.id, format)
    ElMessage.success('导出任务已提交')
  } catch {
    ElMessage.warning('导出功能需服务端支持')
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
</style>
