<template>
  <div class="admin-layout min-h-screen bg-background">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && !sidebarCollapsed"
      class="fixed inset-0 z-40 bg-black/50 lg:hidden"
      @click="closeSidebar"
    />

    <!-- 侧边栏 -->
    <aside
      class="sidebar"
      :class="{
        'sidebar-collapsed': sidebarCollapsed,
        'sidebar-mobile-open': isMobile && !sidebarCollapsed,
        'sidebar-mobile-closed': isMobile && sidebarCollapsed
      }"
    >
      <div class="sidebar-header">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
            <SearchIcon class="w-5 h-5 text-primary-foreground" />
          </div>
          <div v-if="!sidebarCollapsed" class="space-y-1">
            <h1 class="text-lg font-bold tracking-tight">智能搜索</h1>
            <p class="text-xs text-muted-foreground">管理后台</p>
          </div>
        </div>
        <Button
          variant="ghost"
          size="icon"
          @click="toggleSidebar"
          class="ml-auto"
        >
          <MenuIcon class="w-4 h-4" />
        </Button>
      </div>

      <ScrollArea class="flex-1 px-3">
        <nav class="space-y-2 py-4">
          <div v-for="group in navigationGroups" :key="group.title" class="space-y-1">
            <div v-if="!sidebarCollapsed && group.title" class="px-3 py-2">
              <h3 class="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                {{ group.title }}
              </h3>
            </div>
            <div class="space-y-1">
              <Button
                v-for="item in group.items"
                :key="item.path"
                variant="ghost"
                :class="[
                  'nav-item',
                  { 'nav-item-active': $route.path === item.path }
                ]"
                @click="navigateTo(item.path)"
              >
                <component :is="item.icon" class="nav-icon" />
                <span v-if="!sidebarCollapsed" class="nav-text">{{ item.label }}</span>
                <Badge
                  v-if="!sidebarCollapsed && item.badge"
                  variant="secondary"
                  class="ml-auto"
                >
                  {{ item.badge }}
                </Badge>
              </Button>
            </div>
          </div>
        </nav>
      </ScrollArea>

      <!-- 侧边栏底部 -->
      <div class="sidebar-footer">
        <div v-if="!sidebarCollapsed" class="p-3 border-t">
          <div class="text-xs text-muted-foreground">
            版本 v1.0.0
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <div class="header-left">
          <Button
            v-if="isMobile"
            variant="ghost"
            size="icon"
            @click="toggleSidebar"
            class="lg:hidden"
          >
            <MenuIcon class="w-5 h-5" />
          </Button>
          <div class="space-y-1">
            <h2 class="text-2xl font-bold tracking-tight">{{ pageTitle }}</h2>
            <p v-if="pageDescription" class="text-sm text-muted-foreground">
              {{ pageDescription }}
            </p>
          </div>
        </div>

        <div class="header-right">
          <!-- 搜索框 -->
          <div class="relative hidden md:block">
            <SearchIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="搜索..."
              class="pl-10 w-64"
              v-model="globalSearch"
            />
          </div>

          <!-- 通知按钮 -->
          <Button variant="ghost" size="icon" class="relative">
            <BellIcon class="w-5 h-5" />
            <Badge variant="destructive" class="absolute -top-1 -right-1 w-5 h-5 p-0 flex items-center justify-center text-xs">
              3
            </Badge>
          </Button>

          <!-- 主题切换 -->
          <Button
            variant="ghost"
            size="icon"
            @click="toggleTheme"
          >
            <SunIcon v-if="isDark" class="w-5 h-5" />
            <MoonIcon v-else class="w-5 h-5" />
          </Button>

          <!-- 用户菜单 -->
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" class="relative h-10 w-10 rounded-full">
                <Avatar class="h-10 w-10">
                  <AvatarImage :src="userAvatar" :alt="userName" />
                  <AvatarFallback>{{ userInitials }}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent class="w-56" align="end">
              <DropdownMenuLabel class="font-normal">
                <div class="flex flex-col space-y-1">
                  <p class="text-sm font-medium leading-none">{{ userName }}</p>
                  <p class="text-xs leading-none text-muted-foreground">{{ userEmail }}</p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem @click="navigateTo('/admin/settings')">
                <SettingsIcon class="mr-2 h-4 w-4" />
                设置
              </DropdownMenuItem>
              <DropdownMenuItem @click="navigateTo('/profile')">
                <UserIcon class="mr-2 h-4 w-4" />
                个人资料
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem @click="logout" class="text-red-600">
                <LogOutIcon class="mr-2 h-4 w-4" />
                退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <!-- 内容区域 -->
      <div class="content-area">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import {
  Button,
  Input,
  Badge,
  ScrollArea,
  Avatar,
  AvatarFallback,
  AvatarImage,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui'
import {
  MenuIcon,
  SunIcon,
  MoonIcon,
  LayoutDashboardIcon,
  SearchIcon,
  BarChart3Icon,
  SettingsIcon,
  UsersIcon,
  DatabaseIcon,
  BellIcon,
  UserIcon,
  LogOutIcon
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const { isDark } = storeToRefs(themeStore)
const { userDisplayName, userAvatar, userEmail } = storeToRefs(authStore)

const sidebarCollapsed = ref(false)
const isMobile = ref(false)
const globalSearch = ref('')

// 计算属性
const userName = computed(() => userDisplayName.value || '管理员')
const userInitials = computed(() => {
  const name = userName.value
  return name.length >= 2 ? name.substring(0, 2).toUpperCase() : name.charAt(0).toUpperCase()
})

// 导航分组
const navigationGroups = computed(() => [
  {
    title: '主要功能',
    items: [
      { path: '/admin/dashboard', label: '仪表板', icon: LayoutDashboardIcon },
      { path: '/admin/search', label: '搜索管理', icon: SearchIcon, badge: 'New' },
      { path: '/admin/analytics', label: '数据分析', icon: BarChart3Icon }
    ]
  },
  {
    title: '系统管理',
    items: [
      { path: '/admin/users', label: '用户管理', icon: UsersIcon },
      { path: '/admin/data', label: '数据管理', icon: DatabaseIcon },
      { path: '/admin/settings', label: '系统设置', icon: SettingsIcon }
    ]
  }
])

const pageTitle = computed(() => {
  for (const group of navigationGroups.value) {
    const item = group.items.find(item => item.path === route.path)
    if (item) return item.label
  }
  return '管理后台'
})

const pageDescription = computed(() => {
  const descriptions: Record<string, string> = {
    '/admin/dashboard': '查看系统概览和关键指标',
    '/admin/search': '管理搜索功能和索引',
    '/admin/analytics': '分析搜索数据和用户行为',
    '/admin/users': '管理系统用户和权限',
    '/admin/data': '管理数据源和内容',
    '/admin/settings': '配置系统参数和偏好'
  }
  return descriptions[route.path] || ''
})

// 响应式处理
const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024
  if (isMobile.value) {
    sidebarCollapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

// 方法
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const closeSidebar = () => {
  if (isMobile.value) {
    sidebarCollapsed.value = true
  }
}

const toggleTheme = () => {
  themeStore.toggle()
}

const navigateTo = (path: string) => {
  router.push(path)
  if (isMobile.value) {
    closeSidebar()
  }
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* 侧边栏基础样式 */
.sidebar {
  @apply fixed left-0 top-0 z-50 h-full w-64 transform bg-card border-r border-border transition-all duration-300 ease-in-out lg:relative lg:z-auto lg:translate-x-0;
  display: flex;
  flex-direction: column;
}

.sidebar-collapsed {
  @apply w-16;
}

.sidebar-mobile-open {
  @apply translate-x-0;
}

.sidebar-mobile-closed {
  @apply -translate-x-full lg:translate-x-0;
}

/* 侧边栏头部 */
.sidebar-header {
  @apply flex items-center justify-between p-4 border-b border-border;
}

/* 侧边栏底部 */
.sidebar-footer {
  @apply mt-auto;
}

/* 导航项样式 */
.nav-item {
  @apply w-full justify-start h-10 px-3 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground hover:bg-accent;
}

.nav-item-active {
  @apply bg-primary text-primary-foreground hover:bg-primary/90 hover:text-primary-foreground;
}

.nav-icon {
  @apply w-4 h-4 mr-3 flex-shrink-0;
}

.sidebar-collapsed .nav-icon {
  @apply mr-0;
}

.nav-text {
  @apply truncate;
}

/* 主内容区 */
.main-content {
  @apply flex-1 flex flex-col overflow-hidden;
  margin-left: 0;
}

@media (min-width: 1024px) {
  .main-content {
    margin-left: 16rem; /* w-64 */
  }

  .sidebar-collapsed + .main-content {
    margin-left: 4rem; /* w-16 */
  }
}

/* 顶部导航栏 */
.top-header {
  @apply flex items-center justify-between px-6 py-4 bg-card border-b border-border;
}

.header-left {
  @apply flex items-center gap-4;
}

.header-right {
  @apply flex items-center gap-4;
}

/* 内容区域 */
.content-area {
  @apply flex-1 overflow-auto p-6 bg-background;
}

/* 响应式调整 */
@media (max-width: 1023px) {
  .main-content {
    margin-left: 0 !important;
  }
}

/* 动画和过渡效果 */
.sidebar {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 滚动条优化 */
.content-area::-webkit-scrollbar {
  width: 8px;
}

.content-area::-webkit-scrollbar-track {
  @apply bg-muted/30;
}

.content-area::-webkit-scrollbar-thumb {
  @apply bg-muted rounded-md;
}

.content-area::-webkit-scrollbar-thumb:hover {
  @apply bg-muted/80;
}

/* Focus 状态优化 */
.nav-item:focus-visible {
  @apply outline-none ring-2 ring-primary ring-offset-2 ring-offset-background;
}

/* 暗色主题优化 */
@media (prefers-color-scheme: dark) {
  .sidebar {
    @apply bg-card/95 backdrop-blur-sm;
  }
}
</style>