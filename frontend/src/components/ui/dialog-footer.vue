<template>
  <div
    :class="cn('flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2 px-6 py-3 bg-muted/20 border-t', $attrs.class as string)"
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