<template>
  <div class="login-page min-h-screen flex items-center justify-center bg-background">
    <div class="w-full max-w-md space-y-8 p-8">
      <!-- Logo和标题 -->
      <div class="text-center">
        <div class="mx-auto h-12 w-12 bg-primary rounded-lg flex items-center justify-center mb-4">
          <span class="text-2xl text-primary-foreground">🔍</span>
        </div>
        <h2 class="text-3xl font-bold text-foreground">用户登录</h2>
        <p class="mt-2 text-muted-foreground">登录到智能搜索平台</p>
      </div>

      <!-- 登录表单 -->
      <Card>
        <CardContent class="pt-6">
          <form @submit.prevent="handleLogin" class="space-y-6">
            <!-- 邮箱输入 -->
            <div>
              <label for="email" class="text-sm font-medium mb-2 block">
                邮箱地址
              </label>
              <input
                id="email"
                v-model="loginForm.email"
                type="email"
                required
                :disabled="loading"
                class="w-full px-3 py-2 border border-border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                placeholder="请输入邮箱地址"
              />
              <div v-if="errors.email" class="mt-1 text-sm text-red-600">
                {{ errors.email }}
              </div>
            </div>

            <!-- 密码输入 -->
            <div>
              <label for="password" class="text-sm font-medium mb-2 block">
                密码
              </label>
              <div class="relative">
                <input
                  id="password"
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  :disabled="loading"
                  class="w-full px-3 py-2 border border-border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent pr-10"
                  placeholder="请输入密码"
                />
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground hover:text-foreground"
                >
                  <span class="text-sm">{{ showPassword ? '隐藏' : '显示' }}</span>
                </button>
              </div>
              <div v-if="errors.password" class="mt-1 text-sm text-red-600">
                {{ errors.password }}
              </div>
            </div>

            <!-- 记住我和忘记密码 -->
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <input
                  id="remember"
                  v-model="loginForm.remember"
                  type="checkbox"
                  class="h-4 w-4 text-primary focus:ring-primary border-border rounded"
                />
                <label for="remember" class="ml-2 text-sm text-muted-foreground">
                  记住我
                </label>
              </div>
              <a
                href="#"
                @click.prevent="handleForgotPassword"
                class="text-sm text-primary hover:text-primary/80"
              >
                忘记密码？
              </a>
            </div>

            <!-- 登录按钮 -->
            <Button
              type="submit"
              :disabled="loading"
              class="w-full"
            >
              {{ loading ? '登录中...' : '登录' }}
            </Button>

            <!-- 错误提示 -->
            <div v-if="loginError" class="text-center">
              <div class="text-sm text-red-600 bg-red-50 dark:bg-red-900/20 p-3 rounded-md">
                {{ loginError }}
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      <!-- 其他登录方式 -->
      <div class="text-center">
        <div class="relative">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-border"></div>
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-2 bg-background text-muted-foreground">或者</span>
          </div>
        </div>

        <div class="mt-6 space-y-3">
          <Button
            variant="outline"
            class="w-full"
            @click="handleSSOLogin('ldap')"
            :disabled="loading"
          >
            <span class="mr-2">🏢</span>
            使用企业账号登录
          </Button>

          <Button
            variant="outline"
            class="w-full"
            @click="handleSSOLogin('oauth')"
            :disabled="loading"
          >
            <span class="mr-2">🔐</span>
            使用OAuth登录
          </Button>
        </div>
      </div>

      <!-- 注册链接 -->
      <div class="text-center">
        <p class="text-sm text-muted-foreground">
          还没有账号？
          <a
            href="#"
            @click.prevent="handleRegister"
            class="text-primary hover:text-primary/80 font-medium"
          >
            立即注册
          </a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Card, CardContent, Button } from '@/components/ui'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const showPassword = ref(false)
const loginError = ref('')

// 登录表单
const loginForm = ref({
  email: '',
  password: '',
  remember: false
})

// 表单验证错误
const errors = ref<Record<string, string>>({})

// 验证表单
const validateForm = () => {
  errors.value = {}

  if (!loginForm.value.email) {
    errors.value.email = '请输入邮箱地址'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(loginForm.value.email)) {
    errors.value.email = '请输入有效的邮箱地址'
  }

  if (!loginForm.value.password) {
    errors.value.password = '请输入密码'
  } else if (loginForm.value.password.length < 6) {
    errors.value.password = '密码长度至少6位'
  }

  return Object.keys(errors.value).length === 0
}

// 处理登录
const handleLogin = async () => {
  if (!validateForm()) {
    return
  }

  loading.value = true
  loginError.value = ''

  try {
    await authStore.login({
      username: loginForm.value.email,
      password: loginForm.value.password,
      rememberMe: loginForm.value.remember
    })

    // 登录成功，重定向到目标页面
    const redirectPath = route.query.redirect as string || '/'
    router.push(redirectPath)
  } catch (error: any) {
    loginError.value = error.message || '登录失败，请检查邮箱和密码'
  } finally {
    loading.value = false
  }
}

// 处理SSO登录
const handleSSOLogin = async (provider: string) => {
  loading.value = true
  try {
    // TODO: 实现SSO登录功能
    console.log(`SSO login with ${provider}`)
    loginError.value = '暂不支持SSO登录'
  } catch (error: any) {
    loginError.value = error.message || `${provider}登录失败`
  } finally {
    loading.value = false
  }
}

// 处理忘记密码
const handleForgotPassword = () => {
  // 跳转到忘记密码页面或显示重置密码对话框
  console.log('Forgot password clicked')
  // 这里可以实现忘记密码的逻辑
}

// 处理注册
const handleRegister = () => {
  // 跳转到注册页面
  console.log('Register clicked')
  // 由于当前系统可能不允许注册，这里只是示例
}

onMounted(() => {
  // 如果用户已经登录，直接重定向
  if (authStore.isAuthenticated) {
    const redirectPath = route.query.redirect as string || '/'
    router.push(redirectPath)
  }

  // 开发环境下可以预填测试账号
  if (import.meta.env.DEV) {
    loginForm.value.email = 'admin@example.com'
    loginForm.value.password = 'password'
  }
})
</script>

<style scoped>
.login-page {
  background: linear-gradient(135deg, hsl(var(--background)) 0%, hsl(var(--muted)) 100%);
}

/* 暗色模式下的背景 */
.dark .login-page {
  background: linear-gradient(135deg, hsl(var(--background)) 0%, hsl(var(--muted)/0.5) 100%);
}
</style>