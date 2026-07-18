<template>
  <div class="admin-app">
    <el-container style="height: 100vh">
      <Sidebar />
      <el-container>
        <Navbar />
        <el-main class="admin-main">
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
.admin-app {
  height: 100vh;
  display: flex;
  background: #080C1A;
  color: #CBD5E1;
}
.admin-main {
  background: #0A0F1E;
  padding: 20px;
  overflow-y: auto;
}
</style>
