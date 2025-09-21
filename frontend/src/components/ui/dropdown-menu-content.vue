<template>
  <div
    v-if="visible"
    :class="cn(
      'z-50 min-w-[8rem] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
      $attrs.class as string
    )"
    :data-state="visible ? 'open' : 'closed'"
    :data-side="side"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { cn } from '@/lib/utils'

export interface DropdownMenuContentProps {
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
  loop?: boolean
}

// 组件属性
const props = withDefaults(defineProps<DropdownMenuContentProps>(), {
  side: 'bottom',
  sideOffset: 4,
  align: 'start',
  alignOffset: 0,
  avoidCollisions: true,
  collisionPadding: 10,
  arrowPadding: 0,
  sticky: 'partial',
  hideWhenDetached: false,
  loop: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 获取下拉菜单上下文
const dropdownContext = inject<any>('dropdown-context', null)
const visible = dropdownContext?.visible || false
const side = props.side
</script>

<style scoped>
/* 下拉菜单内容动画 */
[data-state="open"] {
  animation-duration: 0.15s;
  animation-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
}

[data-state="closed"] {
  animation-duration: 0.1s;
  animation-timing-function: ease-in;
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

/* 动画关键帧 */
@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fade-out {
  from { opacity: 1; }
  to { opacity: 0; }
}

@keyframes zoom-in {
  from { transform: scale(0.95); }
  to { transform: scale(1); }
}

@keyframes zoom-out {
  from { transform: scale(1); }
  to { transform: scale(0.95); }
}

@keyframes slide-in-from-top {
  from { transform: translateY(-8px); }
  to { transform: translateY(0); }
}

@keyframes slide-in-from-bottom {
  from { transform: translateY(8px); }
  to { transform: translateY(0); }
}

@keyframes slide-in-from-left {
  from { transform: translateX(-8px); }
  to { transform: translateX(0); }
}

@keyframes slide-in-from-right {
  from { transform: translateX(8px); }
  to { transform: translateX(0); }
}
</style>