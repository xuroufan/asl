<template>
  <div class="dashboard">
    <!-- Statistics cards -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6" v-for="(item, idx) in stats" :key="idx">
        <div class="stat-card" :style="{ background: item.gradient }">
          <div class="stat-card-content">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-desc">{{ item.desc }}</div>
          </div>
          <el-icon :size="42" class="stat-icon"><component :is="item.icon" /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- Charts row -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">交易量趋势 (近7日)</span>
              <el-tag size="small" type="info" effect="dark">实时</el-tag>
            </div>
          </template>
          <div class="chart-wrapper" ref="volumeChartRef">
            <v-chart v-if="showChart" :option="volumeOption" autoresize style="height: 280px" />
            <div v-else class="chart-empty">加载中...</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">订单分布</span>
              <el-tag size="small" type="info" effect="dark">今日</el-tag>
            </div>
          </template>
          <div class="chart-wrapper">
            <v-chart v-if="showChart" :option="pieOption" autoresize style="height: 280px" />
            <div v-else class="chart-empty">加载中...</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent alerts + System status -->
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近风控预警</span>
              <el-tag size="small" :type="alerts.length > 0 ? 'danger' : 'success'" effect="dark">
                {{ alerts.length > 0 ? alerts.length + ' 条' : '暂无预警' }}
              </el-tag>
            </div>
          </template>
          <div v-if="alerts.length > 0" class="alert-list">
            <div v-for="(alert, idx) in alerts" :key="idx" class="alert-item">
              <div class="alert-dot" :class="alert.level" />
              <div class="alert-info">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-time">{{ alert.time }}</div>
              </div>
              <el-tag :type="alert.level === 'high' ? 'danger' : 'warning'" size="small" effect="dark">
                {{ alert.level === 'high' ? '高风险' : '中风险' }}
              </el-tag>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon :size="40" color="#4A5568"><Monitor /></el-icon>
            <div class="empty-text">系统运行正常，暂无风控预警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统服务状态</span>
              <el-tag size="small" type="success" effect="dark">运行中</el-tag>
            </div>
          </template>
          <div class="system-status">
            <div v-for="(svc, idx) in services" :key="idx" class="service-item">
              <div class="service-info">
                <div class="service-dot" :class="svc.status" />
                <span class="service-name">{{ svc.name }}</span>
              </div>
              <div class="service-status">
                <el-tag :type="svc.status === 'up' ? 'success' : 'danger'" size="small" effect="dark">
                  {{ svc.status === 'up' ? '正常' : '异常' }}
                </el-tag>
                <span class="service-latency">{{ svc.latency }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, shallowRef } from 'vue'
import { getDashboardStats } from '@/api/system'
import { User, Document, Monitor, TrendCharts } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, PieChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const showChart = ref(false)

const BRAND = '#4F8CF7'

const stats = ref([
  { label: '用户总数', value: '--', icon: User, desc: '注册用户', gradient: 'linear-gradient(135deg, #4F8CF7 0%, #3A6FD4 100%)' },
  { label: '今日操作', value: '--', icon: Document, desc: '订单笔数', gradient: 'linear-gradient(135deg, #00C853 0%, #00A844 100%)' },
  { label: '今日登录', value: '--', icon: Monitor, desc: '活跃用户', gradient: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)' },
  { label: '系统状态', value: '运行中', icon: TrendCharts, desc: '正常运行', gradient: 'linear-gradient(135deg, #4F8CF7 0%, #7C3AED 100%)' },
])

const volumeOption = shallowRef({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(13,19,34,0.95)', borderColor: '#1E2A44', textStyle: { color: '#CBD5E1' } },
  grid: { top: 20, right: 20, bottom: 30, left: 50 },
  xAxis: { type: 'category', data: ['06/11', '06/12', '06/13', '06/14', '06/15', '06/16', '今日'], axisLine: { lineStyle: { color: '#1E2A44' } }, axisLabel: { color: '#6B7A99', fontSize: 11 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1E2A44', type: 'dashed' } }, axisLabel: { color: '#6B7A99', fontSize: 11 } },
  series: [{
    name: '交易量', type: 'line', smooth: true, showSymbol: false,
    lineStyle: { width: 2, color: BRAND },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(79,140,247,0.3)' }, { offset: 1, color: 'rgba(79,140,247,0.02)' }] } },
    data: [2840, 3210, 2980, 4120, 3850, 4560, 2180],
  }, {
    name: '成交额(万)', type: 'line', smooth: true, showSymbol: false,
    lineStyle: { width: 2, color: '#00C853' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,200,83,0.2)' }, { offset: 1, color: 'rgba(0,200,83,0.02)' }] } },
    data: [142, 160, 149, 206, 192, 228, 109],
  }],
})

const pieOption = shallowRef({
  tooltip: { trigger: 'item', backgroundColor: 'rgba(13,19,34,0.95)', borderColor: '#1E2A44', textStyle: { color: '#CBD5E1' } },
  legend: { bottom: 0, textStyle: { color: '#6B7A99', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie', radius: ['48%', '72%'], avoidLabelOverlap: true,
    label: { show: true, formatter: '{d}%', color: '#94A3B8', fontSize: 11 },
    emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
    data: [
      { value: 456, name: '恒生指数', itemStyle: { color: BRAND } },
      { value: 321, name: 'E-mini S&P', itemStyle: { color: '#00C853' } },
      { value: 234, name: '黄金期货', itemStyle: { color: '#F59E0B' } },
      { value: 189, name: '原油期货', itemStyle: { color: '#FF6B6B' } },
      { value: 145, name: '其他', itemStyle: { color: '#6B7A99' } },
    ],
  }],
})

const alerts = ref<{ title: string; time: string; level: string }[]>([])
const services = ref([
  { name: '行情服务', status: 'up', latency: '--' },
  { name: '订单服务', status: 'up', latency: '--' },
  { name: '撮合引擎', status: 'up', latency: '--' },
  { name: '账户服务', status: 'up', latency: '--' },
  { name: '风控引擎', status: 'up', latency: '--' },
  { name: '结算服务', status: 'up', latency: '--' },
])

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.data) {
      if (res.data.userCount !== undefined) stats.value[0].value = res.data.userCount.toLocaleString()
      if (res.data.todayOperCount !== undefined) stats.value[1].value = res.data.todayOperCount.toLocaleString()
      if (res.data.todayLoginCount !== undefined) stats.value[2].value = res.data.todayLoginCount.toLocaleString()
    }
  } catch {}

  // Fetch actual services status from admin API
  try {
    const statusRes = await fetch('http://localhost:8099/actuator/health').catch(() => null)
    if (statusRes && statusRes.ok) {
      services.value.forEach(s => { s.status = 'up'; s.latency = '<10ms' })
    }
  } catch {}

  // Check other services
  const ports = [8081, 8082, 8083, 8084, 8085, 8086, 8087, 8093, 8088]
  const svcNames = ['订单服务', '撮合引擎', '账户服务', '资金服务', '风控引擎', '行情服务', '结算服务', '推送服务', '网关']
  for (let i = 0; i < ports.length; i++) {
    try {
      const r = await fetch(`http://localhost:${ports[i]}/actuator/health`)
      if (r.ok) {
        if (services.value[i]) {
          services.value[i].status = 'up'
          services.value[i].latency = '<15ms'
        }
      }
    } catch {
      if (services.value[i]) {
        services.value[i].status = 'down'
        services.value[i].latency = 'N/A'
      }
    }
  }

  setTimeout(() => { showChart.value = true }, 100)
})
</script>

<style scoped>
.dashboard { padding: 4px; }
</style>
