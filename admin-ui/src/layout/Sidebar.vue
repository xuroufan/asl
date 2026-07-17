<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="isCollapse"
    background-color="#001529"
    text-color="#ffffffb3"
    active-text-color="#fff"
    :router="true"
    style="height: 100vh; overflow-y: auto; border-right: none"
  >
    <div class="logo" style="height: 50px; line-height: 50px; text-align: center; color: #fff; font-size: 16px; font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1)">
      <span v-if="!isCollapse">📊 期货管理后台</span>
      <span v-else>📊</span>
    </div>
    <MenuItem v-for="item in menuTree" :key="item.id" :item="item" />
  </el-menu>
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
.logo { background: #002140; }
.el-menu { border-right: none; }
</style>