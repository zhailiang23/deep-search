<template>
  <div
    :class="cn(
      'relative z-50 max-h-96 min-w-[8rem] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface SelectContentProps {
  position?: 'popper' | 'item-aligned'
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
const props = withDefaults(defineProps<SelectContentProps>(), {
  position: 'item-aligned',
  side: 'bottom',
  sideOffset: 4,
  align: 'start',
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