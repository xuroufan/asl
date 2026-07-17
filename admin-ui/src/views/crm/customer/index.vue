<template>
  <div class="crm-customer">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">客户信息管理</h2>
      <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="mb-4">
      <el-form :model="filters" inline size="small">
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="客户ID/姓名/手机号" style="width: 220px" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="filters.level" style="width: 100px" clearable placeholder="全部">
            <el-option label="全部" value="" /><el-option label="普通" value="普通" />
            <el-option label="白银" value="白银" /><el-option label="黄金" value="黄金" /><el-option label="钻石" value="钻石" />
          </el-select>
        </el-form-item>
        <el-form-item label="KYC">
          <el-select v-model="filters.kycStatus" style="width: 110px" clearable placeholder="全部">
            <el-option label="全部" value="" /><el-option label="已通过" value="已通过" />
            <el-option label="审核中" value="审核中" /><el-option label="未提交" value="未提交" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" style="width: 100px" clearable placeholder="全部">
            <el-option label="全部" value="" /><el-option label="正常" value="正常" /><el-option label="冻结" value="冻结" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData" :icon="'Search'">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 客户列表 -->
    <el-card shadow="hover">
      <el-table :data="customers" stripe size="small" style="width: 100%">
        <el-table-column prop="customerId" label="客户ID" width="90"><template #default="{ row }"><code class="text-xs">{{ row.customerId }}</code></template></el-table-column>
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="level" label="等级" width="80" align="center">
          <template #default="{ row }"><el-tag :type="levelTag(row.level)" size="small">{{ row.level }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="kycStatus" label="KYC" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.kycStatus === '已通过' ? 'success' : 'warning'" size="small">{{ row.kycStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="balance" label="总资产" width="130" align="right">
          <template #default="{ row }">¥{{ row.balance?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="totalTradeVolume" label="交易量(手)" width="100" align="right" />
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t" size="small" class="mr-1">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row.customerId)">详情</el-button>
            <el-button type="success" link size="small" @click="showPortfolio(row.customerId)">资产</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 客户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="客户详情" width="800px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="客户ID">{{ detail.customerId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.realName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ detail.idCardNo }}</el-descriptions-item>
          <el-descriptions-item label="等级"><el-tag :type="levelTag(detail.level)" size="small">{{ detail.level }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="KYC状态"><el-tag :type="detail.kycStatus === '已通过' ? 'success' : 'warning'" size="small">{{ detail.kycStatus }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="账户状态"><el-tag :type="detail.status === '正常' ? 'success' : 'danger'" size="small">{{ detail.status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detail.registerTime }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ detail.lastLoginTime }}</el-descriptions-item>
          <el-descriptions-item label="总入金">¥{{ detail.totalDeposit?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="总出金">¥{{ detail.totalWithdraw?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="手续费总计">¥{{ detail.totalFee?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="累计盈亏">¥{{ detail.totalProfit?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="风险度">{{ detail.riskRatio }}%</el-descriptions-item>
          <el-descriptions-item label="负责客服">{{ detail.assignedStaff }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="t in detail.tags" :key="t" size="small" class="mr-1">{{ t }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 客户资产弹窗 -->
    <el-dialog v-model="portfolioVisible" :title="'资产详情 - 客户ID: ' + portfolioCustomerId" width="900px">
      <template v-if="portfolio">
        <el-tabs>
          <el-tab-pane label="当前持仓">
            <el-table :data="portfolio.positions" stripe size="small" style="width: 100%">
              <el-table-column prop="symbol" label="合约" width="100" />
              <el-table-column prop="direction" label="方向" width="60" align="center">
                <template #default="{ row }"><el-tag :type="row.direction === '多' ? 'danger' : 'success'" size="small">{{ row.direction }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="volume" label="手数" width="60" align="center" />
              <el-table-column prop="avgPrice" label="开仓均价" width="110" align="right" />
              <el-table-column prop="currentPrice" label="最新价" width="110" align="right" />
              <el-table-column prop="floatPnl" label="浮动盈亏" width="110" align="right">
                <template #default="{ row }"><span :style="{ color: row.floatPnl >= 0 ? '#f56c6c' : '#67c23a' }">¥{{ row.floatPnl?.toLocaleString() }}</span></template>
              </el-table-column>
              <el-table-column prop="margin" label="保证金" width="110" align="right" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="近期交易">
            <el-table :data="portfolio.recentTrades" stripe size="small" style="width: 100%">
              <el-table-column prop="tradeId" label="编号" width="140"><template #default="{ row }"><code class="text-xs">{{ row.tradeId }}</code></template></el-table-column>
              <el-table-column prop="symbol" label="合约" width="100" />
              <el-table-column prop="direction" label="方向" width="60" align="center">
                <template #default="{ row }"><el-tag :type="row.direction === '买' ? 'danger' : 'success'" size="small">{{ row.direction }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="price" label="价格" width="110" align="right" />
              <el-table-column prop="volume" label="手数" width="60" align="center" />
              <el-table-column prop="time" label="时间" width="160" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="资金流水">
            <el-table :data="portfolio.fundFlows" stripe size="small" style="width: 100%">
              <el-table-column prop="flowId" label="编号" width="150"><template #default="{ row }"><code class="text-xs">{{ row.flowId }}</code></template></el-table-column>
              <el-table-column prop="type" label="类型" width="80">
                <template #default="{ row }"><el-tag :type="row.type === '入金' ? 'success' : (row.type === '出金' ? 'warning' : 'info')" size="small">{{ row.type }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="120" align="right">
                <template #default="{ row }"><span :style="{ color: row.amount >= 0 ? '#67c23a' : '#f56c6c' }">¥{{ row.amount?.toLocaleString() }}</span></template>
              </el-table-column>
              <el-table-column prop="balance" label="余额" width="130" align="right">
                <template #default="{ row }">¥{{ row.balance?.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="time" label="时间" width="160" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCustomerList, getCustomerDetail, getCustomerPortfolio } from '@/api/crm'

const filters = ref({ keyword: '', level: '', kycStatus: '', status: '' })
const customers = ref<any[]>([])
const detail = ref<any>(null)
const detailVisible = ref(false)
const portfolio = ref<any>(null)
const portfolioCustomerId = ref(0)
const portfolioVisible = ref(false)

const levelTag = (level: string) => ({ 普通: 'info', 白银: 'default', 黄金: 'warning', 钻石: 'primary' }[level] || 'info')

const resetFilters = () => {
  filters.value = { keyword: '', level: '', kycStatus: '', status: '' }
  loadData()
}

const loadData = async () => {
  try {
    const res = await getCustomerList({ ...filters.value, page: 1, size: 50 })
    customers.value = res.data?.records || res.data || []
  } catch { customers.value = [] }
}

const showDetail = async (customerId: number) => {
  detailVisible.value = true
  try {
    const res = await getCustomerDetail(customerId)
    detail.value = res.data
  } catch { detail.value = null }
}

const showPortfolio = async (customerId: number) => {
  portfolioCustomerId.value = customerId
  portfolioVisible.value = true
  try {
    const res = await getCustomerPortfolio(customerId)
    portfolio.value = res.data
  } catch { portfolio.value = null }
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.mr-1 { margin-right: 4px; }
</style>
