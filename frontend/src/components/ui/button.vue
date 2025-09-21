<template>
  <button
    :class="cn(buttonVariants({ variant, size }), $attrs.class as string)"
    :disabled="disabled"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 按钮变体配置
const buttonVariants = cva(
  'inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-primary/90',
        destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
        outline: 'border border-input bg-background hover:bg-accent hover:text-accent-foreground',
        secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 rounded-md px-3',
        lg: 'h-11 rounded-md px-8',
        icon: 'h-10 w-10',
        xs: 'h-8 rounded-md px-2 text-xs',
        xl: 'h-12 rounded-md px-10 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface ButtonProps extends /* @vue-ignore */ VariantProps<typeof buttonVariants> {
  disabled?: boolean
  loading?: boolean
  loadingText?: string
}

// 组件属性
const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'default',
  size: 'default',
  disabled: false,
  loading: false,
  loadingText: '加载中...'
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 计算最终的禁用状态
const isDisabled = computed(() => props.disabled || props.loading)
</script>

<style scoped>
/* 自定义按钮状态 */
button:active {
  transform: scale(0.98);
}

button:focus-visible {
  outline: none;
}

/* 加载状态动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading {
  position: relative;
}

.loading::after {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  margin: auto;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
</style>