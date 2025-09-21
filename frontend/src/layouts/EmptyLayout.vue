<template>
  <div class="empty-layout">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 空布局，只包含路由视图，用于登录页面等不需要导航的页面
</script>

<style scoped>
.empty-layout {
  min-height: 100vh;
  width: 100%;
}
</style>