<template>
  <button
    type="button"
    role="combobox"
    :aria-expanded="open"
    :aria-haspopup="true"
    :disabled="disabled"
    :class="cn(selectTriggerVariants({ size }), $attrs.class as string)"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot>
      <span class="block truncate">{{ placeholder || '请选择...' }}</span>
    </slot>
    <ChevronDown class="ml-2 h-4 w-4 shrink-0 opacity-50" />
  </button>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { ChevronDown } from 'lucide-vue-next'

// 选择框触发器变体配置
const selectTriggerVariants = cva(
  'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
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

export interface SelectTriggerProps extends /* @vue-ignore */ VariantProps<typeof selectTriggerVariants> {
  placeholder?: string
  disabled?: boolean
  open?: boolean
}

// 组件属性
const props = withDefaults(defineProps<SelectTriggerProps>(), {
  size: 'default',
  disabled: false,
  open: false,
  placeholder: '请选择...'
})

// 组件事件
const emit = defineEmits<{
  'click': [event: MouseEvent]
  'toggle': [open: boolean]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  if (!props.disabled) {
    emit('click', event)
    emit('toggle', !props.open)
  }
}
</script>

<style scoped>
/* 焦点状态 */
button:focus {
  outline: none;
}

/* 悬停效果 */
button:hover:not(:disabled) {
  background-color: var(--accent);
}

/* 禁用状态 */
button:disabled {
  background-color: var(--muted);
  cursor: not-allowed;
}

/* 下拉箭头动画 */
button[aria-expanded="true"] .chevron-down {
  transform: rotate(180deg);
}

.chevron-down {
  transition: transform 0.2s ease-in-out;
}
</style>