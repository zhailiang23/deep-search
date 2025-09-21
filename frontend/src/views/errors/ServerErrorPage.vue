<template>
  <div class="server-error-page min-h-screen flex items-center justify-center bg-background">
    <div class="text-center px-4">
      <!-- 500图标 -->
      <div class="mb-8">
        <div class="text-9xl font-bold text-muted-foreground/20 mb-4">500</div>
        <div class="text-6xl mb-4">⚠️</div>
      </div>

      <!-- 错误信息 -->
      <div class="max-w-md mx-auto">
        <h1 class="text-3xl font-bold text-foreground mb-4">服务器错误</h1>
        <p class="text-muted-foreground mb-8">
          抱歉，服务器遇到了一些问题。我们的技术团队已经收到通知并正在修复中。请稍后再试。
        </p>

        <!-- 错误ID -->
        <div class="mb-6 p-3 bg-muted rounded-lg">
          <p class="text-sm text-muted-foreground">错误ID:</p>
          <p class="text-sm font-mono">{{ errorId }}</p>
        </div>

        <!-- 操作按钮 -->
        <div class="space-y-4">
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <Button @click="retryPage" class="min-w-32">
              重试
            </Button>
            <Button @click="goHome" variant="outline" class="min-w-32">
              返回首页
            </Button>
          </div>
        </div>
      </div>

      <!-- 错误详情（仅开发环境显示） -->
      <div v-if="isDev && errorDetails" class="mt-12 max-w-2xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle class="text-lg text-red-600">开发环境 - 错误详情</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-left space-y-4">
              <div v-if="errorDetails.message">
                <h4 class="font-medium mb-2">错误消息:</h4>
                <p class="text-sm text-muted-foreground font-mono bg-muted p-2 rounded">
                  {{ errorDetails.message }}
                </p>
              </div>
              <div v-if="errorDetails.stack">
                <h4 class="font-medium mb-2">堆栈跟踪:</h4>
                <pre class="text-xs text-muted-foreground bg-muted p-2 rounded overflow-auto max-h-40">{{ errorDetails.stack }}</pre>
              </div>
              <div v-if="errorDetails.url">
                <h4 class="font-medium mb-2">请求URL:</h4>
                <p class="text-sm text-muted-foreground font-mono bg-muted p-2 rounded">
                  {{ errorDetails.url }}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 系统状态 -->
      <div class="mt-12 max-w-2xl mx-auto">
        <h3 class="text-lg font-semibold mb-4">系统状态</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">🔍</div>
              <div class="font-medium text-sm">搜索服务</div>
              <div class="flex items-center justify-center mt-2">
                <div :class="getStatusClass(systemStatus.search)"></div>
                <span class="text-xs text-muted-foreground ml-2">
                  {{ getStatusText(systemStatus.search) }}
                </span>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">💾</div>
              <div class="font-medium text-sm">数据库</div>
              <div class="flex items-center justify-center mt-2">
                <div :class="getStatusClass(systemStatus.database)"></div>
                <span class="text-xs text-muted-foreground ml-2">
                  {{ getStatusText(systemStatus.database) }}
                </span>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">⚡</div>
              <div class="font-medium text-sm">缓存服务</div>
              <div class="flex items-center justify-center mt-2">
                <div :class="getStatusClass(systemStatus.cache)"></div>
                <span class="text-xs text-muted-foreground ml-2">
                  {{ getStatusText(systemStatus.cache) }}
                </span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <!-- 建议操作 -->
      <div class="mt-12 max-w-2xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">您可以尝试</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4 text-sm text-left">
              <div class="flex items-start space-x-3">
                <div class="w-2 h-2 bg-primary rounded-full mt-2"></div>
                <div>
                  <p class="font-medium">刷新页面</p>
                  <p class="text-muted-foreground">有时网络问题可能导致临时错误</p>
                </div>
              </div>
              <div class="flex items-start space-x-3">
                <div class="w-2 h-2 bg-primary rounded-full mt-2"></div>
                <div>
                  <p class="font-medium">稍后再试</p>
                  <p class="text-muted-foreground">如果是服务器维护，通常几分钟后会恢复</p>
                </div>
              </div>
              <div class="flex items-start space-x-3">
                <div class="w-2 h-2 bg-primary rounded-full mt-2"></div>
                <div>
                  <p class="font-medium">检查网络连接</p>
                  <p class="text-muted-foreground">确保您的网络连接正常</p>
                </div>
              </div>
              <div class="flex items-start space-x-3">
                <div class="w-2 h-2 bg-primary rounded-full mt-2"></div>
                <div>
                  <p class="font-medium">联系技术支持</p>
                  <p class="text-muted-foreground">如果问题持续存在，请提供错误ID</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 联系信息 -->
      <div class="mt-12 text-sm text-muted-foreground">
        <p>问题持续存在？请联系技术支持：</p>
        <div class="mt-2 space-y-1">
          <p>
            邮箱: <a href="mailto:support@example.com" class="text-primary hover:underline">support@example.com</a>
          </p>
          <p>
            电话: <a href="tel:400-000-0000" class="text-primary hover:underline">400-000-0000</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { Button } from '@/components/ui'

const router = useRouter()

// 错误ID (通常由后端提供)
const errorId = ref(generateErrorId())

// 是否为开发环境
const isDev = ref(import.meta.env.DEV)

// 错误详情 (开发环境使用)
const errorDetails = ref<{
  message: string
  stack: string
  url: string
} | null>({
  message: 'Internal Server Error: Database connection timeout',
  stack: 'Error: Database connection timeout\n    at DatabaseConnection.connect (/app/src/db.js:45:12)\n    at async SearchService.search (/app/src/services/search.js:23:8)\n    at async SearchController.handleSearch (/app/src/controllers/search.js:15:10)',
  url: window.location.href
})

// 系统状态
const systemStatus = ref({
  search: 'error', // 'healthy', 'warning', 'error'
  database: 'error',
  cache: 'healthy'
})

// 生成错误ID
function generateErrorId() {
  const timestamp = Date.now()
  const random = Math.random().toString(36).substring(2, 8)
  return `ERR-${timestamp}-${random.toUpperCase()}`
}

// 获取状态样式类
const getStatusClass = (status: string) => {
  const baseClass = 'w-2 h-2 rounded-full'
  switch (status) {
    case 'healthy':
      return `${baseClass} bg-green-500`
    case 'warning':
      return `${baseClass} bg-yellow-500`
    case 'error':
      return `${baseClass} bg-red-500`
    default:
      return `${baseClass} bg-gray-500`
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'healthy':
      return '正常'
    case 'warning':
      return '警告'
    case 'error':
      return '错误'
    default:
      return '未知'
  }
}

// 重试页面
const retryPage = () => {
  window.location.reload()
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 检查系统状态
const checkSystemStatus = async () => {
  try {
    // 这里可以调用健康检查API
    // const response = await fetch('/api/health')
    // const status = await response.json()
    // systemStatus.value = status

    // 模拟状态检查
    console.log('Checking system status...')
  } catch (error) {
    console.error('Failed to check system status:', error)
  }
}

onMounted(() => {
  // 页面加载时检查系统状态
  checkSystemStatus()

  // 在生产环境中清除错误详情
  if (!isDev.value) {
    errorDetails.value = null
  }
})
</script>

<style scoped>
.server-error-page {
  /* 自定义样式 */
}
</style>