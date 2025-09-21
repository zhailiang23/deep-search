<template>
  <MenuItem v-slot="{ active, disabled }">
    <component
      :is="as"
      :class="cn(
        'group flex w-full items-center px-4 py-2 text-sm transition-colors',
        active ? 'bg-accent text-accent-foreground' : 'text-foreground',
        disabled && 'opacity-50 cursor-not-allowed',
        destructive && 'text-destructive focus:text-destructive',
        $attrs.class as string
      )"
      :disabled="disabled"
      v-bind="$attrs"
      @click="handleClick"
    >
      <!-- 图标插槽 -->
      <div v-if="$slots.icon" class="mr-3 flex-shrink-0">
        <slot name="icon" :active="active" :disabled="disabled" />
      </div>

      <!-- 主要内容 */
      <div class="flex-1">
        <slot :active="active" :disabled="disabled" />
      </div>

      <!-- 右侧内容（如快捷键、箭头等） -->
      <div v-if="$slots.suffix" class="ml-3 flex-shrink-0">
        <slot name="suffix" :active="active" :disabled="disabled" />
      </div>
    </component>
  </MenuItem>
</template>

<script setup lang="ts">
import { MenuItem } from '@headlessui/vue'
import { cn } from '@/lib/utils'

export interface DropdownMenuItemProps {
  as?: string | object
  disabled?: boolean
  destructive?: boolean
}

// 组件属性
const props = withDefaults(defineProps<DropdownMenuItemProps>(), {
  as: 'button',
  disabled: false,
  destructive: false
})

// 组件事件
const emit = defineEmits<{
  click: [event: Event]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理点击
const handleClick = (event: Event) => {
  if (!props.disabled) {
    emit('click', event)
  }
}
</script>