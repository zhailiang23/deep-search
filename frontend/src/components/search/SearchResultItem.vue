<template>
  <div
    :class="[
      'search-result-item group border border-border rounded-lg bg-background hover:shadow-md transition-all duration-200 cursor-pointer',
      compactMode ? 'p-3' : 'p-4',
      isSelected && 'ring-2 ring-primary'
    ]"
    @click="handleClick"
  >
    <div class="flex gap-4">
      <!-- 缩略图 -->
      <div v-if="showThumbnail && result.thumbnail" class="flex-shrink-0">
        <img
          :src="result.thumbnail"
          :alt="result.title"
          :class="[
            'rounded-md object-cover bg-muted',
            compactMode ? 'w-16 h-16' : 'w-20 h-20'
          ]"
          @error="handleImageError"
        />
      </div>

      <!-- 类型图标（无缩略图时显示） -->
      <div v-else-if="!result.thumbnail" class="flex-shrink-0">
        <div
          :class="[
            'bg-muted rounded-md flex items-center justify-center text-muted-foreground',
            compactMode ? 'w-12 h-12 text-lg' : 'w-16 h-16 text-xl'
          ]"
        >
          {{ getTypeIcon(result.type) }}
        </div>
      </div>

      <!-- 主要内容 -->
      <div class="flex-1 min-w-0">
        <div class="flex items-start justify-between">
          <!-- 标题和描述 -->
          <div class="flex-1 min-w-0">
            <!-- 标题 -->
            <h3
              :class="[
                'font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2',
                compactMode ? 'text-base mb-1' : 'text-lg mb-2'
              ]"
              v-html="highlightText(result.title, highlightQuery)"
            ></h3>

            <!-- 描述 -->
            <p
              :class="[
                'text-muted-foreground line-clamp-3 mb-2',
                compactMode ? 'text-sm' : 'text-base'
              ]"
              v-html="highlightText(result.description, highlightQuery)"
            ></p>

            <!-- 元数据 -->
            <div v-if="showMetadata" class="flex items-center flex-wrap gap-2 text-xs text-muted-foreground">
              <!-- 类型标识 -->
              <Badge variant="outline" class="text-xs">
                {{ getTypeLabel(result.type) }}
              </Badge>

              <!-- 分类 -->
              <span v-if="result.category" class="flex items-center">
                <span class="mr-1">📁</span>
                {{ result.category }}
              </span>

              <!-- 日期 -->
              <span v-if="result.date" class="flex items-center">
                <span class="mr-1">📅</span>
                {{ formatDate(result.date) }}
              </span>

              <!-- 相关性评分 -->
              <span v-if="result.score && result.score > 0" class="flex items-center">
                <span class="mr-1">⭐</span>
                {{ Math.round(result.score * 100) }}%
              </span>

              <!-- URL -->
              <span class="flex items-center truncate max-w-48">
                <span class="mr-1">🔗</span>
                <span class="truncate">{{ formatUrl(result.url) }}</span>
              </span>
            </div>

            <!-- 标签 -->
            <div v-if="result.tags?.length" class="flex items-center flex-wrap gap-1 mt-2">
              <Badge
                v-for="tag in result.tags.slice(0, 3)"
                :key="tag"
                variant="secondary"
                class="text-xs"
              >
                {{ tag }}
              </Badge>
              <span v-if="result.tags.length > 3" class="text-xs text-muted-foreground">
                +{{ result.tags.length - 3 }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex-shrink-0 ml-4">
            <div class="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <!-- 预览按钮 -->
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click.stop="handlePreview"
                    class="h-8 w-8 p-0"
                  >
                    <span class="text-sm">👁️</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>预览</p>
                </TooltipContent>
              </Tooltip>

              <!-- 书签按钮 -->
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click.stop="handleBookmark"
                    class="h-8 w-8 p-0"
                    :class="isBookmarked && 'text-yellow-500'"
                  >
                    <span class="text-sm">{{ isBookmarked ? '⭐' : '☆' }}</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>{{ isBookmarked ? '取消收藏' : '收藏' }}</p>
                </TooltipContent>
              </Tooltip>

              <!-- 分享按钮 -->
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click.stop="handleShare"
                    class="h-8 w-8 p-0"
                  >
                    <span class="text-sm">📤</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>分享</p>
                </TooltipContent>
              </Tooltip>

              <!-- 更多操作 -->
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click.stop
                    class="h-8 w-8 p-0"
                  >
                    <span class="text-sm">⋮</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem @click="handleCopyLink">
                    <span class="mr-2">🔗</span>
                    复制链接
                  </DropdownMenuItem>
                  <DropdownMenuItem @click="handleOpenInNewTab">
                    <span class="mr-2">🗗</span>
                    新标签页打开
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem @click="handleReportIssue">
                    <span class="mr-2">🚨</span>
                    报告问题
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        </div>

        <!-- 展开内容（可选） -->
        <div v-if="expanded && result.content" class="mt-4 pt-4 border-t border-border">
          <div class="prose prose-sm max-w-none text-muted-foreground">
            <div v-html="truncateContent(result.content, 200)"></div>
          </div>
          <Button
            v-if="result.content.length > 200"
            size="sm"
            variant="ghost"
            @click.stop="toggleExpanded"
            class="mt-2 text-xs"
          >
            {{ contentExpanded ? '收起' : '展开全文' }}
          </Button>
        </div>

        <!-- 子结果（相关结果） -->
        <div v-if="result.subResults?.length" class="mt-3 pl-4 border-l-2 border-muted">
          <div class="text-xs text-muted-foreground mb-2">相关结果:</div>
          <div class="space-y-1">
            <div
              v-for="subResult in result.subResults.slice(0, 2)"
              :key="subResult.id"
              class="text-sm hover:text-primary cursor-pointer"
              @click.stop="handleSubResultClick(subResult)"
            >
              {{ subResult.title }}
            </div>
            <div v-if="result.subResults.length > 2" class="text-xs text-muted-foreground">
              还有 {{ result.subResults.length - 2 }} 个相关结果
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 结果序号 -->
    <div
      v-if="typeof index === 'number'"
      class="absolute -left-8 top-4 text-xs text-muted-foreground font-mono"
    >
      {{ index + 1 }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Badge, Button, Tooltip, TooltipContent, TooltipTrigger, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui'

interface SearchResult {
  id: string
  title: string
  description: string
  url: string
  type: 'product' | 'article' | 'faq' | 'page'
  score: number
  date?: Date
  category?: string
  tags?: string[]
  thumbnail?: string
  content?: string
  subResults?: Array<{ id: string; title: string; url: string }>
  metadata?: Record<string, any>
}

interface Props {
  result: SearchResult
  index?: number
  highlightQuery?: string
  showThumbnail?: boolean
  showMetadata?: boolean
  compactMode?: boolean
  selectable?: boolean
  isSelected?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  highlightQuery: '',
  showThumbnail: true,
  showMetadata: true,
  compactMode: false,
  selectable: false,
  isSelected: false
})

const emit = defineEmits<{
  click: [result: SearchResult]
  preview: [result: SearchResult]
  bookmark: [result: SearchResult]
  share: [result: SearchResult]
}>()

// 响应式数据
const expanded = ref(false)
const contentExpanded = ref(false)
const isBookmarked = ref(false) // 实际应该从存储中获取

// 计算属性
const shouldShowImage = computed(() => {
  return props.showThumbnail && props.result.thumbnail
})

// 方法
const getTypeIcon = (type: string): string => {
  const icons: Record<string, string> = {
    product: '🏦',
    article: '📄',
    faq: '❓',
    page: '📝'
  }
  return icons[type] || '📄'
}

const getTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    product: '产品',
    article: '文章',
    faq: '问答',
    page: '页面'
  }
  return labels[type] || '内容'
}

const highlightText = (text: string, query: string): string => {
  if (!query.trim()) return text

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<mark class="bg-primary/20 text-primary font-medium">$1</mark>')
}

const formatDate = (date: Date): string => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return '今天'
  } else if (days === 1) {
    return '昨天'
  } else if (days < 30) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const formatUrl = (url: string): string => {
  try {
    const urlObj = new URL(url)
    return urlObj.hostname + urlObj.pathname
  } catch {
    return url
  }
}

const truncateContent = (content: string, maxLength: number): string => {
  if (content.length <= maxLength || contentExpanded.value) {
    return content
  }
  return content.substring(0, maxLength) + '...'
}

// 事件处理
const handleClick = () => {
  emit('click', props.result)
}

const handlePreview = () => {
  emit('preview', props.result)
}

const handleBookmark = () => {
  isBookmarked.value = !isBookmarked.value
  emit('bookmark', props.result)
}

const handleShare = () => {
  emit('share', props.result)
}

const handleCopyLink = async () => {
  try {
    await navigator.clipboard.writeText(props.result.url)
    // 可以显示成功提示
    console.log('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
  }
}

const handleOpenInNewTab = () => {
  window.open(props.result.url, '_blank')
}

const handleReportIssue = () => {
  // 实现问题报告功能
  console.log('报告问题:', props.result.id)
}

const handleSubResultClick = (subResult: any) => {
  // 处理子结果点击
  console.log('子结果点击:', subResult)
}

const toggleExpanded = () => {
  contentExpanded.value = !contentExpanded.value
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped>
.search-result-item {
  position: relative;
}

/* 高亮标记样式 */
:deep(mark) {
  background-color: rgb(var(--primary) / 0.2);
  color: rgb(var(--primary));
  font-weight: 500;
  padding: 0 2px;
  border-radius: 2px;
}

/* 文本截断 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-result-item {
    /* 移动端样式调整 */
  }
}
</style>