<template>
  <!-- 遮罩层 -->
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 bg-black/50"
      @click="handleClose"
    >
      <!-- 侧边菜单 -->
      <div
        class="fixed top-0 left-0 bottom-0 w-80 max-w-[85vw] bg-background overflow-hidden"
        @click.stop
        :class="[
          'transform transition-transform duration-300 ease-out',
          open ? 'translate-x-0' : '-translate-x-full'
        ]"
      >
        <!-- 菜单头部 -->
        <div class="bg-primary text-primary-foreground p-6 safe-top">
          <div class="flex items-center mb-4">
            <div class="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center mr-4">
              <UserIcon class="w-6 h-6" />
            </div>
            <div>
              <h3 class="font-semibold">智能搜索</h3>
              <p class="text-sm opacity-90">银行业务助手</p>
            </div>
          </div>
        </div>

        <!-- 菜单内容 -->
        <div class="flex-1 overflow-y-auto">
          <!-- 主要功能 -->
          <div class="py-2">
            <div class="px-4 py-2">
              <h4 class="text-xs font-medium text-muted-foreground uppercase tracking-wider">主要功能</h4>
            </div>
            <nav class="space-y-1">
              <button
                v-for="item in primaryMenuItems"
                :key="item.id"
                @click="handleItemClick(item.id)"
                class="flex items-center w-full px-4 py-3 text-left hover:bg-accent mobile-touch-target"
              >
                <component :is="item.icon" class="w-5 h-5 mr-3 text-muted-foreground" />
                <span class="text-foreground">{{ item.label }}</span>
                <ChevronRightIcon class="w-4 h-4 ml-auto text-muted-foreground" />
              </button>
            </nav>
          </div>

          <!-- 分隔线 -->
          <div class="border-t border-border my-2"></div>

          <!-- 工具功能 -->
          <div class="py-2">
            <div class="px-4 py-2">
              <h4 class="text-xs font-medium text-muted-foreground uppercase tracking-wider">工具</h4>
            </div>
            <nav class="space-y-1">
              <button
                v-for="item in toolMenuItems"
                :key="item.id"
                @click="handleItemClick(item.id)"
                class="flex items-center w-full px-4 py-3 text-left hover:bg-accent mobile-touch-target"
              >
                <component :is="item.icon" class="w-5 h-5 mr-3 text-muted-foreground" />
                <span class="text-foreground">{{ item.label }}</span>
                <ChevronRightIcon class="w-4 h-4 ml-auto text-muted-foreground" />
              </button>
            </nav>
          </div>

          <!-- 分隔线 -->
          <div class="border-t border-border my-2"></div>

          <!-- 设置和帮助 */
          <div class="py-2">
            <div class="px-4 py-2">
              <h4 class="text-xs font-medium text-muted-foreground uppercase tracking-wider">设置</h4>
            </div>
            <nav class="space-y-1">
              <button
                v-for="item in settingsMenuItems"
                :key="item.id"
                @click="handleItemClick(item.id)"
                class="flex items-center w-full px-4 py-3 text-left hover:bg-accent mobile-touch-target"
              >
                <component :is="item.icon" class="w-5 h-5 mr-3 text-muted-foreground" />
                <span class="text-foreground">{{ item.label }}</span>
                <ChevronRightIcon class="w-4 h-4 ml-auto text-muted-foreground" />
              </button>
            </nav>
          </div>

          <!-- 主题切换 -->
          <div class="px-4 py-3 border-t border-border">
            <button
              @click="toggleTheme"
              class="flex items-center justify-between w-full mobile-touch-target"
            >
              <div class="flex items-center">
                <component :is="themeIcon" class="w-5 h-5 mr-3 text-muted-foreground" />
                <span class="text-foreground">{{ themeLabel }}</span>
              </div>
              <div class="w-12 h-6 bg-muted rounded-full relative">
                <div
                  :class="[
                    'absolute top-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200',
                    isDark ? 'translate-x-6' : 'translate-x-0.5'
                  ]"
                ></div>
              </div>
            </button>
          </div>
        </div>

        <!-- 菜单底部 -->
        <div class="border-t border-border p-4 safe-bottom">
          <div class="text-center">
            <p class="text-xs text-muted-foreground">版本 1.0.0</p>
            <p class="text-xs text-muted-foreground mt-1">© 2024 智能搜索平台</p>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  UserIcon,
  SearchIcon,
  HomeIcon,
  BookmarkIcon,
  ClockIcon,
  BarChart3Icon,
  CalculatorIcon,
  MapPinIcon,
  HelpCircleIcon,
  SettingsIcon,
  InfoIcon,
  SunIcon,
  MoonIcon,
  ChevronRightIcon
} from 'lucide-vue-next'
import { useDark, useToggle } from '@vueuse/core'

interface Props {
  open: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'menu-item-click': [itemId: string]
}>()

const router = useRouter()

// 主题切换
const isDark = useDark()
const toggleTheme = useToggle(isDark)

const themeIcon = computed(() => isDark.value ? MoonIcon : SunIcon)
const themeLabel = computed(() => isDark.value ? '深色模式' : '浅色模式')

// 菜单项配置
const primaryMenuItems = [
  {
    id: 'home',
    label: '首页',
    icon: HomeIcon
  },
  {
    id: 'search',
    label: '智能搜索',
    icon: SearchIcon
  },
  {
    id: 'favorites',
    label: '我的收藏',
    icon: BookmarkIcon
  },
  {
    id: 'history',
    label: '搜索历史',
    icon: ClockIcon
  },
  {
    id: 'analytics',
    label: '搜索统计',
    icon: BarChart3Icon
  }
]

const toolMenuItems = [
  {
    id: 'calculator',
    label: '存款计算器',
    icon: CalculatorIcon
  },
  {
    id: 'branches',
    label: '网点查询',
    icon: MapPinIcon
  }
]

const settingsMenuItems = [
  {
    id: 'settings',
    label: '应用设置',
    icon: SettingsIcon
  },
  {
    id: 'help',
    label: '帮助中心',
    icon: HelpCircleIcon
  },
  {
    id: 'about',
    label: '关于我们',
    icon: InfoIcon
  }
]

// 事件处理函数
const handleClose = () => {
  emit('update:open', false)
}

const handleItemClick = (itemId: string) => {
  emit('menu-item-click', itemId)

  // 根据菜单项ID进行路由跳转
  switch (itemId) {
    case 'home':
      router.push('/mobile')
      break
    case 'search':
      router.push('/mobile/search')
      break
    case 'favorites':
      router.push('/mobile/favorites')
      break
    case 'history':
      router.push('/mobile/history')
      break
    case 'analytics':
      router.push('/mobile/analytics')
      break
    case 'calculator':
      router.push('/mobile/calculator')
      break
    case 'branches':
      router.push('/mobile/branches')
      break
    case 'settings':
      router.push('/mobile/settings')
      break
    case 'help':
      router.push('/mobile/help')
      break
    case 'about':
      router.push('/mobile/about')
      break
  }
}
</script>

<style scoped>
.mobile-touch-target {
  min-height: 44px;
  min-width: 44px;
  touch-action: manipulation;
}

.mobile-touch-target:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

.safe-top {
  padding-top: env(safe-area-inset-top);
}

.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

/* 侧边菜单动画 */
@keyframes slideInFromLeft {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

@keyframes slideOutToLeft {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-100%);
  }
}

/* 改进的滚动 */
.overflow-y-auto {
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}
</style>