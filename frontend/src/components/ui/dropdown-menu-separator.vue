<template>
  <div
    :class="cn('my-1 h-px bg-border', $attrs.class as string)"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>