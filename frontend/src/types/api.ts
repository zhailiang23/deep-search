// API请求配置
export interface ApiConfig {
  baseURL: string
  timeout: number
  retries: number
  retryDelay: number
  headers: Record<string, string>
}

// HTTP方法
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS'

// 请求选项
export interface RequestOptions {
  method?: HttpMethod
  headers?: Record<string, string>
  params?: Record<string, any>
  data?: any
  timeout?: number
  retries?: number
  retryDelay?: number
  signal?: AbortSignal
  onUploadProgress?: (progressEvent: ProgressEvent) => void
  onDownloadProgress?: (progressEvent: ProgressEvent) => void
}

// API错误
export interface ApiError {
  code: string
  message: string
  statusCode: number
  details?: Record<string, any>
  timestamp: number
  path?: string
  method?: string
  requestId?: string
}

// 请求拦截器
export interface RequestInterceptor {
  onFulfilled?: (config: any) => any | Promise<any>
  onRejected?: (error: any) => any | Promise<any>
}

// 响应拦截器
export interface ResponseInterceptor {
  onFulfilled?: (response: any) => any | Promise<any>
  onRejected?: (error: any) => any | Promise<any>
}