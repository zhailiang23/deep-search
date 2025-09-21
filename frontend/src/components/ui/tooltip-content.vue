<template>
  <div
    role="tooltip"
    :class="cn(
      'z-50 overflow-hidden rounded-md border bg-popover px-3 py-1.5 text-sm text-popover-foreground shadow-md animate-in fade-in-0 zoom-in-95 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface TooltipContentProps {
  side?: 'top' | 'right' | 'bottom' | 'left'
  sideOffset?: number
  align?: 'start' | 'center' | 'end'
  alignOffset?: number
  avoidCollisions?: boolean
  collisionBoundary?: Element | null
  collisionPadding?: number | Partial<Record<'top' | 'right' | 'bottom' | 'left', number>>
  arrowPadding?: number
  sticky?: 'partial' | 'always'
  hideWhenDetached?: boolean
}

// 组件属性
const props = withDefaults(defineProps<TooltipContentProps>(), {
  side: 'top',
  sideOffset: 4,
  align: 'center',
  alignOffset: 0,
  avoidCollisions: true,
  collisionPadding: 10,
  arrowPadding: 0,
  sticky: 'partial',
  hideWhenDetached: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})
</script>

<style scoped>
/* 工具提示内容动画 */
[role="tooltip"] {
  animation-duration: 0.15s;
  animation-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
}

/* 不同方向的滑入动画 */
[data-side="top"] {
  transform-origin: bottom;
}

[data-side="bottom"] {
  transform-origin: top;
}

[data-side="left"] {
  transform-origin: right;
}

[data-side="right"] {
  transform-origin: left;
}

/* 淡入淡出动画 */
@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fade-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

@keyframes zoom-in {
  from {
    transform: scale(0.95);
  }
  to {
    transform: scale(1);
  }
}

@keyframes zoom-out {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(0.95);
  }
}
</style>