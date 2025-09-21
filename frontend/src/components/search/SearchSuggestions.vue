<template>
  <div
    v-if="visible && (suggestions.length > 0 || loading)"
    class="search-suggestions bg-background border border-border rounded-lg shadow-lg overflow-hidden"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="p-3">
      <div class="flex items-center space-x-2">
        <div class="animate-spin w-4 h-4 border-2 border-primary border-t-transparent rounded-full"></div>
        <span class="text-sm text-muted-foreground">正在获取建议...</span>
      </div>
    </div>

    <!-- 建议列表 -->
    <div v-else-if="suggestions.length > 0" class="max-h-64 overflow-y-auto">
      <!-- 分组建议 -->
      <div v-if="groupedSuggestions.length > 1">
        <div
          v-for="(group, groupIndex) in groupedSuggestions"
          :key="group.type"
          class="suggestion-group"
        >
          <!-- 分组标题 -->
          <div class="px-3 py-2 bg-muted/30 border-b border-border">
            <div class="flex items-center space-x-2">
              <span class="text-xs">{{ group.icon }}</span>
              <span class="text-xs font-medium text-muted-foreground">{{ group.title }}</span>
              <span class="text-xs text-muted-foreground">({{ group.suggestions.length }})</span>
            </div>
          </div>

          <!-- 分组建议项 -->
          <div
            v-for="(suggestion, index) in group.suggestions"
            :key="`${group.type}-${index}`"
            :class="[
              'suggestion-item px-3 py-2 cursor-pointer transition-colors border-b border-border/50 last:border-b-0',
              isHighlighted(groupIndex, index) ? 'bg-primary/10 text-primary' : 'hover:bg-muted/50'
            ]"
            @click="handleSuggestionClick(suggestion.text)"
            @mouseenter="handleSuggestionHover(getGlobalIndex(groupIndex, index))"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-3 flex-1 min-w-0">
                <!-- 建议图标 -->
                <div class="flex-shrink-0 text-muted-foreground">
                  <span class="text-sm">{{ suggestion.icon || getDefaultIcon(suggestion.type) }}</span>
                </div>

                <!-- 建议内容 -->
                <div class="flex-1 min-w-0">
                  <div class="text-sm" v-html="highlightQuery(suggestion.text)"></div>
                  <div v-if="suggestion.description" class="text-xs text-muted-foreground mt-0.5 truncate">
                    {{ suggestion.description }}
                  </div>
                </div>
              </div>

              <!-- 建议元信息 -->
              <div class="flex items-center space-x-2 flex-shrink-0">
                <!-- 热门标识 -->
                <div v-if="suggestion.isPopular" class="text-xs text-orange-500" title="热门搜索">
                  🔥
                </div>

                <!-- 搜索次数 -->
                <div v-if="suggestion.searchCount" class="text-xs text-muted-foreground">
                  {{ formatSearchCount(suggestion.searchCount) }}
                </div>

                <!-- 快捷操作 -->
                <div class="flex items-center space-x-1">
                  <button
                    v-if="suggestion.type === 'history'"
                    @click.stop="removeSuggestion(suggestion)"
                    class="text-xs text-muted-foreground hover:text-foreground p-1"
                    title="删除历史记录"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>

            <!-- 子建议 -->
            <div v-if="suggestion.subSuggestions?.length" class="mt-2 pl-6 space-y-1">
              <div
                v-for="subSuggestion in suggestion.subSuggestions"
                :key="subSuggestion"
                class="text-xs text-muted-foreground hover:text-foreground cursor-pointer py-1"
                @click.stop="handleSuggestionClick(subSuggestion)"
              >
                {{ subSuggestion }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 普通建议列表 -->
      <div v-else>
        <div
          v-for="(suggestion, index) in allSuggestions"
          :key="index"
          :class="[
            'suggestion-item px-3 py-2 cursor-pointer transition-colors border-b border-border/50 last:border-b-0',
            highlightIndex === index ? 'bg-primary/10 text-primary' : 'hover:bg-muted/50'
          ]"
          @click="handleSuggestionClick(suggestion.text)"
          @mouseenter="handleSuggestionHover(index)"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-3 flex-1 min-w-0">
              <div class="flex-shrink-0 text-muted-foreground">
                <span class="text-sm">{{ suggestion.icon || getDefaultIcon(suggestion.type) }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm" v-html="highlightQuery(suggestion.text)"></div>
                <div v-if="suggestion.description" class="text-xs text-muted-foreground mt-0.5 truncate">
                  {{ suggestion.description }}
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-2 flex-shrink-0">
              <div v-if="suggestion.isPopular" class="text-xs text-orange-500">🔥</div>
              <div v-if="suggestion.searchCount" class="text-xs text-muted-foreground">
                {{ formatSearchCount(suggestion.searchCount) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 无建议状态 -->
    <div v-else class="p-3 text-center text-sm text-muted-foreground">
      暂无搜索建议
    </div>

    <!-- 底部操作 -->
    <div v-if="suggestions.length > 0" class="px-3 py-2 bg-muted/30 border-t border-border">
      <div class="flex items-center justify-between text-xs text-muted-foreground">
        <div>使用方向键选择，回车搜索</div>
        <div class="flex items-center space-x-2">
          <span>共 {{ totalSuggestions }} 条建议</span>
          <button
            v-if="hasMoreSuggestions"
            @click="loadMoreSuggestions"
            class="text-primary hover:text-primary/80"
          >
            查看更多
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Suggestion {
  text: string
  type: 'query' | 'product' | 'article' | 'faq' | 'history' | 'popular'
  icon?: string
  description?: string
  isPopular?: boolean
  searchCount?: number
  subSuggestions?: string[]
}

interface SuggestionGroup {
  type: string
  title: string
  icon: string
  suggestions: Suggestion[]
}

interface Props {
  suggestions: string[] | Suggestion[]
  loading?: boolean
  visible?: boolean
  query?: string
  highlightIndex?: number
  maxSuggestions?: number
  groupSuggestions?: boolean
  showMetadata?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  visible: false,
  query: '',
  highlightIndex: -1,
  maxSuggestions: 10,
  groupSuggestions: true,
  showMetadata: true
})

const emit = defineEmits<{
  'suggestion-select': [suggestion: string]
  'suggestion-hover': [index: number]
  'suggestion-remove': [suggestion: Suggestion]
  'load-more': []
}>()

// 响应式数据
const hasMoreSuggestions = ref(false)

// 将字符串数组转换为建议对象数组
const normalizeSuggestions = (suggestions: string[] | Suggestion[]): Suggestion[] => {
  return suggestions.map((item, index) => {
    if (typeof item === 'string') {
      return {
        text: item,
        type: determineSuggestionType(item),
        searchCount: Math.floor(Math.random() * 1000) + 100,
        isPopular: index < 3
      } as Suggestion
    }
    return item
  })
}

// 确定建议类型
const determineSuggestionType = (text: string): Suggestion['type'] => {
  if (text.includes('产品') || text.includes('服务')) return 'product'
  if (text.includes('问题') || text.includes('如何') || text.includes('怎么')) return 'faq'
  if (text.includes('新闻') || text.includes('通知') || text.includes('公告')) return 'article'
  return 'query'
}

// 标准化后的建议列表
const allSuggestions = computed(() => {
  return normalizeSuggestions(props.suggestions).slice(0, props.maxSuggestions)
})

// 分组建议
const groupedSuggestions = computed((): SuggestionGroup[] => {
  if (!props.groupSuggestions) return []

  const groups: Record<string, Suggestion[]> = {}

  allSuggestions.value.forEach(suggestion => {
    if (!groups[suggestion.type]) {
      groups[suggestion.type] = []
    }
    groups[suggestion.type].push(suggestion)
  })

  return Object.entries(groups).map(([type, suggestions]) => ({
    type,
    title: getGroupTitle(type),
    icon: getGroupIcon(type),
    suggestions
  }))
})

// 总建议数
const totalSuggestions = computed(() => allSuggestions.value.length)

// 获取分组标题
const getGroupTitle = (type: string): string => {
  const titles: Record<string, string> = {
    query: '搜索建议',
    product: '产品服务',
    article: '文章资讯',
    faq: '常见问题',
    history: '搜索历史',
    popular: '热门搜索'
  }
  return titles[type] || '其他'
}

// 获取分组图标
const getGroupIcon = (type: string): string => {
  const icons: Record<string, string> = {
    query: '🔍',
    product: '🏦',
    article: '📄',
    faq: '❓',
    history: '🕒',
    popular: '🔥'
  }
  return icons[type] || '📝'
}

// 获取默认图标
const getDefaultIcon = (type: string): string => {
  const icons: Record<string, string> = {
    query: '🔍',
    product: '🏦',
    article: '📄',
    faq: '❓',
    history: '🕒',
    popular: '🔥'
  }
  return icons[type] || '📝'
}

// 高亮查询文本
const highlightQuery = (text: string): string => {
  if (!props.query.trim()) return text

  const regex = new RegExp(`(${props.query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<mark class="bg-primary/20 text-primary font-medium">$1</mark>')
}

// 格式化搜索次数
const formatSearchCount = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}千`
  }
  return count.toString()
}

// 检查是否高亮（分组模式）
const isHighlighted = (groupIndex: number, itemIndex: number): boolean => {
  return getGlobalIndex(groupIndex, itemIndex) === props.highlightIndex
}

// 获取全局索引（分组模式）
const getGlobalIndex = (groupIndex: number, itemIndex: number): number => {
  let globalIndex = 0
  for (let i = 0; i < groupIndex; i++) {
    globalIndex += groupedSuggestions.value[i].suggestions.length
  }
  return globalIndex + itemIndex
}

// 事件处理
const handleSuggestionClick = (suggestionText: string) => {
  emit('suggestion-select', suggestionText)
}

const handleSuggestionHover = (index: number) => {
  emit('suggestion-hover', index)
}

const removeSuggestion = (suggestion: Suggestion) => {
  emit('suggestion-remove', suggestion)
}

const loadMoreSuggestions = () => {
  emit('load-more')
}

// 监听建议变化
watch(
  () => props.suggestions,
  (newSuggestions) => {
    // 检查是否有更多建议
    hasMoreSuggestions.value = newSuggestions.length >= props.maxSuggestions
  },
  { immediate: true }
)
</script>

<style scoped>
.search-suggestions {
  /* 自定义样式 */
}

.suggestion-item {
  /* 建议项样式 */
}

.suggestion-group {
  /* 建议分组样式 */
}

/* 高亮标记样式 */
:deep(mark) {
  background-color: rgb(var(--primary) / 0.2);
  color: rgb(var(--primary));
  font-weight: 500;
  padding: 0;
  border-radius: 2px;
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
</style>