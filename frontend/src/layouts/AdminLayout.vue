<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h1 v-if="!sidebarCollapsed">智能搜索管理</h1>
        <button @click="toggleSidebar" class="toggle-btn">
          <MenuIcon class="w-5 h-5" />
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navigationItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span v-if="!sidebarCollapsed" class="nav-text">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-left">
          <h2>{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <button @click="toggleTheme" class="theme-toggle">
            <SunIcon v-if="isDark" class="w-5 h-5" />
            <MoonIcon v-else class="w-5 h-5" />
          </button>
          <div class="user-menu">
            <img :src="userAvatar" alt="用户头像" class="avatar" />
            <span>{{ userName }}</span>
          </div>
        </div>
      </header>

      <div class="content-area">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import {
  MenuIcon,
  SunIcon,
  MoonIcon,
  LayoutDashboardIcon,
  SearchIcon,
  BarChart3Icon,
  SettingsIcon,
  UsersIcon,
  DatabaseIcon
} from 'lucide-vue-next'

const route = useRoute()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const { isDark } = storeToRefs(themeStore)
const { userDisplayName, userAvatar } = storeToRefs(authStore)

const sidebarCollapsed = ref(false)
const userName = computed(() => userDisplayName.value)

const navigationItems = [
  { path: '/admin/dashboard', label: '仪表板', icon: LayoutDashboardIcon },
  { path: '/admin/search', label: '搜索管理', icon: SearchIcon },
  { path: '/admin/analytics', label: '统计分析', icon: BarChart3Icon },
  { path: '/admin/users', label: '用户管理', icon: UsersIcon },
  { path: '/admin/data', label: '数据管理', icon: DatabaseIcon },
  { path: '/admin/settings', label: '系统设置', icon: SettingsIcon }
]

const pageTitle = computed(() => {
  const item = navigationItems.find(item => item.path === route.path)
  return item?.label || '管理后台'
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const toggleTheme = () => {
  themeStore.toggle()
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
}

.sidebar {
  width: 250px;
  background: hsl(var(--card));
  border-right: 1px solid hsl(var(--border));
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid hsl(var(--border));
}

.sidebar-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.toggle-btn {
  padding: 8px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  color: hsl(var(--foreground));
}

.toggle-btn:hover {
  background: hsl(var(--accent));
}

.sidebar-nav {
  padding: 16px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: hsl(var(--muted-foreground));
  text-decoration: none;
  transition: all 0.2s;
  border-radius: 0 25px 25px 0;
  margin-right: 8px;
}

.nav-item:hover {
  background: hsl(var(--accent));
  color: hsl(var(--accent-foreground));
}

.nav-item.active {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.nav-icon {
  width: 20px;
  height: 20px;
  margin-right: 12px;
}

.sidebar.collapsed .nav-text {
  display: none;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: hsl(var(--card));
  border-bottom: 1px solid hsl(var(--border));
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.theme-toggle {
  padding: 8px;
  background: none;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
  color: hsl(var(--foreground));
}

.theme-toggle:hover {
  background: hsl(var(--accent));
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: hsl(var(--accent));
  border-radius: 6px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: hsl(var(--background));
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1000;
    height: 100vh;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar.collapsed {
    transform: translateX(0);
    width: 250px;
  }

  .main-content {
    margin-left: 0;
  }
}
</style>