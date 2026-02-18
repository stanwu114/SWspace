import axios, { AxiosInstance, AxiosError } from 'axios'
import type { ApiResponse, PageResponse, ApiError } from '@/types/api'
import type { Project, Customer, Knowledge, AISession } from '@/types'

class ApiService {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: 'http://localhost:8080/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // 请求拦截器
    this.client.interceptors.request.use(
      (config) => {
        // 可以在这里添加认证 token
        return config
      },
      (error) => Promise.reject(error)
    )

    // 响应拦截器
    this.client.interceptors.response.use(
      (response) => response.data,
      (error: AxiosError<ApiError>) => {
        const apiError = error.response?.data || {
          code: error.response?.status || 500,
          message: error.message || '网络错误',
          timestamp: Date.now(),
        }
        return Promise.reject(apiError)
      }
    )
  }

  // 项目 API
  projects = {
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<Project>>>('/projects', { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<Project>>(`/projects/${id}`),
    
    create: (data: Partial<Project>) =>
      this.client.post<ApiResponse<Project>>('/projects', data),
    
    update: (id: string, data: Partial<Project>) =>
      this.client.put<ApiResponse<Project>>(`/projects/${id}`, data),
    
    updateStatus: (id: string, status: string, note?: string) =>
      this.client.patch<ApiResponse<Project>>(`/projects/${id}/status`, { status, note }),
    
    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/projects/${id}`),
  }

  // 客户 API
  customers = {
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<Customer>>>('/customers', { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<Customer>>(`/customers/${id}`),
    
    create: (data: Partial<Customer>) =>
      this.client.post<ApiResponse<Customer>>('/customers', data),
    
    update: (id: string, data: Partial<Customer>) =>
      this.client.put<ApiResponse<Customer>>(`/customers/${id}`, data),
    
    addInteraction: (customerId: string, data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>(`/customers/${customerId}/interactions`, data),
  }

  // 知识库 API
  knowledge = {
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<Knowledge>>>('/knowledge', { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<Knowledge>>(`/knowledge/${id}`),
    
    create: (data: Partial<Knowledge>) =>
      this.client.post<ApiResponse<Knowledge>>('/knowledge', data),
    
    search: (query: string, categories?: string[], limit?: number) =>
      this.client.post<ApiResponse<{ items: Knowledge[] }>>('/knowledge/semantic-search', {
        query,
        categories,
        limit,
      }),
  }

  // AI Agent API
  agent = {
    createSession: (agentType: string, context?: Record<string, unknown>) =>
      this.client.post<ApiResponse<AISession>>('/agent/sessions', { agentType, ...context }),
    
    getSessions: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<AISession>>>('/agent/sessions', { params }),
    
    getSession: (id: string) =>
      this.client.get<ApiResponse<AISession>>(`/agent/sessions/${id}`),
    
    getSessionMessages: (sessionId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/agent/sessions/${sessionId}/messages`),
    
    chat: (sessionId: string, content: string, context?: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/agent/chat', { sessionId, content, context }),
    
    archiveSession: (id: string) =>
      this.client.post<ApiResponse<void>>(`/agent/sessions/${id}/archive`),
    
    deleteSession: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/agent/sessions/${id}`),
    
    feedbackMessage: (messageId: string, feedback: string) =>
      this.client.post<ApiResponse<void>>(`/agent/messages/${messageId}/feedback`, { feedback }),
    
    analyzeBid: (documentId: string, focusAreas?: string[]) =>
      this.client.post<ApiResponse<unknown>>('/agent/intel/analyze-bid', {
        documentId,
        focusAreas,
      }),
    
    generateSolution: (projectId: string, templateType: string) =>
      this.client.post<ApiResponse<unknown>>('/agent/doc/generate-solution', {
        projectId,
        templateType,
      }),
    
    summarizeInteraction: (content: string, type: string, participants?: string[]) =>
      this.client.post<ApiResponse<unknown>>('/agent/crm/summarize-interaction', {
        content,
        type,
        participants,
      }),
  }

  // 文档 API
  documents = {
    upload: (file: File, type?: string, projectId?: string, customerId?: string) => {
      const formData = new FormData()
      formData.append('file', file)
      if (type) formData.append('type', type)
      if (projectId) formData.append('projectId', projectId)
      if (customerId) formData.append('customerId', customerId)
      return this.client.post<ApiResponse<unknown>>('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
    
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>('/documents', { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/documents/${id}`),
    
    getByProject: (projectId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/documents/project/${projectId}`),
    
    getByCustomer: (customerId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/documents/customer/${customerId}`),
    
    analyze: (id: string) =>
      this.client.post<ApiResponse<string>>(`/documents/${id}/analyze`),
    
    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/documents/${id}`),
  }

  // 提醒 API
  reminders = {
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>('/reminders', { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/reminders/${id}`),
    
    create: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/reminders', data),
    
    today: () =>
      this.client.get<ApiResponse<unknown[]>>('/reminders/today'),
    
    upcoming: (hours?: number, limit?: number) =>
      this.client.get<ApiResponse<unknown[]>>('/reminders/upcoming', { params: { hours, limit } }),
    
    getByProject: (projectId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/reminders/project/${projectId}`),
    
    getByCustomer: (customerId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/reminders/customer/${customerId}`),
    
    complete: (id: string) =>
      this.client.patch<ApiResponse<void>>(`/reminders/${id}/complete`),
    
    dismiss: (id: string) =>
      this.client.patch<ApiResponse<void>>(`/reminders/${id}/dismiss`),
    
    snooze: (id: string, minutes: number) =>
      this.client.patch<ApiResponse<void>>(`/reminders/${id}/snooze`, { minutes }),
    
    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/reminders/${id}`),
  }

  // 联系人 API
  contacts = {
    listByCustomer: (customerId: string, params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>(`/contacts/customer/${customerId}`, { params }),
    
    search: (keyword: string, params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>('/contacts/search', { params: { keyword, ...params } }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/contacts/${id}`),
    
    create: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/contacts', data),
    
    update: (id: string, data: Record<string, unknown>) =>
      this.client.put<ApiResponse<unknown>>(`/contacts/${id}`, data),
    
    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/contacts/${id}`),
    
    getKeyContacts: (customerId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/contacts/customer/${customerId}/key`),
  }

  // 交互记录 API
  interactions = {
    listByCustomer: (customerId: string, params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>(`/interactions/customer/${customerId}`, { params }),
    
    listByProject: (projectId: string, params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>(`/interactions/project/${projectId}`, { params }),
    
    get: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/interactions/${id}`),
    
    create: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/interactions', data),
    
    update: (id: string, data: Record<string, unknown>) =>
      this.client.put<ApiResponse<unknown>>(`/interactions/${id}`, data),
    
    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/interactions/${id}`),
    
    getRecent: (customerId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/interactions/customer/${customerId}/recent`),
    
    getPendingFollowUps: (deadline?: string) =>
      this.client.get<ApiResponse<unknown[]>>('/interactions/pending-followups', { params: { deadline } }),
    
    getTypeStats: (customerId: string) =>
      this.client.get<ApiResponse<Record<string, number>>>(`/interactions/customer/${customerId}/stats/types`),
    
    getSentimentStats: (customerId: string) =>
      this.client.get<ApiResponse<Record<string, number>>>(`/interactions/customer/${customerId}/stats/sentiment`),
  }

  // 招标监控 API
  bidding = {
    listItems: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>('/bidding/items', { params }),

    getItem: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/bidding/items/${id}`),

    createItem: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/bidding/items', data),

    markAsRead: (id: string) =>
      this.client.post<ApiResponse<void>>(`/bidding/items/${id}/read`),

    toggleStar: (id: string) =>
      this.client.post<ApiResponse<void>>(`/bidding/items/${id}/star`),

    getStatistics: () =>
      this.client.get<ApiResponse<Record<string, unknown>>>('/bidding/statistics'),

    getMatched: () =>
      this.client.get<ApiResponse<unknown[]>>('/bidding/matched'),

    getUpcoming: (days?: number) =>
      this.client.get<ApiResponse<unknown[]>>('/bidding/upcoming', { params: { days } }),

    listSources: () =>
      this.client.get<ApiResponse<unknown[]>>('/bidding/sources'),

    createSource: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/bidding/sources', data),

    crawlSource: (id: string) =>
      this.client.post<ApiResponse<unknown>>(`/bidding/sources/${id}/crawl`),

    getKeywords: () =>
      this.client.get<ApiResponse<string[]>>('/bidding/keywords'),

    updateKeywords: (keywords: string[]) =>
      this.client.put<ApiResponse<void>>('/bidding/keywords', { keywords }),

    init: () =>
      this.client.post<ApiResponse<void>>('/bidding/init'),
  }

  // 案例库 API
  cases = {
    list: (params?: Record<string, unknown>) =>
      this.client.get<ApiResponse<PageResponse<unknown>>>('/cases', { params }),

    get: (id: string) =>
      this.client.get<ApiResponse<unknown>>(`/cases/${id}`),

    create: (data: Record<string, unknown>) =>
      this.client.post<ApiResponse<unknown>>('/cases', data),

    update: (id: string, data: Record<string, unknown>) =>
      this.client.put<ApiResponse<unknown>>(`/cases/${id}`, data),

    delete: (id: string) =>
      this.client.delete<ApiResponse<void>>(`/cases/${id}`),

    publish: (id: string) =>
      this.client.post<ApiResponse<unknown>>(`/cases/${id}/publish`),

    archive: (id: string) =>
      this.client.post<ApiResponse<unknown>>(`/cases/${id}/archive`),

    toggleFeatured: (id: string) =>
      this.client.post<ApiResponse<unknown>>(`/cases/${id}/toggle-featured`),

    getFeatured: (limit?: number) =>
      this.client.get<ApiResponse<unknown[]>>('/cases/featured', { params: { limit } }),

    getStatistics: () =>
      this.client.get<ApiResponse<Record<string, unknown>>>('/cases/statistics'),

    getFilters: () =>
      this.client.get<ApiResponse<Record<string, string[]>>>('/cases/filters'),

    getByProject: (projectId: string) =>
      this.client.get<ApiResponse<unknown[]>>(`/cases/by-project/${projectId}`),

    createFromProject: (projectId: string, author?: string) =>
      this.client.post<ApiResponse<unknown>>(`/cases/from-project/${projectId}`, null, { params: { author } }),
  }

  // 系统管理 API
  system = {
    health: () =>
      this.client.get<ApiResponse<unknown>>('/system/health'),

    info: () =>
      this.client.get<ApiResponse<unknown>>('/system/info'),

    backup: () =>
      this.client.post<ApiResponse<unknown>>('/system/backup'),

    listBackups: () =>
      this.client.get<ApiResponse<unknown[]>>('/system/backups'),

    cleanupBackups: () =>
      this.client.post<ApiResponse<unknown>>('/system/backups/cleanup'),

    // LLM 配置相关
    listModels: (endpoint: string, apiKey: string) =>
      this.client.post<ApiResponse<{ id: string; name: string; ownedBy: string }[]>>('/system/llm/models', { endpoint, apiKey }),

    testLLM: (endpoint: string, apiKey: string) =>
      this.client.post<ApiResponse<{ success: boolean; message: string; latencyMs: number }>>('/system/llm/test', { endpoint, apiKey }),

    // 配置保存（保存后自动重启）
    saveAIConfig: (endpoint: string, apiKey: string, model: string) =>
      this.client.post<ApiResponse<string>>('/system/config/ai', { endpoint, apiKey, model }),

    saveNotificationConfig: (feishuWebhook?: string, telegramToken?: string, telegramChatId?: string) =>
      this.client.post<ApiResponse<string>>('/system/config/notification', { feishuWebhook, telegramToken, telegramChatId }),

    // 手动重启
    restart: () =>
      this.client.post<ApiResponse<string>>('/system/restart'),
  }

  // 健康检查 API (兼容旧路径)
  health = {
    check: () =>
      this.client.get<ApiResponse<{ status: string }>>('/system/health'),

    info: () =>
      this.client.get<ApiResponse<Record<string, unknown>>>('/system/info'),
  }
}

export const api = new ApiService()
export default api
