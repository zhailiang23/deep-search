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
            <SearchIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.totalSearches.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground">
              <TrendingUpIcon class="inline h-3 w-3 mr-1" />
              +{{ stats.searchGrowth }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">活跃用户</CardTitle>
            <UsersIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.activeUsers.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground">
              <TrendingUpIcon class="inline h-3 w-3 mr-1" />
              +{{ stats.userGrowth }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">平均响应时间</CardTitle>
            <ZapIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.avgResponseTime }}ms</div>
            <p class="text-xs text-muted-foreground">
              <TrendingDownIcon class="inline h-3 w-3 mr-1" />
              -{{ stats.performanceImprovement }}% 相比上月
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium">搜索成功率</CardTitle>
            <TargetIcon class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.successRate }}%</div>
            <p class="text-xs text-muted-foreground">
              <TrendingUpIcon class="inline h-3 w-3 mr-1" />
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
            <div class="h-64">
              <v-chart
                class="chart"
                :option="searchTrendOption"
                autoresize
              />
            </div>
          </CardContent>
        </Card>

        <!-- 用户活跃度分布 -->
        <Card>
          <CardHeader>
            <CardTitle>用户活跃度分布</CardTitle>
            <CardDescription>各类用户活跃度占比</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64">
              <v-chart
                class="chart"
                :option="userActivityOption"
                autoresize
              />
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 第二行图表 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 搜索类型分布 -->
        <Card>
          <CardHeader>
            <CardTitle>搜索类型分布</CardTitle>
            <CardDescription>不同产品类型的搜索量</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="h-64">
              <v-chart
                class="chart"
                :option="searchTypeOption"
                autoresize
              />
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
                class="flex items-center justify-between p-3 rounded-lg hover:bg-muted/50 transition-colors"
              >
                <div class="flex items-center space-x-3">
                  <Badge
                    :variant="index < 3 ? 'default' : 'outline'"
                    class="w-6 h-6 p-0 flex items-center justify-center text-xs"
                  >
                    {{ index + 1 }}
                  </Badge>
                  <span class="font-medium">{{ item.keyword }}</span>
                </div>
                <div class="flex items-center space-x-2">
                  <span class="text-sm text-muted-foreground">{{ item.count }}</span>
                  <TrendingUpIcon class="h-3 w-3 text-green-500" />
                </div>
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
import { ref, computed, onMounted, provide } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui'
import { Badge } from '@/components/ui'
import {
  SearchIcon,
  UsersIcon,
  ZapIcon,
  TargetIcon,
  TrendingUpIcon,
  TrendingDownIcon
} from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import VChart, { THEME_KEY } from 'vue-echarts'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
])

// 提供主题
provide(THEME_KEY, 'light')

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

// 搜索趋势图表配置
const searchTrendOption = computed(() => {
  const days = []
  const searchCounts = []

  // 生成过去30天的数据
  for (let i = 29; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }))

    // 模拟搜索量数据（带有一些随机波动）
    const baseCount = 4000 + Math.sin(i * 0.2) * 1000 + Math.random() * 500
    searchCounts.push(Math.round(baseCount))
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        animation: false
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: {
        color: '#64748b'
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
        formatter: '{value}'
      },
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      }
    },
    series: [
      {
        name: '搜索次数',
        type: 'line',
        data: searchCounts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          color: '#3b82f6',
          width: 3
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
              {
                offset: 0,
                color: 'rgba(59, 130, 246, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(59, 130, 246, 0.05)'
              }
            ]
          }
        }
      }
    ]
  }
})

// 用户活跃度分布图表配置
const userActivityOption = computed(() => ({
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
      name: '用户活跃度',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '18',
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: 335, name: '日活用户', itemStyle: { color: '#22c55e' } },
        { value: 200, name: '周活用户', itemStyle: { color: '#3b82f6' } },
        { value: 150, name: '月活用户', itemStyle: { color: '#f59e0b' } },
        { value: 80, name: '沉睡用户', itemStyle: { color: '#ef4444' } }
      ]
    }
  ]
}))

// 搜索类型分布柱状图配置
const searchTypeOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  grid: {
    left: '15%',
    right: '10%',
    bottom: '15%',
    top: '10%'
  },
  xAxis: {
    type: 'value',
    axisLabel: {
      color: '#64748b'
    },
    axisLine: {
      lineStyle: {
        color: '#e2e8f0'
      }
    },
    splitLine: {
      lineStyle: {
        color: '#f1f5f9'
      }
    }
  },
  yAxis: {
    type: 'category',
    data: ['信用卡', '贷款', '理财', '保险', '基金'],
    axisLabel: {
      color: '#64748b'
    },
    axisLine: {
      lineStyle: {
        color: '#e2e8f0'
      }
    }
  },
  series: [
    {
      name: '搜索次数',
      type: 'bar',
      data: [1234, 987, 876, 654, 543],
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            {
              offset: 0,
              color: '#3b82f6'
            },
            {
              offset: 1,
              color: '#1d4ed8'
            }
          ]
        }
      },
      barWidth: '50%'
    }
  ]
}))

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

.chart {
  width: 100%;
  height: 100%;
}

/* 确保图表在容器中正确显示 */
:deep(.chart > div) {
  width: 100% !important;
  height: 100% !important;
}

/* 图表动画优化 */
.chart {
  transition: all 0.3s ease;
}

/* 卡片悬停效果 */
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

/* 响应式图表调整 */
@media (max-width: 768px) {
  .chart {
    height: 200px;
  }
}
</style>