<template>
  <div>
    <h2 style="margin-bottom:16px">Redis 缓存监控</h2>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="never"><statistic :value="c.value" :label="c.label" :color="c.color" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12">
      <el-col :span="14"><el-card shadow="never"><div ref="hitChart" style="height:280px"></div></el-card></el-col>
      <el-col :span="10"><el-card shadow="never">
        <template #header><span>缓存 Key 分布</span></template>
        <div ref="keyChart" style="height:240px"></div>
      </el-card></el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import statistic from '@/components/Statistic.vue'

const hitChart = ref<HTMLDivElement>(); const keyChart = ref<HTMLDivElement>()
let ch1: echarts.ECharts | null, ch2: echarts.ECharts | null, timer: number
const cards = ref([
  { label: '内存使用', value: '182 MB / 512 MB', color: '#409eff' },
  { label: '缓存命中率', value: '97.3%', color: '#67c23a' },
  { label: 'Key 总数', value: '12,847', color: '#e6a23c' },
  { label: 'QPS', value: '2,341', color: '#909399' },
])
onMounted(() => {
  ch1 = echarts.init(hitChart.value!); ch2 = echarts.init(keyChart.value!)
  const up = () => {
    const hits = Math.round(Math.random() * 200 + 800), miss = Math.round(Math.random() * 30 + 10)
    ch1!.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: 50, right: 20, top: 20, bottom: 20 },
      xAxis: { type: 'category', data: Array.from({ length: 12 }, (_, i) => `${i*5}s`), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      legend: { data: ['命中', '未命中'], textStyle: { color: '#ccc' } },
      series: [
        { name: '命中', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*100+800)), lineStyle: { color: '#67c23a' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } },
        { name: '未命中', type: 'line', smooth: true, data: Array.from({ length: 12 }, () => Math.round(Math.random()*20+10)), lineStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245,108,108,0.1)' } },
      ],
    })
    ch2!.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { name: 'Session', value: 5240 }, { name: 'Token', value: 3820 },
          { name: 'Quote Cache', value: 2100 }, { name: 'Order', value: 1050 },
          { name: 'Other', value: 637 },
        ].map(d => ({ ...d, itemStyle: { color: ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399'][['Session','Token','Quote Cache','Order','Other'].indexOf(d.name)] } })),
        label: { color: '#ccc', formatter: '{b}: {d}%' },
      }],
    })
  }; up(); timer = window.setInterval(up, 5000)
})
onUnmounted(() => { clearInterval(timer); ch1?.dispose(); ch2?.dispose() })
</script>
