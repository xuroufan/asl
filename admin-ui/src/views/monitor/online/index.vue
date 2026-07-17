<template>
  <el-card>
    <template #header><span>在线用户</span></template>
    <el-alert title="在线用户监控功能待集成 — 需后端通过 Redis 记录活跃 Session" type="info" show-icon :closable="false" style="margin-bottom: 15px" />
    <div style="text-align: center; color: #999; padding: 60px 0">暂无在线用户数据</div>
  </el-card>
</template>