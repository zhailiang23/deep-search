<template>
  <DialogDescription
    :class="cn('text-sm text-muted-foreground', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </DialogDescription>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue'
import { cn } from '@/lib/utils'

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>