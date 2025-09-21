<template>
  <div class="search-results-page min-h-screen bg-background">
    <div class="container mx-auto px-4 py-6">
      <div class="max-w-7xl mx-auto">
        <!-- 搜索框区域 -->
        <div class="mb-6">
          <SmartSearchBox
            :initial-query="query"
            @search="handleSearch"
            @suggestion-select="handleSuggestionSelect"
            show-suggestions
            class="w-full max-w-2xl"
          />
        </div>

        <div class="flex gap-6">
          <!-- 左侧筛选器 -->
          <div class="w-64 flex-shrink-0 hidden lg:block">
            <SearchFilters
              :filters="currentFilters"
              @filter-change="handleFilterChange"
              @filter-clear="handleFilterClear"
            />
          </div>

          <!-- 主要内容区域 -->
          <div class="flex-1 min-w-0">
            <!-- 搜索结果头部 -->
            <div class="mb-6">
              <div class="flex items-center justify-between mb-4">
                <div>
                  <h1 class="text-2xl font-bold text-foreground">
                    搜索结果
                  </h1>
                  <p class="text-muted-foreground mt-1" v-if="query">
                    搜索 "{{ query }}" 找到 {{ totalResults }} 个结果
                  </p>
                </div>

                <!-- 排序选择器 -->
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-foreground">排序:</span>
                  <select
                    v-model="currentSort"
                    @change="handleSortChange"
                    class="px-3 py-1 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    <option value="relevance">相关性</option>
                    <option value="date">最新</option>
                    <option value="popularity">热门</option>
                  </select>
                </div>
              </div>

              <!-- 搜索标签栏 -->
              <SearchTabs
                :active-tab="activeTab"
                :tabs="searchTabs"
                @tab-change="handleTabChange"
              />
            </div>

            <!-- 加载状态 -->
            <div v-if="loading" class="flex justify-center py-8">
              <Loading />
            </div>

            <!-- 搜索结果 -->
            <div v-else-if="searchResults.length > 0">
              <SearchResults
                :results="searchResults"
                :loading="loadingMore"
                @load-more="handleLoadMore"
                @result-click="handleResultClick"
              />
            </div>

            <!-- 无结果状态 -->
            <div v-else class="text-center py-12">
              <div class="text-6xl mb-4">🔍</div>
              <h3 class="text-xl font-semibold mb-2">未找到相关结果</h3>
              <p class="text-muted-foreground mb-6">
                尝试使用不同的关键词或调整筛选条件
              </p>
              <div class="flex justify-center gap-4">
                <Button @click="handleClearFilters" variant="outline">
                  清除筛选条件
                </Button>
                <Button @click="handleBackToSearch">
                  返回搜索
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SmartSearchBox from '@/components/search/SmartSearchBox.vue'
import SearchFilters from '@/components/search/SearchFilters.vue'
import SearchResults from '@/components/search/SearchResults.vue'
import SearchTabs from '@/components/search/SearchTabs.vue'
// import Loading from '@/components/ui/loading.vue' // 暂时注释掉
import { Button } from '@/components/ui'

const router = useRouter()
const route = useRoute()

// Props
interface Props {
  query?: string
  filters?: string
  sort?: string
  page?: number
}

const props = withDefaults(defineProps<Props>(), {
  query: '',
  filters: '',
  sort: 'relevance',
  page: 1
})

// 响应式数据
const loading = ref(false)
const loadingMore = ref(false)
const searchResults = ref<any[]>([])
const totalResults = ref(0)
const currentPage = ref(props.page)
const currentSort = ref(props.sort)
const currentFilters = ref<Record<string, any>>({})
const activeTab = ref('all')

// 搜索标签配置
const searchTabs = ref([
  { key: 'all', label: '全部', count: 0 },
  { key: 'products', label: '产品服务', count: 0 },
  { key: 'articles', label: '文章资讯', count: 0 },
  { key: 'faqs', label: '常见问题', count: 0 }
])

// 计算属性
const query = computed(() => props.query || route.query.q as string || '')

// 监听路由变化
watch(
  () => route.query,
  (newQuery) => {
    if (newQuery.q !== query.value) {
      performSearch()
    }
  },
  { immediate: true }
)

// 组件挂载
onMounted(() => {
  initializeFilters()
  if (query.value) {
    performSearch()
  }
})

// 初始化筛选器
const initializeFilters = () => {
  try {
    if (props.filters) {
      currentFilters.value = JSON.parse(props.filters)
    }
  } catch (e) {
    console.warn('Invalid filters format:', props.filters)
  }
}

// 执行搜索
const performSearch = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 800))

    // 模拟搜索结果
    const mockResults = generateMockResults()
    searchResults.value = mockResults
    totalResults.value = mockResults.length

    // 更新标签计数
    updateTabCounts()
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    loading.value = false
  }
}

// 生成模拟搜索结果
const generateMockResults = () => {
  const results = []
  for (let i = 1; i <= 20; i++) {
    results.push({
      id: i,
      title: `搜索结果标题 ${i} - ${query.value}`,
      description: `这是关于${query.value}的详细描述，包含了相关的银行产品和服务信息...`,
      url: `/result/${i}`,
      type: ['products', 'articles', 'faqs'][Math.floor(Math.random() * 3)],
      score: Math.random(),
      date: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
      category: '银行服务',
      tags: ['热门', '推荐', '新品'].slice(0, Math.floor(Math.random() * 3) + 1)
    })
  }
  return results
}

// 更新标签计数
const updateTabCounts = () => {
  searchTabs.value.forEach(tab => {
    if (tab.key === 'all') {
      tab.count = searchResults.value.length
    } else {
      tab.count = searchResults.value.filter(result => result.type === tab.key).length
    }
  })
}

// 处理搜索
const handleSearch = (newQuery: string) => {
  router.push({
    name: 'SearchResults',
    query: {
      q: newQuery,
      filters: JSON.stringify(currentFilters.value),
      sort: currentSort.value
    }
  })
}

// 处理搜索建议选择
const handleSuggestionSelect = (suggestion: string) => {
  handleSearch(suggestion)
}

// 处理筛选器变化
const handleFilterChange = (filters: Record<string, any>) => {
  currentFilters.value = filters
  updateSearchQuery()
}

// 处理筛选器清除
const handleFilterClear = () => {
  currentFilters.value = {}
  updateSearchQuery()
}

// 处理排序变化
const handleSortChange = () => {
  updateSearchQuery()
}

// 处理标签切换
const handleTabChange = (tabKey: string) => {
  activeTab.value = tabKey
  // 可以根据标签筛选结果
  performSearch()
}

// 处理加载更多
const handleLoadMore = async () => {
  loadingMore.value = true
  try {
    // 模拟加载更多
    await new Promise(resolve => setTimeout(resolve, 500))
    const moreResults = generateMockResults()
    searchResults.value.push(...moreResults)
  } catch (error) {
    console.error('Load more failed:', error)
  } finally {
    loadingMore.value = false
  }
}

// 处理结果点击
const handleResultClick = (result: any) => {
  // 处理搜索结果点击事件
  console.log('Result clicked:', result)
}

// 更新搜索查询
const updateSearchQuery = () => {
  router.push({
    name: 'SearchResults',
    query: {
      q: query.value,
      filters: Object.keys(currentFilters.value).length > 0
        ? JSON.stringify(currentFilters.value)
        : undefined,
      sort: currentSort.value
    }
  })
}

// 清除筛选条件
const handleClearFilters = () => {
  currentFilters.value = {}
  updateSearchQuery()
}

// 返回搜索页面
const handleBackToSearch = () => {
  router.push({ name: 'Search' })
}
</script>

<style scoped>
.search-results-page {
  /* 自定义样式 */
}
</style>