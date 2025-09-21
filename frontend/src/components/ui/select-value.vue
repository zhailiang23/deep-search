<template>
  <span
    :class="cn(
      'block truncate',
      placeholder && !modelValue && 'text-muted-foreground',
      $attrs.class as string
    )"
  >
    <slot>{{ displayValue }}</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

export interface SelectValueProps {
  placeholder?: string
  modelValue?: string | number
}

// 组件属性
const props = withDefaults(defineProps<SelectValueProps>(), {
  placeholder: '请选择...'
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 计算显示值
const displayValue = computed(() => {
  if (props.modelValue !== undefined && props.modelValue !== null && props.modelValue !== '') {
    return props.modelValue
  }
  return props.placeholder
})
</script>

<style scoped>
/* 选择值显示样式 */
span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 占位符样式 */
.placeholder {
  color: var(--muted-foreground);
}

/* 选中值样式 */
.selected {
  color: var(--foreground);
}
</style>