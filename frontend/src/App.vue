<template>
  <div id="app" :class="themeClass">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useHead } from '@vueuse/head'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { useSearchStore } from '@/stores/search'

// 使用stores
const themeStore = useThemeStore()
const authStore = useAuthStore()
const searchStore = useSearchStore()

// 主题相关
const { themeClass } = storeToRefs(themeStore)

// 设置页面头部信息
useHead({
  title: '智能搜索平台',
  meta: [
    {
      name: 'description',
      content: '为银行业务场景提供智能语义搜索服务的专业平台'
    },
    {
      name: 'keywords',
      content: '智能搜索,语义搜索,银行业务,Vue.js,搜索平台'
    },
    {
      name: 'author',
      content: '智能搜索平台团队'
    },
    {
      property: 'og:title',
      content: '智能搜索平台'
    },
    {
      property: 'og:description',
      content: '为银行业务场景提供智能语义搜索服务'
    },
    {
      property: 'og:type',
      content: 'website'
    },
    {
      name: 'twitter:card',
      content: 'summary_large_image'
    },
    {
      name: 'twitter:title',
      content: '智能搜索平台'
    },
    {
      name: 'twitter:description',
      content: '为银行业务场景提供智能语义搜索服务'
    }
  ],
  link: [
    {
      rel: 'canonical',
      href: window.location.origin
    }
  ]
})

// 初始化应用
onMounted(async () => {
  try {
    // 初始化主题
    const themeCleanup = themeStore.initialize()

    // 初始化认证状态
    await authStore.initialize()

    // 初始化搜索状态
    searchStore.initialize()

    // 设置token自动刷新
    authStore.setupTokenRefresh()

    // 页面可见性变化监听
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && authStore.isAuthenticated) {
        // 页面重新可见时检查token状态
        if (authStore.isTokenExpiringSoon()) {
          authStore.refreshAccessToken()
        }
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    // 网络状态监听
    const handleOnline = () => {
      console.log('网络已连接')
      // 网络恢复时可以重试失败的请求
    }

    const handleOffline = () => {
      console.log('网络已断开')
      // 可以显示离线提示
    }

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    // 清理函数
    onUnmounted(() => {
      if (themeCleanup) {
        themeCleanup()
      }
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    })

  } catch (error) {
    console.error('应用初始化失败:', error)
  }
})

// 全局键盘快捷键
onMounted(() => {
  const handleKeydown = (event: KeyboardEvent) => {
    // Ctrl/Cmd + K 打开搜索
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault()
      // 导航到搜索页面或打开搜索模态框
      router.push('/search')
    }

    // Ctrl/Cmd + / 显示快捷键帮助
    if ((event.ctrlKey || event.metaKey) && event.key === '/') {
      event.preventDefault()
      // 显示快捷键帮助
      console.log('显示快捷键帮助')
    }

    // ESC 关闭模态框或返回
    if (event.key === 'Escape') {
      // 处理ESC键逻辑
    }
  }

  document.addEventListener('keydown', handleKeydown)

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })
})
</script>

<style>
/* 全局样式已在 globals.css 中定义 */

/* 应用根容器样式 */
#app {
  min-height: 100vh;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: hsl(var(--foreground));
  background-color: hsl(var(--background));
}

/* 路由过渡动画 */
.router-view {
  transition: opacity 0.2s ease-in-out;
}

/* 无障碍访问优化 */
@media (prefers-reduced-motion: reduce) {
  .router-view {
    transition: none;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  #app {
    --border: 0 0% 0%;
    --input: 0 0% 0%;
  }

  .dark #app {
    --border: 0 0% 100%;
    --input: 0 0% 100%;
  }
}

/* 移动端优化 */
@media (max-width: 768px) {
  #app {
    /* 确保在移动设备上正确显示 */
    -webkit-text-size-adjust: 100%;
    -webkit-tap-highlight-color: transparent;
  }
}

/* 打印样式 */
@media print {
  #app {
    background: white !important;
    color: black !important;
  }

  /* 隐藏导航和其他非打印元素 */
  nav,
  .no-print {
    display: none !important;
  }
}

/* 加载状态 */
.app-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-size: 16px;
  color: hsl(var(--muted-foreground));
}

/* 错误状态 */
.app-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
  text-align: center;
}

.app-error h1 {
  margin-bottom: 16px;
  font-size: 24px;
  color: hsl(var(--destructive));
}

.app-error p {
  margin-bottom: 20px;
  color: hsl(var(--muted-foreground));
}

.app-error button {
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.app-error button:hover {
  background: hsl(var(--primary) / 0.9);
}
</style>