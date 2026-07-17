<template>
  <div class="sidebar-wrapper">
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :router="true"
      class="sidebar-menu"
    >
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" stroke="#409EFF" />
          </svg>
        </div>
        <span v-if="!isCollapse" class="logo-text">期货管理后台</span>
      </div>

      <MenuItem v-for="item in menuTree" :key="item.id" :item="item" />
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import MenuItem from './MenuItem.vue'

const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const menuTree = computed(() => userStore.menus)
</script>

<style scoped>
.sidebar-wrapper {
  height: 100vh;
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar-menu {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(180deg, #0c1929 0%, #0a1525 100%);
  border-right: 1px solid rgba(255,255,255,0.06);
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(255,255,255,0.05);
  --el-menu-active-color: #409EFF;
  --el-menu-text-color: rgba(255,255,255,0.6);
  --el-menu-hover-text-color: rgba(255,255,255,0.9);
}
.sidebar-menu:not(.el-menu--collapse) { width: 220px; }
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-icon {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(64,158,255,0.12);
  border-radius: 8px;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.3px;
}
</style>
