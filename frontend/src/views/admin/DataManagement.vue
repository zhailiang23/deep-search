<template>
  <div class="data-management-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold text-foreground">数据管理</h1>
          <p class="text-muted-foreground mt-2">管理搜索数据和内容索引</p>
        </div>
        <Button @click="showImportDialog = true">
          导入数据
        </Button>
      </div>

      <!-- 数据统计 -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">总数据量</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.totalRecords.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">所有类型数据</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">产品数据</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.products.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">银行产品信息</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">文章内容</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.articles.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">文章和资讯</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-sm font-medium">问答数据</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-2xl font-bold">{{ stats.faqs.toLocaleString() }}</div>
            <p class="text-xs text-muted-foreground mt-1">常见问题</p>
          </CardContent>
        </Card>
      </div>

      <!-- 数据源管理 -->
      <Card>
        <CardHeader>
          <CardTitle>数据源管理</CardTitle>
          <CardDescription>管理不同的数据来源和同步状态</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-4">
            <div
              v-for="source in dataSources"
              :key="source.id"
              class="flex items-center justify-between p-4 border border-border rounded-lg"
            >
              <div class="flex items-center space-x-4">
                <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
                  <span class="text-lg">{{ source.icon }}</span>
                </div>
                <div>
                  <h4 class="font-semibold">{{ source.name }}</h4>
                  <p class="text-sm text-muted-foreground">{{ source.description }}</p>
                  <div class="flex items-center space-x-4 mt-1">
                    <span class="text-xs text-muted-foreground">
                      最后同步: {{ formatDateTime(source.lastSync) }}
                    </span>
                    <Badge :variant="source.status === 'active' ? 'default' : 'secondary'">
                      {{ source.status === 'active' ? '正常' : '暂停' }}
                    </Badge>
                  </div>
                </div>
              </div>
              <div class="flex items-center space-x-2">
                <Button size="sm" variant="outline" @click="syncDataSource(source)">
                  同步
                </Button>
                <Button size="sm" variant="outline" @click="configDataSource(source)">
                  配置
                </Button>
                <Button
                  size="sm"
                  :variant="source.status === 'active' ? 'outline' : 'default'"
                  @click="toggleDataSource(source)"
                >
                  {{ source.status === 'active' ? '暂停' : '启用' }}
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 数据列表 -->
      <Card>
        <CardHeader>
          <CardTitle>数据列表</CardTitle>
          <CardDescription>浏览和管理具体的数据记录</CardDescription>
        </CardHeader>
        <CardContent>
          <!-- 筛选器 -->
          <div class="flex flex-col md:flex-row gap-4 mb-6">
            <div class="flex-1">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索标题、内容..."
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
                @input="filterData"
              />
            </div>
            <div class="flex gap-2">
              <select
                v-model="filterType"
                @change="filterData"
                class="px-3 py-2 border border-border rounded-md bg-background"
              >
                <option value="">所有类型</option>
                <option value="product">产品</option>
                <option value="article">文章</option>
                <option value="faq">FAQ</option>
              </select>
              <select
                v-model="filterStatus"
                @change="filterData"
                class="px-3 py-2 border border-border rounded-md bg-background"
              >
                <option value="">所有状态</option>
                <option value="published">已发布</option>
                <option value="draft">草稿</option>
                <option value="archived">已归档</option>
              </select>
            </div>
          </div>

          <!-- 数据表格 -->
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border">
                  <th class="text-left py-3">标题</th>
                  <th class="text-left py-3">类型</th>
                  <th class="text-left py-3">状态</th>
                  <th class="text-left py-3">最后更新</th>
                  <th class="text-left py-3">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in filteredData"
                  :key="item.id"
                  class="border-b border-border hover:bg-muted/50"
                >
                  <td class="py-3">
                    <div>
                      <div class="font-medium">{{ item.title }}</div>
                      <div class="text-xs text-muted-foreground line-clamp-1">
                        {{ item.description }}
                      </div>
                    </div>
                  </td>
                  <td class="py-3">
                    <Badge variant="outline">
                      {{ getTypeText(item.type) }}
                    </Badge>
                  </td>
                  <td class="py-3">
                    <Badge :variant="getStatusVariant(item.status)">
                      {{ getStatusText(item.status) }}
                    </Badge>
                  </td>
                  <td class="py-3 text-muted-foreground">
                    {{ formatDateTime(item.updatedAt) }}
                  </td>
                  <td class="py-3">
                    <div class="flex items-center space-x-2">
                      <Button size="sm" variant="outline" @click="editItem(item)">
                        编辑
                      </Button>
                      <Button size="sm" variant="outline" @click="viewItem(item)">
                        查看
                      </Button>
                      <Button size="sm" variant="outline" @click="deleteItem(item)">
                        删除
                      </Button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="flex items-center justify-between mt-6">
            <div class="text-sm text-muted-foreground">
              显示 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, filteredData.length) }}
              共 {{ filteredData.length }} 条记录
            </div>
            <div class="flex items-center space-x-2">
              <Button
                size="sm"
                variant="outline"
                :disabled="currentPage === 1"
                @click="currentPage--"
              >
                上一页
              </Button>
              <span class="text-sm">{{ currentPage }} / {{ totalPages }}</span>
              <Button
                size="sm"
                variant="outline"
                :disabled="currentPage === totalPages"
                @click="currentPage++"
              >
                下一页
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 导入数据对话框 -->
    <Dialog :open="showImportDialog" @update:open="showImportDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>导入数据</DialogTitle>
          <DialogDescription>
            上传JSON文件批量导入数据
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div>
            <label class="text-sm font-medium mb-2 block">数据类型</label>
            <select
              v-model="importForm.type"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            >
              <option value="product">产品数据</option>
              <option value="article">文章数据</option>
              <option value="faq">FAQ数据</option>
            </select>
          </div>
          <div>
            <label class="text-sm font-medium mb-2 block">选择文件</label>
            <input
              ref="fileInput"
              type="file"
              accept=".json"
              @change="handleFileSelect"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            />
          </div>
          <div v-if="importFile">
            <p class="text-sm text-muted-foreground">
              已选择文件: {{ importFile.name }} ({{ formatFileSize(importFile.size) }})
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showImportDialog = false">
            取消
          </Button>
          <Button @click="importData" :disabled="!importFile">
            导入
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui'
import { Button } from '@/components/ui'
import { Badge } from '@/components/ui'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui'

// 数据统计
const stats = ref({
  totalRecords: 28567,
  products: 1234,
  articles: 5678,
  faqs: 987
})

// 数据源
const dataSources = ref([
  {
    id: 1,
    name: '产品数据库',
    description: '银行产品信息数据源',
    icon: '🏦',
    status: 'active',
    lastSync: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  {
    id: 2,
    name: 'CMS系统',
    description: '内容管理系统文章数据',
    icon: '📄',
    status: 'active',
    lastSync: new Date(Date.now() - 4 * 60 * 60 * 1000)
  },
  {
    id: 3,
    name: '客服系统',
    description: '常见问题FAQ数据',
    icon: '❓',
    status: 'inactive',
    lastSync: new Date(Date.now() - 24 * 60 * 60 * 1000)
  }
])

// 搜索和筛选
const searchQuery = ref('')
const filterType = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

// 对话框
const showImportDialog = ref(false)
const importForm = ref({
  type: 'product'
})
const importFile = ref<File | null>(null)

// 模拟数据
const dataList = ref([
  {
    id: 1,
    title: '信用卡产品介绍',
    description: '详细介绍各类信用卡产品的特点和申请条件',
    type: 'product',
    status: 'published',
    updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  {
    id: 2,
    title: '房贷利率调整通知',
    description: '关于房贷利率调整的最新政策通知',
    type: 'article',
    status: 'published',
    updatedAt: new Date(Date.now() - 4 * 60 * 60 * 1000)
  },
  {
    id: 3,
    title: '如何办理网银转账？',
    description: '网银转账的详细操作步骤和注意事项',
    type: 'faq',
    status: 'draft',
    updatedAt: new Date(Date.now() - 6 * 60 * 60 * 1000)
  }
])

// 筛选后的数据
const filteredData = computed(() => {
  return dataList.value.filter(item => {
    const matchesSearch = !searchQuery.value ||
      item.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.description.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesType = !filterType.value || item.type === filterType.value
    const matchesStatus = !filterStatus.value || item.status === filterStatus.value

    return matchesSearch && matchesType && matchesStatus
  })
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredData.value.length / pageSize.value)
})

// 工具函数
const getTypeText = (type: string) => {
  switch (type) {
    case 'product': return '产品'
    case 'article': return '文章'
    case 'faq': return 'FAQ'
    default: return type
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'published': return 'default'
    case 'draft': return 'secondary'
    case 'archived': return 'outline'
    default: return 'outline'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'published': return '已发布'
    case 'draft': return '草稿'
    case 'archived': return '已归档'
    default: return status
  }
}

const formatDateTime = (date: Date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 操作方法
const filterData = () => {
  currentPage.value = 1
}

const syncDataSource = (source: any) => {
  console.log('Syncing data source:', source.name)
  source.lastSync = new Date()
}

const configDataSource = (source: any) => {
  console.log('Configuring data source:', source.name)
}

const toggleDataSource = (source: any) => {
  source.status = source.status === 'active' ? 'inactive' : 'active'
}

const editItem = (item: any) => {
  console.log('Editing item:', item.title)
}

const viewItem = (item: any) => {
  console.log('Viewing item:', item.title)
}

const deleteItem = (item: any) => {
  if (confirm(`确定要删除 "${item.title}" 吗？`)) {
    const index = dataList.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      dataList.value.splice(index, 1)
    }
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    importFile.value = target.files[0]
  }
}

const importData = async () => {
  if (!importFile.value) return

  try {
    console.log('Importing data:', {
      type: importForm.value.type,
      file: importFile.value.name
    })

    // 模拟导入过程
    await new Promise(resolve => setTimeout(resolve, 2000))

    // 重置表单
    importFile.value = null
    importForm.value.type = 'product'
    showImportDialog.value = false

    console.log('Data imported successfully')
  } catch (error) {
    console.error('Import failed:', error)
  }
}

onMounted(() => {
  console.log('Data management page mounted')
})
</script>

<style scoped>
.data-management-page {
  /* 自定义样式 */
}

.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>