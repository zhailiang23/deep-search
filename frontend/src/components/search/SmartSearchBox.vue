<template>
  <div class="smart-search-box relative" ref="searchBoxRef">
    <!-- 主搜索框 -->
    <div class="relative">
      <div
        class="flex items-center border-2 border-border rounded-lg bg-background transition-all duration-200 focus-within:border-primary focus-within:shadow-md"
        :class="[
          isFocused && 'border-primary shadow-md',
          mobileMode ? 'h-12' : size === 'large' ? 'h-14' : size === 'medium' ? 'h-12' : 'h-10'
        ]"
      >
        <!-- 搜索图标 -->
        <div class="flex items-center pl-4">
          <Search class="w-5 h-5 text-muted-foreground" />
        </div>

        <!-- 输入框 -->
        <input
          ref="inputRef"
          v-model="searchQuery"
          type="text"
          :placeholder="placeholder"
          :disabled="disabled"
          class="flex-1 px-3 py-2 bg-transparent border-none outline-none text-foreground placeholder:text-muted-foreground"
          :class="[
            mobileMode ? 'text-base' : size === 'large' ? 'text-lg' : size === 'medium' ? 'text-base' : 'text-sm'
          ]"
          :style="mobileMode ? { fontSize: '16px' } : {}"
          @focus="handleFocus"
          @blur="handleBlur"
          @input="handleInput"
          @keydown="handleKeydown"
          @keyup.enter="handleSearch"
        />

        <!-- 清除按钮 -->
        <button
          v-if="searchQuery && !disabled"
          @click="clearSearch"
          :class="[
            'flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors',
            mobileMode ? 'w-10 h-10 mobile-touch-target' : 'w-8 h-8'
          ]"
          type="button"
          title="清除搜索"
        >
          <X class="w-4 h-4" />
        </button>

        <!-- 语音搜索按钮 -->
        <button
          v-if="enableVoiceSearch && !disabled"
          @click="toggleVoiceSearch"
          :class="[
            'flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors mx-1',
            mobileMode ? 'w-10 h-10 mobile-touch-target' : 'w-8 h-8',
            isListening && 'text-red-500 animate-pulse'
          ]"
          type="button"
          :title="isListening ? '停止语音搜索' : '语音搜索'"
        >
          <Mic class="w-4 h-4" v-if="!isListening" />
          <MicOff class="w-4 h-4" v-else />
        </button>

        <!-- 搜索按钮 -->
        <button
          @click="handleSearch"
          :disabled="disabled || !searchQuery.trim()"
          :class="[
            'flex items-center justify-center mr-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed',
            mobileMode ? 'px-6 py-3 mobile-touch-target' : 'px-4 py-2'
          ]"
          type="button"
        >
          <span class="text-sm font-medium">搜索</span>
        </button>
      </div>

      <!-- 搜索类型选择器 -->
      <div v-if="showSearchTypes && searchTypes.length > 1" class="absolute right-2 top-2">
        <select
          v-model="selectedSearchType"
          class="text-xs border border-border rounded px-2 py-1 bg-background text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary"
        >
          <option
            v-for="type in searchTypes"
            :key="type.value"
            :value="type.value"
          >
            {{ type.label }}
          </option>
        </select>
      </div>
    </div>

    <!-- 搜索建议下拉框 -->
    <SearchSuggestions
      v-if="showSuggestions && (suggestions.length > 0 || isLoadingSuggestions)"
      :suggestions="suggestions"
      :loading="isLoadingSuggestions"
      :query="searchQuery"
      :visible="suggestionsVisible"
      :highlight-index="highlightedIndex"
      @suggestion-select="handleSuggestionSelect"
      @suggestion-hover="handleSuggestionHover"
      class="absolute top-full left-0 right-0 z-50 mt-1"
    />

    <!-- 最近搜索 -->
    <div
      v-if="showRecentSearches && recentSearches.length > 0 && isFocused && !searchQuery"
      class="absolute top-full left-0 right-0 z-50 mt-1 bg-background border border-border rounded-lg shadow-lg max-h-64 overflow-y-auto"
    >
      <div class="p-3 border-b border-border">
        <div class="flex items-center justify-between">
          <h4 class="text-sm font-medium text-foreground">最近搜索</h4>
          <button
            @click="clearRecentSearches"
            class="text-xs text-muted-foreground hover:text-foreground"
          >
            清除
          </button>
        </div>
      </div>
      <div class="py-2">
        <button
          v-for="(recent, index) in recentSearches"
          :key="index"
          @click="selectRecentSearch(recent)"
          class="w-full px-3 py-2 text-left text-sm hover:bg-muted transition-colors flex items-center space-x-2"
        >
          <Clock class="w-4 h-4 text-muted-foreground" />
          <span>{{ recent }}</span>
        </button>
      </div>
    </div>

    <!-- 热门搜索 -->
    <div
      v-if="showPopularSearches && popularSearches.length > 0 && isFocused && !searchQuery && !recentSearches.length"
      class="absolute top-full left-0 right-0 z-50 mt-1 bg-background border border-border rounded-lg shadow-lg max-h-64 overflow-y-auto"
    >
      <div class="p-3 border-b border-border">
        <h4 class="text-sm font-medium text-foreground">热门搜索</h4>
      </div>
      <div class="py-2">
        <button
          v-for="(popular, index) in popularSearches"
          :key="index"
          @click="selectPopularSearch(popular)"
          class="w-full px-3 py-2 text-left text-sm hover:bg-muted transition-colors flex items-center space-x-2"
        >
          <TrendingUp class="w-4 h-4 text-muted-foreground" />
          <span>{{ popular }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import SearchSuggestions from './SearchSuggestions.vue'
import {
  Search,
  X,
  Mic,
  MicOff,
  Clock,
  TrendingUp
} from 'lucide-vue-next'

interface SearchType {
  value: string
  label: string
}

interface Props {
  // 基础属性
  initialQuery?: string
  placeholder?: string
  disabled?: boolean
  size?: 'small' | 'medium' | 'large'

  // 功能开关
  showSuggestions?: boolean
  showSearchTypes?: boolean
  showRecentSearches?: boolean
  showPopularSearches?: boolean
  enableVoiceSearch?: boolean
  mobileMode?: boolean

  // 搜索类型
  searchTypes?: SearchType[]
  defaultSearchType?: string

  // 样式
  fullWidth?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入搜索关键词...',
  disabled: false,
  size: 'medium',
  showSuggestions: true,
  showSearchTypes: false,
  showRecentSearches: true,
  showPopularSearches: true,
  enableVoiceSearch: true,
  mobileMode: false,
  searchTypes: () => [
    { value: 'all', label: '全部' },
    { value: 'products', label: '产品' },
    { value: 'articles', label: '文章' },
    { value: 'faqs', label: 'FAQ' }
  ],
  defaultSearchType: 'all',
  fullWidth: false
})

// 事件定义
const emit = defineEmits<{
  search: [query: string, type?: string]
  'suggestion-select': [suggestion: string]
  'query-change': [query: string]
  focus: []
  blur: []
}>()

// 响应式数据
const searchBoxRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()
const searchQuery = ref(props.initialQuery || '')
const selectedSearchType = ref(props.defaultSearchType)
const isFocused = ref(false)
const isListening = ref(false)
const suggestions = ref<string[]>([])
const isLoadingSuggestions = ref(false)
const highlightedIndex = ref(-1)

// 搜索历史和热门搜索
const recentSearches = ref<string[]>([])
const popularSearches = ref<string[]>([
  '信用卡申请',
  '房贷利率',
  '理财产品',
  '存款利息',
  '手机银行',
  '转账汇款'
])

// 计算属性
const suggestionsVisible = computed(() => {
  return isFocused.value && searchQuery.value.length > 0
})

// 语音搜索相关
let recognition: any = null

// 防抖的建议获取函数
const debouncedGetSuggestions = useDebounceFn(async (query: string) => {
  if (!query.trim() || !props.showSuggestions) {
    suggestions.value = []
    isLoadingSuggestions.value = false
    return
  }

  // 最小查询长度检查
  if (query.trim().length < 2) {
    suggestions.value = []
    isLoadingSuggestions.value = false
    return
  }

  isLoadingSuggestions.value = true
  try {
    // 模拟API调用获取搜索建议
    await new Promise(resolve => setTimeout(resolve, 200))

    // 检查查询是否仍然有效（避免过期的请求覆盖新的结果）
    if (query !== searchQuery.value.trim()) {
      return
    }

    // 模拟建议数据
    const mockSuggestions = [
      `${query}相关产品`,
      `${query}申请条件`,
      `${query}办理流程`,
      `${query}费用标准`,
      `${query}常见问题`,
      `${query}使用指南`,
      `${query}注意事项`
    ].filter(suggestion => suggestion.length <= 25).slice(0, 6)

    suggestions.value = mockSuggestions
  } catch (error) {
    console.error('获取搜索建议失败:', error)
    suggestions.value = []
  } finally {
    isLoadingSuggestions.value = false
  }
}, 250)

// 监听搜索查询变化
watch(searchQuery, (newQuery) => {
  emit('query-change', newQuery)

  if (newQuery.trim()) {
    debouncedGetSuggestions(newQuery)
  } else {
    suggestions.value = []
    highlightedIndex.value = -1
  }
})

// 事件处理
const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

const handleBlur = () => {
  // 延迟失焦以允许点击建议项
  setTimeout(() => {
    isFocused.value = false
    emit('blur')
  }, 200)
}

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  searchQuery.value = target.value
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!suggestionsVisible.value) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(
        highlightedIndex.value + 1,
        suggestions.value.length - 1
      )
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0) {
        handleSuggestionSelect(suggestions.value[highlightedIndex.value])
      } else {
        handleSearch()
      }
      break
    case 'Escape':
      event.preventDefault()
      inputRef.value?.blur()
      break
  }
}

const handleSearch = () => {
  const query = searchQuery.value.trim()
  if (!query || props.disabled) return

  // 添加到搜索历史
  addToRecentSearches(query)

  // 清除建议
  suggestions.value = []
  highlightedIndex.value = -1

  // 失焦
  inputRef.value?.blur()

  // 触发搜索事件
  emit('search', query, selectedSearchType.value)
}

const handleSuggestionSelect = (suggestion: string) => {
  searchQuery.value = suggestion
  emit('suggestion-select', suggestion)
  handleSearch()
}

const handleSuggestionHover = (index: number) => {
  highlightedIndex.value = index
}

const clearSearch = () => {
  searchQuery.value = ''
  suggestions.value = []
  highlightedIndex.value = -1
  inputRef.value?.focus()
}

// 语音搜索
const toggleVoiceSearch = () => {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    console.warn('浏览器不支持语音搜索功能')
    // 可以显示一个友好的提示组件而不是alert
    return
  }

  if (isListening.value) {
    stopVoiceSearch()
  } else {
    startVoiceSearch()
  }
}

const startVoiceSearch = () => {
  try {
    const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition
    recognition = new SpeechRecognition()

    recognition.lang = 'zh-CN'
    recognition.continuous = false
    recognition.interimResults = false

    recognition.onstart = () => {
      isListening.value = true
    }

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript
      searchQuery.value = transcript
      handleSearch()
    }

    recognition.onerror = (event: any) => {
      console.error('Speech recognition error:', event.error)
      isListening.value = false
    }

    recognition.onend = () => {
      isListening.value = false
    }

    recognition.start()
  } catch (error) {
    console.error('Failed to start voice search:', error)
    isListening.value = false
  }
}

const stopVoiceSearch = () => {
  if (recognition) {
    recognition.stop()
    recognition = null
  }
  isListening.value = false
}

// 搜索历史管理
const addToRecentSearches = (query: string) => {
  const searches = [...recentSearches.value]
  const index = searches.indexOf(query)

  if (index > -1) {
    searches.splice(index, 1)
  }

  searches.unshift(query)
  recentSearches.value = searches.slice(0, 10) // 保留最近10条

  // 保存到localStorage
  try {
    localStorage.setItem('search-history', JSON.stringify(recentSearches.value))
  } catch (error) {
    console.warn('Failed to save search history:', error)
  }
}

const selectRecentSearch = (query: string) => {
  searchQuery.value = query
  handleSearch()
}

const selectPopularSearch = (query: string) => {
  searchQuery.value = query
  handleSearch()
}

const clearRecentSearches = () => {
  recentSearches.value = []
  try {
    localStorage.removeItem('search-history')
  } catch (error) {
    console.warn('Failed to clear search history:', error)
  }
}

// 加载搜索历史
const loadRecentSearches = () => {
  try {
    const saved = localStorage.getItem('search-history')
    if (saved) {
      recentSearches.value = JSON.parse(saved)
    }
  } catch (error) {
    console.warn('Failed to load search history:', error)
  }
}

// 外部方法
const focus = () => {
  inputRef.value?.focus()
}

const clear = () => {
  clearSearch()
}

// 暴露给父组件的方法
defineExpose({
  focus,
  clear
})

// 生命周期
onMounted(() => {
  loadRecentSearches()

  // 点击外部关闭建议
  document.addEventListener('click', (event) => {
    if (!searchBoxRef.value?.contains(event.target as Node)) {
      isFocused.value = false
    }
  })
})

onUnmounted(() => {
  if (recognition) {
    recognition.stop()
  }
})
</script>

<style scoped>
.smart-search-box {
  /* 自定义样式 */
}

/* 语音搜索动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.animate-pulse {
  animation: pulse 1s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>