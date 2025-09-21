import { ref, computed } from 'vue'

interface SearchHistoryItem {
  query: string
  timestamp: number
  results?: number
}

const STORAGE_KEY = 'mobile-search-history'
const MAX_HISTORY_ITEMS = 50

// 全局状态
const searchHistory = ref<SearchHistoryItem[]>([])

// 从localStorage加载历史记录
const loadHistory = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      searchHistory.value = JSON.parse(stored)
    }
  } catch (error) {
    console.warn('Failed to load search history:', error)
    searchHistory.value = []
  }
}

// 保存历史记录到localStorage
const saveHistory = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(searchHistory.value))
  } catch (error) {
    console.warn('Failed to save search history:', error)
  }
}

// 初始化加载
loadHistory()

export function useSearchHistory() {
  // 最近搜索（按时间倒序）
  const recentSearches = computed(() => {
    return [...searchHistory.value]
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 20)
  })

  // 添加搜索记录
  const addSearch = (query: string, results?: number) => {
    if (!query.trim()) return

    const timestamp = Date.now()

    // 移除已存在的相同查询
    const existingIndex = searchHistory.value.findIndex(item => item.query === query)
    if (existingIndex > -1) {
      searchHistory.value.splice(existingIndex, 1)
    }

    // 添加新的搜索记录到开头
    searchHistory.value.unshift({
      query: query.trim(),
      timestamp,
      results
    })

    // 限制历史记录数量
    if (searchHistory.value.length > MAX_HISTORY_ITEMS) {
      searchHistory.value = searchHistory.value.slice(0, MAX_HISTORY_ITEMS)
    }

    saveHistory()
  }

  // 移除单个搜索记录
  const removeSearch = (query: string) => {
    const index = searchHistory.value.findIndex(item => item.query === query)
    if (index > -1) {
      searchHistory.value.splice(index, 1)
      saveHistory()
    }
  }

  // 清除所有历史记录
  const clearHistory = () => {
    searchHistory.value = []
    saveHistory()
  }

  // 获取热门搜索（基于搜索频率）
  const getPopularSearches = (limit = 10) => {
    const queryCount = new Map<string, number>()

    searchHistory.value.forEach(item => {
      const count = queryCount.get(item.query) || 0
      queryCount.set(item.query, count + 1)
    })

    return Array.from(queryCount.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .map(([query]) => query)
  }

  // 获取相关搜索建议
  const getRelatedSearches = (query: string, limit = 5) => {
    if (!query.trim()) return []

    const lowerQuery = query.toLowerCase()

    return searchHistory.value
      .filter(item =>
        item.query.toLowerCase().includes(lowerQuery) &&
        item.query.toLowerCase() !== lowerQuery
      )
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, limit)
      .map(item => item.query)
  }

  // 导出搜索历史
  const exportHistory = () => {
    const data = {
      version: '1.0',
      exported: new Date().toISOString(),
      history: searchHistory.value
    }

    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json'
    })

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `search-history-${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  // 导入搜索历史
  const importHistory = (file: File): Promise<void> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()

      reader.onload = (e) => {
        try {
          const result = e.target?.result as string
          const data = JSON.parse(result)

          if (data.history && Array.isArray(data.history)) {
            // 合并导入的历史记录
            const importedHistory = data.history.filter((item: any) =>
              item.query && typeof item.timestamp === 'number'
            )

            // 去重并合并
            const combined = [...searchHistory.value, ...importedHistory]
            const uniqueMap = new Map()

            combined.forEach(item => {
              const existing = uniqueMap.get(item.query)
              if (!existing || item.timestamp > existing.timestamp) {
                uniqueMap.set(item.query, item)
              }
            })

            searchHistory.value = Array.from(uniqueMap.values())
              .sort((a, b) => b.timestamp - a.timestamp)
              .slice(0, MAX_HISTORY_ITEMS)

            saveHistory()
            resolve()
          } else {
            reject(new Error('Invalid file format'))
          }
        } catch (error) {
          reject(error)
        }
      }

      reader.onerror = () => reject(new Error('File read error'))
      reader.readAsText(file)
    })
  }

  // 获取搜索统计
  const getSearchStats = () => {
    const now = Date.now()
    const oneDay = 24 * 60 * 60 * 1000
    const oneWeek = 7 * oneDay
    const oneMonth = 30 * oneDay

    const stats = {
      total: searchHistory.value.length,
      today: searchHistory.value.filter(item => now - item.timestamp < oneDay).length,
      thisWeek: searchHistory.value.filter(item => now - item.timestamp < oneWeek).length,
      thisMonth: searchHistory.value.filter(item => now - item.timestamp < oneMonth).length,
      uniqueQueries: new Set(searchHistory.value.map(item => item.query)).size
    }

    return stats
  }

  return {
    searchHistory: searchHistory.value,
    recentSearches,
    addSearch,
    removeSearch,
    clearHistory,
    getPopularSearches,
    getRelatedSearches,
    exportHistory,
    importHistory,
    getSearchStats
  }
}