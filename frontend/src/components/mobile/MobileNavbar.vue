<template>
  <header class="mobile-navbar bg-background border-b border-border">
    <div class="flex items-center justify-between h-14 px-4">
      <!-- 左侧区域 -->
      <div class="flex items-center">
        <button
          v-if="showBack"
          @click="handleBack"
          class="flex items-center justify-center w-10 h-10 rounded-full hover:bg-accent mobile-touch-target mr-2"
          aria-label="返回"
        >
          <ArrowLeftIcon class="w-5 h-5 text-foreground" />
        </button>

        <slot name="left">
          <div v-if="!showBack" class="w-10 h-10 flex items-center justify-center">
            <SearchIcon class="w-6 h-6 text-primary" />
          </div>
        </slot>
      </div>

      <!-- 中间标题区域 -->
      <div class="flex-1 mx-4">
        <h1 class="text-lg font-semibold text-foreground text-center truncate">
          {{ title }}
        </h1>
      </div>

      <!-- 右侧区域 -->
      <div class="flex items-center">
        <slot name="right">
          <button
            v-if="showMenu"
            @click="handleMenu"
            class="flex items-center justify-center w-10 h-10 rounded-full hover:bg-accent mobile-touch-target"
            aria-label="菜单"
          >
            <MenuIcon class="w-5 h-5 text-foreground" />
          </button>

          <button
            v-if="showSearch"
            @click="handleSearch"
            class="flex items-center justify-center w-10 h-10 rounded-full hover:bg-accent mobile-touch-target ml-2"
            aria-label="搜索"
          >
            <SearchIcon class="w-5 h-5 text-foreground" />
          </button>
        </slot>
      </div>
    </div>

    <!-- 可选的副标题或进度指示器 -->
    <div v-if="subtitle || showProgress" class="px-4 pb-2">
      <p v-if="subtitle" class="text-sm text-muted-foreground">{{ subtitle }}</p>

      <div v-if="showProgress" class="w-full bg-muted rounded-full h-1 mt-2">
        <div
          class="bg-primary h-1 rounded-full transition-all duration-300"
          :style="{ width: `${progress}%` }"
        ></div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ArrowLeftIcon, SearchIcon, MenuIcon } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

interface Props {
  title: string
  subtitle?: string
  showBack?: boolean
  showMenu?: boolean
  showSearch?: boolean
  showProgress?: boolean
  progress?: number
}

const props = withDefaults(defineProps<Props>(), {
  showBack: true,
  showMenu: false,
  showSearch: false,
  showProgress: false,
  progress: 0
})

const emit = defineEmits<{
  back: []
  menu: []
  search: []
}>()

const router = useRouter()

const handleBack = () => {
  emit('back')
  if (router.canGoBack()) {
    router.back()
  } else {
    router.push('/')
  }
}

const handleMenu = () => {
  emit('menu')
}

const handleSearch = () => {
  emit('search')
}
</script>

<style scoped>
.mobile-navbar {
  /* 固定在顶部 */
  position: sticky;
  top: 0;
  z-index: 50;

  /* 背景模糊效果 */
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  /* iOS Safari 优化 */
  -webkit-touch-callout: none;
  -webkit-user-select: none;
}

.mobile-touch-target {
  min-height: 44px;
  min-width: 44px;
  touch-action: manipulation;
}

.mobile-touch-target:active {
  transform: scale(0.95);
  transition: transform 0.1s ease;
}

/* 确保在暗色模式下的可见性 */
.mobile-navbar {
  background-color: rgba(var(--background-rgb, 255, 255, 255), 0.9);
}

.dark .mobile-navbar {
  background-color: rgba(var(--background-rgb, 0, 0, 0), 0.9);
}

/* 安全区域适配 */
@supports (padding-top: env(safe-area-inset-top)) {
  .mobile-navbar {
    padding-top: env(safe-area-inset-top);
  }
}
</style>