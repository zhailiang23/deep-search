<template>
  <div class="search-results">
    <!-- 结果概览 -->
    <div v-if="results.length > 0" class="mb-6">
      <div class="flex items-center justify-between">
        <div class="text-sm text-muted-foreground">
          找到 {{ totalResults.toLocaleString() }} 个结果
          <span v-if="searchTime">(用时 {{ searchTime }}ms)</span>
        </div>
        <div v-if="showResultsPerPage" class="flex items-center space-x-2 text-sm">
          <span class="text-muted-foreground">每页显示:</span>
          <select
            v-model="selectedPageSize"
            @change="handlePageSizeChange"
            class="px-2 py-1 border border-border rounded bg-background text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 搜索结果列表 -->
    <div class="space-y-4">
      <SearchResultItem
        v-for="(result, index) in results"
        :key="result.id"
        :result="result"
        :index="index"
        :highlight-query="highlightQuery"
        :show-thumbnail="showThumbnails"
        :show-metadata="showMetadata"
        :compact-mode="compactMode"
        @click="handleResultClick(result, index)"
        @bookmark="handleBookmark(result)"
        @share="handleShare(result)"
        @preview="handlePreview(result)"
      />
    </div>

    <!-- 加载更多/分页 -->
    <div class="mt-8">
      <!-- 无限滚动模式 -->
      <div v-if="infiniteScroll" class="text-center">
        <div v-if="hasMore && !loading" ref="loadMoreTrigger" class="py-4">
          <Button
            @click="handleLoadMore"
            variant="outline"
            class="min-w-32"
          >
            加载更多
          </Button>
        </div>
        <div v-if="loading" class="py-8">
          <div class="flex items-center justify-center space-x-2">
            <div class="animate-spin w-5 h-5 border-2 border-primary border-t-transparent rounded-full"></div>
            <span class="text-sm text-muted-foreground">加载中...</span>
          </div>
        </div>
        <div v-if="!hasMore && results.length > 0" class="py-4 text-center text-sm text-muted-foreground">
          已显示所有结果
        </div>
      </div>

      <!-- 分页模式 -->
      <div v-else-if="showPagination && totalPages > 1" class="flex items-center justify-center space-x-2">
        <Button
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage === 1"
          variant="outline"
          size="sm"
        >
          上一页
        </Button>

        <div class="flex items-center space-x-1">
          <template v-for="page in visiblePages" :key="page">
            <Button
              v-if="typeof page === 'number'"
              @click="goToPage(page)"
              :variant="page === currentPage ? 'default' : 'outline'"
              size="sm"
              class="min-w-10"
            >
              {{ page }}
            </Button>
            <span v-else class="px-2 text-muted-foreground">...</span>
          </template>
        </div>

        <Button
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage === totalPages"
          variant="outline"
          size="sm"
        >
          下一页
        </Button>
      </div>
    </div>

    <!-- 无结果状态 -->
    <div v-if="!loading && results.length === 0" class="text-center py-12">
      <div class="max-w-md mx-auto">
        <div class="text-6xl mb-4">🔍</div>
        <h3 class="text-xl font-semibold mb-2">未找到相关结果</h3>
        <p class="text-muted-foreground mb-6">
          尝试使用不同的关键词或检查拼写
        </p>
        <div class="space-y-3">
          <div class="text-sm text-muted-foreground text-left">
            <p class="font-medium mb-2">搜索建议:</p>
            <ul class="list-disc list-inside space-y-1">
              <li>使用更通用的关键词</li>
              <li>减少搜索词的数量</li>
              <li>检查拼写是否正确</li>
              <li>尝试使用同义词</li>
            </ul>
          </div>
          <div class="flex justify-center space-x-2">
            <Button @click="handleClearFilters" variant="outline" size="sm">
              清除筛选条件
            </Button>
            <Button @click="handleSearchSuggestion" size="sm">
              获取搜索建议
            </Button>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="text-center py-12">
      <div class="max-w-md mx-auto">
        <div class="text-6xl mb-4">⚠️</div>
        <h3 class="text-xl font-semibold mb-2 text-red-600">搜索出错</h3>
        <p class="text-muted-foreground mb-6">{{ error }}</p>
        <Button @click="handleRetry" size="sm">
          重试
        </Button>
      </div>
    </div>

    <!-- 结果预览对话框 -->
    <Dialog :open="showPreview" @update:open="showPreview = $event">
      <DialogContent class="max-w-3xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ previewResult?.title }}</DialogTitle>
          <DialogDescription>{{ previewResult?.description }}</DialogDescription>
        </DialogHeader>
        <div v-if="previewResult" class="py-4">
          <div class="space-y-4">
            <div v-if="previewResult.content" class="prose prose-sm max-w-none">
              <div v-html="previewResult.content"></div>
            </div>
            <div v-else class="text-center py-8 text-muted-foreground">
              内容加载中...
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button @click="showPreview = false" variant="outline">
            关闭
          </Button>
          <Button @click="visitResult(previewResult)">
            访问原文
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import SearchResultItem from './SearchResultItem.vue'
import { Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui'

interface SearchResult {
  id: string
  title: string
  description: string
  url: string
  type: 'product' | 'article' | 'faq' | 'page'
  score: number
  date?: Date
  category?: string
  tags?: string[]
  thumbnail?: string
  content?: string
  metadata?: Record<string, any>
}

interface Props {
  results: SearchResult[]
  loading?: boolean
  error?: string
  totalResults?: number
  currentPage?: number
  totalPages?: number
  pageSize?: number
  searchTime?: number
  highlightQuery?: string

  // 功能开关
  infiniteScroll?: boolean
  showPagination?: boolean
  showThumbnails?: boolean
  showMetadata?: boolean
  showResultsPerPage?: boolean
  compactMode?: boolean

  // 无限滚动相关
  hasMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  error: '',
  totalResults: 0,
  currentPage: 1,
  totalPages: 1,
  pageSize: 20,
  searchTime: 0,
  highlightQuery: '',
  infiniteScroll: true,
  showPagination: false,
  showThumbnails: true,
  showMetadata: true,
  showResultsPerPage: true,
  compactMode: false,
  hasMore: true
})

const emit = defineEmits<{
  'result-click': [result: SearchResult, index: number]
  'load-more': []
  'page-change': [page: number]
  'page-size-change': [size: number]
  'bookmark': [result: SearchResult]
  'share': [result: SearchResult]
  'clear-filters': []
  'retry': []
}>()

// 响应式数据
const selectedPageSize = ref(props.pageSize)
const showPreview = ref(false)
const previewResult = ref<SearchResult | null>(null)
const loadMoreTrigger = ref<HTMLElement>()

// 计算属性
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const { currentPage, totalPages } = props

  if (totalPages <= 7) {
    // 总页数少于等于7页，显示所有页码
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i)
    }
  } else {
    // 总页数大于7页，智能显示页码
    pages.push(1)

    if (currentPage > 4) {
      pages.push('...')
    }

    const start = Math.max(2, currentPage - 2)
    const end = Math.min(totalPages - 1, currentPage + 2)

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }

    if (currentPage < totalPages - 3) {
      pages.push('...')
    }

    if (totalPages > 1) {
      pages.push(totalPages)
    }
  }

  return pages
})

// 无限滚动观察器
const { stop } = useIntersectionObserver(
  loadMoreTrigger,
  ([{ isIntersecting }]) => {
    if (isIntersecting && props.hasMore && !props.loading && props.infiniteScroll) {
      // 增加小延迟避免频繁触发
      setTimeout(() => {
        if (props.hasMore && !props.loading) {
          handleLoadMore()
        }
      }, 100)
    }
  },
  {
    threshold: 0.1,
    rootMargin: '50px' // 提前50px开始加载
  }
)

// 事件处理
const handleResultClick = (result: SearchResult, index: number) => {
  emit('result-click', result, index)
}

const handleBookmark = (result: SearchResult) => {
  emit('bookmark', result)
}

const handleShare = (result: SearchResult) => {
  emit('share', result)
}

const handlePreview = async (result: SearchResult) => {
  previewResult.value = result
  showPreview.value = true

  // 如果没有内容，尝试加载
  if (!result.content) {
    try {
      // 模拟内容加载
      await new Promise(resolve => setTimeout(resolve, 1000))
      result.content = `
        <h3>${result.title}</h3>
        <p>${result.description}</p>
        <p>这里是详细的内容预览...</p>
      `
    } catch (error) {
      console.error('Failed to load preview:', error)
    }
  }
}

const handleLoadMore = () => {
  if (!props.loading && props.hasMore && props.infiniteScroll) {
    emit('load-more')
  }
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= props.totalPages && page !== props.currentPage) {
    emit('page-change', page)
  }
}

const handlePageSizeChange = () => {
  emit('page-size-change', selectedPageSize.value)
}

const handleClearFilters = () => {
  emit('clear-filters')
}

const handleSearchSuggestion = () => {
  // 可以实现搜索建议逻辑
  console.log('Getting search suggestions...')
}

const handleRetry = () => {
  emit('retry')
}

const visitResult = (result: SearchResult | null) => {
  if (result?.url) {
    window.open(result.url, '_blank')
  }
  showPreview.value = false
}

// 监听页面大小变化
watch(
  () => props.pageSize,
  (newSize) => {
    selectedPageSize.value = newSize
  }
)

// 生命周期
onMounted(() => {
  // 可以在这里初始化一些功能
})

onUnmounted(() => {
  stop()
})
</script>

<style scoped>
.search-results {
  /* 自定义样式 */
}

/* 加载动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-results {
    /* 移动端样式调整 */
  }
}
</style>