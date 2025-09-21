<template>
  <option
    :value="value"
    :disabled="disabled"
    :class="cn(
      'relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <span class="absolute left-2 flex h-3.5 w-3.5 items-center justify-center">
      <Check v-if="selected" class="h-4 w-4" />
    </span>
    <slot>{{ label || value }}</slot>
  </option>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { Check } from 'lucide-vue-next'

export interface SelectItemProps {
  value: string | number
  label?: string
  disabled?: boolean
  selected?: boolean
}

// 组件属性
const props = withDefaults(defineProps<SelectItemProps>(), {
  disabled: false,
  selected: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 计算是否选中
const isSelected = computed(() => props.selected)
</script>

<style scoped>
/* 选项悬停效果 */
option:hover {
  background-color: var(--accent);
  color: var(--accent-foreground);
}

/* 选中状态 */
option:checked {
  background-color: var(--primary);
  color: var(--primary-foreground);
}

/* 禁用状态 */
option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>