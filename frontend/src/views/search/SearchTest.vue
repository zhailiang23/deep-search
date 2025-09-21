<template>
  <div class="search-test-page">
    <div class="container mx-auto px-4 py-8">
      <!-- 页面标题 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-foreground mb-2">搜索组件集成测试</h1>
        <p class="text-muted-foreground">
          测试所有搜索相关组件的功能和交互
        </p>
      </div>

      <!-- 测试控制面板 -->
      <div class="bg-background border border-border rounded-lg p-6 mb-8">
        <h2 class="text-xl font-semibold mb-4">测试控制</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Button @click="generateMockResults" :loading="generating" class="w-full">
            <RefreshCw class="w-4 h-4 mr-2" />
            生成模拟数据
          </Button>
          <Button @click="clearResults" variant="outline" class="w-full">
            <Trash2 class="w-4 h-4 mr-2" />
            清除结果
          </Button>
          <Button @click="toggleInfiniteScroll" variant="outline" class="w-full">
            <ToggleLeft class="w-4 h-4 mr-2" />
            切换滚动模式
          </Button>
          <Button @click="exportTestResults" variant="outline" class="w-full">
            <Download class="w-4 h-4 mr-2" />
            导出测试结果
          </Button>
        </div>
      </div>

      <!-- 主要搜索区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <!-- 左侧筛选器 -->
        <div class="lg:col-span-1">
          <SearchFilters
            v-model="currentFilters"
            @filtersChanged="handleFiltersChange"
          />

          <!-- 搜索历史 -->
          <div class="mt-6">
            <SearchHistory
              @selectHistory="handleHistorySelect"
              @reSearch="handleReSearch"
            />
          </div>
        </div>

        <!-- 主要内容区域 -->
        <div class="lg:col-span-3 space-y-6">
          <!-- 搜索框 -->
          <div class="search-section">
            <SmartSearchBox
              ref="searchBoxRef"
              v-model:query="searchQuery"
              :size="searchBoxSize"
              :showSuggestions="showSuggestions"
              :enableVoiceSearch="enableVoiceSearch"
              @search="handleSearch"
              @query-change="handleQueryChange"
              @suggestion-select="handleSuggestionSelect"
            />
          </div>

          <!-- 搜索状态指示器 -->
          <div v-if="searchQuery" class="search-status bg-muted/30 rounded-lg p-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium">当前搜索: "{{ searchQuery }}"</span>
              <Badge :variant="isLoading ? 'default' : 'secondary'">
                {{ isLoading ? '搜索中' : '已完成' }}
              </Badge>
            </div>
            <div class="text-xs text-muted-foreground">
              <span>筛选器: {{ Object.keys(activeFilters).length }} 项</span>
              <span class="mx-2">•</span>
              <span>结果: {{ searchResults.length }} 条</span>
              <span class="mx-2">•</span>
              <span>模式: {{ infiniteScroll ? '无限滚动' : '分页' }}</span>
            </div>
          </div>

          <!-- 搜索结果标签 -->
          <div v-if="searchResults.length > 0">
            <SearchTabs
              :results="searchResults"
              :loading="isLoading"
              :defaultTab="activeTab"
              @tabChange="handleTabChange"
            >
              <template #all-results>
                <SearchResults
                  :results="searchResults"
                  :loading="isLoading"
                  :totalResults="totalResults"
                  :hasMore="hasMore"
                  :infiniteScroll="infiniteScroll"
                  :highlightQuery="searchQuery"
                  @result-click="handleResultClick"
                  @load-more="handleLoadMore"
                  @bookmark="handleBookmark"
                  @share="handleShare"
                />
              </template>

              <template #product-results="{ results }">
                <SearchResults
                  :results="results"
                  :loading="isLoading"
                  :totalResults="results.length"
                  :hasMore="false"
                  :infiniteScroll="false"
                  :highlightQuery="searchQuery"
                  @result-click="handleResultClick"
                  @bookmark="handleBookmark"
                  @share="handleShare"
                />
              </template>

              <template #service-results="{ results }">
                <SearchResults
                  :results="results"
                  :loading="isLoading"
                  :totalResults="results.length"
                  :hasMore="false"
                  :infiniteScroll="false"
                  :highlightQuery="searchQuery"
                  @result-click="handleResultClick"
                  @bookmark="handleBookmark"
                  @share="handleShare"
                />
              </template>

              <template #activity-results="{ results }">
                <SearchResults
                  :results="results"
                  :loading="isLoading"
                  :totalResults="results.length"
                  :hasMore="false"
                  :infiniteScroll="false"
                  :highlightQuery="searchQuery"
                  @result-click="handleResultClick"
                  @bookmark="handleBookmark"
                  @share="handleShare"
                />
              </template>
            </SearchTabs>
          </div>

          <!-- 无搜索状态 -->
          <div v-else-if="!searchQuery" class="text-center py-16">
            <Search class="w-16 h-16 mx-auto text-muted-foreground mb-4" />
            <h3 class="text-xl font-semibold mb-2">开始搜索</h3>
            <p class="text-muted-foreground mb-6">
              在上方搜索框中输入关键词，或点击生成模拟数据进行测试
            </p>
            <div class="flex items-center justify-center space-x-4">
              <Button @click="searchBoxRef?.focus()" variant="outline">
                <Search class="w-4 h-4 mr-2" />
                开始搜索
              </Button>
              <Button @click="generateMockResults">
                <Plus class="w-4 h-4 mr-2" />
                生成测试数据
              </Button>
            </div>
          </div>

          <!-- 测试日志 -->
          <div v-if="testLogs.length > 0" class="test-logs bg-background border border-border rounded-lg p-4">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-semibold">测试日志</h3>
              <Button @click="clearLogs" size="sm" variant="ghost">
                <Trash2 class="w-4 h-4" />
              </Button>
            </div>
            <div class="space-y-2 max-h-48 overflow-y-auto">
              <div
                v-for="(log, index) in testLogs"
                :key="index"
                :class="[
                  'text-xs p-2 rounded border-l-2',
                  log.type === 'success' && 'bg-green-50 border-green-400 text-green-800',
                  log.type === 'error' && 'bg-red-50 border-red-400 text-red-800',
                  log.type === 'info' && 'bg-blue-50 border-blue-400 text-blue-800'
                ]"
              >
                <div class="flex items-center justify-between">
                  <span>{{ log.message }}</span>
                  <span class="text-muted-foreground">{{ formatTime(log.timestamp) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button, Badge } from '@/components/ui'
import {
  Search,
  RefreshCw,
  Trash2,
  ToggleLeft,
  Download,
  Plus
} from 'lucide-vue-next'

// 搜索组件导入
import SmartSearchBox from '@/components/search/SmartSearchBox.vue'
import SearchFilters from '@/components/search/SearchFilters.vue'
import SearchHistory from '@/components/search/SearchHistory.vue'
import SearchResults from '@/components/search/SearchResults.vue'
import SearchTabs from '@/components/search/SearchTabs.vue'

// 接口定义
interface SearchResult {
  id: string
  title: string
  description: string
  url: string
  type: 'product' | 'service' | 'activity' | 'page'
  score: number
  date?: Date
  category?: string
  tags?: string[]
  thumbnail?: string
}

interface TestLog {
  type: 'success' | 'error' | 'info'
  message: string
  timestamp: number
}

// 响应式数据
const searchBoxRef = ref()
const searchQuery = ref('')
const searchResults = ref<SearchResult[]>([])
const isLoading = ref(false)
const generating = ref(false)
const totalResults = ref(0)
const hasMore = ref(false)
const infiniteScroll = ref(true)
const activeTab = ref('all')

// 搜索配置
const searchBoxSize = ref<'small' | 'medium' | 'large'>('medium')
const showSuggestions = ref(true)
const enableVoiceSearch = ref(true)

// 筛选器和历史
const currentFilters = ref({
  contentTypes: [],
  timeRange: 'all',
  sortBy: 'relevance',
  permissionLevels: []
})

// 测试日志
const testLogs = ref<TestLog[]>([])

// 计算属性
const activeFilters = computed(() => {
  const filters: Record<string, any> = {}
  if (currentFilters.value.contentTypes.length > 0) {
    filters.contentTypes = currentFilters.value.contentTypes
  }
  if (currentFilters.value.timeRange !== 'all') {
    filters.timeRange = currentFilters.value.timeRange
  }
  if (currentFilters.value.sortBy !== 'relevance') {
    filters.sortBy = currentFilters.value.sortBy
  }
  if (currentFilters.value.permissionLevels.length > 0) {
    filters.permissionLevels = currentFilters.value.permissionLevels
  }
  return filters
})

// 方法
const addLog = (type: TestLog['type'], message: string) => {
  testLogs.value.unshift({
    type,
    message,
    timestamp: Date.now()
  })

  if (testLogs.value.length > 50) {
    testLogs.value = testLogs.value.slice(0, 50)
  }
}

const handleSearch = (query: string, type?: string) => {
  searchQuery.value = query
  addLog('info', `开始搜索: "${query}"${type ? ` (类型: ${type})` : ''}`)
  performSearch(query)
}

const handleQueryChange = (query: string) => {
  // 实时查询变化
}

const handleSuggestionSelect = (suggestion: string) => {
  addLog('success', `选择了搜索建议: "${suggestion}"`)
  performSearch(suggestion)
}

const handleFiltersChange = (filters: any) => {
  addLog('info', `筛选器已更新: ${JSON.stringify(filters)}`)
  if (searchQuery.value) {
    performSearch(searchQuery.value)
  }
}

const handleHistorySelect = (item: any) => {
  addLog('info', `从历史中选择: "${item.query}"`)
  searchQuery.value = item.query
  performSearch(item.query)
}

const handleReSearch = (item: any) => {
  addLog('info', `重新搜索: "${item.query}"`)
  searchQuery.value = item.query
  performSearch(item.query)
}

const handleTabChange = (tab: string) => {
  activeTab.value = tab
  addLog('info', `切换到标签: ${tab}`)
}

const handleResultClick = (result: SearchResult, index: number) => {
  addLog('success', `点击了结果 #${index + 1}: "${result.title}"`)
}

const handleLoadMore = () => {
  addLog('info', '触发加载更多')
  loadMoreResults()
}

const handleBookmark = (result: SearchResult) => {
  addLog('success', `收藏了: "${result.title}"`)
}

const handleShare = (result: SearchResult) => {
  addLog('success', `分享了: "${result.title}"`)
}

const performSearch = async (query: string) => {
  isLoading.value = true

  try {
    // 模拟搜索延迟
    await new Promise(resolve => setTimeout(resolve, 800))

    // 生成模拟结果
    const mockResults = generateMockSearchResults(query, 20)
    searchResults.value = mockResults
    totalResults.value = mockResults.length + Math.floor(Math.random() * 200)
    hasMore.value = searchResults.value.length < totalResults.value

    addLog('success', `搜索完成: 找到 ${mockResults.length} 个结果`)
  } catch (error) {
    addLog('error', `搜索失败: ${error}`)
  } finally {
    isLoading.value = false
  }
}

const loadMoreResults = async () => {
  if (isLoading.value || !hasMore.value) return

  isLoading.value = true

  try {
    await new Promise(resolve => setTimeout(resolve, 500))

    const moreResults = generateMockSearchResults(searchQuery.value, 10)
    searchResults.value.push(...moreResults)
    hasMore.value = searchResults.value.length < totalResults.value

    addLog('success', `加载了 ${moreResults.length} 个更多结果`)
  } catch (error) {
    addLog('error', `加载更多失败: ${error}`)
  } finally {
    isLoading.value = false
  }
}

const generateMockResults = async () => {
  generating.value = true

  try {
    await new Promise(resolve => setTimeout(resolve, 1000))

    const query = searchQuery.value || '测试'
    const mockResults = generateMockSearchResults(query, 15)
    searchResults.value = mockResults
    totalResults.value = mockResults.length + 50
    hasMore.value = true

    if (!searchQuery.value) {
      searchQuery.value = '测试'
    }

    addLog('success', `生成了 ${mockResults.length} 个模拟结果`)
  } finally {
    generating.value = false
  }
}

const generateMockSearchResults = (query: string, count: number): SearchResult[] => {
  const types: SearchResult['type'][] = ['product', 'service', 'activity', 'page']
  const categories = ['信用卡', '存贷款', '理财', '保险', '支付']

  return Array.from({ length: count }, (_, index) => ({
    id: `result-${Date.now()}-${index}`,
    title: `${query}相关${types[index % types.length]} #${index + 1}`,
    description: `这是关于"${query}"的详细描述。包含了相关的产品信息、申请条件、使用方法等重要内容。`,
    url: `https://example.com/results/${index + 1}`,
    type: types[index % types.length],
    score: Math.random(),
    date: new Date(Date.now() - Math.random() * 90 * 24 * 60 * 60 * 1000),
    category: categories[index % categories.length],
    tags: [`标签${index + 1}`, `${query}相关`, '热门'],
    thumbnail: index % 3 === 0 ? `https://picsum.photos/200/150?random=${index}` : undefined
  }))
}

const clearResults = () => {
  searchResults.value = []
  searchQuery.value = ''
  totalResults.value = 0
  hasMore.value = false
  addLog('info', '清除了所有搜索结果')
}

const toggleInfiniteScroll = () => {
  infiniteScroll.value = !infiniteScroll.value
  addLog('info', `切换到${infiniteScroll.value ? '无限滚动' : '分页'}模式`)
}

const exportTestResults = () => {
  const data = {
    timestamp: new Date().toISOString(),
    searchQuery: searchQuery.value,
    resultCount: searchResults.value.length,
    filters: activeFilters.value,
    logs: testLogs.value.slice(0, 20)
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `search-test-${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  addLog('success', '导出了测试结果')
}

const clearLogs = () => {
  testLogs.value = []
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString()
}

// 生命周期
onMounted(() => {
  addLog('info', '搜索组件测试页面已加载')
})
</script>

<style scoped>
.search-test-page {
  min-height: 100vh;
  background: rgb(var(--background));
}

.container {
  max-width: 1400px;
}

.search-section {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgb(var(--background));
  padding: 1rem 0;
  margin: -1rem 0 1rem 0;
}

.test-logs {
  max-height: 300px;
}

/* 滚动条样式 */
.test-logs ::-webkit-scrollbar {
  width: 6px;
}

.test-logs ::-webkit-scrollbar-track {
  background: rgb(var(--muted));
}

.test-logs ::-webkit-scrollbar-thumb {
  background: rgb(var(--muted-foreground));
  border-radius: 3px;
}

.test-logs ::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--foreground));
}
</style>