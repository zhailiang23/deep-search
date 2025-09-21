<template>
  <div
    :class="cn('w-full', $attrs.class as string)"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { provide, ref, computed } from 'vue'
import { cn } from '@/lib/utils'

export interface TabsProps {
  defaultValue?: string
  modelValue?: string
  orientation?: 'horizontal' | 'vertical'
  activationMode?: 'automatic' | 'manual'
  dir?: 'ltr' | 'rtl'
}

// 组件属性
const props = withDefaults(defineProps<TabsProps>(), {
  orientation: 'horizontal',
  activationMode: 'automatic',
  dir: 'ltr'
})

// 组件事件
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
}>()

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 内部状态
const activeTab = ref(props.modelValue || props.defaultValue || '')

// 计算当前激活的标签
const currentTab = computed({
  get: () => props.modelValue ?? activeTab.value,
  set: (value: string) => {
    activeTab.value = value
    emit('update:modelValue', value)
    emit('change', value)
  }
})

// 标签列表
const tabs = ref<Array<{ value: string; label: string; disabled?: boolean }>>([])

// 注册标签
const registerTab = (tab: { value: string; label: string; disabled?: boolean }) => {
  const existingIndex = tabs.value.findIndex(t => t.value === tab.value)
  if (existingIndex >= 0) {
    tabs.value[existingIndex] = tab
  } else {
    tabs.value.push(tab)
  }
}

// 注销标签
const unregisterTab = (value: string) => {
  const index = tabs.value.findIndex(t => t.value === value)
  if (index >= 0) {
    tabs.value.splice(index, 1)
  }
}

// 设置激活标签
const setActiveTab = (value: string) => {
  const tab = tabs.value.find(t => t.value === value)
  if (tab && !tab.disabled) {
    currentTab.value = value
  }
}

// 提供给子组件的上下文
provide('tabs-context', {
  currentTab,
  setActiveTab,
  registerTab,
  unregisterTab,
  orientation: props.orientation,
  activationMode: props.activationMode,
  dir: props.dir
})
</script>