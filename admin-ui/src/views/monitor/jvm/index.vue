<template>
  <div>
    <h2 style="margin-bottom:16px">JVM 监控</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in summaryCards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12">
      <el-col :span="12"><el-card shadow="never"><div ref="heapChart" style="height:280px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="never"><div ref="gcChart" style="height:280px"></div></el-card></el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import statistic from '@/components/Statistic.vue'

const heapChart = ref<HTMLDivElement>()
const gcChart = ref<HTMLDivElement>()
let chart1: echarts.ECharts | null = null
let chart2: echarts.ECharts | null = null
const r = () => Math.round(Math.random() * 40 + 60)
let timer: number

const summaryCards = ref([
  { label: '堆内存使用', value: '486 MB / 1 GB', color: '#409eff' },
  { label: 'GC 次数', value: '127 次', color: '#67c23a' },
  { label: 'GC 耗时', value: '2.3 s', color: '#e6a23c' },
  { label: '活跃线程', value: '42', color: '#909399' },
])

onMounted(() => {
  chart1 = echarts.init(heapChart.value!)
  chart2 = echarts.init(gcChart.value!)
  const update = () => {
    chart1!.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['Heap Used', 'Non-Heap'], textStyle: { color: '#ccc' } },
      grid: { left: 40, right: 20, top: 30, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i * 5}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', max: 1024, axisLabel: { color: '#999', formatter: '{value} MB' } },
      series: [
        { name: 'Heap Used', type: 'line', smooth: true, data: Array.from({ length: 12 }, r), lineStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.1)' } },
        { name: 'Non-Heap', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random() * 30 + 20)), lineStyle: { color: '#67c23a' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } },
      ],
    })
    chart2!.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['Young GC', 'Full GC'], textStyle: { color: '#ccc' } },
      grid: { left: 40, right: 20, top: 30, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i * 30}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      series: [
        { name: 'Young GC', type: 'bar', data: Array.from({ length: 12 }, () => Math.round(Math.random() * 10)), itemStyle: { color: '#409eff' } },
        { name: 'Full GC', type: 'bar', data: Array.from({ length: 12 }, () => Math.round(Math.random() * 3)), itemStyle: { color: '#e6a23c' } },
      ],
    })
  }
  update()
  timer = window.setInterval(update, 5000)
})
onUnmounted(() => { clearInterval(timer); chart1?.dispose(); chart2?.dispose() })
</script>
