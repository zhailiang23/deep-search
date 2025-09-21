<template>
  <div class="analytics-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">统计分析</h1>
        <p class="text-muted-foreground mt-2">搜索数据统计和用户行为分析</p>
      </div>

      <!-- 控制面板 -->
      <Card>
        <CardContent class="pt-6">
          <div class="flex flex-wrap items-center gap-4">
            <div class="flex items-center gap-2">
              <CalendarIcon class="w-4 h-4" />
              <span class="text-sm font-medium">时间范围:</span>
              <Select v-model="selectedTimeRange" @update:modelValue="loadAnalyticsData">
                <SelectTrigger class="w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1d">今天</SelectItem>
                  <SelectItem value="7d">最近7天</SelectItem>
                  <SelectItem value="30d">最近30天</SelectItem>
                  <SelectItem value="90d">最近90天</SelectItem>
                  <SelectItem value="1y">最近1年</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Separator orientation="vertical" class="h-6" />

            <div class="flex items-center gap-2">
              <FilterIcon class="w-4 h-4" />
              <span class="text-sm font-medium">分析维度:</span>
              <Select v-model="selectedDimension">
                <SelectTrigger class="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="overall">整体分析</SelectItem>
                  <SelectItem value="channel">渠道分析</SelectItem>
                  <SelectItem value="content">内容分析</SelectItem>
                  <SelectItem value="user">用户分析</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div class="ml-auto flex gap-2">
              <Button @click="exportReport" variant="outline" size="sm">
                <DownloadIcon class="w-4 h-4 mr-2" />
                导出报告
              </Button>
              <Button @click="refreshData" variant="outline" size="sm" :loading="isLoading">
                <RefreshCwIcon class="w-4 h-4 mr-2" />
                刷新数据
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 关键指标 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">搜索次数</CardTitle>
            <SearchIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.searchCount.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1 flex items-center">
              <component
                :is="metrics.searchGrowth >= 0 ? TrendingUpIcon : TrendingDownIcon"
                class="h-3 w-3 mr-1"
                :class="metrics.searchGrowth >= 0 ? 'text-green-600' : 'text-red-600'"
              />
              <span :class="metrics.searchGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.searchGrowth >= 0 ? '+' : '' }}{{ metrics.searchGrowth }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">独立用户</CardTitle>
            <UsersIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.uniqueUsers.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1 flex items-center">
              <component
                :is="metrics.userGrowth >= 0 ? TrendingUpIcon : TrendingDownIcon"
                class="h-3 w-3 mr-1"
                :class="metrics.userGrowth >= 0 ? 'text-green-600' : 'text-red-600'"
              />
              <span :class="metrics.userGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.userGrowth >= 0 ? '+' : '' }}{{ metrics.userGrowth }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">点击率</CardTitle>
            <MousePointerClickIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.clickRate }}%</div>
            <p class="text-xs text-muted-foreground mt-1 flex items-center">
              <component
                :is="metrics.clickRateChange >= 0 ? TrendingUpIcon : TrendingDownIcon"
                class="h-3 w-3 mr-1"
                :class="metrics.clickRateChange >= 0 ? 'text-green-600' : 'text-red-600'"
              />
              <span :class="metrics.clickRateChange >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ metrics.clickRateChange >= 0 ? '+' : '' }}{{ metrics.clickRateChange }}%
              </span>
              相比上期
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">平均会话时长</CardTitle>
            <ClockIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ metrics.avgSessionTime }}m</div>
            <p class="text-xs text-muted-foreground mt-1 flex items-center">
              <component
                :is="metrics.sessionTimeChange >= 0 ? TrendingUpIcon : TrendingDownIcon"
                class="h-3 w-3 mr-1"
                :class="metrics.sessionTimeChange >= 0 ? 'text-green-600' : 'text-red-600'"
              />
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
            <div class="h-64">
              <v-chart
                class="chart"
                :option="searchTrendOption"
                autoresize
              />
            </div>
          </CardContent>
        </Card>

        <!-- 搜索热力图 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索时段热力图</CardTitle>
            <CardDescription>24小时搜索活跃度分布</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64">
              <v-chart
                class="chart"
                :option="heatmapOption"
                autoresize
              />
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 第二行图表 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 搜索类型分布饼图 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索类型分布</CardTitle>
            <CardDescription>不同内容类型的搜索占比</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64">
              <v-chart
                class="chart"
                :option="categoryPieOption"
                autoresize
              />
            </div>
          </CardContent>
        </Card>

        <!-- 热门搜索词云图 -->
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
                class="flex items-center justify-between p-2 rounded-lg hover:bg-muted/50 transition-colors"
              >
                <div class="flex items-center space-x-3">
                  <Badge
                    :variant="index < 3 ? 'default' : 'outline'"
                    class="w-6 h-6 p-0 flex items-center justify-center text-xs"
                  >
                    {{ index + 1 }}
                  </Badge>
                  <span class="font-medium">{{ item.term }}</span>
                </div>
                <div class="text-right">
                  <div class="text-sm font-medium">{{ item.count.toLocaleString() }}</div>
                  <div class="text-xs text-muted-foreground">{{ item.percentage }}%</div>
                  <div class="w-16 bg-muted rounded-full h-1 mt-1">
                    <div
                      class="bg-primary h-1 rounded-full transition-all"
                      :style="{ width: `${item.percentage * 6}%` }"
                    ></div>
                  </div>
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
import { ref, computed, onMounted, provide } from 'vue'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Badge,
  Button,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Separator
} from '@/components/ui'
import {
  CalendarIcon,
  FilterIcon,
  DownloadIcon,
  RefreshCwIcon,
  SearchIcon,
  UsersIcon,
  MousePointerClickIcon,
  ClockIcon,
  TrendingUpIcon,
  TrendingDownIcon
} from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart, HeatmapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CalendarComponent,
  VisualMapComponent
} from 'echarts/components'
import VChart, { THEME_KEY } from 'vue-echarts'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CalendarComponent,
  VisualMapComponent
])

// 提供主题
provide(THEME_KEY, 'light')

// 状态管理
const isLoading = ref(false)
const selectedTimeRange = ref('30d')
const selectedDimension = ref('overall')

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

// 搜索趋势图表配置
const searchTrendOption = computed(() => {
  const days = []
  const searchCounts = []
  const userCounts = []

  // 生成过去30天的数据
  for (let i = 29; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }))

    // 模拟搜索量数据
    const baseSearchCount = 1500 + Math.sin(i * 0.3) * 500 + Math.random() * 200
    searchCounts.push(Math.round(baseSearchCount))

    // 模拟用户数据
    const baseUserCount = 300 + Math.sin(i * 0.25) * 100 + Math.random() * 50
    userCounts.push(Math.round(baseUserCount))
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['搜索次数', '活跃用户']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: {
        color: '#64748b'
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '搜索次数',
        position: 'left',
        axisLabel: {
          color: '#64748b'
        }
      },
      {
        type: 'value',
        name: '活跃用户',
        position: 'right',
        axisLabel: {
          color: '#64748b'
        }
      }
    ],
    series: [
      {
        name: '搜索次数',
        type: 'line',
        data: searchCounts,
        smooth: true,
        lineStyle: {
          color: '#3b82f6'
        },
        itemStyle: {
          color: '#3b82f6'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
            ]
          }
        }
      },
      {
        name: '活跃用户',
        type: 'line',
        yAxisIndex: 1,
        data: userCounts,
        smooth: true,
        lineStyle: {
          color: '#10b981'
        },
        itemStyle: {
          color: '#10b981'
        }
      }
    ]
  }
})

// 热力图配置
const heatmapOption = computed(() => {
  const hours = Array.from({ length: 24 }, (_, i) => i + '')
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const data = []

  for (let i = 0; i < 7; i++) {
    for (let j = 0; j < 24; j++) {
      // 模拟热力数据：工作日工作时间搜索量较高
      let value = Math.random() * 100
      if (i < 5 && j >= 9 && j <= 17) {
        value = 60 + Math.random() * 40
      } else if (i >= 5 && j >= 10 && j <= 16) {
        value = 40 + Math.random() * 30
      }
      data.push([j, i, Math.round(value)])
    }
  }

  return {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${days[params.data[1]]} ${params.data[0]}:00<br/>搜索量: ${params.data[2]}`
      }
    },
    grid: {
      height: '50%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: {
        show: true
      },
      axisLabel: {
        color: '#64748b'
      }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: {
        show: true
      },
      axisLabel: {
        color: '#64748b'
      }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '10%',
      inRange: {
        color: ['#f0f9ff', '#0ea5e9', '#0369a1']
      }
    },
    series: [
      {
        name: '搜索热力',
        type: 'heatmap',
        data: data,
        label: {
          show: false
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
})

// 分类饼图配置
const categoryPieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)'
  },
  legend: {
    bottom: '0%',
    left: 'center'
  },
  series: [
    {
      name: '搜索类型',
      type: 'pie',
      radius: ['40%', '70%'],
      data: searchCategories.value.map(item => ({
        value: item.percentage,
        name: item.name,
        itemStyle: {
          color: item.name === '银行产品' ? '#3b82f6' :
                 item.name === '服务流程' ? '#10b981' :
                 item.name === '常见问题' ? '#f59e0b' : '#8b5cf6'
        }
      })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))

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
const loadAnalyticsData = async () => {
  isLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('Loading analytics data for:', selectedTimeRange.value)
    // 根据时间范围加载数据
  } finally {
    isLoading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  loadAnalyticsData()
}

// 导出报告
const exportReport = () => {
  console.log('Exporting analytics report')
  // 实现导出功能
}

onMounted(() => {
  loadAnalyticsData()
})
</script>

<style scoped>
.analytics-page {
  /* 自定义样式 */
}

.chart {
  width: 100%;
  height: 100%;
}

/* 确保图表在容器中正确显示 */
:deep(.chart > div) {
  width: 100% !important;
  height: 100% !important;
}

/* 图表加载动画 */
.chart {
  transition: opacity 0.3s ease;
}

/* 指标卡片悬停效果 */
.card:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

/* 热门搜索词进度条动画 */
.chart .bg-primary {
  transition: width 0.5s ease-in-out;
}

/* 响应式图表调整 */
@media (max-width: 768px) {
  .chart {
    height: 200px;
  }
}

/* 表格样式优化 */
table {
  border-collapse: collapse;
}

tbody tr:hover {
  background-color: hsl(var(--muted) / 0.5);
}

/* 分隔符样式 */
.separator {
  background-color: hsl(var(--border));
}
</style>