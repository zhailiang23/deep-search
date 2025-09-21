// 用户类型
export interface User {
  id: string
  username: string
  name: string
  email: string
  avatar?: string
  phone?: string
  department?: string
  position?: string
  status: 'active' | 'inactive' | 'suspended'
  emailVerified: boolean
  phoneVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  loginCount: number
  preferences: UserPreferences
}

// 用户偏好设置
export interface UserPreferences {
  theme: 'light' | 'dark' | 'system'
  language: string
  timezone: string
  notifications: NotificationSettings
  searchPreferences: SearchPreferences
}

// 通知设置
export interface NotificationSettings {
  email: boolean
  push: boolean
  sms: boolean
  marketing: boolean
  security: boolean
}

// 搜索偏好
export interface SearchPreferences {
  defaultSort: 'relevance' | 'date' | 'popularity'
  resultsPerPage: number
  showPreview: boolean
  enableAutoComplete: boolean
  saveSearchHistory: boolean
}

// 登录凭据
export interface LoginCredentials {
  username: string
  password: string
  rememberMe?: boolean
  captcha?: string
}

// 登录响应
export interface LoginResponse {
  user: User
  token: string
  refreshToken?: string
  expiresIn: number
  permissions: string[]
  roles: string[]
}

// 注册数据
export interface RegisterData {
  username: string
  email: string
  password: string
  confirmPassword: string
  name: string
  phone?: string
  department?: string
  invitationCode?: string
  agreement: boolean
}

// 用户资料更新
export interface ProfileUpdateData {
  name?: string
  email?: string
  phone?: string
  avatar?: string
  department?: string
  position?: string
  preferences?: Partial<UserPreferences>
}

// 密码修改
export interface PasswordChangeData {
  oldPassword: string
  newPassword: string
  confirmPassword?: string
}

// 密码重置
export interface PasswordResetData {
  email: string
  token?: string
  newPassword?: string
}

// 权限和角色
export interface Permission {
  id: string
  name: string
  code: string
  description: string
  resource: string
  action: string
}

export interface Role {
  id: string
  name: string
  code: string
  description: string
  permissions: Permission[]
  isSystem: boolean
  createdAt: string
  updatedAt: string
}

// 会话信息
export interface SessionInfo {
  id: string
  userId: string
  deviceType: string
  browser: string
  os: string
  ip: string
  location?: string
  createdAt: string
  lastActiveAt: string
  expiresAt: string
  isCurrent: boolean
}

// JWT载荷
export interface JWTPayload {
  sub: string // 用户ID
  username: string
  email: string
  roles: string[]
  permissions: string[]
  iat: number // 签发时间
  exp: number // 过期时间
  jti: string // JWT ID
}

// 认证错误
export interface AuthError {
  code: string
  message: string
  field?: string
  details?: Record<string, any>
}

// OAuth提供商
export interface OAuthProvider {
  name: string
  displayName: string
  icon: string
  color: string
  enabled: boolean
}

// OAuth回调数据
export interface OAuthCallbackData {
  code: string
  state: string
  provider: string
}