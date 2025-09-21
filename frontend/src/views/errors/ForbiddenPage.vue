<template>
  <div class="forbidden-page min-h-screen flex items-center justify-center bg-background">
    <div class="text-center px-4">
      <!-- 403图标 -->
      <div class="mb-8">
        <div class="text-9xl font-bold text-muted-foreground/20 mb-4">403</div>
        <div class="text-6xl mb-4">🚫</div>
      </div>

      <!-- 错误信息 -->
      <div class="max-w-md mx-auto">
        <h1 class="text-3xl font-bold text-foreground mb-4">访问被禁止</h1>
        <p class="text-muted-foreground mb-8">
          抱歉，您没有访问此页面的权限。请联系管理员获取必要的访问权限，或使用有权限的账号登录。
        </p>

        <!-- 操作按钮 -->
        <div class="space-y-4">
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <Button @click="goHome" class="min-w-32">
              返回首页
            </Button>
            <Button @click="switchAccount" variant="outline" class="min-w-32">
              切换账号
            </Button>
          </div>
        </div>
      </div>

      <!-- 权限说明 -->
      <div class="mt-12 max-w-2xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">权限说明</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4 text-sm text-left">
              <div>
                <h4 class="font-medium mb-2">可能的原因：</h4>
                <ul class="list-disc list-inside text-muted-foreground space-y-1">
                  <li>您的账号没有访问此功能的权限</li>
                  <li>您的会话已过期，需要重新登录</li>
                  <li>此功能仅限管理员访问</li>
                  <li>您的账号可能被暂停或限制</li>
                </ul>
              </div>
              <div>
                <h4 class="font-medium mb-2">解决方案：</h4>
                <ul class="list-disc list-inside text-muted-foreground space-y-1">
                  <li>联系系统管理员申请相应权限</li>
                  <li>使用具有相应权限的账号登录</li>
                  <li>确认您访问的URL是否正确</li>
                  <li>如需帮助，请联系技术支持</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 权限级别说明 -->
      <div class="mt-8 max-w-2xl mx-auto">
        <h3 class="text-lg font-semibold mb-4">系统权限级别</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">👤</div>
              <div class="font-medium text-sm">普通用户</div>
              <div class="text-xs text-muted-foreground mt-1">
                搜索功能和基本信息查看
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">✏️</div>
              <div class="font-medium text-sm">编辑者</div>
              <div class="text-xs text-muted-foreground mt-1">
                内容编辑和数据管理
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent class="p-4 text-center">
              <div class="text-2xl mb-2">👨‍💼</div>
              <div class="font-medium text-sm">管理员</div>
              <div class="text-xs text-muted-foreground mt-1">
                完整系统管理权限
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <!-- 联系信息 -->
      <div class="mt-12 text-sm text-muted-foreground">
        <p>需要帮助？请联系：</p>
        <div class="mt-2 space-y-1">
          <p>
            技术支持: <a href="mailto:support@example.com" class="text-primary hover:underline">support@example.com</a>
          </p>
          <p>
            管理员: <a href="mailto:admin@example.com" class="text-primary hover:underline">admin@example.com</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui'
import { Button } from '@/components/ui'

const router = useRouter()
const authStore = useAuthStore()

// 返回首页
const goHome = () => {
  router.push('/')
}

// 切换账号
const switchAccount = async () => {
  try {
    await authStore.logout()
    router.push('/auth/login')
  } catch (error) {
    console.error('Logout failed:', error)
    // 强制跳转到登录页
    router.push('/auth/login')
  }
}
</script>

<style scoped>
.forbidden-page {
  /* 自定义样式 */
}
</style>