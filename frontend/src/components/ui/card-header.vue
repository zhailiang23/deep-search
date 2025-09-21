<template>
  <div
    :class="cn('flex flex-col space-y-1.5 p-6', $attrs.class as string)"
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