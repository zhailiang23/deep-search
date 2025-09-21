<template>
  <div
    role="tablist"
    :aria-orientation="orientation"
    :class="cn(
      'inline-flex h-10 items-center justify-center rounded-md bg-muted p-1 text-muted-foreground',
      orientation === 'vertical' && 'flex-col h-auto w-auto space-y-1',
      $attrs.class as string
    )"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { cn } from '@/lib/utils'

export interface TabsListProps {
  loop?: boolean
}

// 组件属性
const props = withDefaults(defineProps<TabsListProps>(), {
  loop: true
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 获取标签上下文
const tabsContext = inject<any>('tabs-context')
const orientation = tabsContext?.orientation || 'horizontal'
</script>

<style scoped>
/* 垂直方向的标签列表 */
.vertical {
  flex-direction: column;
  height: auto;
  width: auto;
}

/* 水平方向的标签列表 */
.horizontal {
  flex-direction: row;
}

/* 标签列表容器 */
[role="tablist"] {
  position: relative;
}

/* 焦点指示器 */
[role="tablist"]:focus-within {
  outline: 2px solid var(--ring);
  outline-offset: 2px;
  border-radius: 6px;
}
</style>