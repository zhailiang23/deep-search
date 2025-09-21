<template>
  <tr
    :class="cn(
      'border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <slot />
  </tr>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>