<template>
  <div class="dashboard">
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div>
              <div style="font-size: 12px; color: #999">{{ item.label }}</div>
              <div style="font-size: 28px; font-weight: bold; margin-top: 10px">{{ item.value }}</div>
            </div>
            <el-icon :size="40" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>交易量趋势</span></template>
          <div style="height: 300px; display: flex; align-items: center; justify-content: center; color: #999">图表区域（待集成 ECharts）</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>最近风控预警</span></template>
          <div style="color: #999; text-align: center; padding: 80px 0">暂无预警</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/api/system'
import { User, Document, Monitor, TrendCharts } from '@element-plus/icons-vue'

const stats = ref([
  { label: '用户总数', value: 0, icon: User, color: '#409eff' },
  { label: '今日操作', value: 0, icon: Document, color: '#67c23a' },
  { label: '今日登录', value: 0, icon: Monitor, color: '#e6a23c' },
  { label: '系统状态', value: '运行中', icon: TrendCharts, color: '#409eff' },
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
})
</script>