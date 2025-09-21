<template>
  <tbody
    :class="cn('[&_tr:last-child]:border-0', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </tbody>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>