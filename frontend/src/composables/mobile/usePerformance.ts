import { ref, computed, onMounted, onUnmounted } from 'vue'

// 性能监控数据类型
interface PerformanceMetrics {
  fcp?: number // First Contentful Paint
  lcp?: number // Largest Contentful Paint
  fid?: number // First Input Delay
  cls?: number // Cumulative Layout Shift
  ttfb?: number // Time to First Byte
}

interface MemoryInfo {
  used: number
  total: number
  percentage: number
}

interface NetworkInfo {
  effectiveType: string
  downlink: number
  rtt: number
  saveData: boolean
}

// 全局性能状态
const performanceMetrics = ref<PerformanceMetrics>({})
const isSlowDevice = ref(false)
const isSlowNetwork = ref(false)
const memoryPressure = ref(false)

// 检测设备性能
const detectDevicePerformance = () => {
  // 检测CPU核心数
  const cores = navigator.hardwareConcurrency || 4

  // 检测内存
  const memory = (navigator as any).deviceMemory || 4

  // 简单性能评分
  const performanceScore = cores * memory

  // 低于8分认为是低性能设备
  isSlowDevice.value = performanceScore < 8

  console.log('[Performance] Device info:', {
    cores,
    memory,
    score: performanceScore,
    isSlowDevice: isSlowDevice.value
  })
}

// 检测网络质量
const detectNetworkQuality = () => {
  const connection = (navigator as any).connection ||
                   (navigator as any).mozConnection ||
                   (navigator as any).webkitConnection

  if (connection) {
    const { effectiveType, downlink, rtt, saveData } = connection

    // 网络质量判断
    isSlowNetwork.value =
      effectiveType === 'slow-2g' ||
      effectiveType === '2g' ||
      downlink < 1.5 ||
      rtt > 500 ||
      saveData

    console.log('[Performance] Network info:', {
      effectiveType,
      downlink,
      rtt,
      saveData,
      isSlowNetwork: isSlowNetwork.value
    })

    return {
      effectiveType,
      downlink,
      rtt,
      saveData
    }
  }

  return {
    effectiveType: 'unknown',
    downlink: 0,
    rtt: 0,
    saveData: false
  }
}

// 监控内存使用
const monitorMemoryUsage = () => {
  if ('memory' in performance) {
    const memory = (performance as any).memory

    const memoryInfo: MemoryInfo = {
      used: memory.usedJSHeapSize,
      total: memory.totalJSHeapSize,
      percentage: Math.round((memory.usedJSHeapSize / memory.totalJSHeapSize) * 100)
    }

    // 内存使用超过80%认为有压力
    memoryPressure.value = memoryInfo.percentage > 80

    return memoryInfo
  }

  return null
}

// 监控 Web Vitals
const monitorWebVitals = () => {
  // FCP - First Contentful Paint
  const observer = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      if (entry.entryType === 'paint' && entry.name === 'first-contentful-paint') {
        performanceMetrics.value.fcp = entry.startTime
      }
    }
  })

  observer.observe({ entryTypes: ['paint'] })

  // LCP - Largest Contentful Paint
  const lcpObserver = new PerformanceObserver((list) => {
    const entries = list.getEntries()
    const lastEntry = entries[entries.length - 1]
    performanceMetrics.value.lcp = lastEntry.startTime
  })

  lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] })

  // FID - First Input Delay
  const fidObserver = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      if (entry.entryType === 'first-input') {
        performanceMetrics.value.fid = (entry as any).processingStart - entry.startTime
      }
    }
  })

  fidObserver.observe({ entryTypes: ['first-input'] })

  // CLS - Cumulative Layout Shift
  let clsValue = 0
  const clsObserver = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      if (entry.entryType === 'layout-shift' && !(entry as any).hadRecentInput) {
        clsValue += (entry as any).value
        performanceMetrics.value.cls = clsValue
      }
    }
  })

  clsObserver.observe({ entryTypes: ['layout-shift'] })

  return () => {
    observer.disconnect()
    lcpObserver.disconnect()
    fidObserver.disconnect()
    clsObserver.disconnect()
  }
}

// 获取页面加载时间
const getPageLoadTime = () => {
  const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming

  if (navigation) {
    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
      domInteractive: navigation.domInteractive - navigation.fetchStart,
      ttfb: navigation.responseStart - navigation.fetchStart
    }
  }

  return null
}

// 性能优化建议
const getPerformanceRecommendations = () => {
  const recommendations: string[] = []

  if (isSlowDevice.value) {
    recommendations.push('检测到低性能设备，建议减少动画效果')
    recommendations.push('启用简化模式以提升性能')
  }

  if (isSlowNetwork.value) {
    recommendations.push('检测到慢网络，建议启用数据节省模式')
    recommendations.push('优先加载核心功能')
  }

  if (memoryPressure.value) {
    recommendations.push('内存使用较高，建议清理缓存')
    recommendations.push('减少同时加载的内容')
  }

  if (performanceMetrics.value.fcp && performanceMetrics.value.fcp > 2500) {
    recommendations.push('首次内容绘制较慢，优化资源加载')
  }

  if (performanceMetrics.value.lcp && performanceMetrics.value.lcp > 4000) {
    recommendations.push('最大内容绘制较慢，优化关键渲染路径')
  }

  if (performanceMetrics.value.fid && performanceMetrics.value.fid > 100) {
    recommendations.push('首次输入延迟较高，优化JavaScript执行')
  }

  if (performanceMetrics.value.cls && performanceMetrics.value.cls > 0.25) {
    recommendations.push('布局偏移较大，请优化元素加载')
  }

  return recommendations
}

// 预加载关键资源
const preloadCriticalResources = (resources: string[]) => {
  resources.forEach(resource => {
    const link = document.createElement('link')
    link.rel = 'preload'
    link.href = resource

    // 根据文件类型设置as属性
    if (resource.endsWith('.css')) {
      link.as = 'style'
    } else if (resource.endsWith('.js')) {
      link.as = 'script'
    } else if (/\.(jpg|jpeg|png|webp|svg)$/i.test(resource)) {
      link.as = 'image'
    } else if (/\.(woff|woff2|ttf)$/i.test(resource)) {
      link.as = 'font'
      link.crossOrigin = 'anonymous'
    }

    document.head.appendChild(link)
  })
}

// 资源优先级提示
const setPriorityHints = (selector: string, priority: 'high' | 'low' | 'auto') => {
  const elements = document.querySelectorAll(selector)
  elements.forEach(element => {
    if (element instanceof HTMLImageElement ||
        element instanceof HTMLLinkElement ||
        element instanceof HTMLScriptElement) {
      // @ts-ignore
      element.fetchPriority = priority
    }
  })
}

// 懒加载优化
const optimizeLazyLoading = () => {
  // 根据设备性能调整懒加载阈值
  const threshold = isSlowDevice.value ? '100px' : '50px'

  const images = document.querySelectorAll('img[loading="lazy"]')
  images.forEach(img => {
    if (img instanceof HTMLImageElement) {
      img.style.setProperty('--lazy-threshold', threshold)
    }
  })
}

// 内存清理
const performMemoryCleanup = () => {
  // 清理未使用的对象
  if (window.gc) {
    window.gc()
  }

  // 清理事件监听器
  document.querySelectorAll('[data-cleanup]').forEach(element => {
    element.remove()
  })

  console.log('[Performance] Memory cleanup performed')
}

// 自适应质量设置
const getAdaptiveQualitySettings = () => {
  return {
    // 图片质量
    imageQuality: isSlowNetwork.value ? 'low' : isSlowDevice.value ? 'medium' : 'high',

    // 动画设置
    enableAnimations: !isSlowDevice.value,

    // 懒加载设置
    lazyLoadThreshold: isSlowDevice.value ? 200 : 100,

    // 缓存策略
    cacheStrategy: isSlowNetwork.value ? 'aggressive' : 'normal',

    // 预加载设置
    enablePreload: !isSlowNetwork.value && !isSlowDevice.value,

    // 并发请求数
    maxConcurrentRequests: isSlowDevice.value ? 2 : isSlowNetwork.value ? 3 : 6
  }
}

export function usePerformance() {
  let vitalsCleanup: (() => void) | null = null

  onMounted(() => {
    // 检测设备和网络性能
    detectDevicePerformance()
    detectNetworkQuality()

    // 开始监控 Web Vitals
    vitalsCleanup = monitorWebVitals()

    // 设置内存监控
    const memoryInterval = setInterval(() => {
      monitorMemoryUsage()
    }, 30000) // 每30秒检查一次

    // 优化懒加载
    optimizeLazyLoading()

    onUnmounted(() => {
      if (vitalsCleanup) {
        vitalsCleanup()
      }
      clearInterval(memoryInterval)
    })
  })

  // 计算属性
  const performanceGrade = computed(() => {
    const metrics = performanceMetrics.value
    let score = 100

    if (metrics.fcp) {
      if (metrics.fcp > 2500) score -= 20
      else if (metrics.fcp > 1800) score -= 10
    }

    if (metrics.lcp) {
      if (metrics.lcp > 4000) score -= 25
      else if (metrics.lcp > 2500) score -= 15
    }

    if (metrics.fid) {
      if (metrics.fid > 100) score -= 20
      else if (metrics.fid > 50) score -= 10
    }

    if (metrics.cls) {
      if (metrics.cls > 0.25) score -= 15
      else if (metrics.cls > 0.1) score -= 8
    }

    if (score >= 90) return 'A'
    if (score >= 80) return 'B'
    if (score >= 70) return 'C'
    if (score >= 60) return 'D'
    return 'F'
  })

  const shouldUseLowPowerMode = computed(() => {
    return isSlowDevice.value || memoryPressure.value
  })

  const shouldUseDataSaveMode = computed(() => {
    return isSlowNetwork.value
  })

  return {
    // 状态
    performanceMetrics: computed(() => performanceMetrics.value),
    isSlowDevice: computed(() => isSlowDevice.value),
    isSlowNetwork: computed(() => isSlowNetwork.value),
    memoryPressure: computed(() => memoryPressure.value),
    performanceGrade,
    shouldUseLowPowerMode,
    shouldUseDataSaveMode,

    // 方法
    detectNetworkQuality,
    monitorMemoryUsage,
    getPageLoadTime,
    getPerformanceRecommendations,
    preloadCriticalResources,
    setPriorityHints,
    performMemoryCleanup,
    getAdaptiveQualitySettings
  }
}