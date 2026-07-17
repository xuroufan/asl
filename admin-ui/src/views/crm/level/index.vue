<template>
  <div class="crm-level">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">客户等级管理</h2>
      <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
    </div>

    <el-row :gutter="16" class="mb-4">
      <el-col :span="6" v-for="lv in levels" :key="lv.levelId">
        <el-card shadow="hover" class="level-card" :style="{ borderTop: '3px solid ' + lv.color }">
          <div class="flex items-center justify-between mb-2">
            <span class="text-lg font-bold" :style="{ color: lv.color }">{{ lv.levelName }}</span>
            <el-tag :color="lv.color" effect="dark" size="small">第{{ lv.levelOrder }}级</el-tag>
          </div>
          <div class="text-xs text-gray-400 mb-2">{{ lv.description }}</div>
          <el-descriptions :column="1" size="mini" border>
            <el-descriptions-item label="最低交易量">{{ lv.minVolume }}手</el-descriptions-item>
            <el-descriptions-item label="最低资金">¥{{ lv.minBalance?.toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="最低交易次数">{{ lv.minTrades }}次</el-descriptions-item>
            <el-descriptions-item label="手续费折扣">{{ (lv.feeDiscount * 100) }}%</el-descriptions-item>
            <el-descriptions-item label="持仓限额">{{ lv.positionLimit }}手</el-descriptions-item>
            <el-descriptions-item label="专属客服">{{ lv.dedicatedSupport ? '是' : '否' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-4">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>升级规则</span></template>
          <el-table :data="rules" stripe size="small" style="width: 100%">
            <el-table-column prop="ruleName" label="规则名称" width="120" />
            <el-table-column label="条件" min-width="200">
              <template #default="{ row }">
                <div v-for="(threshold, levelName) in row.thresholds" :key="levelName" class="text-sm">
                  {{ levelName }}: {{ row.metric === 'balance' ? '¥' : '' }}{{ threshold?.toLocaleString() }}
                  {{ row.metric === 'balance' ? '' : '手/次' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="自动升级" width="80" align="center">
              <template #default="{ row }"><el-tag :type="row.autoUpgrade ? 'success' : 'info'" size="small">{{ row.autoUpgrade ? '是' : '否' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="checkCycle" label="检查周期" width="100" align="center" />
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center justify-between">
              <span>等级变更记录</span>
              <div>
                <el-input v-model="levelHistoryCustomerId" placeholder="输入客户ID" size="small" style="width: 150px; margin-right: 8px" />
                <el-button size="small" type="primary" @click="loadLevelHistory">查询</el-button>
              </div>
            </div>
          </template>
          <el-table :data="levelHistory" stripe size="small" style="width: 100%" max-height="300">
            <el-table-column label="变更" min-width="120">
              <template #default="{ row }">{{ row.fromLevel }} → <el-tag :type="levelTag(row.toLevel)" size="small">{{ row.toLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
            <el-table-column prop="operator" label="操作人" width="80" />
            <el-table-column prop="changeTime" label="时间" width="150" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLevelDefinitions, getLevelRules, getLevelHistory } from '@/api/crm'

const levels = ref<any[]>([])
const rules = ref<any[]>([])
const levelHistory = ref<any[]>([])
const levelHistoryCustomerId = ref('1001')

const levelTag = (level: string) => ({ 普通: 'info', 白银: 'default', 黄金: 'warning', 钻石: 'primary' }[level] || 'info')

const loadLevelHistory = async () => {
  try {
    const res = await getLevelHistory({ customerId: parseInt(levelHistoryCustomerId.value) || 1001, page: 1, size: 10 })
    levelHistory.value = res.data || []
  } catch { levelHistory.value = [] }
}

const loadData = async () => {
  try {
    const [levelsRes, rulesRes] = await Promise.all([getLevelDefinitions(), getLevelRules()])
    levels.value = levelsRes.data || []
    rules.value = rulesRes.data || []
  } catch { /* ignore */ }
  await loadLevelHistory()
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.level-card { cursor: default; }
</style>
