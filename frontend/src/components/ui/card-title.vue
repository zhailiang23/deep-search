<template>
  <h3
    :class="cn('text-2xl font-semibold leading-none tracking-tight', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </h3>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>