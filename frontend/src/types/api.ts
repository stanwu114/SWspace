// API 响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data?: T
  timestamp: number
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ApiError {
  code: number
  message: string
  errors?: FieldError[]
  timestamp: number
}

export interface FieldError {
  field: string
  message: string
}

// 请求参数类型
export interface PageParams {
  page?: number
  pageSize?: number
  sort?: string
  order?: 'asc' | 'desc'
}

export interface ProjectListParams extends PageParams {
  status?: string
  customerId?: string
  keyword?: string
}

export interface CustomerListParams extends PageParams {
  type?: string
  level?: string
  region?: string
  keyword?: string
  needFollowUp?: boolean
}

export interface KnowledgeSearchParams {
  query: string
  categories?: string[]
  limit?: number
  minScore?: number
}
