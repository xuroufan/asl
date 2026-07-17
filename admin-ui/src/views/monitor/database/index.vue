<template>
  <div>
    <h2 style="margin-bottom:16px">数据库监控</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="12"><el-card shadow="never"><div ref="connChart" style="height:260px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="never"><div ref="slowChart" style="height:260px"></div></el-card></el-col>
    </el-row>
    <el-card shadow="never">
      <template #header><span>慢查询 Top 5</span></template>
      <el-table :data="slowQueries" size="small" stripe>
        <el-table-column prop="query" label="SQL" min-width="350"><template #default="{row}"><code style="font-size:11px;color:#e6a23c">{{row.query}}</code></template></el-table-column>
        <el-table-column prop="time" label="耗时(ms)" width="90" />
        <el-table-column prop="rows" label="扫描行" width="80" />
        <el-table-column prop="source" label="来源" width="120" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import statistic from '@/components/Statistic.vue'

const connChart = ref<HTMLDivElement>(); const slowChart = ref<HTMLDivElement>()
let c1: echarts.ECharts | null, c2: echarts.ECharts | null, timer: number
const cards = ref([
  { label: '活跃连接', value: '23 / 100', color: '#409eff' },
  { label: 'QPS', value: '3,421', color: '#67c23a' },
  { label: '慢查询', value: '2', color: '#f56c6c' },
  { label: '磁盘使用', value: '4.2 GB / 10 GB', color: '#e6a23c' },
])
const slowQueries = ref([
  { query: 'SELECT * FROM order_book WHERE status = ? ORDER BY created_at DESC LIMIT 1000', time: 2340, rows: 84512, source: 'futures-matching' },
  { query: 'INSERT INTO order_history (user_id, ...) SELECT * FROM orders WHERE ...', time: 1820, rows: 32000, source: 'futures-settlement' },
  { query: 'UPDATE positions SET unrealized_pnl = ? WHERE symbol IN (...)', time: 1560, rows: 12800, source: 'futures-risk' },
  { query: 'SELECT * FROM trade_logs WHERE created_at > ? ORDER BY id', time: 890, rows: 56000, source: 'futures-admin' },
  { query: 'SELECT COUNT(*) FROM users WHERE last_login < ?', time: 670, rows: 35000, source: 'futures-account' },
])

onMounted(() => {
  c1 = echarts.init(connChart.value!); c2 = echarts.init(slowChart.value!)
  const up = () => {
    c1!.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: 45, right: 20, top: 25, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i*5}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', max: 100, axisLabel: { color: '#999' } },
      legend: { data: ['活跃连接','空闲连接'], textStyle: { color: '#ccc' } },
      series: [
        { name: '活跃连接', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*20+10)), lineStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.1)' } },
        { name: '空闲连接', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*15+40)), lineStyle: { color: '#67c23a' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } },
      ],
    })
    c2!.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: 45, right: 20, top: 25, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i*10}m`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      series: [{ name: '慢查询', type: 'bar', data: Array.from({ length: 12 }, () => Math.round(Math.random()*3)), itemStyle: { color: '#f56c6c' } }],
    })
  }; up(); timer = window.setInterval(up, 5000)
})
onUnmounted(() => { clearInterval(timer); c1?.dispose(); c2?.dispose() })
</script>
