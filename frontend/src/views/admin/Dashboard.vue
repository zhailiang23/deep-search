<template>
  <div class="dashboard-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">仪表板</h1>
        <p class="text-muted-foreground mt-2">系统概览和关键指标</p>
      </div>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">总搜索次数</CardTitle>
            <div class="h-4 w-4 text-muted-foreground">🔍</div>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.totalSearches.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground">
              +{{ stats.searchGrowth }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">活跃用户</CardTitle>
            <div class="h-4 w-4 text-muted-foreground">👥</div>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.activeUsers.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground">
              +{{ stats.userGrowth }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">平均响应时间</CardTitle>
            <div class="h-4 w-4 text-muted-foreground">⚡</div>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.avgResponseTime }}ms</div>
            <p class="text-xs text-muted-foreground">
              -{{ stats.performanceImprovement }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">搜索成功率</CardTitle>
            <div class="h-4 w-4 text-muted-foreground">🎯</div>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.successRate }}%</div>
            <p class="text-xs text-muted-foreground">
              +{{ stats.successImprovement }}% 相比上月
            </p>
          </CardContent>
        </Card>
      </div>

      <!-- 图表区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 搜索趋势图 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索趋势</CardTitle>
            <CardDescription>过去30天的搜索量变化</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64 flex items-center justify-center text-muted-foreground">
              图表占位符 - 搜索趋势
            </div>
          </CardContent>
        </Card>

        <!-- 热门搜索 -->
        <Card>
          <CardHeader>
            <CardTitle>热门搜索</CardTitle>
            <CardDescription>最受欢迎的搜索关键词</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-3">
              <div
                v-for="(item, index) in popularSearches"
                :key="item.keyword"
                class="flex items-center justify-between"
              >
                <div class="flex items-center space-x-3">
                  <Badge variant="outline" class="w-6 h-6 p-0 flex items-center justify-center text-xs">
                    {{ index + 1 }}
                  </Badge>
                  <span class="font-medium">{{ item.keyword }}</span>
                </div>
                <span class="text-sm text-muted-foreground">{{ item.count }}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 最近活动 -->
      <Card>
        <CardHeader>
          <CardTitle>最近活动</CardTitle>
          <CardDescription>系统最新活动和事件</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-4">
            <div
              v-for="activity in recentActivities"
              :key="activity.id"
              class="flex items-start space-x-3 p-3 rounded-lg hover:bg-muted/50 transition-colors"
            >
              <div class="w-2 h-2 bg-primary rounded-full mt-2"></div>
              <div class="flex-1">
                <p class="text-sm font-medium">{{ activity.title }}</p>
                <p class="text-xs text-muted-foreground">{{ activity.description }}</p>
                <p class="text-xs text-muted-foreground mt-1">{{ formatTime(activity.timestamp) }}</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui'
import { Badge } from '@/components/ui'

// 统计数据
const stats = ref({
  totalSearches: 125890,
  searchGrowth: 12.5,
  activeUsers: 8247,
  userGrowth: 8.2,
  avgResponseTime: 234,
  performanceImprovement: 15.3,
  successRate: 94.7,
  successImprovement: 2.1
})

// 热门搜索
const popularSearches = ref([
  { keyword: '信用卡申请', count: 1234 },
  { keyword: '房贷利率', count: 987 },
  { keyword: '理财产品', count: 876 },
  { keyword: '手机银行', count: 654 },
  { keyword: '转账汇款', count: 543 }
])

// 最近活动
const recentActivities = ref([
  {
    id: 1,
    title: '新用户注册',
    description: '用户 zhang@example.com 完成注册',
    timestamp: new Date(Date.now() - 5 * 60 * 1000)
  },
  {
    id: 2,
    title: '搜索索引更新',
    description: '成功更新了 1,250 条产品数据',
    timestamp: new Date(Date.now() - 15 * 60 * 1000)
  },
  {
    id: 3,
    title: '系统维护',
    description: '完成了搜索引擎性能优化',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  {
    id: 4,
    title: '数据导入',
    description: '导入了新的银行产品数据',
    timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000)
  }
])

// 格式化时间
const formatTime = (timestamp: Date) => {
  const now = new Date()
  const diff = now.getTime() - timestamp.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 60) {
    return `${minutes} 分钟前`
  } else if (hours < 24) {
    return `${hours} 小时前`
  } else {
    return `${days} 天前`
  }
}

onMounted(() => {
  // 组件挂载后可以加载真实数据
  console.log('Dashboard mounted')
})
</script>

<style scoped>
.dashboard-page {
  /* 自定义样式 */
}
</style>