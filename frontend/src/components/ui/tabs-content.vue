<template>
  <div
    v-show="isActive"
    role="tabpanel"
    :id="panelId"
    :aria-labelledby="tabId"
    :data-state="isActive ? 'active' : 'inactive'"
    :class="cn(
      'mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
      $attrs.class as string
    )"
    :tabindex="isActive ? 0 : -1"
    v-bind="$attrs"
  >
    <slot v-if="isActive || forceMount" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { cn } from '@/lib/utils'

export interface TabsContentProps {
  value: string
  forceMount?: boolean
}

// 组件属性
const props = withDefaults(defineProps<TabsContentProps>(), {
  forceMount: false
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
</script>

<style scoped>
/* 标签内容面板 */
[role="tabpanel"] {
  animation: fadeIn 0.2s ease-in-out;
}

/* 激活状态的面板 */
[role="tabpanel"][data-state="active"] {
  display: block;
}

/* 非激活状态的面板 */
[role="tabpanel"][data-state="inactive"] {
  display: none;
}

/* 焦点状态 */
[role="tabpanel"]:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--ring);
  border-radius: 6px;
}

/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 强制挂载时的样式 */
.force-mount {
  display: block !important;
}
</style>