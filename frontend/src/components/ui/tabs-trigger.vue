<template>
  <button
    role="tab"
    type="button"
    :id="tabId"
    :aria-controls="panelId"
    :aria-selected="isActive"
    :data-state="isActive ? 'active' : 'inactive'"
    :disabled="disabled"
    :class="cn(tabsTriggerVariants(), isActive && 'bg-background text-foreground shadow-sm', $attrs.class as string)"
    @click="handleClick"
    @keydown="handleKeydown"
    v-bind="$attrs"
  >
    <slot>{{ label }}</slot>
  </button>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted } from 'vue'
import { cva } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// 标签触发器变体配置
const tabsTriggerVariants = cva(
  'inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm'
)

export interface TabsTriggerProps {
  value: string
  label?: string
  disabled?: boolean
}

// 组件属性
const props = withDefaults(defineProps<TabsTriggerProps>(), {
  disabled: false
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 获取标签上下文
const tabsContext = inject<any>('tabs-context')

// 计算是否激活
const isActive = computed(() => tabsContext?.currentTab?.value === props.value)

// 生成唯一ID
const tabId = computed(() => `tab-${props.value}`)
const panelId = computed(() => `panel-${props.value}`)

// 处理点击事件
const handleClick = () => {
  if (!props.disabled && tabsContext?.setActiveTab) {
    tabsContext.setActiveTab(props.value)
  }
}

// 处理键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  if (props.disabled) return

  const { key } = event
  const isHorizontal = tabsContext?.orientation === 'horizontal'

  if (isHorizontal) {
    if (key === 'ArrowLeft' || key === 'ArrowRight') {
      event.preventDefault()
      // 实现左右切换逻辑
    }
  } else {
    if (key === 'ArrowUp' || key === 'ArrowDown') {
      event.preventDefault()
      // 实现上下切换逻辑
    }
  }

  if (key === 'Enter' || key === ' ') {
    event.preventDefault()
    handleClick()
  }
}

// 注册标签
onMounted(() => {
  if (tabsContext?.registerTab) {
    tabsContext.registerTab({
      value: props.value,
      label: props.label || props.value,
      disabled: props.disabled
    })
  }
})

// 注销标签
onUnmounted(() => {
  if (tabsContext?.unregisterTab) {
    tabsContext.unregisterTab(props.value)
  }
})
</script>

<style scoped>
/* 标签按钮基础样式 */
button[role="tab"] {
  position: relative;
  user-select: none;
}

/* 激活状态 */
button[role="tab"][data-state="active"] {
  z-index: 1;
}

/* 悬停效果 */
button[role="tab"]:hover:not(:disabled) {
  background-color: var(--muted);
  color: var(--foreground);
}

/* 焦点状态 */
button[role="tab"]:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--ring);
}

/* 禁用状态 */
button[role="tab"]:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* 过渡动画 */
button[role="tab"] {
  transition: all 0.2s ease-in-out;
}
</style>