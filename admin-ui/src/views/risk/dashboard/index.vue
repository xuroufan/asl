<template>
  <div class="risk-dashboard">
    <h2 class="text-lg font-medium mb-4">风控监控大屏</h2>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6" v-for="kpi in kpiList" :key="kpi.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>风险度分布</span></template>
          <v-chart :option="riskDistributionOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>高风险用户 TOP 20</span></template>
          <el-table :data="highRiskUsers" size="small" max-height="280" stripe>
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="riskRatio" label="风险度(%)" width="100">
              <template #default="{ row }">
                <el-tag :type="row.riskRatio > 100 ? 'danger' : row.riskRatio > 80 ? 'warning' : 'info'" size="small">
                  {{ row.riskRatio }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="symbol" label="品种" width="80" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewUserRisk(row.userId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警滚动 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>最新风控预警</span></template>
          <el-table :data="recentAlerts" size="small" max-height="250" stripe>
            <el-table-column prop="alertType" label="预警类型" width="120">
              <template #default="{ row }">
                <el-tag :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">
                  {{ row.alertType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>今日强平记录</span></template>
          <el-table :data="recentLiquidations" size="small" max-height="250" stripe>
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="symbol" label="合约" width="80" />
            <el-table-column prop="direction" label="方向" width="60" />
            <el-table-column prop="volume" label="手数" width="60" />
            <el-table-column prop="price" label="价格" width="100" />
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getRiskDashboard } from '@/api/risk'

use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const kpiList = ref([
  { label: '风控配置数', value: '-', color: '#409eff' },
  { label: '预警总数', value: '-', color: '#e6a23c' },
  { label: '平均风险度', value: '-', color: '#67c23a' },
  { label: '今日强平', value: '-', color: '#f56c6c' },
])

const highRiskUsers = ref<any[]>([])
const recentAlerts = ref<any[]>([])
const recentLiquidations = ref<any[]>([])
const riskDistribution = ref<any[]>([])

const riskDistributionOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['40%', '65%'],
    center: ['50%', '45%'],
    avoidLabelOverlap: true,
    label: { show: true, formatter: '{b}\n{d}%' },
    data: riskDistribution.value,
    emphasis: {
      label: { show: true, fontSize: 16, fontWeight: 'bold' },
    },
  }],
}))

const loadData = async () => {
  try {
    const res = await getRiskDashboard()
    if (res.data) {
      const d = res.data
      kpiList.value = [
        { label: '风控配置数', value: String(d.totalConfigs ?? '-'), color: '#409eff' },
        { label: '预警总数', value: String(d.totalAlerts ?? '-'), color: '#e6a23c' },
        { label: '平均风险度', value: d.averageRiskRatio != null ? d.averageRiskRatio + '%' : '-', color: '#67c23a' },
        { label: '今日强平', value: String(d.todayLiquidations ?? '-'), color: '#f56c6c' },
      ]
      highRiskUsers.value = d.highRiskUsers || []
      recentAlerts.value = d.recentAlerts || []
      recentLiquidations.value = d.recentLiquidations || []
      riskDistribution.value = d.riskDistribution || []
    }
  } catch {
    // 加载模拟数据
    kpiList.value = [
      { label: '风控配置数', value: '8', color: '#409eff' },
      { label: '预警总数', value: '23', color: '#e6a23c' },
      { label: '平均风险度', value: '62.5%', color: '#67c23a' },
      { label: '今日强平', value: '2', color: '#f56c6c' },
    ]
    highRiskUsers.value = [
      { userId: 1001, username: 'test_user_01', riskRatio: 95.3, symbol: 'HSI' },
      { userId: 1002, username: 'test_user_02', riskRatio: 88.7, symbol: 'ES' },
    ]
    riskDistribution.value = [
      { name: '< 50%', value: 120 },
      { name: '50-80%', value: 65 },
      { name: '80-100%', value: 28 },
      { name: '> 100%', value: 12 },
    ]
  }
}

const viewUserRisk = (userId: number) => {
  // 预留：查看用户详情
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.kpi-card { text-align: center; }
.kpi-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.kpi-value { font-size: 28px; font-weight: bold; }
</style>
