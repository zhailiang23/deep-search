<template>
  <component
    :is="as"
    :class="cn(badgeVariants({ variant, size }), $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 徽章变体配置
const badgeVariants = cva(
  'inline-flex items-center rounded-full border font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-primary text-primary-foreground hover:bg-primary/90',
        secondary: 'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
        destructive: 'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/90',
        outline: 'border-border text-foreground',
        success: 'border-transparent bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100',
        warning: 'border-transparent bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-100',
        info: 'border-transparent bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-100',
      },
      size: {
        default: 'px-2.5 py-0.5 text-xs',
        sm: 'px-2 py-0.5 text-xs',
        lg: 'px-3 py-1 text-sm',
        xl: 'px-4 py-1.5 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface BadgeProps extends /* @vue-ignore */ VariantProps<typeof badgeVariants> {
  as?: string | object
  removable?: boolean
}

// 组件属性
const props = withDefaults(defineProps<BadgeProps>(), {
  variant: 'default',
  size: 'default',
  as: 'span',
  removable: false
})

// 组件事件
const emit = defineEmits<{
  remove: []
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理移除
const handleRemove = (event: Event) => {
  event.stopPropagation()
  emit('remove')
}
</script>