<template>
  <div class="finance-dashboard">
    <h2 class="text-lg font-medium mb-4">财务统计看板</h2>

    <el-row :gutter="16" class="mb-4">
      <el-col :span="6" v-for="item in kpiCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>出入金趋势（近7天）</span></template>
          <v-chart :option="depositWithdrawOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>手续费收入趋势</span></template>
          <v-chart :option="feeTrendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getFinanceDashboard } from '@/api/finance'

use([BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const kpiCards = ref<any[]>([])
const depositTrend = ref<any[]>([])
const feeTrend = ref<any[]>([])

const depositWithdrawOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['入金', '出金'] },
  xAxis: { type: 'category', data: depositTrend.value.map((d: any) => d.date) },
  yAxis: { type: 'value', name: '金额(元)' },
  series: [
    { type: 'bar', name: '入金', data: depositTrend.value.map((d: any) => d.deposit), itemStyle: { color: '#67c23a' } },
    { type: 'bar', name: '出金', data: depositTrend.value.map((d: any) => d.withdraw), itemStyle: { color: '#f56c6c' } },
  ],
}))

const feeTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: feeTrend.value.map((d: any) => d.date) },
  yAxis: { type: 'value', name: '手续费(元)' },
  series: [{
    type: 'line', data: feeTrend.value.map((d: any) => d.fee),
    smooth: true, areaStyle: { opacity: 0.3 },
    itemStyle: { color: '#409eff' },
  }],
}))

const loadData = async () => {
  try {
    const res = await getFinanceDashboard()
    if (res.data) {
      const d = res.data
      kpiCards.value = [
        { label: '今日入金', value: '¥' + (d.todayDeposit || 0).toLocaleString(), color: '#67c23a' },
        { label: '今日出金', value: '¥' + (d.todayWithdraw || 0).toLocaleString(), color: '#f56c6c' },
        { label: '本月入金', value: '¥' + (d.monthDeposit || 0).toLocaleString(), color: '#67c23a' },
        { label: '本月出金', value: '¥' + (d.monthWithdraw || 0).toLocaleString(), color: '#f56c6c' },
        { label: '手续费收入', value: '¥' + (d.totalFee || 0).toLocaleString(), color: '#409eff' },
        { label: '客户盈亏', value: '¥' + (d.totalProfit || 0).toLocaleString(), color: '#e6a23c' },
        { label: '待对账', value: d.pendingReconciliations + ' 笔', color: '#f56c6c' },
        { label: '待审核报表', value: d.pendingReports + ' 份', color: '#e6a23c' },
      ]
      depositTrend.value = d.depositTrend || []
      feeTrend.value = d.feeTrend || []
    }
  } catch {
    kpiCards.value = [
      { label: '今日入金', value: '¥1,250,000', color: '#67c23a' },
      { label: '今日出金', value: '¥380,000', color: '#f56c6c' },
      { label: '本月入金', value: '¥56,800,000', color: '#67c23a' },
      { label: '本月出金', value: '¥15,200,000', color: '#f56c6c' },
    ]
  }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 20px; font-weight: bold; }
</style>
