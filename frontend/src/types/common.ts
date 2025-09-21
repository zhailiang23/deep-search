// 通用API响应格式
export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message: string
  code: number
  timestamp: number
  requestId?: string
}

// 分页参数
export interface PaginationParams {
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 分页响应
export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasNext: boolean
  hasPrev: boolean
}

// 选项类型
export interface Option {
  value: string | number
  label: string
  disabled?: boolean
  data?: any
}

// 树形结构选项
export interface TreeOption extends Option {
  children?: TreeOption[]
  parent?: string
  level?: number
  expanded?: boolean
  selected?: boolean
}

// 表格列定义
export interface TableColumn {
  key: string
  title: string
  dataIndex?: string
  width?: number
  minWidth?: number
  maxWidth?: number
  sortable?: boolean
  filterable?: boolean
  resizable?: boolean
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  render?: (value: any, record: any, index: number) => any
  filters?: Option[]
  defaultFilterValue?: any
  sorter?: boolean | ((a: any, b: any) => number)
}

// 表格配置
export interface TableConfig {
  rowKey: string
  columns: TableColumn[]
  pagination?: PaginationConfig
  selection?: SelectionConfig
  loading?: boolean
  bordered?: boolean
  striped?: boolean
  size?: 'small' | 'medium' | 'large'
}

// 分页配置
export interface PaginationConfig {
  current: number
  pageSize: number
  total: number
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  showTotal?: boolean
  pageSizeOptions?: number[]
}

// 选择配置
export interface SelectionConfig {
  type: 'checkbox' | 'radio'
  selectedRowKeys?: (string | number)[]
  onChange?: (selectedRowKeys: (string | number)[], selectedRows: any[]) => void
  getCheckboxProps?: (record: any) => any
}

// 表单字段类型
export interface FormField {
  name: string
  label: string
  type: 'text' | 'password' | 'email' | 'number' | 'textarea' | 'select' | 'checkbox' | 'radio' | 'date' | 'daterange' | 'file' | 'switch'
  placeholder?: string
  required?: boolean
  disabled?: boolean
  readonly?: boolean
  defaultValue?: any
  options?: Option[]
  rules?: ValidationRule[]
  props?: Record<string, any>
  slot?: string
}

// 表单验证规则
export interface ValidationRule {
  required?: boolean
  message?: string
  min?: number
  max?: number
  pattern?: RegExp
  validator?: (value: any, field: FormField) => boolean | Promise<boolean>
  trigger?: 'blur' | 'change' | 'submit'
}

// 文件上传类型
export interface UploadFile {
  uid: string
  name: string
  size: number
  type: string
  url?: string
  thumbUrl?: string
  status: 'uploading' | 'done' | 'error' | 'removed'
  percent?: number
  response?: any
  error?: any
  originFileObj?: File
}

// 上传配置
export interface UploadConfig {
  action: string
  method?: 'POST' | 'PUT'
  headers?: Record<string, string>
  data?: Record<string, any>
  name?: string
  accept?: string
  multiple?: boolean
  maxCount?: number
  maxSize?: number
  beforeUpload?: (file: File) => boolean | Promise<boolean>
  onChange?: (fileList: UploadFile[]) => void
  onProgress?: (percent: number, file: UploadFile) => void
  onSuccess?: (response: any, file: UploadFile) => void
  onError?: (error: any, file: UploadFile) => void
}

// 模态框配置
export interface ModalConfig {
  title: string
  content?: string
  width?: number
  height?: number
  closable?: boolean
  maskClosable?: boolean
  keyboard?: boolean
  centered?: boolean
  destroyOnClose?: boolean
  footer?: boolean | any[]
  onOk?: () => void | Promise<void>
  onCancel?: () => void
  onClose?: () => void
}

// 通知类型
export interface NotificationConfig {
  type: 'success' | 'info' | 'warning' | 'error'
  title: string
  description?: string
  duration?: number
  placement?: 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight'
  icon?: string
  onClick?: () => void
  onClose?: () => void
}

// 菜单项类型
export interface MenuItem {
  key: string
  label: string
  icon?: string
  path?: string
  children?: MenuItem[]
  disabled?: boolean
  hidden?: boolean
  permission?: string
  badge?: string | number
  target?: '_blank' | '_self'
}

// 面包屑项
export interface BreadcrumbItem {
  title: string
  path?: string
  icon?: string
}

// 设备信息
export interface DeviceInfo {
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  userAgent: string
  platform: string
  language: string
  screen: {
    width: number
    height: number
    ratio: number
  }
}

// 网络状态
export interface NetworkStatus {
  online: boolean
  type: string
  downlink?: number
  rtt?: number
  effectiveType?: string
}

// 地理位置
export interface GeolocationPosition {
  latitude: number
  longitude: number
  accuracy: number
  altitude?: number
  altitudeAccuracy?: number
  heading?: number
  speed?: number
  timestamp: number
}

// 错误信息
export interface ErrorInfo {
  name: string
  message: string
  stack?: string
  code?: string | number
  statusCode?: number
  timestamp: number
  userAgent?: string
  url?: string
  userId?: string
  additional?: Record<string, any>
}