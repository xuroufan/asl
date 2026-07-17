<template>
  <el-sub-menu v-if="item.children && item.children.length > 0 && item.children[0].meta && !item.children[0].meta.hideMenu" :index="item.path">
    <template #title>
      <el-icon v-if="item.meta.icon"><component :is="item.meta.icon" /></el-icon>
      <span>{{ item.meta.title }}</span>
    </template>
    <MenuItem v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>
  <el-menu-item v-else-if="!item.meta?.hideMenu" :index="item.path">
    <el-icon v-if="item.meta.icon"><component :is="item.meta.icon" /></el-icon>
    <template #title>{{ item.meta.title }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
defineProps<{ item: any }>()
</script>