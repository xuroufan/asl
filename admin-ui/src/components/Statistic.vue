<template>
  <div style="text-align:center;padding:8px">
    <div style="font-size:22px;font-weight:700;color:var(--color, #409eff);font-family:monospace">{{ value }}</div>
    <div style="font-size:12px;color:#909399;margin-top:4px">{{ label }}</div>
  </div>
</template>
<script setup lang="ts">
defineProps<{ value: string; label: string; color?: string }>()
</script>
<style scoped>div { --color: v-bind(color); }</style>
