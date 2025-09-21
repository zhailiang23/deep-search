<template>
  <div class="search-filters">
    <div class="filters-header">
      <h3 class="text-lg font-semibold text-foreground">搜索筛选</h3>
      <Button
        variant="ghost"
        size="sm"
        @click="clearFilters"
        class="text-sm text-muted-foreground hover:text-foreground"
      >
        <X class="w-4 h-4 mr-1" />
        清除所有
      </Button>
    </div>

    <div class="filters-content space-y-4">
      <!-- 内容类型筛选 -->
      <div class="filter-group">
        <div class="flex items-center space-x-2 mb-3">
          <FileText class="w-4 h-4 text-muted-foreground" />
          <Label class="text-sm font-medium text-foreground">内容类型</Label>
        </div>
        <div class="space-y-3">
          <div class="flex items-center space-x-3" v-for="type in contentTypes" :key="type.value">
            <Checkbox
              :id="type.value"
              :checked="selectedContentTypes.includes(type.value)"
              @update:modelValue="(checked) => toggleContentType(type.value, checked)"
            />
            <Label :for="type.value" class="text-sm text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
              {{ type.label }}
            </Label>
          </div>
        </div>
      </div>

      <!-- 时间范围筛选 -->
      <div class="filter-group">
        <div class="flex items-center space-x-2 mb-3">
          <Clock class="w-4 h-4 text-muted-foreground" />
          <Label class="text-sm font-medium text-foreground">时间范围</Label>
        </div>
        <Select v-model="selectedTimeRange" @update:modelValue="updateFilters">
          <SelectTrigger class="w-full">
            <SelectValue placeholder="选择时间范围" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部时间</SelectItem>
            <SelectItem value="day">最近一天</SelectItem>
            <SelectItem value="week">最近一周</SelectItem>
            <SelectItem value="month">最近一月</SelectItem>
            <SelectItem value="year">最近一年</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- 相关度排序 -->
      <div class="filter-group">
        <div class="flex items-center space-x-2 mb-3">
          <ArrowUpDown class="w-4 h-4 text-muted-foreground" />
          <Label class="text-sm font-medium text-foreground">排序方式</Label>
        </div>
        <Select v-model="selectedSortBy" @update:modelValue="updateFilters">
          <SelectTrigger class="w-full">
            <SelectValue placeholder="选择排序方式" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="relevance">相关度</SelectItem>
            <SelectItem value="date">时间</SelectItem>
            <SelectItem value="title">标题</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- 权限级别 -->
      <div class="filter-group">
        <div class="flex items-center space-x-2 mb-3">
          <Shield class="w-4 h-4 text-muted-foreground" />
          <Label class="text-sm font-medium text-foreground">权限级别</Label>
        </div>
        <div class="space-y-3">
          <div class="flex items-center space-x-3" v-for="level in permissionLevels" :key="level.value">
            <Checkbox
              :id="level.value"
              :checked="selectedPermissionLevels.includes(level.value)"
              @update:modelValue="(checked) => togglePermissionLevel(level.value, checked)"
            />
            <Label :for="level.value" class="text-sm text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
              {{ level.label }}
            </Label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Button, Checkbox, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui'
import {
  X,
  FileText,
  Clock,
  ArrowUpDown,
  Shield
} from 'lucide-vue-next'

// 定义筛选器接口
interface SearchFilters {
  contentTypes: string[]
  timeRange: string
  sortBy: string
  permissionLevels: string[]
}

// 组件属性
interface Props {
  modelValue?: SearchFilters
}

// 组件事件
interface Emits {
  (e: 'update:modelValue', filters: SearchFilters): void
  (e: 'filtersChanged', filters: SearchFilters): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => ({
    contentTypes: [],
    timeRange: 'all',
    sortBy: 'relevance',
    permissionLevels: []
  })
})

const emit = defineEmits<Emits>()

// 内容类型选项
const contentTypes = ref([
  { label: '银行产品', value: 'product' },
  { label: '服务介绍', value: 'service' },
  { label: '活动信息', value: 'activity' },
  { label: '政策公告', value: 'policy' },
  { label: '帮助文档', value: 'help' }
])

// 权限级别选项
const permissionLevels = ref([
  { label: '公开信息', value: 'public' },
  { label: '客户专享', value: 'customer' },
  { label: 'VIP专享', value: 'vip' },
  { label: '内部信息', value: 'internal' }
])

// 筛选器状态
const selectedContentTypes = ref<string[]>(props.modelValue.contentTypes)
const selectedTimeRange = ref<string>(props.modelValue.timeRange)
const selectedSortBy = ref<string>(props.modelValue.sortBy)
const selectedPermissionLevels = ref<string[]>(props.modelValue.permissionLevels)

// 切换内容类型
const toggleContentType = (value: string, checked: boolean) => {
  if (checked) {
    if (!selectedContentTypes.value.includes(value)) {
      selectedContentTypes.value.push(value)
    }
  } else {
    const index = selectedContentTypes.value.indexOf(value)
    if (index > -1) {
      selectedContentTypes.value.splice(index, 1)
    }
  }
  updateFilters()
}

// 切换权限级别
const togglePermissionLevel = (value: string, checked: boolean) => {
  if (checked) {
    if (!selectedPermissionLevels.value.includes(value)) {
      selectedPermissionLevels.value.push(value)
    }
  } else {
    const index = selectedPermissionLevels.value.indexOf(value)
    if (index > -1) {
      selectedPermissionLevels.value.splice(index, 1)
    }
  }
  updateFilters()
}

// 更新筛选器
const updateFilters = () => {
  const filters: SearchFilters = {
    contentTypes: selectedContentTypes.value,
    timeRange: selectedTimeRange.value,
    sortBy: selectedSortBy.value,
    permissionLevels: selectedPermissionLevels.value
  }

  emit('update:modelValue', filters)
  emit('filtersChanged', filters)
}

// 清除所有筛选器
const clearFilters = () => {
  selectedContentTypes.value = []
  selectedTimeRange.value = 'all'
  selectedSortBy.value = 'relevance'
  selectedPermissionLevels.value = []
  updateFilters()
}

// 暴露组件方法
defineExpose({
  clearFilters,
  updateFilters
})
</script>

<style scoped>
.search-filters {
  @apply bg-background border border-border rounded-lg p-6;
}

.filters-header {
  @apply flex items-center justify-between pb-4 border-b border-border;
}

.filters-content {
  @apply pt-6;
}

.filter-group {
  @apply pb-6 border-b border-border last:border-b-0 last:pb-0;
}

/* 增强hover效果 */
.filter-group:hover {
  @apply bg-muted/30 rounded-md p-3 -m-3 transition-colors;
}

/* 复选框组样式 */
.filter-group .space-y-3 > div {
  @apply p-2 rounded-md hover:bg-muted/50 transition-colors;
}
</style>