<template>
  <div class="not-found-page min-h-screen flex items-center justify-center bg-background">
    <div class="text-center px-4">
      <!-- 404图标 -->
      <div class="mb-8">
        <div class="text-9xl font-bold text-muted-foreground/20 mb-4">404</div>
        <div class="text-6xl mb-4">🔍</div>
      </div>

      <!-- 错误信息 -->
      <div class="max-w-md mx-auto">
        <h1 class="text-3xl font-bold text-foreground mb-4">页面未找到</h1>
        <p class="text-muted-foreground mb-8">
          抱歉，您要访问的页面不存在。可能页面已被移动、删除或您输入了错误的地址。
        </p>

        <!-- 操作按钮 -->
        <div class="space-y-4">
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <Button @click="goHome" class="min-w-32">
              返回首页
            </Button>
            <Button @click="goBack" variant="outline" class="min-w-32">
              返回上页
            </Button>
          </div>

          <!-- 搜索建议 -->
          <div class="mt-8">
            <p class="text-sm text-muted-foreground mb-4">或者尝试搜索您需要的内容：</p>
            <div class="flex max-w-sm mx-auto">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索..."
                class="flex-1 px-3 py-2 border border-border rounded-l-md bg-background focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                @keyup.enter="handleSearch"
              />
              <Button @click="handleSearch" class="rounded-l-none">
                搜索
              </Button>
            </div>
          </div>
        </div>
      </div>

      <!-- 常用链接 -->
      <div class="mt-12 max-w-2xl mx-auto">
        <h3 class="text-lg font-semibold mb-4">您可能需要：</h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <Card
            v-for="link in popularLinks"
            :key="link.path"
            class="cursor-pointer hover:shadow-md transition-shadow"
            @click="navigateTo(link.path)"
          >
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">{{ link.icon }}</div>
              <div class="font-medium text-sm">{{ link.title }}</div>
              <div class="text-xs text-muted-foreground mt-1">{{ link.description }}</div>
            </CardContent>
          </Card>
        </div>
      </div>

      <!-- 帮助信息 -->
      <div class="mt-12 text-sm text-muted-foreground">
        <p>如果问题持续存在，请联系技术支持：</p>
        <p class="mt-1">
          邮箱: <a href="mailto:support@example.com" class="text-primary hover:underline">support@example.com</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent } from '@/components/ui'
import { Button } from '@/components/ui'

const router = useRouter()
const searchQuery = ref('')

// 常用链接
const popularLinks = ref([
  {
    path: '/',
    icon: '🏠',
    title: '首页',
    description: '返回主页'
  },
  {
    path: '/search',
    icon: '🔍',
    title: '搜索',
    description: '智能搜索'
  },
  {
    path: '/admin/dashboard',
    icon: '📊',
    title: '管理后台',
    description: '系统管理'
  }
])

// 返回首页
const goHome = () => {
  router.push('/')
}

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    router.push('/')
  }
}

// 处理搜索
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({
      name: 'SearchResults',
      query: { q: searchQuery.value.trim() }
    })
  }
}

// 导航到指定路径
const navigateTo = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.not-found-page {
  /* 自定义样式 */
}
</style>