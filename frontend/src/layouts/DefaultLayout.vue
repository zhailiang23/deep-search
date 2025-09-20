<template>
  <div class="default-layout">
    <header class="main-header">
      <div class="header-container">
        <div class="header-left">
          <router-link to="/" class="logo">
            <img src="/logo.svg" alt="智能搜索平台" class="logo-image" />
            <span class="logo-text">智能搜索平台</span>
          </router-link>
        </div>

        <nav class="main-nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/search" class="nav-link">搜索</router-link>
        </nav>

        <div class="header-right">
          <button @click="toggleTheme" class="theme-toggle" aria-label="切换主题">
            <SunIcon v-if="isDark" class="w-5 h-5" />
            <MoonIcon v-else class="w-5 h-5" />
          </button>

          <div v-if="isAuthenticated" class="user-menu">
            <button @click="showUserMenu = !showUserMenu" class="user-button">
              <img :src="userAvatar" :alt="userDisplayName" class="user-avatar" />
              <span class="user-name">{{ userDisplayName }}</span>
              <ChevronDownIcon class="w-4 h-4" />
            </button>

            <div v-if="showUserMenu" class="user-dropdown">
              <router-link to="/profile" class="dropdown-item" @click="closeUserMenu">
                <UserIcon class="w-4 h-4" />
                个人资料
              </router-link>
              <router-link to="/settings" class="dropdown-item" @click="closeUserMenu">
                <SettingsIcon class="w-4 h-4" />
                设置
              </router-link>
              <router-link v-if="isAdmin" to="/admin" class="dropdown-item" @click="closeUserMenu">
                <ShieldIcon class="w-4 h-4" />
                管理后台
              </router-link>
              <div class="dropdown-divider"></div>
              <button @click="handleLogout" class="dropdown-item logout">
                <LogOutIcon class="w-4 h-4" />
                退出登录
              </button>
            </div>
          </div>

          <div v-else class="auth-buttons">
            <router-link to="/auth/login" class="login-button">登录</router-link>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="main-footer">
      <div class="footer-container">
        <div class="footer-content">
          <div class="footer-section">
            <h3>智能搜索平台</h3>
            <p>为银行业务场景提供智能语义搜索服务</p>
          </div>

          <div class="footer-section">
            <h4>产品</h4>
            <ul>
              <li><a href="/search">智能搜索</a></li>
              <li><a href="/analytics">搜索分析</a></li>
            </ul>
          </div>

          <div class="footer-section">
            <h4>支持</h4>
            <ul>
              <li><a href="/help">帮助中心</a></li>
              <li><a href="/contact">联系我们</a></li>
            </ul>
          </div>

          <div class="footer-section">
            <h4>关于</h4>
            <ul>
              <li><a href="/about">关于我们</a></li>
              <li><a href="/privacy">隐私政策</a></li>
            </ul>
          </div>
        </div>

        <div class="footer-bottom">
          <p>&copy; 2024 智能搜索平台. 保留所有权利.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import {
  SunIcon,
  MoonIcon,
  ChevronDownIcon,
  UserIcon,
  SettingsIcon,
  ShieldIcon,
  LogOutIcon
} from 'lucide-vue-next'

const router = useRouter()
const themeStore = useThemeStore()
const authStore = useAuthStore()

// 响应式数据
const { isDark } = storeToRefs(themeStore)
const { isAuthenticated, isAdmin, userDisplayName, userAvatar } = storeToRefs(authStore)

const showUserMenu = ref(false)

// 方法
const toggleTheme = () => {
  themeStore.toggle()
}

const closeUserMenu = () => {
  showUserMenu.value = false
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    closeUserMenu()
    router.push('/')
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}

// 点击外部关闭用户菜单
const handleClickOutside = (event: Event) => {
  const target = event.target as Element
  if (!target.closest('.user-menu')) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-header {
  background: hsl(var(--background));
  border-bottom: 1px solid hsl(var(--border));
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: hsl(var(--foreground));
  font-weight: 600;
  font-size: 18px;
}

.logo-image {
  width: 32px;
  height: 32px;
}

.logo-text {
  @media (max-width: 768px) {
    display: none;
  }
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 24px;

  @media (max-width: 768px) {
    display: none;
  }
}

.nav-link {
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  transition: color 0.2s;
  padding: 8px 12px;
  border-radius: 6px;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: hsl(var(--primary));
  background: hsl(var(--accent));
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  padding: 8px;
  background: none;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  cursor: pointer;
  color: hsl(var(--foreground));
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: hsl(var(--accent));
}

.user-menu {
  position: relative;
}

.user-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: none;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  cursor: pointer;
  color: hsl(var(--foreground));
  transition: all 0.2s;
}

.user-button:hover {
  background: hsl(var(--accent));
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  @media (max-width: 640px) {
    display: none;
  }
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  min-width: 200px;
  background: hsl(var(--popover));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  z-index: 50;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  background: none;
  border: none;
  text-decoration: none;
  color: hsl(var(--popover-foreground));
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}

.dropdown-item:hover {
  background: hsl(var(--accent));
}

.dropdown-item.logout {
  color: hsl(var(--destructive));
}

.dropdown-divider {
  height: 1px;
  background: hsl(var(--border));
  margin: 4px 0;
}

.auth-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.login-button {
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  text-decoration: none;
  border-radius: 6px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.login-button:hover {
  background: hsl(var(--primary) / 0.9);
}

.main-content {
  flex: 1;
}

.main-footer {
  background: hsl(var(--muted));
  border-top: 1px solid hsl(var(--border));
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 16px 16px;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 32px;
  margin-bottom: 32px;
}

.footer-section h3,
.footer-section h4 {
  margin-bottom: 16px;
  color: hsl(var(--foreground));
  font-weight: 600;
}

.footer-section h3 {
  font-size: 18px;
}

.footer-section h4 {
  font-size: 16px;
}

.footer-section p {
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section li {
  margin-bottom: 8px;
}

.footer-section a {
  color: hsl(var(--muted-foreground));
  text-decoration: none;
  transition: color 0.2s;
}

.footer-section a:hover {
  color: hsl(var(--primary));
}

.footer-bottom {
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid hsl(var(--border));
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}
</style>