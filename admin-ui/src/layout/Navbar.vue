<template>
  <el-header class="admin-navbar">
    <div class="flex items-center gap-3">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="flex items-center gap-3">
      <!-- 系统状态指示灯 -->
      <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-green-500/10 border border-green-500/20">
        <span class="w-1.5 h-1.5 rounded-full bg-[#00C853] animate-pulse" />
        <span class="text-[10px] text-[#00C853] font-medium">系统正常</span>
      </div>
      <div class="text-xs text-gray-500">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</div>
      <el-dropdown @command="handleCommand">
        <span class="flex items-center gap-1.5 cursor-pointer px-2 py-1 rounded-lg hover:bg-white/[0.04] transition-colors">
          <el-avatar :size="24" :icon="UserFilled" style="background:rgba(79,140,247,0.2); color:#4F8CF7" />
          <el-icon class="text-gray-500"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示').then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #0D1322;
  border-bottom: 1px solid #1E2A44;
}
</style>
