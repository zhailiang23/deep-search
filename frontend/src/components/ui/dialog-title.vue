<template>
  <DialogTitle
    as="h3"
    :class="cn('text-lg font-semibold leading-6 text-foreground', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </DialogTitle>
</template>

<script setup lang="ts">
import { DialogTitle } from '@headlessui/vue'
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>