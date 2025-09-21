<template>
  <Menu as="div" class="relative inline-block text-left">
    <slot name="trigger" :open="open" :close="close">
      <MenuButton
        :class="cn('inline-flex justify-center items-center', $attrs.class as string)"
        v-bind="$attrs"
      >
        <slot />
      </MenuButton>
    </slot>

    <transition
      enter-active-class="transition duration-100 ease-out"
      enter-from-class="transform scale-95 opacity-0"
      enter-to-class="transform scale-100 opacity-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="transform scale-100 opacity-100"
      leave-to-class="transform scale-95 opacity-0"
    >
      <MenuItems
        :class="cn(dropdownVariants({ align, size }), 'z-popover', contentClass)"
      >
        <slot name="content" :close="close" />
      </MenuItems>
    </transition>
  </Menu>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { Menu, MenuButton, MenuItems } from '@headlessui/vue'
import { cn } from '@/lib/utils'

// 下拉菜单变体配置
const dropdownVariants = cva(
  'absolute mt-2 rounded-md bg-popover text-popover-foreground shadow-lg ring-1 ring-border ring-opacity-5 focus:outline-none border',
  {
    variants: {
      align: {
        start: 'left-0 origin-top-left',
        center: 'left-1/2 -translate-x-1/2 origin-top',
        end: 'right-0 origin-top-right',
      },
      size: {
        sm: 'w-40',
        default: 'w-56',
        lg: 'w-72',
        xl: 'w-96',
      },
    },
    defaultVariants: {
      align: 'start',
      size: 'default',
    },
  }
)

export interface DropdownMenuProps extends /* @vue-ignore */ VariantProps<typeof dropdownVariants> {
  contentClass?: string
}

// 组件属性
const props = withDefaults(defineProps<DropdownMenuProps>(), {
  align: 'start',
  size: 'default'
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 组件方法（通过 slot props 暴露）
const open = () => {}
const close = () => {}
</script>

<style scoped>
.z-popover {
  z-index: var(--z-popover);
}
</style>