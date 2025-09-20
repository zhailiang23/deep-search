/**
 * 主题相关类型定义
 */

/** 主题模式 */
export type ThemeMode = 'light' | 'dark' | 'system'

/** 主题配置 */
export interface ThemeConfig {
  /** 当前主题模式 */
  mode: ThemeMode
  /** 是否跟随系统 */
  followSystem: boolean
  /** 强制主题（忽略系统设置） */
  forcedTheme?: 'light' | 'dark'
  /** 主题过渡动画启用 */
  enableTransitions: boolean
  /** 主题存储键名 */
  storageKey: string
}

/** 主题颜色变量 */
export interface ThemeColors {
  /** 基础颜色 */
  background: string
  foreground: string

  /** 卡片颜色 */
  card: string
  cardForeground: string

  /** 弹出层颜色 */
  popover: string
  popoverForeground: string

  /** 主色调 */
  primary: string
  primaryForeground: string

  /** 次要色调 */
  secondary: string
  secondaryForeground: string

  /** 静音色调 */
  muted: string
  mutedForeground: string

  /** 强调色调 */
  accent: string
  accentForeground: string

  /** 危险色调 */
  destructive: string
  destructiveForeground: string

  /** 边框和输入 */
  border: string
  input: string
  ring: string
}

/** 主题变量 */
export interface ThemeVariables extends ThemeColors {
  /** 圆角 */
  radius: string

  /** 阴影变量 */
  shadowSm: string
  shadow: string
  shadowMd: string
  shadowLg: string
  shadowXl: string

  /** 动画持续时间 */
  animationDurationFast: string
  animationDurationNormal: string
  animationDurationSlow: string

  /** 层级 */
  zDropdown: number
  zSticky: number
  zFixed: number
  zModalBackdrop: number
  zModal: number
  zPopover: number
  zTooltip: number
  zToast: number
}

/** 主题状态 */
export interface ThemeState {
  /** 当前主题模式 */
  mode: ThemeMode
  /** 实际应用的主题（解析后的） */
  resolvedTheme: 'light' | 'dark'
  /** 系统主题 */
  systemTheme: 'light' | 'dark'
  /** 主题是否已准备就绪 */
  isReady: boolean
  /** 主题是否正在切换 */
  isChanging: boolean
}

/** 主题控制器接口 */
export interface ThemeController {
  /** 主题状态 */
  readonly state: Readonly<ThemeState>

  /** 设置主题模式 */
  setTheme: (mode: ThemeMode) => void

  /** 切换主题 */
  toggleTheme: () => void

  /** 获取当前主题变量 */
  getThemeVariables: () => ThemeVariables

  /** 监听主题变化 */
  onThemeChange: (callback: (theme: ThemeState) => void) => () => void

  /** 应用自定义主题 */
  applyCustomTheme: (colors: Partial<ThemeColors>) => void

  /** 重置为默认主题 */
  resetTheme: () => void
}

/** 主题提供者属性 */
export interface ThemeProviderProps {
  /** 默认主题 */
  defaultTheme?: ThemeMode
  /** 主题存储键名 */
  storageKey?: string
  /** 是否启用过渡动画 */
  enableTransitions?: boolean
  /** 强制主题 */
  forcedTheme?: 'light' | 'dark'
  /** 主题变化回调 */
  onThemeChange?: (theme: ThemeState) => void
  /** 子组件 */
  children?: any
}

/** 媒体查询断点 */
export interface ThemeBreakpoints {
  xs: string
  sm: string
  md: string
  lg: string
  xl: string
  '2xl': string
}

/** 主题常量 */
export const THEME_CONSTANTS = {
  /** 默认存储键名 */
  DEFAULT_STORAGE_KEY: 'ui-theme',

  /** 默认主题模式 */
  DEFAULT_THEME: 'system' as ThemeMode,

  /** 系统主题媒体查询 */
  SYSTEM_THEME_QUERY: '(prefers-color-scheme: dark)',

  /** 主题类名 */
  THEME_CLASS_NAMES: {
    light: '',
    dark: 'dark',
  },

  /** 过渡持续时间 */
  TRANSITION_DURATION: 150,

  /** 断点定义 */
  BREAKPOINTS: {
    xs: '475px',
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px',
  } as ThemeBreakpoints,
} as const

/** 主题事件类型 */
export type ThemeEvent =
  | { type: 'THEME_CHANGED'; payload: ThemeState }
  | { type: 'THEME_LOADING' }
  | { type: 'THEME_READY' }
  | { type: 'SYSTEM_THEME_CHANGED'; payload: 'light' | 'dark' }

/** 组件变体类型 */
export type ComponentVariant =
  | 'default'
  | 'primary'
  | 'secondary'
  | 'destructive'
  | 'ghost'
  | 'link'

/** 组件尺寸类型 */
export type ComponentSize = 'sm' | 'md' | 'lg' | 'xl'

/** 组件主题属性 */
export interface ComponentThemeProps {
  /** 变体 */
  variant?: ComponentVariant
  /** 尺寸 */
  size?: ComponentSize
  /** 自定义类名 */
  className?: string
}

/** 主题工具函数类型 */
export interface ThemeUtils {
  /** 获取 CSS 变量值 */
  getCSSVariable: (variable: string) => string

  /** 设置 CSS 变量 */
  setCSSVariable: (variable: string, value: string) => void

  /** 检测系统主题 */
  getSystemTheme: () => 'light' | 'dark'

  /** 应用主题到DOM */
  applyThemeToDOM: (theme: 'light' | 'dark') => void

  /** 验证主题模式 */
  isValidThemeMode: (mode: string) => mode is ThemeMode
}