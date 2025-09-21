<template>
  <input
    :id="id"
    :type="type"
    :class="cn(inputVariants({ size }), $attrs.class as string)"
    :placeholder="placeholder"
    :disabled="disabled"
    :readonly="readonly"
    :value="modelValue"
    @input="handleInput"
    @change="handleChange"
    @focus="handleFocus"
    @blur="handleBlur"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 输入框变体配置
const inputVariants = cva(
  'flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-colors',
  {
    variants: {
      size: {
        default: 'h-10',
        sm: 'h-9 px-2 text-xs',
        lg: 'h-11 px-4',
        xl: 'h-12 px-4 text-base',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  }
)

export interface InputProps extends /* @vue-ignore */ VariantProps<typeof inputVariants> {
  id?: string
  type?: string
  modelValue?: string | number
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
}

// 组件属性
const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  modelValue: '',
  disabled: false,
  readonly: false,
  size: 'default'
})

// 组件事件
const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  change: [value: string | number]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 事件处理
const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  let value: string | number = target.value

  if (props.type === 'number') {
    value = target.valueAsNumber || 0
  }

  emit('update:modelValue', value)
}

const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  let value: string | number = target.value

  if (props.type === 'number') {
    value = target.valueAsNumber || 0
  }

  emit('change', value)
}

const handleFocus = (event: FocusEvent) => {
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  emit('blur', event)
}
</script>

<style scoped>
/* 数字输入框隐藏默认的增减按钮 */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}

/* 文件输入框样式 */
input[type="file"]::file-selector-button {
  background: transparent;
  border: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
}
</style>