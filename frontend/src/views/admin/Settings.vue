<template>
  <div class="settings-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-3xl font-bold text-foreground">系统设置</h1>
        <p class="text-muted-foreground mt-2">配置系统参数和功能选项</p>
      </div>

      <!-- 系统配置 -->
      <Card>
        <CardHeader>
          <CardTitle>系统配置</CardTitle>
          <CardDescription>基础系统参数配置</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="text-sm font-medium mb-2 block">系统名称</label>
                <input
                  v-model="settings.systemName"
                  type="text"
                  class="w-full px-3 py-2 border border-border rounded-md bg-background"
                />
              </div>
              <div>
                <label class="text-sm font-medium mb-2 block">系统版本</label>
                <input
                  v-model="settings.version"
                  type="text"
                  readonly
                  class="w-full px-3 py-2 border border-border rounded-md bg-muted"
                />
              </div>
              <div>
                <label class="text-sm font-medium mb-2 block">管理员邮箱</label>
                <input
                  v-model="settings.adminEmail"
                  type="email"
                  class="w-full px-3 py-2 border border-border rounded-md bg-background"
                />
              </div>
              <div>
                <label class="text-sm font-medium mb-2 block">支持邮箱</label>
                <input
                  v-model="settings.supportEmail"
                  type="email"
                  class="w-full px-3 py-2 border border-border rounded-md bg-background"
                />
              </div>
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">系统描述</label>
              <textarea
                v-model="settings.description"
                rows="3"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              ></textarea>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 搜索引擎设置 -->
      <Card>
        <CardHeader>
          <CardTitle>搜索引擎设置</CardTitle>
          <CardDescription>配置搜索引擎相关参数</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium mb-2 block">Elasticsearch地址</label>
              <input
                v-model="settings.elasticsearch.host"
                type="text"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">索引名前缀</label>
              <input
                v-model="settings.elasticsearch.indexPrefix"
                type="text"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">最大搜索结果数</label>
              <input
                v-model="settings.search.maxResults"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">搜索超时时间(秒)</label>
              <input
                v-model="settings.search.timeout"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div class="flex items-center space-x-2">
              <input
                v-model="settings.search.enableSemantic"
                type="checkbox"
                id="enableSemantic"
              />
              <label for="enableSemantic" class="text-sm font-medium">启用语义搜索</label>
            </div>
            <div class="flex items-center space-x-2">
              <input
                v-model="settings.search.enableSuggestions"
                type="checkbox"
                id="enableSuggestions"
              />
              <label for="enableSuggestions" class="text-sm font-medium">启用搜索建议</label>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 用户认证设置 -->
      <Card>
        <CardHeader>
          <CardTitle>用户认证设置</CardTitle>
          <CardDescription>配置用户认证和权限相关参数</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium mb-2 block">JWT密钥</label>
              <input
                v-model="settings.auth.jwtSecret"
                type="password"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">Token过期时间(小时)</label>
              <input
                v-model="settings.auth.tokenExpiry"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">密码最小长度</label>
              <input
                v-model="settings.auth.minPasswordLength"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">登录失败锁定次数</label>
              <input
                v-model="settings.auth.maxLoginAttempts"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div class="flex items-center space-x-2">
              <input
                v-model="settings.auth.enableTwoFactor"
                type="checkbox"
                id="enableTwoFactor"
              />
              <label for="enableTwoFactor" class="text-sm font-medium">启用双因子认证</label>
            </div>
            <div class="flex items-center space-x-2">
              <input
                v-model="settings.auth.enableRegistration"
                type="checkbox"
                id="enableRegistration"
              />
              <label for="enableRegistration" class="text-sm font-medium">允许用户注册</label>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 缓存设置 -->
      <Card>
        <CardHeader>
          <CardTitle>缓存设置</CardTitle>
          <CardDescription>配置Redis缓存相关参数</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium mb-2 block">Redis地址</label>
              <input
                v-model="settings.redis.host"
                type="text"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">Redis端口</label>
              <input
                v-model="settings.redis.port"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">缓存过期时间(分钟)</label>
              <input
                v-model="settings.cache.defaultTtl"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-2 block">搜索结果缓存时间(分钟)</label>
              <input
                v-model="settings.cache.searchResultTtl"
                type="number"
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
              />
            </div>
            <div class="flex items-center space-x-2">
              <input
                v-model="settings.cache.enableCache"
                type="checkbox"
                id="enableCache"
              />
              <label for="enableCache" class="text-sm font-medium">启用缓存</label>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 操作按钮 -->
      <div class="flex gap-4">
        <Button @click="saveSettings" :disabled="saving">
          {{ saving ? '保存中...' : '保存设置' }}
        </Button>
        <Button @click="resetSettings" variant="outline">
          重置为默认值
        </Button>
        <Button @click="testConnection" variant="outline">
          测试连接
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui'
import { Button } from '@/components/ui'

const saving = ref(false)

// 系统设置
const settings = ref({
  systemName: '智能搜索平台',
  version: '1.0.0',
  description: '为银行业务场景提供智能语义搜索服务',
  adminEmail: 'admin@example.com',
  supportEmail: 'support@example.com',

  elasticsearch: {
    host: 'localhost:9200',
    indexPrefix: 'search_'
  },

  search: {
    maxResults: 100,
    timeout: 30,
    enableSemantic: true,
    enableSuggestions: true
  },

  auth: {
    jwtSecret: '****',
    tokenExpiry: 24,
    minPasswordLength: 8,
    maxLoginAttempts: 5,
    enableTwoFactor: false,
    enableRegistration: true
  },

  redis: {
    host: 'localhost',
    port: 6379
  },

  cache: {
    enableCache: true,
    defaultTtl: 60,
    searchResultTtl: 30
  }
})

// 保存设置
const saveSettings = async () => {
  saving.value = true
  try {
    // 模拟保存API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('Settings saved:', settings.value)
    // 显示成功提示
  } catch (error) {
    console.error('Failed to save settings:', error)
    // 显示错误提示
  } finally {
    saving.value = false
  }
}

// 重置设置
const resetSettings = () => {
  if (confirm('确定要重置为默认设置吗？这将丢失当前的所有配置。')) {
    // 重置为默认值
    console.log('Resetting settings to default')
  }
}

// 测试连接
const testConnection = async () => {
  try {
    console.log('Testing connections...')
    // 测试Elasticsearch和Redis连接
    // 显示测试结果
  } catch (error) {
    console.error('Connection test failed:', error)
  }
}

onMounted(() => {
  // 加载现有设置
  console.log('Settings page mounted')
})
</script>

<style scoped>
.settings-page {
  /* 自定义样式 */
}
</style>