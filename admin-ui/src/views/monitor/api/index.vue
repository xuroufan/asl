<template>
  <div>
    <h2 style="margin-bottom:16px">API 监控</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="16"><el-card shadow="never"><div ref="qpsChart" style="height:260px"></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div ref="errorChart" style="height:260px"></div></el-card></el-col>
    </el-row>
    <el-card shadow="never">
      <template #header><span>接口延迟 Top 10</span></template>
      <el-table :data="endpoints" size="small" stripe>
        <el-table-column prop="path" label="路径" min-width="200" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="p50" label="P50 (ms)" width="90" />
        <el-table-column prop="p95" label="P95 (ms)" width="90" />
        <el-table-column prop="p99" label="P99 (ms)" width="90" />
        <el-table-column prop="qps" label="QPS" width="80" />
        <el-table-column prop="errorRate" label="错误率" width="80">
          <template #default="{ row }"><span :style="{ color: row.errorRate > 1 ? '#f56c6c' : '#67c23a' }">{{ row.errorRate }}%</span></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import statistic from '@/components/Statistic.vue'

const qpsChart = ref<HTMLDivElement>(); const errorChart = ref<HTMLDivElement>()
let c1: echarts.ECharts | null, c2: echarts.ECharts | null, timer: number
const cards = ref([
  { label: '总请求数', value: '1,284,732', color: '#409eff' },
  { label: '平均响应时间', value: '23 ms', color: '#67c23a' },
  { label: 'QPS', value: '1,847', color: '#e6a23c' },
  { label: '错误率', value: '0.03%', color: '#f56c6c' },
])
const endpoints = ref(
  ['/api/v1/market/quote','/api/v1/market/kline','/api/v1/auth/login','/api/v1/order','/api/v1/account','/api/v1/risk/dashboard','/api/v1/market/depth','/api/v1/trade/positions','/api/v1/account/overview','/api/v1/market/trades']
    .map((p, i) => ({ path: p, method: ['GET','GET','POST','POST','GET','GET','GET','GET','GET','GET'][i], p50: Math.round(Math.random()*20+5), p95: Math.round(Math.random()*100+30), p99: Math.round(Math.random()*300+100), qps: Math.round(Math.random()*200+50), errorRate: +(Math.random()*0.5).toFixed(2) }))
)

onMounted(() => {
  c1 = echarts.init(qpsChart.value!); c2 = echarts.init(errorChart.value!)
  const up = () => {
    c1!.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: 45, right: 20, top: 25, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i*10}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      legend: { data: ['请求量','延迟'], textStyle: { color: '#ccc' } },
      series: [
        { name: '请求量', type: 'bar', data: Array.from({ length: 12 }, () => Math.round(Math.random()*500+1500)), itemStyle: { color: '#409eff' } },
        { name: '延迟', type: 'line', yAxisIndex: 1, smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*20+10)), lineStyle: { color: '#e6a23c' } },
      ],
      yAxis: [{ type: 'value', axisLabel: { color: '#999' } }, { type: 'value', axisLabel: { color: '#999' }, splitLine: { show: false } }],
    })
    c2!.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['50%','75%'], data: [{ name: '2xx', value: 98 }, { name: '4xx', value: 1.5 }, { name: '5xx', value: 0.5 }].map(d => ({ ...d, itemStyle: { color: { '2xx':'#67c23a','4xx':'#e6a23c','5xx':'#f56c6c'}[d.name] } })), label: { color: '#ccc', formatter: '{b}: {d}%' } }],
    })
  }; up(); timer = window.setInterval(up, 6000)
})
onUnmounted(() => { clearInterval(timer); c1?.dispose(); c2?.dispose() })
</script>
