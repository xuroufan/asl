<template>
  <div>
    <h2 style="margin-bottom:16px">WebSocket 连接监控</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12">
      <el-col :span="16"><el-card shadow="never"><div ref="msgChart" style="height:280px"></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never">
        <template #header><span>连接分布</span></template>
        <div ref="connChart" style="height:240px"></div>
      </el-card></el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import statistic from '@/components/Statistic.vue'

const msgChart = ref<HTMLDivElement>(); const connChart = ref<HTMLDivElement>()
let c1: echarts.ECharts | null, c2: echarts.ECharts | null, timer: number
const cards = ref([
  { label: '活跃连接', value: '847', color: '#409eff' },
  { label: '消息吞吐', value: '12.4K/s', color: '#67c23a' },
  { label: '今日连接总数', value: '32,847', color: '#e6a23c' },
  { label: '平均延迟', value: '8 ms', color: '#909399' },
])
onMounted(() => {
  c1 = echarts.init(msgChart.value!); c2 = echarts.init(connChart.value!)
  const up = () => {
    c1!.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: 45, right: 20, top: 25, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i*5}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      legend: { data: ['消息/秒','连接数'], textStyle: { color: '#ccc' } },
      series: [
        { name: '消息/秒', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*5000+8000)), lineStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.1)' } },
        { name: '连接数', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*300+600)), lineStyle: { color: '#67c23a' } },
      ],
    })
    c2!.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%','70%'],
        data: [
          { name: '行情推送', value: 423, itemStyle: { color: '#409eff' } },
          { name: '订单推送', value: 186, itemStyle: { color: '#67c23a' } },
          { name: '账户推送', value: 98, itemStyle: { color: '#e6a23c' } },
          { name: '风控告警', value: 140, itemStyle: { color: '#f56c6c' } },
        ],
        label: { color: '#ccc', formatter: '{b}\n{d}%' },
      }],
    })
  }; up(); timer = window.setInterval(up, 5000)
})
onUnmounted(() => { clearInterval(timer); c1?.dispose(); c2?.dispose() })
</script>
