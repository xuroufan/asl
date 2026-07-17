<template>
  <el-card>
    <template #header><span>个人信息</span></template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ userStore.roles.join(', ') }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
</script>