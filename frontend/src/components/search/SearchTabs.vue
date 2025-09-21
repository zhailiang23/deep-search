<template>
  <div class="search-tabs">
    <Tabs v-model="activeTab" @update:modelValue="handleTabChange" class="w-full">
      <TabsList class="grid w-full grid-cols-4">
        <TabsTrigger
          v-for="tab in searchTabs"
          :key="tab.value"
          :value="tab.value"
          class="flex items-center space-x-2"
        >
          <component :is="tab.icon" class="w-4 h-4" />
          <span>{{ tab.label }}</span>
          <Badge
            v-if="tab.count !== undefined"
            variant="secondary"
            class="ml-1 px-1 py-0 text-xs"
          >
            {{ tab.count }}
          </Badge>
        </TabsTrigger>
      </TabsList>

      <div class="mt-4">
        <TabsContent
          v-for="tab in searchTabs"
          :key="tab.value"
          :value="tab.value"
          class="mt-0"
        >
          <div class="search-tab-content">
            <!-- 全部结果 -->
            <div v-if="tab.value === 'all'" class="space-y-4">
              <div class="results-summary">
                <p class="text-sm text-gray-600 dark:text-gray-400">
                  找到 <span class="font-semibold text-gray-900 dark:text-gray-100">{{ totalResults }}</span> 个相关结果
                </p>
              </div>
              <slot name="all-results" />
            </div>

            <!-- 银行产品 -->
            <div v-else-if="tab.value === 'products'" class="space-y-4">
              <div class="results-summary">
                <p class="text-sm text-gray-600 dark:text-gray-400">
                  找到 <span class="font-semibold text-gray-900 dark:text-gray-100">{{ productResults.length }}</span> 个产品
                </p>
              </div>
              <slot name="product-results" :results="productResults" />
            </div>

            <!-- 服务介绍 -->
            <div v-else-if="tab.value === 'services'" class="space-y-4">
              <div class="results-summary">
                <p class="text-sm text-gray-600 dark:text-gray-400">
                  找到 <span class="font-semibold text-gray-900 dark:text-gray-100">{{ serviceResults.length }}</span> 个服务
                </p>
              </div>
              <slot name="service-results" :results="serviceResults" />
            </div>

            <!-- 活动信息 -->
            <div v-else-if="tab.value === 'activities'" class="space-y-4">
              <div class="results-summary">
                <p class="text-sm text-gray-600 dark:text-gray-400">
                  找到 <span class="font-semibold text-gray-900 dark:text-gray-100">{{ activityResults.length }}</span> 个活动
                </p>
              </div>
              <slot name="activity-results" :results="activityResults" />
            </div>

            <!-- 无结果状态 -->
            <div
              v-if="getTabResults(tab.value).length === 0 && !loading"
              class="empty-state text-center py-12"
            >
              <div class="text-gray-400 dark:text-gray-600 mb-4">
                <component :is="tab.icon" class="w-16 h-16 mx-auto" />
              </div>
              <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">
                暂无{{ tab.label }}结果
              </h3>
              <p class="text-gray-500 dark:text-gray-400">
                尝试调整搜索关键词或筛选条件
              </p>
            </div>

            <!-- 加载状态 -->
            <div v-if="loading" class="loading-state text-center py-12">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
              <p class="text-gray-500 dark:text-gray-400">正在搜索{{ tab.label }}...</p>
            </div>
          </div>
        </TabsContent>
      </div>
    </Tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Tabs, TabsContent, TabsList, TabsTrigger, Badge } from '@/components/ui'
import {
  Search,
  CreditCard,
  Headphones,
  Calendar
} from 'lucide-vue-next'

// 搜索结果接口
interface SearchResult {
  id: string
  title: string
  content: string
  type: string
  url?: string
  timestamp?: number
  score?: number
}

// 搜索标签接口
interface SearchTab {
  value: string
  label: string
  icon: any
  count?: number
}

// 组件属性
interface Props {
  results?: SearchResult[]
  loading?: boolean
  defaultTab?: string
}

// 组件事件
interface Emits {
  (e: 'tabChange', tab: string): void
  (e: 'update:modelValue', tab: string): void
}

const props = withDefaults(defineProps<Props>(), {
  results: () => [],
  loading: false,
  defaultTab: 'all'
})

const emit = defineEmits<Emits>()

// 当前活跃标签
const activeTab = ref(props.defaultTab)

// 定义搜索标签
const searchTabs = computed<SearchTab[]>(() => [
  {
    value: 'all',
    label: '全部',
    icon: Search,
    count: totalResults.value
  },
  {
    value: 'products',
    label: '产品',
    icon: CreditCard,
    count: productResults.value.length
  },
  {
    value: 'services',
    label: '服务',
    icon: Headphones,
    count: serviceResults.value.length
  },
  {
    value: 'activities',
    label: '活动',
    icon: Calendar,
    count: activityResults.value.length
  }
])

// 计算不同类型的搜索结果
const productResults = computed(() => {
  return props.results.filter(result => result.type === 'product')
})

const serviceResults = computed(() => {
  return props.results.filter(result => result.type === 'service')
})

const activityResults = computed(() => {
  return props.results.filter(result => result.type === 'activity')
})

const totalResults = computed(() => {
  return props.results.length
})

// 获取指定标签的结果
const getTabResults = (tabValue: string) => {
  switch (tabValue) {
    case 'all':
      return props.results
    case 'products':
      return productResults.value
    case 'services':
      return serviceResults.value
    case 'activities':
      return activityResults.value
    default:
      return []
  }
}

// 处理标签切换
const handleTabChange = (newTab: string) => {
  activeTab.value = newTab
  emit('tabChange', newTab)
  emit('update:modelValue', newTab)
}

// 监听默认标签变化
watch(
  () => props.defaultTab,
  (newTab) => {
    if (newTab && newTab !== activeTab.value) {
      activeTab.value = newTab
    }
  }
)

// 暴露组件方法
defineExpose({
  activeTab,
  setActiveTab: (tab: string) => {
    activeTab.value = tab
  },
  getTabResults
})
</script>

<style scoped>
.search-tabs {
  @apply w-full;
}

.search-tab-content {
  @apply min-h-[400px];
}

.results-summary {
  @apply border-b border-gray-200 dark:border-gray-700 pb-3 mb-4;
}

.empty-state svg {
  @apply opacity-30;
}

.loading-state {
  @apply text-gray-500 dark:text-gray-400;
}

/* 自定义标签样式 */
.search-tabs :deep(.tab-trigger) {
  @apply transition-all duration-200;
}

.search-tabs :deep(.tab-trigger:hover) {
  @apply bg-gray-50 dark:bg-gray-700;
}

.search-tabs :deep(.tab-trigger[data-state="active"]) {
  @apply bg-white dark:bg-gray-800 border-b-2 border-primary;
}
</style>