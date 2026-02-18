// 项目相关类型
export interface Project {
  id: string
  name: string
  code?: string
  customerId?: string
  customer?: Customer
  status: ProjectStatus
  stage?: string
  estimatedValue?: number
  contractValue?: number
  bidDeadline?: string
  contractDate?: string
  description?: string
  requirements?: ProjectRequirements
  techStack?: string[]
  riskAssessment?: RiskAssessment
  tags: string[]
  createdAt: string
  updatedAt: string
  createdBy?: string
}

export type ProjectStatus = 'lead' | 'opportunity' | 'execution' | 'completed' | 'cancelled'

export interface ProjectRequirements {
  mainRequirements: string[]
  techStack?: string[]
  deliverables?: string[]
}

export interface RiskAssessment {
  level: 'low' | 'medium' | 'high'
  factors: string[]
  mitigations: string[]
}

// 客户相关类型
export interface Customer {
  id: string
  name: string
  shortName?: string
  type: CustomerType
  industry?: string
  region?: string
  address?: string
  level: CustomerLevel
  orgStructure?: OrgStructure
  relationshipScore: number
  tags: string[]
  notes?: string
  website?: string
  lastContactAt?: string
  nextFollowUpAt?: string
  createdAt: string
  updatedAt: string
}

export type CustomerType = 'government' | 'enterprise' | 'other'
export type CustomerLevel = 'key' | 'normal' | 'potential'

export interface OrgStructure {
  departments: Department[]
  keyPeople: string[]
}

export interface Department {
  name: string
  head?: string
  contacts: string[]
}

// 联系人相关类型
export interface Contact {
  id: string
  customerId: string
  name: string
  title?: string
  department?: string
  role?: ContactRole
  importance: ContactImportance
  phone?: string
  mobile?: string
  email?: string
  wechat?: string
  birthday?: string
  preferences?: Record<string, string>
  notes?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export type ContactRole = 'decision_maker' | 'influencer' | 'user' | 'champion'
export type ContactImportance = 'key' | 'normal' | 'low'

// 交互记录相关类型
export interface Interaction {
  id: string
  customerId: string
  projectId?: string
  contactIds: string[]
  type: InteractionType
  subject?: string
  content?: string
  summary?: string
  keyPoints?: string[]
  sentiment?: Sentiment
  nextActions?: NextAction[]
  nextActionAt?: string
  interactionAt: string
  duration?: number
  location?: string
  createdAt: string
}

export type InteractionType = 'call' | 'meeting' | 'email' | 'wechat' | 'visit' | 'other'
export type Sentiment = 'positive' | 'neutral' | 'negative'

export interface NextAction {
  action: string
  deadline?: string
  assignee?: string
}

// 文档相关类型
export interface Document {
  id: string
  projectId?: string
  customerId?: string
  name: string
  originalName?: string
  type: DocumentType
  filePath: string
  fileSize?: number
  fileExt?: string
  mimeType?: string
  contentText?: string
  aiAnalysis?: Record<string, unknown>
  aiSummary?: string
  version: number
  isLatest: boolean
  parentId?: string
  tags: string[]
  createdAt: string
  updatedAt: string
}

export type DocumentType = 'bid' | 'solution' | 'report' | 'contract' | 'policy' | 'other'

// 知识库相关类型
export interface Knowledge {
  id: string
  title: string
  content: string
  summary?: string
  category: KnowledgeCategory
  subcategory?: string
  tags: string[]
  keywords: string[]
  sourceType?: KnowledgeSourceType
  sourceId?: string
  sourceName?: string
  author?: string
  effectiveDate?: string
  expireDate?: string
  viewCount: number
  useCount: number
  rating?: number
  isVerified: boolean
  isFeatured: boolean
  createdAt: string
  updatedAt: string
}

export type KnowledgeCategory = 'industry' | 'solution' | 'case' | 'sales' | 'lesson' | 'template' | 'other'
export type KnowledgeSourceType = 'case' | 'experience' | 'document' | 'manual' | 'import'

// AI 会话相关类型
export interface AISession {
  id: string
  agentType: AgentType
  title?: string
  projectId?: string
  customerId?: string
  documentId?: string
  context?: Record<string, unknown>
  summary?: string
  messageCount: number
  status: SessionStatus
  lastMessageAt?: string
  createdAt: string
  updatedAt: string
}

export type AgentType = 'intel' | 'doc' | 'crm' | 'knowledge'
export type SessionStatus = 'active' | 'archived' | 'deleted'

export interface AIMessage {
  id: string
  sessionId: string
  role: MessageRole
  content: string
  toolName?: string
  toolInput?: Record<string, unknown>
  toolOutput?: Record<string, unknown>
  model?: string
  tokensInput?: number
  tokensOutput?: number
  latencyMs?: number
  feedback?: MessageFeedback
  createdAt: string
}

export type MessageRole = 'user' | 'assistant' | 'system' | 'tool'
export type MessageFeedback = 'good' | 'bad' | 'none'

// 提醒相关类型
export interface Reminder {
  id: string
  title: string
  description?: string
  type: ReminderType
  projectId?: string
  customerId?: string
  interactionId?: string
  remindAt: string
  repeatType: RepeatType
  repeatConfig?: Record<string, unknown>
  priority: ReminderPriority
  status: ReminderStatus
  snoozedUntil?: string
  notificationChannels: NotificationChannel[]
  createdAt: string
  updatedAt: string
}

export type ReminderType = 'follow_up' | 'deadline' | 'payment' | 'meeting' | 'custom'
export type RepeatType = 'none' | 'daily' | 'weekly' | 'monthly' | 'yearly'
export type ReminderPriority = 'high' | 'normal' | 'low'
export type ReminderStatus = 'pending' | 'done' | 'dismissed' | 'snoozed'
export type NotificationChannel = 'desktop' | 'feishu' | 'telegram'

// WebSocket 消息类型
export interface ChatResponse {
  type: ChatResponseType
  content: string | null
  messageId: string | null
  timestamp: number
}

export type ChatResponseType = 
  | 'USER_MESSAGE_RECEIVED'
  | 'AI_GENERATING'
  | 'AI_CHUNK'
  | 'AI_COMPLETE'
  | 'ERROR'

// Agent 配置
export interface AgentConfig {
  id: AgentType
  name: string
  icon: string
  description: string
  color: string
}

export const AGENT_CONFIGS: AgentConfig[] = [
  {
    id: 'intel',
    name: '情报分析师',
    icon: '🔍',
    description: '分析招标文件、市场情报，提供竞争分析建议',
    color: 'blue',
  },
  {
    id: 'doc',
    name: '文档写手',
    icon: '📝',
    description: '撰写技术方案、投标文件、项目报告',
    color: 'green',
  },
  {
    id: 'crm',
    name: '客户助理',
    icon: '🤝',
    description: '管理客户关系、记录沟通、设置跟进提醒',
    color: 'purple',
  },
  {
    id: 'knowledge',
    name: '知识管家',
    icon: '📚',
    description: '搜索知识库、提取经验、推荐相关资料',
    color: 'orange',
  },
]
