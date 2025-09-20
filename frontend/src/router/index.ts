import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 布局组件
const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const EmptyLayout = () => import('@/layouts/EmptyLayout.vue')

// 页面组件
const HomePage = () => import('@/views/Home.vue')
const SearchPage = () => import('@/views/search/SearchPage.vue')
const SearchResultsPage = () => import('@/views/search/SearchResultsPage.vue')
const LoginPage = () => import('@/views/auth/LoginPage.vue')
const ComponentTestPage = () => import('@/views/ComponentTest.vue')

// 管理后台页面
const AdminDashboard = () => import('@/views/admin/Dashboard.vue')
const AdminSearchManagement = () => import('@/views/admin/SearchManagement.vue')
const AdminAnalytics = () => import('@/views/admin/Analytics.vue')
const AdminSettings = () => import('@/views/admin/Settings.vue')
const AdminUserManagement = () => import('@/views/admin/UserManagement.vue')
const AdminDataManagement = () => import('@/views/admin/DataManagement.vue')

// 错误页面
const NotFoundPage = () => import('@/views/errors/NotFoundPage.vue')
const ForbiddenPage = () => import('@/views/errors/ForbiddenPage.vue')
const ServerErrorPage = () => import('@/views/errors/ServerErrorPage.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomePage,
        meta: {
          title: '智能搜索平台',
          description: '为银行业务场景提供智能语义搜索服务'
        }
      },
      {
        path: '/search',
        name: 'Search',
        component: SearchPage,
        meta: {
          title: '搜索',
          description: '智能搜索'
        }
      },
      {
        path: '/search/results',
        name: 'SearchResults',
        component: SearchResultsPage,
        meta: {
          title: '搜索结果',
          description: '搜索结果页面'
        },
        props: route => ({
          query: route.query.q,
          filters: route.query.filters,
          sort: route.query.sort,
          page: Number(route.query.page) || 1
        })
      },
      {
        path: '/component-test',
        name: 'ComponentTest',
        component: ComponentTestPage,
        meta: {
          title: 'UI组件测试',
          description: 'UI组件和主题测试页面'
        }
      }
    ]
  },

  // 认证相关路由
  {
    path: '/auth',
    component: EmptyLayout,
    children: [
      {
        path: 'login',
        name: 'Login',
        component: LoginPage,
        meta: {
          title: '用户登录',
          requiresGuest: true
        }
      }
    ]
  },

  // 管理后台路由
  {
    path: '/admin',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      requiresAdmin: true
    },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: {
          title: '仪表板',
          description: '管理后台仪表板'
        }
      },
      {
        path: 'search',
        name: 'AdminSearchManagement',
        component: AdminSearchManagement,
        meta: {
          title: '搜索管理',
          description: '搜索功能管理'
        }
      },
      {
        path: 'analytics',
        name: 'AdminAnalytics',
        component: AdminAnalytics,
        meta: {
          title: '统计分析',
          description: '搜索统计与分析'
        }
      },
      {
        path: 'users',
        name: 'AdminUserManagement',
        component: AdminUserManagement,
        meta: {
          title: '用户管理',
          description: '用户权限管理'
        }
      },
      {
        path: 'data',
        name: 'AdminDataManagement',
        component: AdminDataManagement,
        meta: {
          title: '数据管理',
          description: '搜索数据管理'
        }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: AdminSettings,
        meta: {
          title: '系统设置',
          description: '系统配置管理'
        }
      }
    ]
  },

  // 错误页面路由
  {
    path: '/errors',
    component: EmptyLayout,
    children: [
      {
        path: '403',
        name: 'Forbidden',
        component: ForbiddenPage,
        meta: {
          title: '访问被禁止'
        }
      },
      {
        path: '500',
        name: 'ServerError',
        component: ServerErrorPage,
        meta: {
          title: '服务器错误'
        }
      }
    ]
  },

  // 404 页面必须放在最后
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundPage,
    meta: {
      title: '页面未找到'
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 智能搜索平台`
  }

  // 设置页面描述
  if (to.meta.description) {
    const metaDescription = document.querySelector('meta[name="description"]')
    if (metaDescription) {
      metaDescription.setAttribute('content', to.meta.description as string)
    }
  }

  // 检查认证状态
  if (!authStore.isInitialized) {
    await authStore.initialize()
  }

  // 访客页面检查（如登录页）
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next({ name: 'Home' })
    return
  }

  // 认证检查
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 管理员权限检查
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Forbidden' })
    return
  }

  next()
})

// 全局后置钩子
router.afterEach((to, _from) => {
  // 页面加载完成后的处理
  if (typeof window !== 'undefined') {
    // 发送页面浏览统计
    if (window.gtag) {
      window.gtag('config', 'GA_MEASUREMENT_ID', {
        page_title: to.meta.title,
        page_location: window.location.href
      })
    }
  }
})

export default router