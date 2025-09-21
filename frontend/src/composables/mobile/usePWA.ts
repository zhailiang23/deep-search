import { ref, computed, onMounted, onUnmounted } from 'vue'

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

// PWA 状态管理
const isInstalled = ref(false)
const isInstallable = ref(false)
const isOffline = ref(!navigator.onLine)
const swRegistration = ref<ServiceWorkerRegistration | null>(null)
const installPrompt = ref<BeforeInstallPromptEvent | null>(null)

// 检查PWA安装状态
const checkInstallStatus = () => {
  // 检查是否在独立模式下运行
  isInstalled.value =
    window.matchMedia('(display-mode: standalone)').matches ||
    (window.navigator as any).standalone === true ||
    document.referrer.includes('android-app://')
}

// 网络状态监听
const updateOnlineStatus = () => {
  isOffline.value = !navigator.onLine
}

export function usePWA() {
  // 注册 Service Worker
  const registerServiceWorker = async () => {
    if ('serviceWorker' in navigator) {
      try {
        console.log('[PWA] Registering service worker...')

        const registration = await navigator.serviceWorker.register('/sw.js', {
          scope: '/'
        })

        swRegistration.value = registration

        console.log('[PWA] Service worker registered:', registration.scope)

        // 监听更新
        registration.addEventListener('updatefound', () => {
          console.log('[PWA] Service worker update found')
          const newWorker = registration.installing

          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                console.log('[PWA] New service worker installed, ready to update')
                // 可以在这里显示更新提示
                showUpdateAvailable()
              }
            })
          }
        })

        return registration
      } catch (error) {
        console.error('[PWA] Service worker registration failed:', error)
        throw error
      }
    } else {
      console.warn('[PWA] Service workers not supported')
      throw new Error('Service workers not supported')
    }
  }

  // 显示更新提示
  const showUpdateAvailable = () => {
    // 这里可以触发一个事件或显示更新通知
    console.log('[PWA] Update available, please refresh')
  }

  // 应用更新
  const applyUpdate = async () => {
    if (swRegistration.value?.waiting) {
      swRegistration.value.waiting.postMessage({ type: 'SKIP_WAITING' })

      // 等待控制权转移
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        window.location.reload()
      })
    }
  }

  // 安装PWA
  const installPWA = async () => {
    if (installPrompt.value) {
      try {
        // 显示安装提示
        await installPrompt.value.prompt()

        // 等待用户响应
        const choiceResult = await installPrompt.value.userChoice

        console.log('[PWA] Install prompt result:', choiceResult.outcome)

        if (choiceResult.outcome === 'accepted') {
          console.log('[PWA] User accepted the install prompt')
          isInstallable.value = false
          installPrompt.value = null
        }

        return choiceResult.outcome === 'accepted'
      } catch (error) {
        console.error('[PWA] Install prompt failed:', error)
        return false
      }
    }

    return false
  }

  // 检查更新
  const checkForUpdates = async () => {
    if (swRegistration.value) {
      try {
        await swRegistration.value.update()
        console.log('[PWA] Checked for updates')
      } catch (error) {
        console.error('[PWA] Update check failed:', error)
      }
    }
  }

  // 缓存指定URL
  const cacheUrls = (urls: string[]) => {
    if (navigator.serviceWorker.controller) {
      navigator.serviceWorker.controller.postMessage({
        type: 'CACHE_URLS',
        payload: urls
      })
    }
  }

  // 获取缓存使用情况
  const getCacheUsage = async () => {
    if ('storage' in navigator && 'estimate' in navigator.storage) {
      try {
        const estimate = await navigator.storage.estimate()
        return {
          used: estimate.usage || 0,
          available: estimate.quota || 0,
          percentage: estimate.quota ?
            Math.round(((estimate.usage || 0) / estimate.quota) * 100) : 0
        }
      } catch (error) {
        console.error('[PWA] Failed to get storage estimate:', error)
        return null
      }
    }
    return null
  }

  // 清理缓存
  const clearCache = async () => {
    if ('caches' in window) {
      try {
        const cacheNames = await caches.keys()
        await Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        )
        console.log('[PWA] All caches cleared')
        return true
      } catch (error) {
        console.error('[PWA] Failed to clear caches:', error)
        return false
      }
    }
    return false
  }

  // 获取网络信息
  const getNetworkInfo = () => {
    const connection = (navigator as any).connection ||
                     (navigator as any).mozConnection ||
                     (navigator as any).webkitConnection

    if (connection) {
      return {
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        rtt: connection.rtt,
        saveData: connection.saveData
      }
    }

    return {
      effectiveType: 'unknown',
      downlink: 0,
      rtt: 0,
      saveData: false
    }
  }

  // 检查设备能力
  const getDeviceCapabilities = () => {
    return {
      standalone: isInstalled.value,
      notifications: 'Notification' in window,
      serviceWorker: 'serviceWorker' in navigator,
      storage: 'storage' in navigator,
      share: 'share' in navigator,
      vibrate: 'vibrate' in navigator,
      geolocation: 'geolocation' in navigator,
      camera: 'getUserMedia' in navigator.mediaDevices,
      fullscreen: 'requestFullscreen' in document.documentElement
    }
  }

  // 请求通知权限
  const requestNotificationPermission = async () => {
    if ('Notification' in window) {
      try {
        const permission = await Notification.requestPermission()
        console.log('[PWA] Notification permission:', permission)
        return permission === 'granted'
      } catch (error) {
        console.error('[PWA] Notification permission request failed:', error)
        return false
      }
    }
    return false
  }

  // 发送本地通知
  const sendNotification = (title: string, options?: NotificationOptions) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      const notification = new Notification(title, {
        icon: '/icons/icon-192x192.png',
        badge: '/icons/badge-72x72.png',
        ...options
      })

      notification.onclick = () => {
        window.focus()
        notification.close()
      }

      return notification
    }
    return null
  }

  // 生命周期钩子
  onMounted(() => {
    // 检查安装状态
    checkInstallStatus()

    // 监听安装提示事件
    const handleBeforeInstallPrompt = (e: BeforeInstallPromptEvent) => {
      e.preventDefault()
      console.log('[PWA] Before install prompt triggered')
      installPrompt.value = e
      isInstallable.value = true
    }

    // 监听网络状态变化
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('online', updateOnlineStatus)
    window.addEventListener('offline', updateOnlineStatus)

    // 监听PWA安装完成
    window.addEventListener('appinstalled', () => {
      console.log('[PWA] App installed successfully')
      isInstalled.value = true
      isInstallable.value = false
      installPrompt.value = null
    })

    // 注册 Service Worker
    registerServiceWorker().catch(console.error)

    // 清理函数
    onUnmounted(() => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
      window.removeEventListener('online', updateOnlineStatus)
      window.removeEventListener('offline', updateOnlineStatus)
    })
  })

  // 计算属性
  const canInstall = computed(() => isInstallable.value && !isInstalled.value)
  const isOnline = computed(() => !isOffline.value)

  return {
    // 状态
    isInstalled: computed(() => isInstalled.value),
    isInstallable: computed(() => isInstallable.value),
    canInstall,
    isOnline,
    isOffline: computed(() => isOffline.value),
    swRegistration: computed(() => swRegistration.value),

    // 方法
    installPWA,
    applyUpdate,
    checkForUpdates,
    cacheUrls,
    getCacheUsage,
    clearCache,
    getNetworkInfo,
    getDeviceCapabilities,
    requestNotificationPermission,
    sendNotification
  }
}