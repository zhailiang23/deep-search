<template>
  <div class="search-management-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">搜索管理</h1>
        <p class="text-muted-foreground mt-2">管理搜索配置和索引数据</p>
      </div>

      <!-- 操作按钮 -->
      <div class="flex flex-wrap gap-4">
        <Button @click="refreshIndex" :loading="isRefreshing">
          <RefreshCwIcon class="w-4 h-4 mr-2" />
          刷新索引
        </Button>
        <Button @click="optimizeIndex" variant="outline" :loading="isOptimizing">
          <ZapIcon class="w-4 h-4 mr-2" />
          优化索引
        </Button>
        <Button @click="exportData" variant="outline" :loading="isExporting">
          <DownloadIcon class="w-4 h-4 mr-2" />
          导出数据
        </Button>
        <Button @click="showBulkOperations = !showBulkOperations" variant="secondary">
          <SettingsIcon class="w-4 h-4 mr-2" />
          批量操作
        </Button>
      </div>

      <!-- 批量操作面板 -->
      <Card v-if="showBulkOperations">
        <CardHeader>
          <CardTitle>批量操作</CardTitle>
          <CardDescription>对多个索引执行批量操作</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-4">
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="index in selectedIndexes"
                :key="index"
                variant="outline"
                class="cursor-pointer"
                @click="toggleIndexSelection(index)"
              >
                {{ index }}
                <XIcon class="w-3 h-3 ml-1" />
              </Badge>
            </div>
            <div class="flex gap-2">
              <Button
                @click="bulkRebuild"
                :disabled="selectedIndexes.length === 0"
                size="sm"
              >
                批量重建
              </Button>
              <Button
                @click="bulkOptimize"
                :disabled="selectedIndexes.length === 0"
                size="sm"
                variant="outline"
              >
                批量优化
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 搜索配置 -->
      <Card>
        <CardHeader>
          <CardTitle>搜索配置</CardTitle>
          <CardDescription>配置搜索引擎参数</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <Label for="maxResults">最大搜索结果数</Label>
                <Input
                  id="maxResults"
                  v-model="config.maxResults"
                  type="number"
                  placeholder="100"
                />
                <p class="text-xs text-muted-foreground">设置单次搜索返回的最大结果数量</p>
              </div>

              <div class="space-y-2">
                <Label for="timeout">搜索超时时间 (秒)</Label>
                <Input
                  id="timeout"
                  v-model="config.timeout"
                  type="number"
                  placeholder="30"
                />
                <p class="text-xs text-muted-foreground">搜索请求的最大等待时间</p>
              </div>

              <div class="space-y-2">
                <Label for="similarityThreshold">相似度阈值</Label>
                <Input
                  id="similarityThreshold"
                  v-model="config.similarityThreshold"
                  type="number"
                  step="0.1"
                  min="0"
                  max="1"
                  placeholder="0.7"
                />
                <p class="text-xs text-muted-foreground">语义搜索的最小相似度阈值</p>
              </div>

              <div class="space-y-2">
                <Label for="minQueryLength">最小查询长度</Label>
                <Input
                  id="minQueryLength"
                  v-model="config.minQueryLength"
                  type="number"
                  min="1"
                  placeholder="2"
                />
                <p class="text-xs text-muted-foreground">允许搜索的最小字符数</p>
              </div>
            </div>

            <Separator />

            <div class="space-y-4">
              <h4 class="text-sm font-medium">搜索功能开关</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="flex items-center justify-between">
                  <div class="space-y-0.5">
                    <Label>语义搜索</Label>
                    <p class="text-xs text-muted-foreground">使用AI模型进行语义理解</p>
                  </div>
                  <Switch v-model:checked="config.enableSemantic" />
                </div>

                <div class="flex items-center justify-between">
                  <div class="space-y-0.5">
                    <Label>搜索建议</Label>
                    <p class="text-xs text-muted-foreground">启用自动搜索建议功能</p>
                  </div>
                  <Switch v-model:checked="config.enableSuggestions" />
                </div>

                <div class="flex items-center justify-between">
                  <div class="space-y-0.5">
                    <Label>同义词扩展</Label>
                    <p class="text-xs text-muted-foreground">自动扩展同义词进行搜索</p>
                  </div>
                  <Switch v-model:checked="config.enableSynonyms" />
                </div>

                <div class="flex items-center justify-between">
                  <div class="space-y-0.5">
                    <Label>模糊匹配</Label>
                    <p class="text-xs text-muted-foreground">容忍拼写错误的搜索</p>
                  </div>
                  <Switch v-model:checked="config.enableFuzzy" />
                </div>
              </div>
            </div>

            <div class="flex gap-3">
              <Button @click="saveConfig" :loading="isSaving">
                <SaveIcon class="w-4 h-4 mr-2" />
                保存配置
              </Button>
              <Button @click="resetConfig" variant="outline">
                <RotateCcwIcon class="w-4 h-4 mr-2" />
                重置配置
              </Button>
              <Button @click="testConfig" variant="secondary">
                <PlayIcon class="w-4 h-4 mr-2" />
                测试配置
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 索引状态 -->
      <Card>
        <CardHeader>
          <CardTitle>索引状态</CardTitle>
          <CardDescription>搜索索引的当前状态</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="text-center">
              <div class="text-2xl font-bold text-primary">{{ indexStats.totalDocuments.toLocaleString() }}</div>
              <div class="text-sm text-muted-foreground">总文档数</div>
            </div>
            <div class="text-center">
              <div class="text-2xl font-bold text-green-600">{{ indexStats.indexedDocuments.toLocaleString() }}</div>
              <div class="text-sm text-muted-foreground">已索引文档</div>
            </div>
            <div class="text-center">
              <div class="text-2xl font-bold text-orange-600">{{ indexStats.pendingDocuments.toLocaleString() }}</div>
              <div class="text-sm text-muted-foreground">待索引文档</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 索引管理 -->
      <Card>
        <CardHeader>
          <CardTitle>索引管理</CardTitle>
          <CardDescription>管理搜索索引和数据源</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-4">
            <div
              v-for="index in indexes"
              :key="index.name"
              class="flex items-center justify-between p-4 border border-border rounded-lg hover:bg-muted/50 transition-colors"
            >
              <div class="flex items-center space-x-4">
                <Checkbox
                  :checked="selectedIndexes.includes(index.name)"
                  @update:checked="toggleIndexSelection(index.name)"
                />
                <div class="flex items-center space-x-3">
                  <div class="w-3 h-3 rounded-full" :class="getStatusColor(index.status)"></div>
                  <div>
                    <h4 class="font-semibold">{{ index.displayName }}</h4>
                    <p class="text-sm text-muted-foreground">{{ index.description }}</p>
                    <div class="flex items-center space-x-4 mt-1">
                      <span class="text-xs text-muted-foreground">
                        文档数: {{ index.documentCount.toLocaleString() }}
                      </span>
                      <span class="text-xs text-muted-foreground">
                        大小: {{ formatSize(index.size) }}
                      </span>
                      <span class="text-xs text-muted-foreground">
                        更新: {{ formatTime(index.lastUpdated) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="flex items-center space-x-2">
                <Badge :variant="getStatusVariant(index.status)">
                  {{ getStatusText(index.status) }}
                </Badge>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon">
                      <MoreHorizontalIcon class="w-4 h-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem @click="rebuildIndex(index.name)">
                      <RefreshCwIcon class="mr-2 h-4 w-4" />
                      重建索引
                    </DropdownMenuItem>
                    <DropdownMenuItem @click="optimizeIndex(index.name)">
                      <ZapIcon class="mr-2 h-4 w-4" />
                      优化索引
                    </DropdownMenuItem>
                    <DropdownMenuItem @click="viewIndexDetails(index.name)">
                      <InfoIcon class="mr-2 h-4 w-4" />
                      查看详情
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem @click="deleteIndex(index.name)" class="text-destructive">
                      <TrashIcon class="mr-2 h-4 w-4" />
                      删除索引
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          </div>

          <!-- 添加新索引 -->
          <div class="pt-4 border-t">
            <Button @click="showCreateIndex = true" variant="outline" class="w-full">
              <PlusIcon class="w-4 h-4 mr-2" />
              添加新索引
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- 创建索引对话框 -->
      <Dialog v-model:open="showCreateIndex">
        <DialogContent>
          <DialogHeader>
            <DialogTitle>创建新索引</DialogTitle>
            <DialogDescription>
              配置新的搜索索引
            </DialogDescription>
          </DialogHeader>
          <div class="space-y-4">
            <div class="space-y-2">
              <Label for="indexName">索引名称</Label>
              <Input
                id="indexName"
                v-model="newIndex.name"
                placeholder="例如: products"
              />
            </div>
            <div class="space-y-2">
              <Label for="indexDescription">描述</Label>
              <Input
                id="indexDescription"
                v-model="newIndex.description"
                placeholder="例如: 银行产品和服务数据"
              />
            </div>
            <div class="space-y-2">
              <Label for="indexType">索引类型</Label>
              <Select v-model="newIndex.type">
                <SelectTrigger>
                  <SelectValue placeholder="选择索引类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="products">产品数据</SelectItem>
                  <SelectItem value="articles">文章内容</SelectItem>
                  <SelectItem value="faqs">常见问题</SelectItem>
                  <SelectItem value="documents">文档资料</SelectItem>
                  <SelectItem value="custom">自定义</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button @click="showCreateIndex = false" variant="outline">
              取消
            </Button>
            <Button @click="createIndex" :loading="isCreating">
              创建索引
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Button,
  Input,
  Label,
  Switch,
  Separator,
  Badge,
  Checkbox,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui'
import {
  RefreshCwIcon,
  ZapIcon,
  DownloadIcon,
  SettingsIcon,
  XIcon,
  SaveIcon,
  RotateCcwIcon,
  PlayIcon,
  MoreHorizontalIcon,
  InfoIcon,
  TrashIcon,
  PlusIcon
} from 'lucide-vue-next'

// 加载状态
const isRefreshing = ref(false)
const isOptimizing = ref(false)
const isExporting = ref(false)
const isSaving = ref(false)
const isCreating = ref(false)

// UI状态
const showBulkOperations = ref(false)
const showCreateIndex = ref(false)
const selectedIndexes = ref<string[]>([])

// 搜索配置
const config = ref({
  maxResults: 100,
  timeout: 30,
  similarityThreshold: 0.7,
  minQueryLength: 2,
  enableSemantic: true,
  enableSuggestions: true,
  enableSynonyms: true,
  enableFuzzy: false
})

// 索引统计
const indexStats = ref({
  totalDocuments: 15847,
  indexedDocuments: 15234,
  pendingDocuments: 613
})

// 索引列表
const indexes = ref([
  {
    name: 'products',
    displayName: '产品索引',
    description: '银行产品和服务数据',
    status: 'active',
    documentCount: 5847,
    size: 2048576, // bytes
    lastUpdated: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  {
    name: 'articles',
    displayName: '文章索引',
    description: '文章和资讯内容',
    status: 'active',
    documentCount: 8234,
    size: 4194304,
    lastUpdated: new Date(Date.now() - 1 * 60 * 60 * 1000)
  },
  {
    name: 'faqs',
    displayName: 'FAQ索引',
    description: '常见问题数据',
    status: 'building',
    documentCount: 1766,
    size: 512000,
    lastUpdated: new Date(Date.now() - 30 * 60 * 1000)
  },
  {
    name: 'documents',
    displayName: '文档索引',
    description: '政策文档和规章制度',
    status: 'error',
    documentCount: 0,
    size: 0,
    lastUpdated: new Date(Date.now() - 24 * 60 * 60 * 1000)
  }
])

// 新索引表单
const newIndex = ref({
  name: '',
  description: '',
  type: ''
})

// 辅助函数
const getStatusColor = (status: string) => {
  switch (status) {
    case 'active':
      return 'bg-green-500'
    case 'building':
      return 'bg-yellow-500'
    case 'error':
      return 'bg-red-500'
    default:
      return 'bg-gray-500'
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active':
      return 'default'
    case 'building':
      return 'secondary'
    case 'error':
      return 'destructive'
    default:
      return 'outline'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'active':
      return '正常'
    case 'building':
      return '构建中'
    case 'error':
      return '错误'
    default:
      return '未知'
  }
}

const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (hours > 24) {
    const days = Math.floor(hours / 24)
    return `${days}天前`
  } else if (hours > 0) {
    return `${hours}小时前`
  } else {
    return `${minutes}分钟前`
  }
}

// 索引选择
const toggleIndexSelection = (indexName: string) => {
  const index = selectedIndexes.value.indexOf(indexName)
  if (index > -1) {
    selectedIndexes.value.splice(index, 1)
  } else {
    selectedIndexes.value.push(indexName)
  }
}

// 操作方法
const refreshIndex = async () => {
  isRefreshing.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 2000))
    console.log('Index refreshed')
  } finally {
    isRefreshing.value = false
  }
}

const optimizeIndex = async (indexName?: string) => {
  isOptimizing.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 3000))
    console.log('Index optimized:', indexName || 'all')
  } finally {
    isOptimizing.value = false
  }
}

const exportData = async () => {
  isExporting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 2000))
    console.log('Data exported')
  } finally {
    isExporting.value = false
  }
}

const saveConfig = async () => {
  isSaving.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    console.log('Config saved:', config.value)
  } finally {
    isSaving.value = false
  }
}

const resetConfig = () => {
  config.value = {
    maxResults: 100,
    timeout: 30,
    similarityThreshold: 0.7,
    minQueryLength: 2,
    enableSemantic: true,
    enableSuggestions: true,
    enableSynonyms: true,
    enableFuzzy: false
  }
}

const testConfig = () => {
  console.log('Testing config:', config.value)
  // 实现配置测试逻辑
}

const bulkRebuild = async () => {
  console.log('Bulk rebuilding indexes:', selectedIndexes.value)
  // 实现批量重建逻辑
}

const bulkOptimize = async () => {
  console.log('Bulk optimizing indexes:', selectedIndexes.value)
  // 实现批量优化逻辑
}

const rebuildIndex = async (indexName: string) => {
  console.log('Rebuilding index:', indexName)
  // 实现重建索引逻辑
}

const viewIndexDetails = (indexName: string) => {
  console.log('Viewing index details:', indexName)
  // 实现查看索引详情逻辑
}

const deleteIndex = async (indexName: string) => {
  if (confirm(`确定要删除索引 "${indexName}" 吗？此操作无法撤销。`)) {
    console.log('Deleting index:', indexName)
    // 实现删除索引逻辑
  }
}

const createIndex = async () => {
  isCreating.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 2000))
    console.log('Creating index:', newIndex.value)
    showCreateIndex.value = false
    // 重置表单
    newIndex.value = { name: '', description: '', type: '' }
  } finally {
    isCreating.value = false
  }
}

onMounted(() => {
  console.log('Search management page mounted')
  // 加载配置和状态数据
})
</script>

<style scoped>
.search-management-page {
  /* 自定义样式 */
}
</style>