<template>
  <div class="search-page min-h-screen bg-background">
    <div class="container mx-auto px-4 py-8">
      <div class="max-w-4xl mx-auto">
        <!-- 页面标题 -->
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-foreground mb-2">智能搜索</h1>
          <p class="text-muted-foreground">使用AI驱动的语义搜索查找银行产品和服务</p>
        </div>

        <!-- 搜索框区域 -->
        <div class="mb-8">
          <SmartSearchBox
            @search="handleSearch"
            @suggestion-select="handleSuggestionSelect"
            class="w-full"
          />
        </div>

        <!-- 搜索历史 -->
        <div class="mb-8">
          <SearchHistory @history-select="handleHistorySelect" />
        </div>

        <!-- 热门搜索标签 -->
        <div class="text-center">
          <h3 class="text-lg font-semibold mb-4">热门搜索</h3>
          <div class="flex flex-wrap justify-center gap-2">
            <Badge
              v-for="tag in popularSearches"
              :key="tag"
              variant="outline"
              class="cursor-pointer hover:bg-primary hover:text-primary-foreground transition-colors"
              @click="handleTagClick(tag)"
            >
              {{ tag }}
            </Badge>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SmartSearchBox from '@/components/search/SmartSearchBox.vue'
import SearchHistory from '@/components/search/SearchHistory.vue'
import { Badge } from '@/components/ui'

const router = useRouter()

// 热门搜索标签
const popularSearches = ref([
  '信用卡申请',
  '房贷利率',
  '理财产品',
  '存款利息',
  '手机银行',
  '转账汇款',
  '投资基金',
  '保险产品'
])

// 处理搜索
const handleSearch = (query: string) => {
  if (query.trim()) {
    router.push({
      name: 'SearchResults',
      query: { q: query.trim() }
    })
  }
}

// 处理搜索建议选择
const handleSuggestionSelect = (suggestion: string) => {
  handleSearch(suggestion)
}

// 处理历史记录选择
const handleHistorySelect = (query: string) => {
  handleSearch(query)
}

// 处理标签点击
const handleTagClick = (tag: string) => {
  handleSearch(tag)
}
</script>

<style scoped>
.search-page {
  /* 自定义样式 */
}
</style>