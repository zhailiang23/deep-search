<template>
  <div class="analytics-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">统计分析</h1>
        <p class="text-muted-foreground mt-2">搜索数据统计和用户行为分析</p>
      </div>

      <!-- 时间范围选择器 -->
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center gap-4">
            <span class="text-sm font-medium">时间范围:</span>
            <select
              v-model="selectedTimeRange"
              @change="loadAnalyticsData"
              class="px-3 py-2 border border-border rounded-md bg-background"
            >
              <option value="7d">最近7天</option>
              <option value="30d">最近30天</option>
              <option value="90d">最近90天</option>
              <option value="1y">最近1年</option>
            </select>
          </div>
        </CardContent>
      </Card>

      <!-- 关键指标 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">搜索次数</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.searchCount.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">
              <span :class="metrics.searchGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.searchGrowth >= 0 ? '+' : '' }}{{ metrics.searchGrowth }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">独立用户</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.uniqueUsers.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">
              <span :class="metrics.userGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.userGrowth >= 0 ? '+' : '' }}{{ metrics.userGrowth }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">点击率</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.clickRate }}%</div>
            <p class="text-xs text-muted-foreground mt-1">
              <span :class="metrics.clickRateChange >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.clickRateChange >= 0 ? '+' : '' }}{{ metrics.clickRateChange }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">平均会话时长</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.avgSessionTime }}m</div>
            <p class="text-xs text-muted-foreground mt-1">
              <span :class="metrics.sessionTimeChange >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.sessionTimeChange >= 0 ? '+' : '' }}{{ metrics.sessionTimeChange }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>
      </div>

      <!-- 图表区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 搜索趋势 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索趋势</CardTitle>
            <CardDescription>搜索量随时间变化</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64 flex items-center justify-center text-muted-foreground border border-dashed border-border rounded">
              搜索趋势图表占位符
            </div>
          </CardContent>
        </Card>

        <!-- 热门搜索词 -->
        <Card>
          <CardHeader>
            <CardTitle>热门搜索词</CardTitle>
            <CardDescription>最受欢迎的搜索关键词</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-3">
              <div
                v-for="(item, index) in topSearchTerms"
                :key="item.term"
                class="flex items-center justify-between"
              >
                <div class="flex items-center space-x-3">
                  <Badge variant="outline" class="w-6 h-6 p-0 flex items-center justify-center text-xs">
                    {{ index + 1 }}
                  </Badge>
                  <span class="font-medium">{{ item.term }}</span>
                </div>
                <div class="text-right">
                  <div class="text-sm font-medium">{{ item.count.toLocaleString() }}</div>
                  <div class="text-xs text-muted-foreground">{{ item.percentage }}%</div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 用户行为分析 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 搜索结果点击分布 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索结果点击分布</CardTitle>
            <CardDescription>用户点击搜索结果的位置分布</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-3">
              <div
                v-for="(item, index) in clickDistribution"
                :key="index"
                class="flex items-center justify-between"
              >
                <span class="text-sm">第 {{ index + 1 }} 位</span>
                <div class="flex items-center space-x-2 flex-1 mx-4">
                  <div class="flex-1 bg-muted rounded-full h-2">
                    <div
                      class="bg-primary h-2 rounded-full"
                      :style="{ width: `${item.percentage}%` }"
                    ></div>
                  </div>
                  <span class="text-sm text-muted-foreground w-12 text-right">{{ item.percentage }}%</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 搜索类型分布 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索类型分布</CardTitle>
            <CardDescription>不同类型内容的搜索占比</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-3">
              <div
                v-for="category in searchCategories"
                :key="category.name"
                class="flex items-center justify-between"
              >
                <span class="text-sm font-medium">{{ category.name }}</span>
                <div class="flex items-center space-x-2 flex-1 mx-4">
                  <div class="flex-1 bg-muted rounded-full h-2">
                    <div
                      class="h-2 rounded-full"
                      :class="category.color"
                      :style="{ width: `${category.percentage}%` }"
                    ></div>
                  </div>
                  <span class="text-sm text-muted-foreground w-12 text-right">{{ category.percentage }}%</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 详细报告 -->
      <Card>
        <CardHeader>
          <CardTitle>详细搜索记录</CardTitle>
          <CardDescription>最近的搜索活动记录</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border">
                  <th class="text-left py-2">时间</th>
                  <th class="text-left py-2">搜索词</th>
                  <th class="text-left py-2">用户</th>
                  <th class="text-left py-2">结果数</th>
                  <th class="text-left py-2">点击数</th>
                  <th class="text-left py-2">响应时间</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="record in searchRecords"
                  :key="record.id"
                  class="border-b border-border hover:bg-muted/50"
                >
                  <td class="py-2">{{ formatDateTime(record.timestamp) }}</td>
                  <td class="py-2 font-medium">{{ record.query }}</td>
                  <td class="py-2">{{ record.user }}</td>
                  <td class="py-2">{{ record.resultCount }}</td>
                  <td class="py-2">{{ record.clickCount }}</td>
                  <td class="py-2">{{ record.responseTime }}ms</td>
                </tr>
              </tbody>
            </table>
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

// 时间范围
const selectedTimeRange = ref('30d')

// 关键指标
const metrics = ref({
  searchCount: 45678,
  searchGrowth: 12.5,
  uniqueUsers: 8234,
  userGrowth: 8.7,
  clickRate: 73.2,
  clickRateChange: 2.1,
  avgSessionTime: 4.5,
  sessionTimeChange: -0.8
})

// 热门搜索词
const topSearchTerms = ref([
  { term: '信用卡申请', count: 3456, percentage: 15.2 },
  { term: '房贷利率', count: 2987, percentage: 13.1 },
  { term: '理财产品', count: 2234, percentage: 9.8 },
  { term: '手机银行', count: 1876, percentage: 8.2 },
  { term: '转账汇款', count: 1654, percentage: 7.3 },
  { term: '存款利息', count: 1432, percentage: 6.3 },
  { term: '投资基金', count: 1298, percentage: 5.7 },
  { term: '保险产品', count: 1123, percentage: 4.9 }
])

// 点击分布
const clickDistribution = ref([
  { percentage: 45.2 },
  { percentage: 23.1 },
  { percentage: 12.4 },
  { percentage: 8.7 },
  { percentage: 5.3 },
  { percentage: 3.1 },
  { percentage: 1.8 },
  { percentage: 0.4 }
])

// 搜索类型分布
const searchCategories = ref([
  { name: '银行产品', percentage: 42.3, color: 'bg-blue-500' },
  { name: '服务流程', percentage: 28.7, color: 'bg-green-500' },
  { name: '常见问题', percentage: 18.9, color: 'bg-orange-500' },
  { name: '政策法规', percentage: 10.1, color: 'bg-purple-500' }
])

// 搜索记录
const searchRecords = ref([
  {
    id: 1,
    timestamp: new Date(Date.now() - 5 * 60 * 1000),
    query: '信用卡申请条件',
    user: 'user001',
    resultCount: 23,
    clickCount: 3,
    responseTime: 234
  },
  {
    id: 2,
    timestamp: new Date(Date.now() - 8 * 60 * 1000),
    query: '房贷利率查询',
    user: 'user002',
    resultCount: 15,
    clickCount: 2,
    responseTime: 187
  },
  {
    id: 3,
    timestamp: new Date(Date.now() - 12 * 60 * 1000),
    query: '理财产品推荐',
    user: 'user003',
    resultCount: 32,
    clickCount: 5,
    responseTime: 298
  }
])

// 格式化日期时间
const formatDateTime = (date: Date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 加载分析数据
const loadAnalyticsData = () => {
  console.log('Loading analytics data for:', selectedTimeRange.value)
  // 根据时间范围加载数据
}

onMounted(() => {
  loadAnalyticsData()
})
</script>

<style scoped>
.analytics-page {
  /* 自定义样式 */
}
</style>