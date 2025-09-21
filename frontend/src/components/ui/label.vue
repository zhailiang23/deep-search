<template>
  <label
    :for="htmlFor"
    :class="cn(labelVariants({ size, weight }), $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </label>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 标签变体配置
const labelVariants = cva(
  'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
  {
    variants: {
      size: {
        default: 'text-sm',
        sm: 'text-xs',
        lg: 'text-base',
        xl: 'text-lg',
      },
      weight: {
        normal: 'font-normal',
        medium: 'font-medium',
        semibold: 'font-semibold',
        bold: 'font-bold',
      },
    },
    defaultVariants: {
      size: 'default',
      weight: 'medium',
    },
  }
)

export interface LabelProps extends /* @vue-ignore */ VariantProps<typeof labelVariants> {
  htmlFor?: string
  required?: boolean
}

// 组件属性
const props = withDefaults(defineProps<LabelProps>(), {
  size: 'default',
  weight: 'medium',
  required: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>

<style scoped>
/* 必填标识 */
label.required::after {
  content: ' *';
  color: #ef4444;
}

/* 悬停效果 */
label:hover {
  color: var(--foreground);
}

/* 焦点状态 */
label:focus-within {
  color: var(--primary);
}
</style>