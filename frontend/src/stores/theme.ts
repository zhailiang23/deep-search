import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { Ref } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  // 状态
  const mode: Ref<ThemeMode> = ref('system')
  const systemPrefersDark = ref(false)

  // 计算属性
  const isDark = computed(() => {
    if (mode.value === 'system') {
      return systemPrefersDark.value
    }
    return mode.value === 'dark'
  })

  const themeClass = computed(() => isDark.value ? 'dark' : 'light')

  // 设置主题模式
  const setMode = (newMode: ThemeMode) => {
    mode.value = newMode
    saveToLocalStorage()
    applyTheme()
  }

  // 切换主题
  const toggle = () => {
    if (mode.value === 'system') {
      setMode(systemPrefersDark.value ? 'light' : 'dark')
    } else {
      setMode(mode.value === 'dark' ? 'light' : 'dark')
    }
  }

  // 应用主题到DOM
  const applyTheme = () => {
    const root = document.documentElement

    if (isDark.value) {
      root.classList.add('dark')
      root.classList.remove('light')
    } else {
      root.classList.add('light')
      root.classList.remove('dark')
    }

    // 设置meta theme-color
    const metaThemeColor = document.querySelector('meta[name="theme-color"]')
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', isDark.value ? '#0f172a' : '#ffffff')
    }
  }

  // 保存到localStorage
  const saveToLocalStorage = () => {
    try {
      localStorage.setItem('theme-mode', mode.value)
    } catch (error) {
      console.warn('Failed to save theme to localStorage:', error)
    }
  }

  // 从localStorage加载
  const loadFromLocalStorage = () => {
    try {
      const saved = localStorage.getItem('theme-mode')
      if (saved && ['light', 'dark', 'system'].includes(saved)) {
        mode.value = saved as ThemeMode
      }
    } catch (error) {
      console.warn('Failed to load theme from localStorage:', error)
    }
  }

  // 监听系统主题变化
  const setupSystemThemeDetection = () => {
    if (typeof window === 'undefined') return

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    // 设置初始值
    systemPrefersDark.value = mediaQuery.matches

    // 监听变化
    const handleChange = (e: MediaQueryListEvent) => {
      systemPrefersDark.value = e.matches
    }

    mediaQuery.addEventListener('change', handleChange)

    // 返回清理函数
    return () => {
      mediaQuery.removeEventListener('change', handleChange)
    }
  }

  // 初始化
  const initialize = () => {
    // 加载保存的主题设置
    loadFromLocalStorage()

    // 设置系统主题检测
    const cleanup = setupSystemThemeDetection()

    // 应用初始主题
    applyTheme()

    return cleanup
  }

  // 监听主题变化并应用
  watch([mode, systemPrefersDark], () => {
    applyTheme()
  }, { immediate: true })

  // 获取主题配置
  const getThemeConfig = () => {
    return {
      mode: mode.value,
      isDark: isDark.value,
      systemPrefersDark: systemPrefersDark.value
    }
  }

  // 预设主题颜色
  const themeColors = {
    light: {
      primary: '#3b82f6',
      secondary: '#6b7280',
      success: '#10b981',
      warning: '#f59e0b',
      error: '#ef4444',
      background: '#ffffff',
      surface: '#f8fafc',
      text: '#1f2937'
    },
    dark: {
      primary: '#60a5fa',
      secondary: '#9ca3af',
      success: '#34d399',
      warning: '#fbbf24',
      error: '#f87171',
      background: '#0f172a',
      surface: '#1e293b',
      text: '#f1f5f9'
    }
  }

  const currentColors = computed(() => {
    return isDark.value ? themeColors.dark : themeColors.light
  })

  return {
    // 状态
    mode,
    systemPrefersDark,

    // 计算属性
    isDark,
    themeClass,
    currentColors,

    // 方法
    setMode,
    toggle,
    applyTheme,
    initialize,
    getThemeConfig
  }
})