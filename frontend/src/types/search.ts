// 搜索结果类型
export interface SearchResult {
  id: string
  title: string
  content: string
  excerpt: string
  url?: string
  type: 'document' | 'product' | 'service' | 'news' | 'faq'
  category: string
  tags: string[]
  score: number
  highlights: SearchHighlight[]
  metadata: Record<string, any>
  createdAt: string
  updatedAt: string
}

// 搜索高亮
export interface SearchHighlight {
  field: string
  fragments: string[]
}

// 搜索建议
export interface SearchSuggestion {
  text: string
  type: 'query' | 'completion' | 'correction'
  score: number
  category?: string
  metadata?: Record<string, any>
}

// 搜索筛选器
export interface SearchFilters {
  type?: string[]
  category?: string[]
  dateRange?: {
    start: string
    end: string
  }
  tags?: string[]
  [key: string]: any
}

// 搜索选项
export interface SearchOptions {
  loadMore?: boolean
  sortBy?: 'relevance' | 'date' | 'popularity'
  sortOrder?: 'asc' | 'desc'
  filters?: SearchFilters
  tab?: string
}

// 搜索请求参数
export interface SearchParams {
  query: string
  page: number
  pageSize: number
  sortBy: string
  sortOrder: string
  filters: SearchFilters
}

// 搜索响应
export interface SearchResponse {
  results: SearchResult[]
  totalCount: number
  totalPages: number
  currentPage: number
  pageSize: number
  searchTime: number
  availableFilters: FilterOption[]
  suggestions?: SearchSuggestion[]
  didYouMean?: string
}

// 筛选器选项
export interface FilterOption {
  key: string
  label: string
  type: 'checkbox' | 'radio' | 'range' | 'date'
  options: FilterValue[]
}

export interface FilterValue {
  value: string | number
  label: string
  count: number
}

// 搜索分析
export interface SearchAnalytics {
  query?: string
  resultId?: string
  action: 'search' | 'click' | 'view' | 'download'
  position?: number
  timestamp: number
  sessionId?: string
  userId?: string
  metadata?: Record<string, any>
}

// 搜索统计
export interface SearchStats {
  totalSearches: number
  uniqueQueries: number
  avgResultsPerSearch: number
  avgSearchTime: number
  topQueries: QueryStat[]
  topResults: ResultStat[]
  searchTrends: TrendData[]
}

export interface QueryStat {
  query: string
  count: number
  avgPosition: number
  clickThroughRate: number
}

export interface ResultStat {
  id: string
  title: string
  clickCount: number
  viewCount: number
  clickThroughRate: number
}

export interface TrendData {
  date: string
  searchCount: number
  uniqueQueries: number
  avgSearchTime: number
}

// 自动完成响应
export interface AutocompleteResponse {
  suggestions: SearchSuggestion[]
  corrections: string[]
  completions: string[]
}