<template>
  <div class="search-management-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">搜索管理</h1>
        <p class="text-muted-foreground mt-2">管理搜索配置和索引数据</p>
      </div>

      <!-- 操作按钮 -->
      <div class="flex gap-4">
        <Button @click="refreshIndex">刷新索引</Button>
        <Button @click="optimizeIndex" variant="outline">优化索引</Button>
        <Button @click="exportData" variant="outline">导出数据</Button>
      </div>

      <!-- 搜索配置 -->
      <Card>
        <CardHeader>
          <CardTitle>搜索配置</CardTitle>
          <CardDescription>配置搜索引擎参数</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium mb-2 block">最大搜索结果数</label>
              <input
                v-model="config.maxResults"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">搜索超时时间 (秒)</label>
              <input
                v-model="config.timeout"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">相似度阈值</label>
              <input
                v-model="config.similarityThreshold"
                type="number"
                step="0.1"
                min="0"
                max="1"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">启用语义搜索</label>
              <input
                v-model="config.enableSemantic"
                type="checkbox"
                class="mt-2"
              />
            </div>
          </div>
          <div class="mt-6">
            <Button @click="saveConfig">保存配置</Button>
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
          <CardDescription>管理搜索索引</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-4">
            <div class="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <h4 class="font-semibold">产品索引</h4>
                <p class="text-sm text-muted-foreground">银行产品和服务数据</p>
              </div>
              <div class="flex gap-2">
                <Button size="sm" variant="outline">重建</Button>
                <Button size="sm" variant="outline">删除</Button>
              </div>
            </div>
            <div class="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <h4 class="font-semibold">文章索引</h4>
                <p class="text-sm text-muted-foreground">文章和资讯内容</p>
              </div>
              <div class="flex gap-2">
                <Button size="sm" variant="outline">重建</Button>
                <Button size="sm" variant="outline">删除</Button>
              </div>
            </div>
            <div class="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <h4 class="font-semibold">FAQ索引</h4>
                <p class="text-sm text-muted-foreground">常见问题数据</p>
              </div>
              <div class="flex gap-2">
                <Button size="sm" variant="outline">重建</Button>
                <Button size="sm" variant="outline">删除</Button>
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
import { Button } from '@/components/ui'

// 搜索配置
const config = ref({
  maxResults: 100,
  timeout: 30,
  similarityThreshold: 0.7,
  enableSemantic: true
})

// 索引统计
const indexStats = ref({
  totalDocuments: 15847,
  indexedDocuments: 15234,
  pendingDocuments: 613
})

// 操作方法
const refreshIndex = () => {
  console.log('Refreshing index...')
  // 实现刷新索引逻辑
}

const optimizeIndex = () => {
  console.log('Optimizing index...')
  // 实现优化索引逻辑
}

const exportData = () => {
  console.log('Exporting data...')
  // 实现导出数据逻辑
}

const saveConfig = () => {
  console.log('Saving config...', config.value)
  // 实现保存配置逻辑
}

onMounted(() => {
  // 加载配置和状态数据
  console.log('Search management page mounted')
})
</script>

<style scoped>
.search-management-page {
  /* 自定义样式 */
}
</style>