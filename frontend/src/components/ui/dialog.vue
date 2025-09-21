<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="relative z-modal" @close="handleClose">
      <!-- 背景遮罩 -->
      <TransitionChild
        as="template"
        enter="ease-out duration-300"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-200"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-background/80 backdrop-blur-sm z-modal-backdrop" />
      </TransitionChild>

      <div class="fixed inset-0 z-modal overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4 text-center sm:p-0">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enter-to="opacity-100 translate-y-0 sm:scale-100"
            leave="ease-in duration-200"
            leave-from="opacity-100 translate-y-0 sm:scale-100"
            leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <DialogPanel
              :class="cn(dialogVariants({ size }), $attrs.class as string)"
              v-bind="$attrs"
            >
              <slot />
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { Dialog, DialogPanel, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { cn } from '@/lib/utils'

// 对话框变体配置
const dialogVariants = cva(
  'relative transform overflow-hidden rounded-lg bg-card text-card-foreground shadow-xl transition-all sm:my-8 border',
  {
    variants: {
      size: {
        sm: 'sm:max-w-sm sm:w-full',
        default: 'sm:max-w-lg sm:w-full',
        lg: 'sm:max-w-2xl sm:w-full',
        xl: 'sm:max-w-4xl sm:w-full',
        '2xl': 'sm:max-w-6xl sm:w-full',
        full: 'sm:max-w-full sm:w-full sm:mx-4',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
)

export interface DialogProps extends /* @vue-ignore */ VariantProps<typeof dialogVariants> {
  open?: boolean
  closable?: boolean
}

// 组件属性
const props = withDefaults(defineProps<DialogProps>(), {
  open: false,
  closable: true,
  size: 'default'
})

// 组件事件
const emit = defineEmits<{
  close: []
  'update:open': [value: boolean]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理关闭
const handleClose = () => {
  if (props.closable) {
    emit('update:open', false)
    emit('close')
  }
}
</script>

<style scoped>
/* z-index 使用 CSS 变量 */
.z-modal {
  z-index: var(--z-modal);
}

.z-modal-backdrop {
  z-index: var(--z-modal-backdrop);
}
</style>