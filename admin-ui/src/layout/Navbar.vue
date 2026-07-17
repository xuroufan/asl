<template>
  <el-header style="background: #fff; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 50px">
    <div class="left">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right" style="display: flex; align-items: center; gap: 15px">
      <el-text>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</el-text>
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link" style="cursor: pointer; display: flex; align-items: center; gap: 5px">
          <el-avatar :size="28" :icon="UserFilled" />
          <el-icon><ArrowDown /></el-icon>
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