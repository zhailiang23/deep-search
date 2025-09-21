<template>
  <input
    :id="id"
    type="checkbox"
    :checked="checked"
    :disabled="disabled"
    :class="cn(checkboxVariants({ size }), $attrs.class as string)"
    @change="handleChange"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 复选框变体配置
const checkboxVariants = cva(
  'peer h-4 w-4 shrink-0 rounded-sm border border-primary ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground',
  {
    variants: {
      size: {
        default: 'h-4 w-4',
        sm: 'h-3 w-3',
        lg: 'h-5 w-5',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
)

export interface CheckboxProps extends /* @vue-ignore */ VariantProps<typeof checkboxVariants> {
  id?: string
  checked?: boolean
  disabled?: boolean
  modelValue?: boolean
}

// 组件属性
const props = withDefaults(defineProps<CheckboxProps>(), {
  size: 'default',
  checked: false,
  disabled: false,
  modelValue: false
})

// 组件事件
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'change': [value: boolean]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 处理复选框状态变化
const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.checked
  emit('update:modelValue', value)
  emit('change', value)
}

// 计算当前选中状态
const isChecked = computed(() => props.modelValue ?? props.checked)
</script>

<style scoped>
/* 自定义复选框样式 */
input[type="checkbox"] {
  appearance: none;
  background-color: white;
  border: 1px solid #d1d5db;
  padding: 0;
  print-color-adjust: exact;
  display: inline-block;
  vertical-align: middle;
  background-origin: border-box;
  user-select: none;
  flex-shrink: 0;
}

input[type="checkbox"]:checked {
  background-color: currentColor;
  background-size: 100% 100%;
  background-position: center;
  background-repeat: no-repeat;
  background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='m13.854 3.646-7.5 7.5a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6 10.293l7.146-7.147a.5.5 0 0 1 .708.708z'/%3e%3c/svg%3e");
}

input[type="checkbox"]:focus {
  outline: 2px solid transparent;
  outline-offset: 2px;
  box-shadow: 0 0 0 2px var(--ring);
}

input[type="checkbox"]:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>