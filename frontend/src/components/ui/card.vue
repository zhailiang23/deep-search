<template>
  <div
    :class="cn(cardVariants({ variant, size }), $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 卡片变体配置
const cardVariants = cva(
  'rounded-lg border bg-card text-card-foreground',
  {
    variants: {
      variant: {
        default: 'shadow-sm',
        elevated: 'shadow-md',
        outlined: 'border-2',
        ghost: 'border-transparent shadow-none',
      },
      size: {
        default: 'p-6',
        sm: 'p-4',
        lg: 'p-8',
        xl: 'p-10',
        none: 'p-0',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface CardProps extends /* @vue-ignore */ VariantProps<typeof cardVariants> {
  hoverable?: boolean
  clickable?: boolean
}

// 组件属性
const props = withDefaults(defineProps<CardProps>(), {
  variant: 'default',
  size: 'default',
  hoverable: false,
  clickable: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>

<style scoped>
/* 可悬停效果 */
.hoverable {
  @apply transition-shadow duration-200 hover:shadow-md;
}

/* 可点击效果 */
.clickable {
  @apply cursor-pointer transition-all duration-200 hover:shadow-md active:scale-[0.99];
}

.clickable:hover {
  @apply transform;
}
</style>