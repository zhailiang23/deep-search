<template>
  <th
    :class="cn(
      'h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <slot />
  </th>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>