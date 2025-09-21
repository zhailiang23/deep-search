<template>
  <div class="relative inline-block">
    <!-- 触发元素 -->
    <div
      ref="triggerRef"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
      @focus="handleFocus"
      @blur="handleBlur"
      @click="handleClick"
    >
      <slot />
    </div>

    <!-- 提示内容 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-100 ease-out"
        enter-from-class="transform scale-95 opacity-0"
        enter-to-class="transform scale-100 opacity-100"
        leave-active-class="transition duration-75 ease-in"
        leave-from-class="transform scale-100 opacity-100"
        leave-to-class="transform scale-95 opacity-0"
      >
        <div
          v-if="isVisible"
          ref="contentRef"
          :class="cn(tooltipVariants({ size }), 'z-tooltip fixed')"
          :style="tooltipStyle"
          role="tooltip"
          :aria-describedby="ariaDescribedby"
        >
          <!-- 箭头 -->
          <div
            v-if="showArrow"
            :class="cn('absolute w-2 h-2 bg-inherit border-inherit', arrowClass)"
            :style="arrowStyle"
          />

          <!-- 内容 -->
          <div class="relative z-10">
            <slot name="content">
              {{ content }}
            </slot>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// Tooltip 变体配置
const tooltipVariants = cva(
  'px-3 py-1.5 text-sm font-medium text-popover-foreground bg-popover border border-border rounded-md shadow-md',
  {
    variants: {
      size: {
        sm: 'px-2 py-1 text-xs',
        default: 'px-3 py-1.5 text-sm',
        lg: 'px-4 py-2 text-base',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
)

export type TooltipPlacement =
  | 'top' | 'top-start' | 'top-end'
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'left' | 'left-start' | 'left-end'
  | 'right' | 'right-start' | 'right-end'

export interface TooltipProps extends /* @vue-ignore */ VariantProps<typeof tooltipVariants> {
  content?: string
  placement?: TooltipPlacement
  trigger?: 'hover' | 'focus' | 'click' | 'manual'
  disabled?: boolean
  showArrow?: boolean
  delay?: number
  offset?: number
  ariaDescribedby?: string
}

// 组件属性
const props = withDefaults(defineProps<TooltipProps>(), {
  placement: 'top',
  trigger: 'hover',
  disabled: false,
  showArrow: true,
  delay: 100,
  offset: 8,
  size: 'default'
})

// 组件事件
const emit = defineEmits<{
  'update:visible': [visible: boolean]
  show: []
  hide: []
}>()

// 模板引用
const triggerRef = ref<HTMLElement>()
const contentRef = ref<HTMLElement>()

// 状态
const isVisible = ref(false)
const timeoutId = ref<ReturnType<typeof setTimeout>>()

// 样式和位置计算
const tooltipStyle = ref<Record<string, string>>({})
const arrowStyle = ref<Record<string, string>>({})

// 箭头样式类
const arrowClass = computed(() => {
  const placement = props.placement
  if (placement.startsWith('top')) {
    return 'bottom-[-4px] left-1/2 -translate-x-1/2 border-l-transparent border-r-transparent border-b-transparent'
  } else if (placement.startsWith('bottom')) {
    return 'top-[-4px] left-1/2 -translate-x-1/2 border-l-transparent border-r-transparent border-t-transparent'
  } else if (placement.startsWith('left')) {
    return 'right-[-4px] top-1/2 -translate-y-1/2 border-t-transparent border-b-transparent border-r-transparent'
  } else if (placement.startsWith('right')) {
    return 'left-[-4px] top-1/2 -translate-y-1/2 border-t-transparent border-b-transparent border-l-transparent'
  }
  return ''
})

// 显示 tooltip
const show = async () => {
  if (props.disabled) return

  clearTimeout(timeoutId.value)

  if (props.delay > 0) {
    timeoutId.value = setTimeout(() => {
      isVisible.value = true
      emit('update:visible', true)
      emit('show')
      nextTick(updatePosition)
    }, props.delay)
  } else {
    isVisible.value = true
    emit('update:visible', true)
    emit('show')
    await nextTick()
    updatePosition()
  }
}

// 隐藏 tooltip
const hide = () => {
  clearTimeout(timeoutId.value)
  isVisible.value = false
  emit('update:visible', false)
  emit('hide')
}

// 更新位置
const updatePosition = () => {
  if (!triggerRef.value || !contentRef.value) return

  const trigger = triggerRef.value
  const content = contentRef.value
  const triggerRect = trigger.getBoundingClientRect()
  const contentRect = content.getBoundingClientRect()

  const { placement, offset } = props
  let top = 0
  let left = 0

  // 基础位置计算
  switch (placement) {
    case 'top':
    case 'top-start':
    case 'top-end':
      top = triggerRect.top - contentRect.height - offset
      break
    case 'bottom':
    case 'bottom-start':
    case 'bottom-end':
      top = triggerRect.bottom + offset
      break
    case 'left':
    case 'left-start':
    case 'left-end':
      left = triggerRect.left - contentRect.width - offset
      break
    case 'right':
    case 'right-start':
    case 'right-end':
      left = triggerRect.right + offset
      break
  }

  // 横向对齐
  if (placement.includes('top') || placement.includes('bottom')) {
    if (placement.includes('start')) {
      left = triggerRect.left
    } else if (placement.includes('end')) {
      left = triggerRect.right - contentRect.width
    } else {
      left = triggerRect.left + triggerRect.width / 2 - contentRect.width / 2
    }
  }

  // 纵向对齐
  if (placement.includes('left') || placement.includes('right')) {
    if (placement.includes('start')) {
      top = triggerRect.top
    } else if (placement.includes('end')) {
      top = triggerRect.bottom - contentRect.height
    } else {
      top = triggerRect.top + triggerRect.height / 2 - contentRect.height / 2
    }
  }

  // 边界检测和调整
  const viewport = {
    width: window.innerWidth,
    height: window.innerHeight
  }

  if (left < 0) left = 8
  if (left + contentRect.width > viewport.width) left = viewport.width - contentRect.width - 8
  if (top < 0) top = 8
  if (top + contentRect.height > viewport.height) top = viewport.height - contentRect.height - 8

  tooltipStyle.value = {
    top: `${top}px`,
    left: `${left}px`
  }
}

// 事件处理
const handleMouseEnter = () => {
  if (props.trigger === 'hover') {
    show()
  }
}

const handleMouseLeave = () => {
  if (props.trigger === 'hover') {
    hide()
  }
}

const handleFocus = () => {
  if (props.trigger === 'focus') {
    show()
  }
}

const handleBlur = () => {
  if (props.trigger === 'focus') {
    hide()
  }
}

const handleClick = () => {
  if (props.trigger === 'click') {
    if (isVisible.value) {
      hide()
    } else {
      show()
    }
  }
}

// 全局点击处理（用于 click trigger）
const handleGlobalClick = (event: Event) => {
  if (props.trigger === 'click' && isVisible.value) {
    const target = event.target as Node
    if (!triggerRef.value?.contains(target) && !contentRef.value?.contains(target)) {
      hide()
    }
  }
}

// 窗口大小变化处理
const handleResize = () => {
  if (isVisible.value) {
    updatePosition()
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
  window.removeEventListener('resize', handleResize)
  clearTimeout(timeoutId.value)
})

// 暴露方法
defineExpose({
  show,
  hide,
  isVisible
})
</script>

<style scoped>
.z-tooltip {
  z-index: var(--z-tooltip);
}
</style>