import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { debounce } from 'lodash-es'
import { searchApi } from '@/lib/api/search'
import type {
  SearchResult,
  SearchSuggestion,
  SearchFilters,
  SearchOptions,
  SearchAnalytics
} from '@/types/search'

export const useSearchStore = defineStore('search', () => {
  // 状态
  const query: Ref<string> = ref('')
  const results: Ref<SearchResult[]> = ref([])
  const suggestions: Ref<SearchSuggestion[]> = ref([])
  const filters: Ref<SearchFilters> = ref({})
  const availableFilters: Ref<any[]> = ref([])

  // 加载状态
  const loading = ref(false)
  const loadingMore = ref(false)
  const loadingSuggestions = ref(false)

  // 分页状态
  const currentPage = ref(1)
  const pageSize = ref(20)
  const totalCount = ref(0)
  const totalPages = ref(0)

  // 错误状态
  const error: Ref<string | null> = ref(null)

  // 搜索选项
  const sortBy = ref<'relevance' | 'date' | 'popularity'>('relevance')
  const sortOrder = ref<'asc' | 'desc'>('desc')

  // 搜索历史
  const searchHistory: Ref<string[]> = ref([])
  const maxHistorySize = 50

  // 性能指标
  const searchTime = ref(0)
  const lastSearchTimestamp = ref(0)

  // 计算属性
  const hasResults = computed(() => results.value.length > 0)
  const hasMore = computed(() => currentPage.value < totalPages.value)
  const isEmpty = computed(() => !loading.value && !loadingMore.value && results.value.length === 0)
  const isFirstPage = computed(() => currentPage.value === 1)

  // 搜索历史管理
  const addToHistory = (searchQuery: string) => {
    if (!searchQuery.trim()) return

    const trimmedQuery = searchQuery.trim()
    const index = searchHistory.value.indexOf(trimmedQuery)

    if (index > -1) {
      searchHistory.value.splice(index, 1)
    }

    searchHistory.value.unshift(trimmedQuery)

    if (searchHistory.value.length > maxHistorySize) {
      searchHistory.value.splice(maxHistorySize)
    }

    saveHistoryToLocalStorage()
  }

  const removeFromHistory = (searchQuery: string) => {
    const index = searchHistory.value.indexOf(searchQuery)
    if (index > -1) {
      searchHistory.value.splice(index, 1)
      saveHistoryToLocalStorage()
    }
  }

  const clearHistory = () => {
    searchHistory.value = []
    localStorage.removeItem('search-history')
  }

  const saveHistoryToLocalStorage = () => {
    try {
      localStorage.setItem('search-history', JSON.stringify(searchHistory.value))
    } catch (error) {
      console.warn('Failed to save search history to localStorage:', error)
    }
  }

  const loadHistoryFromLocalStorage = () => {
    try {
      const saved = localStorage.getItem('search-history')
      if (saved) {
        searchHistory.value = JSON.parse(saved)
      }
    } catch (error) {
      console.warn('Failed to load search history from localStorage:', error)
    }
  }

  // 主要搜索功能
  const search = async (searchQuery: string, options: SearchOptions = {}) => {
    if (!searchQuery.trim()) return

    loading.value = true
    error.value = null

    const startTime = Date.now()

    try {
      query.value = searchQuery.trim()

      // 如果是新搜索，重置分页
      if (!options.loadMore) {
        currentPage.value = 1
        results.value = []
      }

      const searchParams = {
        query: query.value,
        page: currentPage.value,
        pageSize: pageSize.value,
        sortBy: options.sortBy || sortBy.value,
        sortOrder: options.sortOrder || sortOrder.value,
        filters: { ...filters.value, ...options.filters }
      }

      const response = await searchApi.search(searchParams)

      if (options.loadMore) {
        results.value.push(...response.results)
      } else {
        results.value = response.results
      }

      totalCount.value = response.totalCount
      totalPages.value = response.totalPages
      availableFilters.value = response.availableFilters || []

      // 添加到搜索历史
      addToHistory(query.value)

      // 记录性能指标
      searchTime.value = (Date.now() - startTime) / 1000
      lastSearchTimestamp.value = Date.now()

      // 发送搜索分析数据
      trackSearch(query.value, response.totalCount)

    } catch (err: any) {
      error.value = err.message || '搜索请求失败'
      console.error('Search error:', err)
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  // 加载更多结果
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return

    loadingMore.value = true
    currentPage.value += 1

    await search(query.value, { loadMore: true })
  }

  // 获取搜索建议
  const getSuggestions = async (searchQuery: string): Promise<SearchSuggestion[]> => {
    if (!searchQuery.trim() || searchQuery.length < 2) {
      suggestions.value = []
      return []
    }

    loadingSuggestions.value = true

    try {
      const response = await searchApi.getSuggestions(searchQuery.trim())
      suggestions.value = response.suggestions
      return response.suggestions
    } catch (err: any) {
      console.error('Failed to get suggestions:', err)
      return []
    } finally {
      loadingSuggestions.value = false
    }
  }

  // 防抖搜索建议
  const debouncedGetSuggestions = debounce(getSuggestions, 300)

  // 筛选器管理
  const setFilters = (newFilters: SearchFilters) => {
    filters.value = { ...newFilters }
  }

  const updateFilter = (key: string, value: any) => {
    filters.value = { ...filters.value, [key]: value }
  }

  const removeFilter = (key: string) => {
    const newFilters = { ...filters.value }
    delete newFilters[key]
    filters.value = newFilters
  }

  const clearFilters = () => {
    filters.value = {}
  }

  // 排序设置
  const setSort = (sort: string, order: 'asc' | 'desc' = 'desc') => {
    sortBy.value = sort as 'relevance' | 'date' | 'popularity'
    sortOrder.value = order
  }

  // 搜索分析跟踪
  const trackSearch = async (searchQuery: string, resultCount: number) => {
    try {
      await searchApi.trackSearch({
        query: searchQuery,
        resultCount,
        timestamp: Date.now(),
        filters: filters.value,
        sortBy: sortBy.value
      })
    } catch (error) {
      console.warn('Failed to track search:', error)
    }
  }

  const trackClick = async (analytics: SearchAnalytics) => {
    try {
      await searchApi.trackClick(analytics)
    } catch (error) {
      console.warn('Failed to track click:', error)
    }
  }

  // 重置搜索状态
  const reset = () => {
    query.value = ''
    results.value = []
    suggestions.value = []
    filters.value = {}
    currentPage.value = 1
    totalCount.value = 0
    totalPages.value = 0
    error.value = null
    loading.value = false
    loadingMore.value = false
    loadingSuggestions.value = false
  }

  // 初始化
  const initialize = () => {
    loadHistoryFromLocalStorage()
  }

  return {
    // 状态
    query,
    results,
    suggestions,
    filters,
    availableFilters,
    loading,
    loadingMore,
    loadingSuggestions,
    currentPage,
    pageSize,
    totalCount,
    totalPages,
    error,
    sortBy,
    sortOrder,
    searchHistory,
    searchTime,
    lastSearchTimestamp,

    // 计算属性
    hasResults,
    hasMore,
    isEmpty,
    isFirstPage,

    // 方法
    search,
    loadMore,
    getSuggestions,
    debouncedGetSuggestions,
    setFilters,
    updateFilter,
    removeFilter,
    clearFilters,
    setSort,
    trackSearch,
    trackClick,
    addToHistory,
    removeFromHistory,
    clearHistory,
    reset,
    initialize
  }
})