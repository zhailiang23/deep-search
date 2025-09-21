<template>
  <div class="mobile-home min-h-screen bg-background">
    <!-- 移动端头部 -->
    <MobileNavbar
      title="智能搜索平台"
      :show-back="false"
      :show-menu="true"
      class="safe-top"
      @menu="handleMenu"
    />

    <!-- 主要内容区域 -->
    <div class="flex-1 px-4 safe-bottom">
      <!-- 欢迎区域 -->
      <div class="py-6 text-center">
        <div class="w-16 h-16 mx-auto mb-4 bg-primary/10 rounded-full flex items-center justify-center">
          <SearchIcon class="w-8 h-8 text-primary" />
        </div>
        <h1 class="text-2xl font-bold text-foreground mb-2">智能搜索</h1>
        <p class="text-sm text-muted-foreground">银行业务智能搜索助手</p>
      </div>

      <!-- 快速搜索框 -->
      <div class="mb-6">
        <router-link
          to="/mobile/search"
          class="flex items-center w-full p-4 bg-card border border-border rounded-xl mobile-touch-target"
        >
          <SearchIcon class="w-5 h-5 text-muted-foreground mr-3" />
          <span class="text-muted-foreground">搜索银行产品和服务...</span>
        </router-link>
      </div>

      <!-- 功能快捷入口 -->
      <div class="mb-6">
        <h2 class="text-lg font-semibold text-foreground mb-4">常用功能</h2>
        <div class="grid grid-cols-4 gap-4">
          <button
            v-for="feature in quickFeatures"
            :key="feature.name"
            @click="handleFeatureClick(feature)"
            class="flex flex-col items-center p-3 rounded-lg hover:bg-accent mobile-touch-target"
          >
            <div class="w-12 h-12 mb-2 bg-primary/10 rounded-full flex items-center justify-center">
              <component :is="feature.icon" class="w-6 h-6 text-primary" />
            </div>
            <span class="text-xs text-center text-foreground">{{ feature.name }}</span>
          </button>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-foreground">热门搜索</h2>
          <router-link
            to="/mobile/search"
            class="text-sm text-primary hover:text-primary/80"
          >
            查看更多
          </router-link>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="tag in popularSearches.slice(0, 8)"
            :key="tag"
            @click="handleSearchTag(tag)"
            class="px-3 py-2 text-sm bg-secondary text-secondary-foreground rounded-full hover:bg-accent mobile-touch-target"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <!-- 最近搜索 */
      <div class="mb-6" v-if="recentSearches.length > 0">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-foreground">最近搜索</h2>
          <button
            @click="clearRecentSearches"
            class="text-sm text-muted-foreground hover:text-foreground"
          >
            清除
          </button>
        </div>
        <div class="space-y-2">
          <button
            v-for="search in recentSearches.slice(0, 5)"
            :key="search.query"
            @click="handleSearchTag(search.query)"
            class="flex items-center w-full p-3 text-left bg-card border border-border rounded-lg mobile-touch-target hover:bg-accent"
          >
            <ClockIcon class="w-4 h-4 text-muted-foreground mr-3" />
            <div class="flex-1">
              <span class="text-foreground">{{ search.query }}</span>
              <div class="text-xs text-muted-foreground">{{ formatTime(search.timestamp) }}</div>
            </div>
            <ArrowRightIcon class="w-4 h-4 text-muted-foreground" />
          </button>
        </div>
      </div>

      <!-- 统计信息卡片 -->
      <div class="mb-6">
        <div class="grid grid-cols-2 gap-4">
          <div class="p-4 bg-card border border-border rounded-lg text-center">
            <div class="text-2xl font-bold text-primary mb-1">10万+</div>
            <div class="text-sm text-muted-foreground">日均搜索</div>
          </div>
          <div class="p-4 bg-card border border-border rounded-lg text-center">
            <div class="text-2xl font-bold text-primary mb-1">95%</div>
            <div class="text-sm text-muted-foreground">搜索准确率</div>
          </div>
        </div>
      </div>

      <!-- 底部安全区域 -->
      <div class="h-4"></div>
    </div>

    <!-- 侧边菜单 -->
    <MobileSideMenu
      v-model:open="sideMenuOpen"
      @menu-item-click="handleMenuItemClick"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  SearchIcon,
  CreditCardIcon,
  HomeIcon,
  TrendingUpIcon,
  PhoneIcon,
  CalculatorIcon,
  FileTextIcon,
  ShieldIcon,
  UserIcon,
  ClockIcon,
  ArrowRightIcon
} from 'lucide-vue-next'

import MobileNavbar from '@/components/mobile/MobileNavbar.vue'
import MobileSideMenu from '@/components/mobile/MobileSideMenu.vue'
import { useSearchHistory } from '@/composables/mobile/useSearchHistory'

const router = useRouter()
const { recentSearches, addSearch, clearHistory } = useSearchHistory()

// 响应式状态
const sideMenuOpen = ref(false)

// 快捷功能
const quickFeatures = [
  {
    name: '信用卡',
    icon: CreditCardIcon,
    action: () => router.push('/mobile/search?q=信用卡')
  },
  {
    name: '房贷',
    icon: HomeIcon,
    action: () => router.push('/mobile/search?q=房贷')
  },
  {
    name: '理财',
    icon: TrendingUpIcon,
    action: () => router.push('/mobile/search?q=理财产品')
  },
  {
    name: '手机银行',
    icon: PhoneIcon,
    action: () => router.push('/mobile/search?q=手机银行')
  },
  {
    name: '存款计算',
    icon: CalculatorIcon,
    action: () => router.push('/mobile/search?q=存款计算')
  },
  {
    name: '业务办理',
    icon: FileTextIcon,
    action: () => router.push('/mobile/search?q=业务办理')
  },
  {
    name: '保险',
    icon: ShieldIcon,
    action: () => router.push('/mobile/search?q=保险产品')
  },
  {
    name: '账户管理',
    icon: UserIcon,
    action: () => router.push('/mobile/search?q=账户管理')
  }
]

// 热门搜索
const popularSearches = [
  '信用卡申请', '房贷利率', '定期存款', '转账汇款',
  '投资基金', '保险理赔', '网点查询', '手机银行',
  '存款证明', '外币兑换', '贷款申请', '理财产品'
]

// 事件处理函数
const handleMenu = () => {
  sideMenuOpen.value = true
}

const handleFeatureClick = (feature: any) => {
  feature.action()
}

const handleSearchTag = (tag: string) => {
  addSearch(tag)
  router.push({
    path: '/mobile/search',
    query: { q: tag }
  })
}

const clearRecentSearches = () => {
  clearHistory()
}

const handleMenuItemClick = (item: string) => {
  sideMenuOpen.value = false
  // 处理菜单项点击
  console.log('Menu item clicked:', item)
}

// 时间格式化
const formatTime = (timestamp: number) => {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚才'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return new Date(timestamp).toLocaleDateString()
}
</script>

<style scoped>
.mobile-home {
  /* iOS Safari 优化 */
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.mobile-touch-target {
  min-height: 44px;
  min-width: 44px;
  touch-action: manipulation;
}

.mobile-touch-target:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

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
</style>