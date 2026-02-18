import { useParams, Link } from 'react-router-dom'
import { useState, useEffect } from 'react'
import {
  ArrowLeft, Building2, Calendar, Clock, Tag, FileText, TrendingUp,
  AlertCircle, Loader2, AlertTriangle, Shield, ChevronRight,
  Download, Eye, MessageSquare
} from 'lucide-react'
import { api } from '@/services/api'
import type { Project, Document } from '@/types'

const statusLabels: Record<string, string> = {
  lead: '线索', opportunity: '机会', execution: '执行中', completed: '已完成', cancelled: '已取消'
}
const statusColors: Record<string, string> = {
  lead: 'bg-gray-100 text-gray-600',
  opportunity: 'bg-yellow-50 text-yellow-600',
  execution: 'bg-blue-50 text-blue-600',
  completed: 'bg-emerald-50 text-emerald-600',
  cancelled: 'bg-red-50 text-red-600',
}
const riskLabels: Record<string, string> = { low: '低风险', medium: '中等风险', high: '高风险' }
const docTypeLabels: Record<string, string> = {
  bid: '标书', solution: '方案', report: '报告', contract: '合同', policy: '政策', other: '其他'
}

function formatDate(date?: string) {
  if (!date) return '-'
  try { return new Date(date).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) }
  catch { return date }
}

function formatFileSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatMoney(value?: number) {
  if (!value) return '-'
  if (value >= 10000) return `${(value / 10000).toFixed(value % 10000 === 0 ? 0 : 1)} 万`
  return `${value}`
}

export default function ProjectDetail() {
  const { id } = useParams<{ id: string }>()
  const [project, setProject] = useState<Project | null>(null)
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'overview' | 'documents' | 'ai'>('overview')

  useEffect(() => {
    if (!id) return
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [projRes, docRes] = await Promise.allSettled([
          api.projects.get(id),
          api.documents.getByProject(id),
        ])

        if (projRes.status === 'fulfilled') {
          const r = projRes.value as any
          if (r?.code === 200) setProject(r.data)
          else setError('加载项目信息失败')
        } else {
          setError('加载项目信息失败')
        }

        if (docRes.status === 'fulfilled') {
          const r = docRes.value as any
          setDocuments(r?.data?.items || r?.data || [])
        }
      } catch (e: any) {
        setError(e?.message || '加载数据失败')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-6 h-6 text-brand-500 animate-spin" />
      </div>
    )
  }

  if (error || !project) {
    return (
      <div className="p-8 max-w-4xl mx-auto">
        <Link to="/projects" className="inline-flex items-center text-gray-500 hover:text-gray-700 mb-6 text-sm">
          <ArrowLeft className="w-4 h-4 mr-1.5" /> 返回项目列表
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-2 text-sm text-red-600">
          <AlertCircle className="w-4 h-4" />
          <span>{error || '项目不存在'}</span>
        </div>
      </div>
    )
  }

  const requirements = project.requirements as any
  const riskAssessment = project.riskAssessment as any

  const tabs = [
    { key: 'overview' as const, label: '概览', icon: Eye },
    { key: 'documents' as const, label: '文档', count: documents.length, icon: FileText },
    { key: 'ai' as const, label: 'AI 协作', icon: MessageSquare },
  ]

  return (
    <div className="h-full overflow-auto">
      <div className="p-6 max-w-5xl mx-auto space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between">
          <Link to="/projects" className="inline-flex items-center text-gray-500 hover:text-gray-700 text-sm gap-1.5">
            <ArrowLeft className="w-4 h-4" /> 返回项目列表
          </Link>
        </div>

        {/* Title Card */}
        <div className="bg-white rounded-lg border border-gray-200 p-5">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-xl font-semibold text-gray-900">{project.name}</h1>
                {project.code && <span className="text-sm text-gray-400">#{project.code}</span>}
                <span className={`px-2 py-0.5 text-xs rounded ${statusColors[project.status] || 'bg-gray-100 text-gray-600'}`}>
                  {statusLabels[project.status] || project.status}
                </span>
              </div>
              {project.stage && (
                <div className="text-sm text-gray-500 mt-1">阶段: {project.stage}</div>
              )}
            </div>
          </div>

          {/* Key metrics */}
          <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-100">
            <div>
              <div className="text-xs text-gray-400 mb-1">预估金额</div>
              <div className="text-sm font-medium text-gray-800 flex items-center gap-1">
                <TrendingUp className="w-3.5 h-3.5 text-emerald-500" />
                ¥{formatMoney(project.estimatedValue)}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-400 mb-1">合同金额</div>
              <div className="text-sm font-medium text-gray-800">
                {project.contractValue ? `¥${formatMoney(project.contractValue)}` : '-'}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-400 mb-1">投标截止</div>
              <div className="text-sm text-gray-700 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-gray-400" />
                {formatDate(project.bidDeadline)}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-400 mb-1">合同日期</div>
              <div className="text-sm text-gray-700 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                {formatDate(project.contractDate)}
              </div>
            </div>
          </div>

          {/* Customer link */}
          {project.customerId && (
            <div className="mt-4 pt-3 border-t border-gray-100">
              <div className="text-xs text-gray-400 mb-1">关联客户</div>
              <Link to={`/customers/${project.customerId}`}
                className="inline-flex items-center gap-1.5 text-sm text-brand-500 hover:underline">
                <Building2 className="w-3.5 h-3.5" />
                {(project.customer as any)?.name || '查看客户'}
                <ChevronRight className="w-3 h-3" />
              </Link>
            </div>
          )}

          {/* Tags */}
          {project.tags && project.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-4 pt-3 border-t border-gray-100">
              {project.tags.map(tag => (
                <span key={tag} className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs flex items-center gap-0.5">
                  <Tag className="w-3 h-3" />{tag}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Tabs */}
        <div className="bg-white rounded-lg border border-gray-200">
          <div className="flex border-b border-gray-200">
            {tabs.map(tab => {
              const Icon = tab.icon
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`flex items-center gap-1.5 px-4 py-3 text-sm border-b-2 transition-colors ${
                    activeTab === tab.key
                      ? 'border-brand-500 text-brand-600 font-medium'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  {tab.label}
                  {tab.count !== undefined && (
                    <span className={`ml-1 px-1.5 py-0.5 rounded-full text-xs ${
                      activeTab === tab.key ? 'bg-brand-50 text-brand-600' : 'bg-gray-100 text-gray-500'
                    }`}>{tab.count}</span>
                  )}
                </button>
              )
            })}
          </div>

          <div className="p-4">
            {/* Overview Tab */}
            {activeTab === 'overview' && (
              <div className="space-y-5">
                {/* Description */}
                {project.description && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-800 mb-2">项目描述</h3>
                    <p className="text-sm text-gray-600 whitespace-pre-wrap leading-relaxed">{project.description}</p>
                  </div>
                )}

                {/* Requirements */}
                {requirements && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-800 mb-2">需求分析</h3>
                    {requirements.mainRequirements && requirements.mainRequirements.length > 0 && (
                      <div className="mb-3">
                        <div className="text-xs text-gray-400 mb-1.5">主要需求</div>
                        <ul className="space-y-1">
                          {requirements.mainRequirements.map((req: string, i: number) => (
                            <li key={i} className="text-sm text-gray-600 flex items-start gap-2">
                              <span className="w-1.5 h-1.5 bg-brand-400 rounded-full mt-1.5 shrink-0" />
                              {req}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                    {requirements.deliverables && requirements.deliverables.length > 0 && (
                      <div>
                        <div className="text-xs text-gray-400 mb-1.5">交付物</div>
                        <div className="flex flex-wrap gap-1.5">
                          {requirements.deliverables.map((d: string, i: number) => (
                            <span key={i} className="px-2 py-0.5 bg-blue-50 text-blue-600 rounded text-xs">{d}</span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Tech Stack */}
                {project.techStack && project.techStack.length > 0 && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-800 mb-2">技术栈</h3>
                    <div className="flex flex-wrap gap-1.5">
                      {project.techStack.map(tech => (
                        <span key={tech} className="px-2.5 py-1 bg-purple-50 text-purple-600 rounded-md text-xs font-medium">{tech}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Risk Assessment */}
                {riskAssessment && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-800 mb-2 flex items-center gap-1.5">
                      <Shield className="w-4 h-4" /> 风险评估
                    </h3>
                    {riskAssessment.level && (
                      <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium mb-2 ${
                        riskAssessment.level === 'high' ? 'bg-red-50 text-red-600' :
                        riskAssessment.level === 'medium' ? 'bg-yellow-50 text-yellow-600' :
                        'bg-emerald-50 text-emerald-600'
                      }`}>
                        <AlertTriangle className="w-3 h-3" />
                        {riskLabels[riskAssessment.level] || riskAssessment.level}
                      </div>
                    )}
                    {riskAssessment.factors && riskAssessment.factors.length > 0 && (
                      <div className="mb-2">
                        <div className="text-xs text-gray-400 mb-1">风险因素</div>
                        <ul className="space-y-0.5">
                          {riskAssessment.factors.map((f: string, i: number) => (
                            <li key={i} className="text-sm text-gray-600 flex items-start gap-1.5">
                              <span className="text-red-400 mt-0.5">-</span> {f}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                    {riskAssessment.mitigations && riskAssessment.mitigations.length > 0 && (
                      <div>
                        <div className="text-xs text-gray-400 mb-1">缓解措施</div>
                        <ul className="space-y-0.5">
                          {riskAssessment.mitigations.map((m: string, i: number) => (
                            <li key={i} className="text-sm text-gray-600 flex items-start gap-1.5">
                              <span className="text-emerald-400 mt-0.5">-</span> {m}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}

                {/* Empty state */}
                {!project.description && !requirements && (!project.techStack || project.techStack.length === 0) && !riskAssessment && (
                  <p className="text-sm text-gray-400 text-center py-8">暂无详细信息</p>
                )}
              </div>
            )}

            {/* Documents Tab */}
            {activeTab === 'documents' && (
              documents.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">暂无项目文档</p>
              ) : (
                <div className="space-y-2">
                  {documents.map((doc: any) => (
                    <div key={doc.id} className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-blue-50 text-blue-500 rounded-lg flex items-center justify-center">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="text-sm font-medium text-gray-800">{doc.name || doc.originalName}</div>
                          <div className="flex items-center gap-3 text-xs text-gray-400 mt-0.5">
                            <span className="px-1.5 py-0.5 bg-gray-100 rounded">{docTypeLabels[doc.type] || doc.type}</span>
                            <span>{formatFileSize(doc.fileSize)}</span>
                            <span>{formatDate(doc.createdAt)}</span>
                          </div>
                          {doc.aiSummary && (
                            <p className="text-xs text-gray-500 mt-1 line-clamp-1">{doc.aiSummary}</p>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1">
                        <button className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded">
                          <Download className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}

            {/* AI Collaboration Tab */}
            {activeTab === 'ai' && (
              <div className="text-center py-8">
                <MessageSquare className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-400">与 AI 员工协作处理此项目</p>
                <Link to="/chat" className="inline-flex items-center gap-1 mt-3 text-sm text-brand-500 hover:underline">
                  前往 AI 对话 <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
