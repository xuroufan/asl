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
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" stroke="#4F8CF7" />
          </svg>
        </div>
        <span v-if="!isCollapse" class="logo-text">ASL 管理后台</span>
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
  background: linear-gradient(180deg, #0A0F1E 0%, #080C1A 100%);
  border-right: 1px solid rgba(79,140,247,0.08);
}
.sidebar-menu:not(.el-menu--collapse) { width: 220px; }
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(79,140,247,0.08);
}
.logo-icon {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(79,140,247,0.12);
  border-radius: 8px;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.3px;
}
</style>
