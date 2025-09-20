import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { authApi } from '@/lib/api/auth'
import type { User, LoginCredentials, LoginResponse } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user: Ref<User | null> = ref(null)
  const token: Ref<string | null> = ref(null)
  const refreshToken: Ref<string | null> = ref(null)
  const isInitialized = ref(false)
  const loading = ref(false)
  const error: Ref<string | null> = ref(null)

  // 权限相关状态
  const permissions: Ref<string[]> = ref([])
  const roles: Ref<string[]> = ref([])

  // 计算属性
  const isAuthenticated = computed(() => !!user.value && !!token.value)
  const isAdmin = computed(() => roles.value.includes('admin') || roles.value.includes('super_admin'))
  const isSuperAdmin = computed(() => roles.value.includes('super_admin'))
  const userDisplayName = computed(() => user.value?.name || user.value?.username || '未知用户')
  const userAvatar = computed(() => user.value?.avatar || '/avatars/default.jpg')

  // Token管理
  const setTokens = (accessToken: string, refreshTokenValue?: string) => {
    token.value = accessToken
    if (refreshTokenValue) {
      refreshToken.value = refreshTokenValue
    }

    // 保存到localStorage
    localStorage.setItem('auth-token', accessToken)
    if (refreshTokenValue) {
      localStorage.setItem('refresh-token', refreshTokenValue)
    }

    // 设置API默认认证头
    authApi.setAuthToken(accessToken)
  }

  const clearTokens = () => {
    token.value = null
    refreshToken.value = null
    localStorage.removeItem('auth-token')
    localStorage.removeItem('refresh-token')
    authApi.clearAuthToken()
  }

  const loadTokensFromStorage = () => {
    const savedToken = localStorage.getItem('auth-token')
    const savedRefreshToken = localStorage.getItem('refresh-token')

    if (savedToken) {
      token.value = savedToken
      authApi.setAuthToken(savedToken)
    }

    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken
    }
  }

  // 用户登录
  const login = async (credentials: LoginCredentials): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      const response: LoginResponse = await authApi.login(credentials)

      // 设置用户信息
      user.value = response.user
      permissions.value = response.permissions || []
      roles.value = response.roles || []

      // 设置token
      setTokens(response.token, response.refreshToken)

      // 记录登录时间
      if (user.value) {
        user.value.lastLoginAt = new Date().toISOString()
      }

    } catch (err: any) {
      error.value = err.message || '登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 用户登出
  const logout = async (): Promise<void> => {
    loading.value = true

    try {
      // 调用登出API
      if (token.value) {
        await authApi.logout()
      }
    } catch (err) {
      console.warn('Logout API call failed:', err)
    } finally {
      // 清除本地状态
      user.value = null
      permissions.value = []
      roles.value = []
      clearTokens()
      loading.value = false
      error.value = null
    }
  }

  // 刷新token
  const refreshAccessToken = async (): Promise<boolean> => {
    if (!refreshToken.value) {
      return false
    }

    try {
      const response = await authApi.refreshToken(refreshToken.value)
      setTokens(response.token, response.refreshToken)
      return true
    } catch (err) {
      console.error('Token refresh failed:', err)
      await logout()
      return false
    }
  }

  // 获取用户信息
  const fetchUserProfile = async (): Promise<void> => {
    if (!token.value) return

    try {
      const userProfile = await authApi.getUserProfile()
      user.value = userProfile.user
      permissions.value = userProfile.permissions || []
      roles.value = userProfile.roles || []
    } catch (err: any) {
      console.error('Failed to fetch user profile:', err)
      if (err.status === 401) {
        // Token无效，尝试刷新
        const refreshed = await refreshAccessToken()
        if (!refreshed) {
          await logout()
        }
      }
    }
  }

  // 更新用户信息
  const updateProfile = async (profileData: Partial<User>): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      const updatedUser = await authApi.updateProfile(profileData)
      user.value = { ...user.value, ...updatedUser }
    } catch (err: any) {
      error.value = err.message || '更新用户信息失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 修改密码
  const changePassword = async (oldPassword: string, newPassword: string): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      await authApi.changePassword({ oldPassword, newPassword })
    } catch (err: any) {
      error.value = err.message || '修改密码失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 权限检查
  const hasPermission = (permission: string): boolean => {
    return permissions.value.includes(permission) || isAdmin.value
  }

  const hasRole = (role: string): boolean => {
    return roles.value.includes(role)
  }

  const hasAnyPermission = (permissionList: string[]): boolean => {
    return permissionList.some(permission => hasPermission(permission))
  }

  const hasAllPermissions = (permissionList: string[]): boolean => {
    return permissionList.every(permission => hasPermission(permission))
  }

  // 初始化认证状态
  const initialize = async (): Promise<void> => {
    if (isInitialized.value) return

    // 从localStorage加载token
    loadTokensFromStorage()

    // 如果有token，尝试获取用户信息
    if (token.value) {
      await fetchUserProfile()
    }

    isInitialized.value = true
  }

  // 清除错误状态
  const clearError = () => {
    error.value = null
  }

  // 检查token是否即将过期
  const isTokenExpiringSoon = (): boolean => {
    if (!token.value) return false

    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      const expirationTime = payload.exp * 1000
      const currentTime = Date.now()
      const fiveMinutes = 5 * 60 * 1000

      return expirationTime - currentTime < fiveMinutes
    } catch {
      return true
    }
  }

  // 自动刷新token
  const setupTokenRefresh = () => {
    setInterval(async () => {
      if (isAuthenticated.value && isTokenExpiringSoon()) {
        await refreshAccessToken()
      }
    }, 60000) // 每分钟检查一次
  }

  return {
    // 状态
    user,
    token,
    refreshToken,
    isInitialized,
    loading,
    error,
    permissions,
    roles,

    // 计算属性
    isAuthenticated,
    isAdmin,
    isSuperAdmin,
    userDisplayName,
    userAvatar,

    // 方法
    login,
    logout,
    refreshAccessToken,
    fetchUserProfile,
    updateProfile,
    changePassword,
    hasPermission,
    hasRole,
    hasAnyPermission,
    hasAllPermissions,
    initialize,
    clearError,
    setupTokenRefresh,
    isTokenExpiringSoon
  }
})