<template>
  <button
    type="button"
    :aria-expanded="isOpen"
    :aria-haspopup="true"
    :disabled="disabled"
    :class="cn('inline-flex items-center justify-center', $attrs.class as string)"
    @click="handleClick"
    @keydown="handleKeydown"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { inject, computed } from 'vue'
import { cn } from '@/lib/utils'

export interface DropdownMenuTriggerProps {
  disabled?: boolean
  asChild?: boolean
}

// 组件属性
const props = withDefaults(defineProps<DropdownMenuTriggerProps>(), {
  disabled: false,
  asChild: false
})

// 组件事件
const emit = defineEmits<{
  'click': [event: MouseEvent]
  'keydown': [event: KeyboardEvent]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 获取下拉菜单上下文
const dropdownContext = inject<any>('dropdown-context', null)

// 计算是否打开
const isOpen = computed(() => dropdownContext?.visible || false)

// 处理点击事件
const handleClick = (event: MouseEvent) => {
  if (!props.disabled) {
    emit('click', event)
    dropdownContext?.toggle?.()
  }
}

// 处理键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  if (props.disabled) return

  emit('keydown', event)

  const { key } = event

  if (key === 'Enter' || key === ' ') {
    event.preventDefault()
    dropdownContext?.toggle?.()
  } else if (key === 'ArrowDown') {
    event.preventDefault()
    if (!isOpen.value) {
      dropdownContext?.show?.()
    }
    // 焦点移到第一个菜单项
  } else if (key === 'ArrowUp') {
    event.preventDefault()
    if (!isOpen.value) {
      dropdownContext?.show?.()
    }
    // 焦点移到最后一个菜单项
  } else if (key === 'Escape') {
    dropdownContext?.hide?.()
  }
}
</script>

<style scoped>
/* 触发器按钮样式 */
button {
  position: relative;
  cursor: pointer;
}

/* 焦点状态 */
button:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: 2px;
  border-radius: 4px;
}

/* 悬停状态 */
button:hover:not(:disabled) {
  opacity: 0.8;
}

/* 激活状态 */
button[aria-expanded="true"] {
  background-color: var(--accent);
  color: var(--accent-foreground);
}

/* 禁用状态 */
button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 过渡动画 */
button {
  transition: all 0.2s ease-in-out;
}
</style>