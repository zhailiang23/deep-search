import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useLocalStorage, useMediaQuery, useEventListener } from '@vueuse/core'
import type {
  ThemeMode,
  ThemeState,
  ThemeController,
  ThemeConfig,
  ThemeVariables,
  ThemeColors
} from '@/types/theme'

/**
 * 主题管理 Composable
 */
export function useTheme(config?: Partial<ThemeConfig>): ThemeController {
  // 默认配置
  const defaultConfig: ThemeConfig = {
    mode: 'system',
    followSystem: true,
    enableTransitions: true,
    storageKey: 'ui-theme'
  }

  const finalConfig = { ...defaultConfig, ...config }

  // 响应式状态
  const isReady = ref(false)
  const isChanging = ref(false)

  // 使用 localStorage 存储主题偏好
  const storedTheme = useLocalStorage<ThemeMode>(
    finalConfig.storageKey,
    finalConfig.mode
  )

  // 系统主题检测
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)')
  const systemTheme = computed(() => prefersDark.value ? 'dark' : 'light')

  // 当前主题模式
  const mode = ref<ThemeMode>(storedTheme.value)

  // 解析后的实际主题
  const resolvedTheme = computed<'light' | 'dark'>(() => {
    if (finalConfig.forcedTheme) {
      return finalConfig.forcedTheme
    }

    if (mode.value === 'system') {
      return systemTheme.value
    }

    return mode.value as 'light' | 'dark'
  })

  // 主题状态
  const state = computed<ThemeState>(() => ({
    mode: mode.value,
    resolvedTheme: resolvedTheme.value,
    systemTheme: systemTheme.value,
    isReady: isReady.value,
    isChanging: isChanging.value
  }))

  // 应用主题到 DOM
  const applyThemeToDOM = (theme: 'light' | 'dark') => {
    const root = document.documentElement

    if (finalConfig.enableTransitions) {
      // 添加过渡效果
      root.style.setProperty('transition', 'color 150ms, background-color 150ms')
    }

    if (theme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }

    // 移除过渡效果
    if (finalConfig.enableTransitions) {
      setTimeout(() => {
        root.style.removeProperty('transition')
      }, 150)
    }

    // 更新 meta 主题色
    updateMetaThemeColor(theme)
  }

  // 更新 meta 标签的主题色
  const updateMetaThemeColor = (theme: 'light' | 'dark') => {
    const metaThemeColor = document.querySelector('meta[name="theme-color"]')
    if (metaThemeColor) {
      const color = theme === 'dark' ? '#0a0a0a' : '#ffffff'
      metaThemeColor.setAttribute('content', color)
    }
  }

  // 设置主题
  const setTheme = async (newMode: ThemeMode) => {
    if (mode.value === newMode) return

    isChanging.value = true

    try {
      mode.value = newMode
      storedTheme.value = newMode

      await nextTick()
      applyThemeToDOM(resolvedTheme.value)

      // 触发主题变化事件
      window.dispatchEvent(
        new CustomEvent('theme-changed', {
          detail: { theme: state.value }
        })
      )
    } finally {
      setTimeout(() => {
        isChanging.value = false
      }, 150)
    }
  }

  // 切换主题
  const toggleTheme = () => {
    const currentResolved = resolvedTheme.value
    const newTheme: ThemeMode = currentResolved === 'dark' ? 'light' : 'dark'
    setTheme(newTheme)
  }

  // 获取 CSS 变量值
  const getCSSVariable = (variable: string): string => {
    return getComputedStyle(document.documentElement)
      .getPropertyValue(`--${variable}`)
      .trim()
  }

  // 设置 CSS 变量
  const setCSSVariable = (variable: string, value: string) => {
    document.documentElement.style.setProperty(`--${variable}`, value)
  }

  // 获取当前主题变量
  const getThemeVariables = (): ThemeVariables => {
    return {
      // 基础颜色
      background: getCSSVariable('background'),
      foreground: getCSSVariable('foreground'),
      card: getCSSVariable('card'),
      cardForeground: getCSSVariable('card-foreground'),
      popover: getCSSVariable('popover'),
      popoverForeground: getCSSVariable('popover-foreground'),

      // 主色调
      primary: getCSSVariable('primary'),
      primaryForeground: getCSSVariable('primary-foreground'),

      // 次要色调
      secondary: getCSSVariable('secondary'),
      secondaryForeground: getCSSVariable('secondary-foreground'),

      // 静音色调
      muted: getCSSVariable('muted'),
      mutedForeground: getCSSVariable('muted-foreground'),

      // 强调色调
      accent: getCSSVariable('accent'),
      accentForeground: getCSSVariable('accent-foreground'),

      // 危险色调
      destructive: getCSSVariable('destructive'),
      destructiveForeground: getCSSVariable('destructive-foreground'),

      // 边框和输入
      border: getCSSVariable('border'),
      input: getCSSVariable('input'),
      ring: getCSSVariable('ring'),

      // 圆角
      radius: getCSSVariable('radius'),

      // 阴影
      shadowSm: getCSSVariable('shadow-sm'),
      shadow: getCSSVariable('shadow'),
      shadowMd: getCSSVariable('shadow-md'),
      shadowLg: getCSSVariable('shadow-lg'),
      shadowXl: getCSSVariable('shadow-xl'),

      // 动画持续时间
      animationDurationFast: getCSSVariable('animation-duration-fast'),
      animationDurationNormal: getCSSVariable('animation-duration-normal'),
      animationDurationSlow: getCSSVariable('animation-duration-slow'),

      // 层级
      zDropdown: parseInt(getCSSVariable('z-dropdown')) || 1000,
      zSticky: parseInt(getCSSVariable('z-sticky')) || 1020,
      zFixed: parseInt(getCSSVariable('z-fixed')) || 1030,
      zModalBackdrop: parseInt(getCSSVariable('z-modal-backdrop')) || 1040,
      zModal: parseInt(getCSSVariable('z-modal')) || 1050,
      zPopover: parseInt(getCSSVariable('z-popover')) || 1060,
      zTooltip: parseInt(getCSSVariable('z-tooltip')) || 1070,
      zToast: parseInt(getCSSVariable('z-toast')) || 1080,
    }
  }

  // 应用自定义主题
  const applyCustomTheme = (colors: Partial<ThemeColors>) => {
    Object.entries(colors).forEach(([key, value]) => {
      if (value) {
        setCSSVariable(key.replace(/([A-Z])/g, '-$1').toLowerCase(), value)
      }
    })
  }

  // 重置为默认主题
  const resetTheme = () => {
    setTheme(defaultConfig.mode)
  }

  // 监听主题变化事件
  const onThemeChange = (callback: (theme: ThemeState) => void) => {
    const handler = () => callback(state.value)

    useEventListener(window, 'theme-changed', handler)

    // 返回清理函数
    return () => {
      window.removeEventListener('theme-changed', handler)
    }
  }

  // 监听系统主题变化
  watch(
    () => prefersDark.value,
    () => {
      if (mode.value === 'system') {
        applyThemeToDOM(resolvedTheme.value)
      }
    }
  )

  // 监听主题模式变化
  watch(
    () => resolvedTheme.value,
    (newTheme) => {
      if (isReady.value) {
        applyThemeToDOM(newTheme)
      }
    }
  )

  // 初始化主题
  onMounted(async () => {
    try {
      // 验证存储的主题是否有效
      const validThemes: ThemeMode[] = ['light', 'dark', 'system']
      if (!validThemes.includes(storedTheme.value)) {
        storedTheme.value = defaultConfig.mode
        mode.value = defaultConfig.mode
      }

      // 应用初始主题
      await nextTick()
      applyThemeToDOM(resolvedTheme.value)

      isReady.value = true

      // 触发就绪事件
      window.dispatchEvent(
        new CustomEvent('theme-ready', {
          detail: { theme: state.value }
        })
      )
    } catch (error) {
      console.error('Theme initialization failed:', error)
      // 降级到默认主题
      resetTheme()
      isReady.value = true
    }
  })

  return {
    state: state.value,
    setTheme,
    toggleTheme,
    getThemeVariables,
    onThemeChange,
    applyCustomTheme,
    resetTheme
  }
}

/**
 * 主题工具函数
 */
export const themeUtils = {
  /** 获取 CSS 变量值 */
  getCSSVariable: (variable: string): string => {
    return getComputedStyle(document.documentElement)
      .getPropertyValue(`--${variable}`)
      .trim()
  },

  /** 设置 CSS 变量 */
  setCSSVariable: (variable: string, value: string): void => {
    document.documentElement.style.setProperty(`--${variable}`, value)
  },

  /** 检测系统主题 */
  getSystemTheme: (): 'light' | 'dark' => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  },

  /** 应用主题到DOM */
  applyThemeToDOM: (theme: 'light' | 'dark'): void => {
    const root = document.documentElement
    if (theme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  },

  /** 验证主题模式 */
  isValidThemeMode: (mode: string): mode is ThemeMode => {
    return ['light', 'dark', 'system'].includes(mode)
  },

  /** 获取主题常量 */
  getConstants: () => ({
    DEFAULT_STORAGE_KEY: 'ui-theme',
    DEFAULT_THEME: 'system' as ThemeMode,
    SYSTEM_THEME_QUERY: '(prefers-color-scheme: dark)',
    THEME_CLASS_NAMES: {
      light: '',
      dark: 'dark',
    },
    TRANSITION_DURATION: 150,
  })
}

/**
 * 简化的主题切换 Hook
 */
export function useSimpleTheme() {
  const { state, setTheme, toggleTheme } = useTheme()

  return {
    isDark: computed(() => state.resolvedTheme === 'dark'),
    theme: computed(() => state.resolvedTheme),
    mode: computed(() => state.mode),
    isReady: computed(() => state.isReady),
    setTheme,
    toggleTheme,
    setLight: () => setTheme('light'),
    setDark: () => setTheme('dark'),
    setSystem: () => setTheme('system'),
  }
}