<template>
  <div
    :class="cn('relative w-full overflow-auto', wrapperClass)"
  >
    <table
      :class="cn('w-full caption-bottom text-sm', $attrs.class as string)"
      v-bind="$attrs"
    >
      <slot />
    </table>
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface TableProps {
  wrapperClass?: string
}

// 组件属性
const props = defineProps<TableProps>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>