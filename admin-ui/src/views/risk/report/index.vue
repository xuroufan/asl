<template>
  <div>
    <h2 class="text-lg font-medium mb-4">风控报表</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="日报" name="daily">
        <div class="mb-4">
          <el-date-picker v-model="reportDate" type="date" placeholder="选择日期" @change="loadDaily" />
          <el-button type="primary" class="ml-2" @click="loadDaily">生成日报</el-button>
          <el-button class="ml-2" @click="handleExport('daily')">导出 Excel</el-button>
        </div>

        <el-row :gutter="16" class="mb-4">
          <el-col :span="6" v-for="item in dailyKpi" :key="item.label">
            <el-card shadow="hover" class="kpi-card">
              <div class="kpi-label">{{ item.label }}</div>
              <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover">
          <template #header><span>风险度分布</span></template>
          <v-chart :option="dailyDistOption" style="height: 300px" autoresize />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="周报" name="weekly">
        <div class="mb-4">
          <el-button type="primary" @click="loadWeekly">生成周报</el-button>
          <el-button class="ml-2" @click="handleExport('weekly')">导出 Excel</el-button>
        </div>

        <el-row :gutter="16" class="mb-4">
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>本周总强平</span></template>
              <div class="kpi-value" style="color: #f56c6c;">{{ weeklyData.totalLiquidations }} 笔</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>本周总预警</span></template>
              <div class="kpi-value" style="color: #e6a23c;">{{ weeklyData.totalAlerts }} 条</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>风险度趋势</span></template>
              <div class="kpi-value" style="color: #409eff;">{{ weeklyData.avgRiskRatioTrend?.[weeklyData.avgRiskRatioTrend.length - 1] }}%</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>风险度趋势</span></template>
              <v-chart :option="riskTrendOption" style="height: 280px" autoresize />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>按品种强平分布</span></template>
              <v-chart :option="liquidationBySymbolOption" style="height: 280px" autoresize />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'

use([BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, XAxisComponent, YAxisComponent, CanvasRenderer])

const activeTab = ref('daily')
const reportDate = ref(new Date())

const dailyKpi = ref<any[]>([])
const weeklyData = ref<any>({})

// 日报 - 风险度分布
const dailyDistOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['< 50%', '50-80%', '80-100%', '> 100%'] },
  yAxis: { type: 'value', name: '账户数' },
  series: [{
    type: 'bar',
    data: dailyKpi.value.slice(4) || [],
    itemStyle: {
      color: (params: any) => {
        const colors = ['#67c23a', '#409eff', '#e6a23c', '#f56c6c']
        return colors[params.dataIndex] || '#409eff'
      }
    },
  }],
}))

// 周报 - 风险度趋势
const riskTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
  yAxis: { type: 'value', name: '风险度(%)' },
  series: [
    { type: 'line', data: weeklyData.value.avgRiskRatioTrend || [], smooth: true, name: '平均风险度' },
    { type: 'line', data: weeklyData.value.highRiskTrend || [], smooth: true, name: '高风险用户数', yAxisIndex: 0 },
  ],
}))

const liquidationBySymbolOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['30%', '55%'],
    data: (weeklyData.value.liquidationBySymbol || []).map((d: any) => ({ name: d.symbol, value: d.count })),
    emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' } },
  }],
}))

const loadDaily = async () => {
  try {
    const date = reportDate.value.toISOString().slice(0, 10)
    const { getDailyReport } = await import('@/api/risk')
    const res = await getDailyReport(date)
    if (res.data) {
      const d = res.data
      dailyKpi.value = [
        { label: '总账户数', value: d.totalAccounts?.toLocaleString() ?? '-', color: '#409eff' },
        { label: '活跃账户', value: d.activeAccounts?.toLocaleString() ?? '-', color: '#67c23a' },
        { label: '总权益', value: d.totalEquity ? '¥' + Number(d.totalEquity).toLocaleString() : '-', color: '#409eff' },
        { label: '占用保证金', value: d.totalMargin ? '¥' + Number(d.totalMargin).toLocaleString() : '-', color: '#e6a23c' },
        { label: '高风险账户', value: String(d.highRiskAccounts ?? '-'), color: '#f56c6c' },
        { label: '今日强平', value: String(d.todayLiquidations ?? '-'), color: '#f56c6c' },
      ]
    }
  } catch {
    // 模拟
    dailyKpi.value = [
      { label: '总账户数', value: '1,580', color: '#409eff' },
      { label: '活跃账户', value: '423', color: '#67c23a' },
      { label: '总权益', value: '¥125,680,000', color: '#409eff' },
      { label: '占用保证金', value: '¥42,350,000', color: '#e6a23c' },
      { value: 210, name: '< 50%' },
      { value: 140, name: '50-80%' },
      { value: 55, name: '80-100%' },
      { value: 18, name: '> 100%' },
    ]
  }
}

const loadWeekly = async () => {
  try {
    const { getWeeklyReport } = await import('@/api/risk')
    const res = await getWeeklyReport()
    if (res.data) weeklyData.value = res.data
  } catch {
    weeklyData.value = {
      totalLiquidations: 8,
      totalAlerts: 35,
      avgRiskRatioTrend: [58.2, 60.1, 59.5, 61.3, 63.0, 62.8, 62.5],
      highRiskTrend: [8, 9, 10, 11, 10, 12, 12],
      liquidationBySymbol: [
        { symbol: 'HSI', count: 3 },
        { symbol: 'ES', count: 2 },
        { symbol: 'CL', count: 2 },
        { symbol: 'GC', count: 1 },
      ],
    }
  }
}

const handleExport = async (type: string) => {
  try {
    const { exportReport } = await import('@/api/risk')
    await exportReport(type)
    ElMessage.success('报表导出任务已提交')
  } catch {
    ElMessage.warning('导出功能需后端支持，当前为模拟')
  }
}

onMounted(loadDaily)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.ml-2 { margin-left: 8px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 22px; font-weight: bold; }
</style>
