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
              <el-tag size="small" type="info" effect="plain">实时</el-tag>
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
              <el-tag size="small" type="info" effect="plain">今日</el-tag>
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
              <el-tag size="small" :type="alerts.length > 0 ? 'danger' : 'success'" effect="plain">
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
              <el-tag :type="alert.level === 'high' ? 'danger' : 'warning'" size="small" effect="light">
                {{ alert.level === 'high' ? '高风险' : '中风险' }}
              </el-tag>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon :size="40" color="#909399"><Monitor /></el-icon>
            <div class="empty-text">系统运行正常，暂无风控预警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统状态</span>
              <el-tag size="small" type="success" effect="light">运行中</el-tag>
            </div>
          </template>
          <div class="system-status">
            <div v-for="(svc, idx) in services" :key="idx" class="service-item">
              <div class="service-info">
                <div class="service-dot" :class="svc.status" />
                <span class="service-name">{{ svc.name }}</span>
              </div>
              <div class="service-status">
                <el-tag :type="svc.status === 'up' ? 'success' : 'danger'" size="small" effect="plain">
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
import { User, Document, Monitor, TrendCharts, WarningFilled } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, PieChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const showChart = ref(false)

// Stat cards
const stats = ref([
  { label: '用户总数', value: 0, icon: User, desc: '注册用户', gradient: 'linear-gradient(135deg, #409EFF 0%, #2d7de0 100%)' },
  { label: '今日操作', value: 0, icon: Document, desc: '订单笔数', gradient: 'linear-gradient(135deg, #67C23A 0%, #529b2e 100%)' },
  { label: '今日登录', value: 0, icon: Monitor, desc: '活跃用户', gradient: 'linear-gradient(135deg, #E6A23C 0%, #cf9236 100%)' },
  { label: '系统状态', value: '运行中', icon: TrendCharts, desc: '正常运行', gradient: 'linear-gradient(135deg, #409EFF 0%, #7c3aed 100%)' },
])

// Volume trend chart - area chart
const volumeOption = shallowRef({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: '#1e293b', textStyle: { color: '#e2e8f0' } },
  grid: { top: 20, right: 20, bottom: 30, left: 50 },
  xAxis: { type: 'category', data: ['06/11', '06/12', '06/13', '06/14', '06/15', '06/16', '今日'], axisLine: { lineStyle: { color: '#1e293b' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1e293b', type: 'dashed' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
  series: [{
    name: '交易量', type: 'line', smooth: true, showSymbol: false,
    lineStyle: { width: 2, color: '#409EFF' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] } },
    data: [2840, 3210, 2980, 4120, 3850, 4560, 2180],
  }, {
    name: '成交额(万)', type: 'line', smooth: true, showSymbol: false,
    lineStyle: { width: 2, color: '#67C23A' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(103,194,58,0.2)' }, { offset: 1, color: 'rgba(103,194,58,0.02)' }] } },
    data: [142, 160, 149, 206, 192, 228, 109],
  }],
})

// Order distribution pie chart
const pieOption = shallowRef({
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: '#1e293b', textStyle: { color: '#e2e8f0' } },
  legend: { bottom: 0, textStyle: { color: '#64748b', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie', radius: ['48%', '72%'], avoidLabelOverlap: true,
    label: { show: true, formatter: '{d}%', color: '#94a3b8', fontSize: 11 },
    emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
    data: [
      { value: 456, name: '恒生指数', itemStyle: { color: '#409EFF' } },
      { value: 321, name: 'E-mini S&P', itemStyle: { color: '#67C23A' } },
      { value: 234, name: '黄金期货', itemStyle: { color: '#E6A23C' } },
      { value: 189, name: '原油期货', itemStyle: { color: '#F56C6C' } },
      { value: 145, name: '其他', itemStyle: { color: '#909399' } },
    ],
  }],
})

// Risk alerts
const alerts = ref<{ title: string; time: string; level: string }[]>([])

// System services
const services = ref([
  { name: '行情服务', status: 'up', latency: '12ms' },
  { name: '订单服务', status: 'up', latency: '8ms' },
  { name: '撮合引擎', status: 'up', latency: '3ms' },
  { name: '账户服务', status: 'up', latency: '15ms' },
  { name: '风控引擎', status: 'up', latency: '5ms' },
  { name: '结算服务', status: 'up', latency: '20ms' },
])

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.data) {
      stats.value[0].value = res.data.userCount || 0
      stats.value[1].value = res.data.todayOperCount || 0
      stats.value[2].value = res.data.todayLoginCount || 0
    }
  } catch {}
  // Enable charts after mount
  setTimeout(() => { showChart.value = true }, 100)
})
</script>

<style scoped>
.dashboard { padding: 4px; }
.stat-card {
  position: relative; overflow: hidden; border-radius: 12px; padding: 20px;
  color: #fff; box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}
.stat-card-content { position: relative; z-index: 1; }
.stat-label { font-size: 13px; opacity: 0.9; margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.stat-desc { font-size: 11px; opacity: 0.6; margin-top: 4px; }
.stat-icon { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); opacity: 0.25; }
.chart-card { border-radius: 12px; border: 1px solid #e5e7eb; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 15px; font-weight: 600; color: #1e293b; }
.chart-wrapper { min-height: 280px; }
.chart-empty { display: flex; align-items: center; justify-content: center; height: 280px; color: #94a3b8; }
.alert-list { display: flex; flex-direction: column; gap: 8px; }
.alert-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
.alert-item:last-child { border-bottom: none; }
.alert-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.alert-dot.high { background: #F56C6C; box-shadow: 0 0 6px rgba(245,108,108,0.4); }
.alert-dot.medium { background: #E6A23C; box-shadow: 0 0 6px rgba(230,162,60,0.4); }
.alert-info { flex: 1; }
.alert-title { font-size: 13px; color: #334155; }
.alert-time { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 40px 0; }
.empty-text { font-size: 13px; color: #94a3b8; }
.system-status { display: flex; flex-direction: column; gap: 6px; }
.service-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
.service-item:last-child { border-bottom: none; }
.service-info { display: flex; align-items: center; gap: 8px; }
.service-dot { width: 8px; height: 8px; border-radius: 50%; }
.service-dot.up { background: #67C23A; box-shadow: 0 0 6px rgba(103,194,58,0.4); }
.service-dot.down { background: #F56C6C; }
.service-name { font-size: 13px; color: #334155; }
.service-status { display: flex; align-items: center; gap: 8px; }
.service-latency { font-size: 11px; color: #94a3b8; }
</style>
