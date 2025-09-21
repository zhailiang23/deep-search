<template>
  <div
    ref="triggerRef"
    :class="cn('inline-block', $attrs.class as string)"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @focus="handleFocus"
    @blur="handleBlur"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import { cn } from '@/lib/utils'

export interface TooltipTriggerProps {
  asChild?: boolean
}

// 组件属性
const props = withDefaults(defineProps<TooltipTriggerProps>(), {
  asChild: false
})

// 组件事件
const emit = defineEmits<{
  'mouseenter': [event: MouseEvent]
  'mouseleave': [event: MouseEvent]
  'focus': [event: FocusEvent]
  'blur': [event: FocusEvent]
  'click': [event: MouseEvent]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 触发器引用
const triggerRef = ref<HTMLElement>()

// 获取工具提示上下文
const tooltipContext = inject<any>('tooltip-context', null)

// 处理鼠标进入
const handleMouseEnter = (event: MouseEvent) => {
  emit('mouseenter', event)
  tooltipContext?.show?.()
}

// 处理鼠标离开
const handleMouseLeave = (event: MouseEvent) => {
  emit('mouseleave', event)
  tooltipContext?.hide?.()
}

// 处理焦点
const handleFocus = (event: FocusEvent) => {
  emit('focus', event)
  tooltipContext?.show?.()
}

// 处理失焦
const handleBlur = (event: FocusEvent) => {
  emit('blur', event)
  tooltipContext?.hide?.()
}

// 处理点击
const handleClick = (event: MouseEvent) => {
  emit('click', event)
  if (tooltipContext?.triggerMode === 'click') {
    tooltipContext?.toggle?.()
  }
}

// 暴露触发器引用
defineExpose({
  triggerRef
})
</script>

<style scoped>
/* 触发器样式 */
.trigger {
  display: inline-block;
  cursor: pointer;
}

/* 焦点状态 */
.trigger:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: 2px;
  border-radius: 4px;
}

/* 悬停状态 */
.trigger:hover {
  opacity: 0.8;
}

/* 禁用状态 */
.trigger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>