<template>
  <div
    :class="cn('px-4 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>