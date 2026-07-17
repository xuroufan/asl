<template>
  <div class="layout-container">
    <el-container style="height: 100vh">
      <!-- 侧边栏 -->
      <Sidebar />
      <el-container>
        <!-- 顶部导航栏 -->
        <Navbar />
        <!-- 主内容区 -->
        <el-main style="background: #f5f7fa; padding: 20px; overflow-y: auto">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
</style>