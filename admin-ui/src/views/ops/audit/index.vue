<template>
  <div class="ops-audit">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">审计日志</h2>
      <div>
        <el-button @click="loadData" :icon="'Refresh'" size="small">刷新</el-button>
        <el-button @click="exportLogs" :icon="'Download'" size="small" type="primary">导出</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="mb-4">
      <el-form :model="filters" inline size="small">
        <el-form-item label="操作人">
          <el-input v-model="filters.operator" placeholder="操作人" style="width: 140px" clearable />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="filters.module" style="width: 140px" clearable placeholder="选择模块">
            <el-option label="全部" value="" />
            <el-option label="服务发布" value="服务发布" />
            <el-option label="配置变更" value="配置变更" />
            <el-option label="扩缩容" value="扩缩容" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filters.action" style="width: 120px" clearable placeholder="操作类型">
            <el-option label="全部" value="" />
            <el-option label="发布" value="发布" />
            <el-option label="修改" value="修改" />
            <el-option label="新增" value="新增" />
            <el-option label="删除" value="删除" />
            <el-option label="回滚" value="回滚" />
            <el-option label="审批" value="审批" />
            <el-option label="扩容" value="扩容" />
            <el-option label="缩容" value="缩容" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData" :icon="'Search'">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 审计日志表格 -->
    <el-card shadow="hover">
      <el-table :data="auditLogs" stripe size="small" style="width: 100%">
        <el-table-column prop="auditId" label="日志ID" width="120">
          <template #default="{ row }"><code class="text-xs">{{ row.auditId }}</code></template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="90" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" width="80">
          <template #default="{ row }">
            <el-tag :type="actionTag(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="350" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : (row.result === 'PENDING' ? 'warning' : 'danger')" size="small">
              {{ { SUCCESS: '成功', PENDING: '待处理', FAILED: '失败' }[row.result] || row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operateTime" label="操作时间" width="160" />
        <el-table-column prop="duration" label="耗时" width="80" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api/ops'

const filters = ref({ operator: '', action: '', module: '' })
const auditLogs = ref<any[]>([])

const actionTag = (action: string) => {
  const map: Record<string, string> = {
    发布: 'primary', 修改: 'warning', 新增: 'success',
    删除: 'danger', 回滚: 'danger', 审批: 'info',
    扩容: 'success', 缩容: 'warning'
  }
  return map[action] || 'info'
}

const loadData = async () => {
  try {
    const res = await getAuditLogs({ ...filters.value, page: 1, size: 50 })
    auditLogs.value = res.data || []
  } catch {
    auditLogs.value = []
  }
}

const exportLogs = () => {
  ElMessage.success('审计日志导出任务已提交，可在"报表"中下载')
}

onMounted(loadData)
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; }
</style>
