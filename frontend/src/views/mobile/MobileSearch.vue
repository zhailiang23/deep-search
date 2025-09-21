<template>
  <div class="mobile-search-page min-h-screen bg-background flex flex-col">
    <!-- 移动端头部 -->
    <MobileNavbar
      title="智能搜索"
      :show-back="false"
      class="safe-top"
    />

    <!-- 搜索内容区域 -->
    <div class="flex-1 flex flex-col px-4 py-4 safe-bottom">
      <!-- 搜索输入区域 -->
      <div class="mb-6">
        <div class="relative">
          <SmartSearchBox
            @search="handleSearch"
            @suggestion-select="handleSuggestionSelect"
            class="w-full mobile-optimized"
            :mobile-mode="true"
          />
        </div>
      </div>

      <!-- 搜索历史（折叠式） -->
      <div class="mb-6" v-if="showHistory">
        <button
          @click="toggleHistory"
          class="flex items-center justify-between w-full p-3 text-left bg-card border border-border rounded-lg mobile-touch-target"
        >
          <span class="text-sm font-medium text-foreground">搜索历史</span>
          <ChevronDownIcon
            :class="[
              'w-4 h-4 text-muted-foreground transition-transform duration-200',
              historyExpanded ? 'rotate-180' : ''
            ]"
          />
        </button>

        <div v-if="historyExpanded" class="mt-2">
          <SearchHistory
            @history-select="handleHistorySelect"
            :mobile-mode="true"
          />
        </div>
      </div>

      <!-- 快捷搜索分类 -->
      <div class="mb-6">
        <h3 class="text-base font-semibold mb-3 text-foreground">快捷搜索</h3>
        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="category in searchCategories"
            :key="category.name"
            @click="handleCategorySearch(category.query)"
            class="flex flex-col items-center p-4 bg-card border border-border rounded-lg mobile-touch-target hover:bg-accent transition-colors"
          >
            <component
              :is="category.icon"
              class="w-6 h-6 text-primary mb-2"
            />
            <span class="text-sm font-medium text-foreground text-center">
              {{ category.name }}
            </span>
          </button>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div class="flex-1">
        <h3 class="text-base font-semibold mb-3 text-foreground">热门搜索</h3>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="tag in popularSearches"
            :key="tag"
            @click="handleTagClick(tag)"
            class="px-3 py-2 text-sm bg-secondary text-secondary-foreground rounded-full mobile-touch-target hover:bg-primary hover:text-primary-foreground transition-colors"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <!-- 底部安全区域 -->
      <div class="h-4 safe-bottom"></div>
    </div>

    <!-- 筛选抽屉触发器 -->
    <FilterDrawer
      v-model:open="filterDrawerOpen"
      @apply-filters="handleApplyFilters"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChevronDownIcon,
  CreditCardIcon,
  HomeIcon,
  TrendingUpIcon,
  ShieldIcon,
  UserIcon,
  PhoneIcon,
  CalculatorIcon,
  FileTextIcon
} from 'lucide-vue-next'

import MobileNavbar from '@/components/mobile/MobileNavbar.vue'
import SmartSearchBox from '@/components/search/SmartSearchBox.vue'
import SearchHistory from '@/components/search/SearchHistory.vue'
import FilterDrawer from '@/components/mobile/FilterDrawer.vue'

const router = useRouter()

// 响应式状态
const historyExpanded = ref(false)
const filterDrawerOpen = ref(false)

// 搜索历史可见性
const showHistory = computed(() => {
  // 可以根据实际的历史记录数据来决定是否显示
  return true
})

// 搜索分类
const searchCategories = ref([
  {
    name: '信用卡',
    icon: CreditCardIcon,
    query: '信用卡产品'
  },
  {
    name: '房贷',
    icon: HomeIcon,
    query: '房屋贷款'
  },
  {
    name: '理财',
    icon: TrendingUpIcon,
    query: '理财产品'
  },
  {
    name: '保险',
    icon: ShieldIcon,
    query: '保险服务'
  },
  {
    name: '个人账户',
    icon: UserIcon,
    query: '个人银行账户'
  },
  {
    name: '手机银行',
    icon: PhoneIcon,
    query: '手机银行功能'
  },
  {
    name: '存款计算',
    icon: CalculatorIcon,
    query: '存款利率计算'
  },
  {
    name: '业务办理',
    icon: FileTextIcon,
    query: '银行业务办理流程'
  }
])

// 热门搜索标签
const popularSearches = ref([
  '信用卡申请',
  '房贷利率',
  '定期存款',
  '转账汇款',
  '投资基金',
  '保险理赔',
  '网点查询',
  '手机银行',
  '存款证明',
  '外币兑换'
])

// 事件处理函数
const toggleHistory = () => {
  historyExpanded.value = !historyExpanded.value
}

const handleSearch = (query: string) => {
  if (query.trim()) {
    router.push({
      name: 'MobileSearchResults',
      query: { q: query.trim() }
    })
  }
}

const handleSuggestionSelect = (suggestion: string) => {
  handleSearch(suggestion)
}

const handleHistorySelect = (query: string) => {
  handleSearch(query)
}

const handleCategorySearch = (query: string) => {
  handleSearch(query)
}

const handleTagClick = (tag: string) => {
  handleSearch(tag)
}

const handleApplyFilters = (filters: any) => {
  // 处理筛选器应用
  console.log('Applied filters:', filters)
  filterDrawerOpen.value = false
}
</script>

<style scoped>
.mobile-search-page {
  /* 确保在iOS Safari中正确显示 */
  -webkit-overflow-scrolling: touch;
}

.mobile-optimized {
  /* 移动端优化的搜索框样式 */
  min-height: 48px;
}

/* 移动端触摸目标优化 */
.mobile-touch-target {
  min-height: 44px;
  min-width: 44px;
}

/* 改进的点击反馈 */
.mobile-touch-target:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

/* 安全区域适配 */
.safe-top {
  padding-top: env(safe-area-inset-top);
}

.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

/* 防止缩放 */
@media screen and (max-width: 768px) {
  input, textarea, select {
    font-size: 16px;
  }
}

/* 改进的滚动性能 */
.mobile-search-page {
  scroll-behavior: smooth;
  overscroll-behavior: contain;
}
</style>