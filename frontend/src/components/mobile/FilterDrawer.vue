<template>
  <!-- 遮罩层 -->
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 bg-black/50"
      @click="handleClose"
    >
      <!-- 抽屉内容 -->
      <div
        class="fixed bottom-0 left-0 right-0 bg-background rounded-t-3xl max-h-[80vh] overflow-hidden"
        @click.stop
        :class="[
          'transform transition-transform duration-300 ease-out',
          open ? 'translate-y-0' : 'translate-y-full'
        ]"
      >
        <!-- 抽屉头部 -->
        <div class="flex items-center justify-between p-4 border-b border-border">
          <h2 class="text-lg font-semibold text-foreground">搜索筛选</h2>
          <button
            @click="handleClose"
            class="flex items-center justify-center w-8 h-8 rounded-full hover:bg-accent mobile-touch-target"
            aria-label="关闭"
          >
            <XIcon class="w-5 h-5 text-muted-foreground" />
          </button>
        </div>

        <!-- 抽屉拖拽指示器 -->
        <div class="flex justify-center py-2">
          <div class="w-8 h-1 bg-muted rounded-full"></div>
        </div>

        <!-- 筛选内容 -->
        <div class="flex-1 overflow-y-auto px-4 pb-4 safe-bottom">
          <!-- 内容类型筛选 -->
          <div class="mb-6">
            <h3 class="text-base font-medium text-foreground mb-3">内容类型</h3>
            <div class="grid grid-cols-2 gap-2">
              <button
                v-for="type in contentTypes"
                :key="type.value"
                @click="toggleContentType(type.value)"
                :class="[
                  'flex items-center justify-between p-3 rounded-lg border mobile-touch-target text-left',
                  localFilters.contentTypes.includes(type.value)
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'bg-card text-card-foreground border-border hover:bg-accent'
                ]"
              >
                <div class="flex items-center">
                  <component :is="type.icon" class="w-4 h-4 mr-2" />
                  <span class="text-sm font-medium">{{ type.label }}</span>
                </div>
                <CheckIcon
                  v-if="localFilters.contentTypes.includes(type.value)"
                  class="w-4 h-4"
                />
              </button>
            </div>
          </div>

          <!-- 时间范围筛选 -->
          <div class="mb-6">
            <h3 class="text-base font-medium text-foreground mb-3">时间范围</h3>
            <div class="space-y-2">
              <button
                v-for="range in timeRanges"
                :key="range.value"
                @click="selectTimeRange(range.value)"
                :class="[
                  'flex items-center justify-between w-full p-3 rounded-lg border mobile-touch-target text-left',
                  localFilters.timeRange === range.value
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'bg-card text-card-foreground border-border hover:bg-accent'
                ]"
              >
                <div class="flex items-center">
                  <ClockIcon class="w-4 h-4 mr-2" />
                  <span class="text-sm font-medium">{{ range.label }}</span>
                </div>
                <CheckIcon
                  v-if="localFilters.timeRange === range.value"
                  class="w-4 h-4"
                />
              </button>
            </div>
          </div>

          <!-- 排序方式 -->
          <div class="mb-6">
            <h3 class="text-base font-medium text-foreground mb-3">排序方式</h3>
            <div class="space-y-2">
              <button
                v-for="sort in sortOptions"
                :key="sort.value"
                @click="selectSortOption(sort.value)"
                :class="[
                  'flex items-center justify-between w-full p-3 rounded-lg border mobile-touch-target text-left',
                  localFilters.sortBy === sort.value
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'bg-card text-card-foreground border-border hover:bg-accent'
                ]"
              >
                <div class="flex items-center">
                  <ArrowUpDownIcon class="w-4 h-4 mr-2" />
                  <span class="text-sm font-medium">{{ sort.label }}</span>
                </div>
                <CheckIcon
                  v-if="localFilters.sortBy === sort.value"
                  class="w-4 h-4"
                />
              </button>
            </div>
          </div>

          <!-- 权限级别（如果需要） -->
          <div class="mb-6">
            <h3 class="text-base font-medium text-foreground mb-3">权限级别</h3>
            <div class="space-y-2">
              <button
                v-for="level in permissionLevels"
                :key="level.value"
                @click="togglePermissionLevel(level.value)"
                :class="[
                  'flex items-center justify-between w-full p-3 rounded-lg border mobile-touch-target text-left',
                  localFilters.permissionLevels.includes(level.value)
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'bg-card text-card-foreground border-border hover:bg-accent'
                ]"
              >
                <div class="flex items-center">
                  <ShieldIcon class="w-4 h-4 mr-2" />
                  <span class="text-sm font-medium">{{ level.label }}</span>
                </div>
                <CheckIcon
                  v-if="localFilters.permissionLevels.includes(level.value)"
                  class="w-4 h-4"
                />
              </button>
            </div>
          </div>
        </div>

        <!-- 底部操作按钮 -->
        <div class="flex gap-3 p-4 border-t border-border bg-background safe-bottom">
          <button
            @click="handleReset"
            class="flex-1 py-3 px-4 border border-border rounded-lg text-foreground hover:bg-accent mobile-touch-target"
          >
            重置
          </button>
          <button
            @click="handleApply"
            class="flex-1 py-3 px-4 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 mobile-touch-target"
          >
            应用筛选
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  XIcon,
  CheckIcon,
  ClockIcon,
  ArrowUpDownIcon,
  ShieldIcon,
  FileTextIcon,
  CreditCardIcon,
  TrendingUpIcon,
  PhoneIcon,
  HelpCircleIcon,
  BookOpenIcon
} from 'lucide-vue-next'

interface FilterOptions {
  contentTypes: string[]
  timeRange: string
  sortBy: string
  permissionLevels: string[]
}

interface Props {
  open: boolean
  filters?: FilterOptions
}

const props = withDefaults(defineProps<Props>(), {
  filters: () => ({
    contentTypes: [],
    timeRange: 'all',
    sortBy: 'relevance',
    permissionLevels: []
  })
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  'apply-filters': [filters: FilterOptions]
}>()

// 本地筛选状态
const localFilters = ref<FilterOptions>({ ...props.filters })

// 内容类型选项
const contentTypes = [
  { value: 'product', label: '产品信息', icon: CreditCardIcon },
  { value: 'service', label: '服务指南', icon: FileTextIcon },
  { value: 'investment', label: '投资理财', icon: TrendingUpIcon },
  { value: 'mobile', label: '手机银行', icon: PhoneIcon },
  { value: 'faq', label: '常见问题', icon: HelpCircleIcon },
  { value: 'policy', label: '政策文档', icon: BookOpenIcon }
]

// 时间范围选项
const timeRanges = [
  { value: 'all', label: '全部时间' },
  { value: 'day', label: '最近一天' },
  { value: 'week', label: '最近一周' },
  { value: 'month', label: '最近一月' },
  { value: 'quarter', label: '最近三月' },
  { value: 'year', label: '最近一年' }
]

// 排序选项
const sortOptions = [
  { value: 'relevance', label: '相关性' },
  { value: 'date', label: '更新时间' },
  { value: 'popularity', label: '热门程度' },
  { value: 'title', label: '标题排序' }
]

// 权限级别选项
const permissionLevels = [
  { value: 'public', label: '公开信息' },
  { value: 'customer', label: '客户专享' },
  { value: 'vip', label: 'VIP专享' },
  { value: 'internal', label: '内部信息' }
]

// 监听 props 变化
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })

// 事件处理函数
const handleClose = () => {
  emit('update:open', false)
}

const toggleContentType = (type: string) => {
  const index = localFilters.value.contentTypes.indexOf(type)
  if (index > -1) {
    localFilters.value.contentTypes.splice(index, 1)
  } else {
    localFilters.value.contentTypes.push(type)
  }
}

const selectTimeRange = (range: string) => {
  localFilters.value.timeRange = range
}

const selectSortOption = (sort: string) => {
  localFilters.value.sortBy = sort
}

const togglePermissionLevel = (level: string) => {
  const index = localFilters.value.permissionLevels.indexOf(level)
  if (index > -1) {
    localFilters.value.permissionLevels.splice(index, 1)
  } else {
    localFilters.value.permissionLevels.push(level)
  }
}

const handleReset = () => {
  localFilters.value = {
    contentTypes: [],
    timeRange: 'all',
    sortBy: 'relevance',
    permissionLevels: []
  }
}

const handleApply = () => {
  emit('apply-filters', { ...localFilters.value })
  emit('update:open', false)
}
</script>

<style scoped>
.mobile-touch-target {
  min-height: 44px;
  min-width: 44px;
  touch-action: manipulation;
}

.mobile-touch-target:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

/* 抽屉滑动动画 */
@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(100%);
  }
}

/* 改进的滚动 */
.overflow-y-auto {
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}
</style>