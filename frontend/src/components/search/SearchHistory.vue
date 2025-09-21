<template>
  <div class="search-history">
    <div class="history-header">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">搜索历史</h3>
      <Button
        variant="ghost"
        size="sm"
        @click="clearHistory"
        class="text-sm text-gray-500 hover:text-gray-700"
        v-if="historyItems.length > 0"
      >
        清除历史
      </Button>
    </div>

    <div class="history-content">
      <!-- 无历史记录状态 -->
      <div
        v-if="historyItems.length === 0"
        class="empty-state text-center py-8"
      >
        <div class="text-gray-400 dark:text-gray-600 mb-2">
          <svg class="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <p class="text-sm text-gray-500 dark:text-gray-400">暂无搜索历史</p>
      </div>

      <!-- 历史记录列表 -->
      <div v-else class="space-y-2">
        <div
          v-for="(item, index) in historyItems"
          :key="index"
          :class="[
            'history-item group flex items-center justify-between rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer transition-colors',
            mobileMode ? 'p-4 mobile-touch-target' : 'p-3'
          ]"
          @click="selectHistoryItem(item)"
        >
          <div class="flex-1 min-w-0">
            <div class="flex items-center space-x-2">
              <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-sm text-gray-700 dark:text-gray-300 truncate">
                {{ item.query }}
              </span>
            </div>
            <div class="flex items-center space-x-4 mt-1">
              <span class="text-xs text-gray-500 dark:text-gray-400">
                {{ formatTime(item.timestamp) }}
              </span>
              <span v-if="item.resultCount" class="text-xs text-gray-500 dark:text-gray-400">
                {{ item.resultCount }} 个结果
              </span>
            </div>
          </div>

          <div class="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <Button
              variant="ghost"
              size="sm"
              @click.stop="reSearch(item)"
              :class="mobileMode ? 'h-10 w-10 p-0 mobile-touch-target' : 'h-8 w-8 p-0'"
              title="重新搜索"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </Button>
            <Button
              variant="ghost"
              size="sm"
              @click.stop="removeHistoryItem(index)"
              :class="[
                'p-0 text-red-500 hover:text-red-700',
                mobileMode ? 'h-10 w-10 mobile-touch-target' : 'h-8 w-8'
              ]"
              title="删除"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </Button>
          </div>
        </div>
      </div>

      <!-- 显示更多按钮 -->
      <div v-if="hasMore" class="text-center pt-4">
        <Button
          variant="outline"
          size="sm"
          @click="loadMore"
          :loading="loading"
        >
          加载更多
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Button } from '@/components/ui'

// 搜索历史项接口
interface SearchHistoryItem {
  query: string
  timestamp: number
  resultCount?: number
  filters?: Record<string, any>
}

// 组件属性
interface Props {
  maxItems?: number
  mobileMode?: boolean
}

// 组件事件
interface Emits {
  (e: 'history-select', query: string): void
  (e: 're-search', item: SearchHistoryItem): void
}

const props = withDefaults(defineProps<Props>(), {
  maxItems: 10,
  mobileMode: false
})

const emit = defineEmits<Emits>()

// 状态管理
const historyItems = ref<SearchHistoryItem[]>([])
const loading = ref(false)
const displayLimit = ref(props.maxItems)

// 计算属性
const hasMore = computed(() => {
  return historyItems.value.length > displayLimit.value
})

// 获取搜索历史
const loadHistory = () => {
  try {
    const stored = localStorage.getItem('searchHistory')
    if (stored) {
      const parsed = JSON.parse(stored)
      historyItems.value = Array.isArray(parsed) ? parsed : []
    }
  } catch (error) {
    console.error('加载搜索历史失败:', error)
    historyItems.value = []
  }
}

// 保存搜索历史
const saveHistory = () => {
  try {
    localStorage.setItem('searchHistory', JSON.stringify(historyItems.value))
  } catch (error) {
    console.error('保存搜索历史失败:', error)
  }
}

// 添加搜索记录
const addHistoryItem = (item: SearchHistoryItem) => {
  // 移除重复项
  const existingIndex = historyItems.value.findIndex(h => h.query === item.query)
  if (existingIndex !== -1) {
    historyItems.value.splice(existingIndex, 1)
  }

  // 添加到开头
  historyItems.value.unshift(item)

  // 限制最大数量
  if (historyItems.value.length > 50) {
    historyItems.value = historyItems.value.slice(0, 50)
  }

  saveHistory()
}

// 选择历史项
const selectHistoryItem = (item: SearchHistoryItem) => {
  emit('history-select', item.query)
}

// 重新搜索
const reSearch = (item: SearchHistoryItem) => {
  emit('re-search', item)
}

// 删除历史项
const removeHistoryItem = (index: number) => {
  historyItems.value.splice(index, 1)
  saveHistory()
}

// 清除所有历史
const clearHistory = () => {
  historyItems.value = []
  localStorage.removeItem('searchHistory')
}

// 加载更多
const loadMore = () => {
  displayLimit.value += props.maxItems
}

// 格式化时间
const formatTime = (timestamp: number) => {
  const now = Date.now()
  const diff = now - timestamp

  if (diff < 60 * 1000) {
    return '刚刚'
  } else if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))}分钟前`
  } else if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  } else if (diff < 30 * 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
  } else {
    return new Date(timestamp).toLocaleDateString()
  }
}

// 组件挂载时加载历史
onMounted(() => {
  loadHistory()
})

// 暴露组件方法
defineExpose({
  addHistoryItem,
  clearHistory,
  loadHistory
})
</script>

<style scoped>
.search-history {
  @apply bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4;
}

.history-header {
  @apply flex items-center justify-between pb-3 border-b border-gray-200 dark:border-gray-700;
}

.history-content {
  @apply pt-4;
}

.history-item {
  @apply border border-transparent hover:border-gray-200 dark:hover:border-gray-600;
}

.empty-state svg {
  @apply opacity-50;
}
</style>