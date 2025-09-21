<template>
  <div class="relative">
    <select
      :id="id"
      :value="modelValue"
      :disabled="disabled"
      :class="cn(selectVariants({ size }), $attrs.class as string)"
      @change="handleChange"
      v-bind="$attrs"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <slot />
    </select>
    <ChevronDown class="absolute right-2 top-1/2 h-4 w-4 -translate-y-1/2 opacity-50 pointer-events-none" />
  </div>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { ChevronDown } from 'lucide-vue-next'

// 选择框变体配置
const selectVariants = cva(
  'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 appearance-none',
  {
    variants: {
      size: {
        default: 'h-10 px-3 py-2',
        sm: 'h-9 px-3 text-xs',
        lg: 'h-11 px-4 py-3',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
)

export interface SelectProps extends /* @vue-ignore */ VariantProps<typeof selectVariants> {
  id?: string
  modelValue?: string | number
  disabled?: boolean
  placeholder?: string
}

// 组件属性
const props = withDefaults(defineProps<SelectProps>(), {
  size: 'default',
  disabled: false
})

// 组件事件
const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  'change': [value: string | number]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理选择变化
const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  const value = target.value
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
/* 移除默认的下拉箭头 */
select {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  background-image: none;
}

/* Firefox 特殊处理 */
select::-moz-focus-inner {
  border: 0;
}

/* 焦点状态 */
select:focus {
  outline: none;
}

/* 禁用状态 */
select:disabled {
  background-color: var(--muted);
  cursor: not-allowed;
}
</style>