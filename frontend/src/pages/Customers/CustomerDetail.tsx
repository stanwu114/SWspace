import { useParams, Link } from 'react-router-dom'
import { useState, useEffect } from 'react'
import {
  ArrowLeft, Building2, MapPin, Globe, Star, Phone, Mail, MessageCircle,
  Calendar, Clock, Tag, Users, Briefcase, TrendingUp, ChevronRight,
  AlertCircle, Loader2
} from 'lucide-react'
import { api } from '@/services/api'
import type { Customer, Contact, Interaction, Project } from '@/types'

const typeLabels: Record<string, string> = { government: '政府', enterprise: '企业', other: '其他' }
const levelLabels: Record<string, string> = { key: '重点客户', normal: '普通客户', potential: '潜在客户' }
const levelColors: Record<string, string> = {
  key: 'bg-red-50 text-red-600 border-red-200',
  normal: 'bg-blue-50 text-blue-600 border-blue-200',
  potential: 'bg-gray-50 text-gray-600 border-gray-200',
}
const interactionTypeLabels: Record<string, string> = {
  call: '电话', meeting: '会议', email: '邮件', wechat: '微信', visit: '拜访', other: '其他'
}
const sentimentColors: Record<string, string> = {
  positive: 'text-emerald-500', neutral: 'text-gray-400', negative: 'text-red-400'
}
const roleLabels: Record<string, string> = {
  decision_maker: '决策者', influencer: '影响者', user: '使用者', champion: '支持者'
}

function formatDate(date?: string) {
  if (!date) return '-'
  try { return new Date(date).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) }
  catch { return date }
}

function formatDateTime(date?: string) {
  if (!date) return '-'
  try { return new Date(date).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }
  catch { return date }
}

function ScoreBar({ score }: { score: number }) {
  const color = score >= 80 ? 'bg-emerald-500' : score >= 60 ? 'bg-blue-500' : score >= 40 ? 'bg-yellow-500' : 'bg-red-500'
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all ${color}`} style={{ width: `${Math.min(100, score)}%` }} />
      </div>
      <span className="text-sm font-medium text-gray-700 w-8 text-right">{score}</span>
    </div>
  )
}

export default function CustomerDetail() {
  const { id } = useParams<{ id: string }>()
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [contacts, setContacts] = useState<Contact[]>([])
  const [interactions, setInteractions] = useState<Interaction[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'contacts' | 'interactions' | 'projects'>('contacts')

  useEffect(() => {
    if (!id) return
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [custRes, contactRes, interactionRes, projectRes] = await Promise.allSettled([
          api.customers.get(id),
          api.contacts.listByCustomer(id, { size: 50 }),
          api.interactions.listByCustomer(id, { size: 20, sort: 'interactionAt,desc' }),
          api.projects.list({ customerId: id, size: 20 }),
        ])

        if (custRes.status === 'fulfilled') {
          const r = custRes.value as any
          if (r?.code === 200) setCustomer(r.data)
          else setError('加载客户信息失败')
        } else {
          setError('加载客户信息失败')
        }

        if (contactRes.status === 'fulfilled') {
          const r = contactRes.value as any
          setContacts(r?.data?.items || r?.data || [])
        }
        if (interactionRes.status === 'fulfilled') {
          const r = interactionRes.value as any
          setInteractions(r?.data?.items || r?.data || [])
        }
        if (projectRes.status === 'fulfilled') {
          const r = projectRes.value as any
          setProjects(r?.data?.items || r?.data || [])
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

  if (error || !customer) {
    return (
      <div className="p-8 max-w-4xl mx-auto">
        <Link to="/customers" className="inline-flex items-center text-gray-500 hover:text-gray-700 mb-6 text-sm">
          <ArrowLeft className="w-4 h-4 mr-1.5" /> 返回客户列表
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-2 text-sm text-red-600">
          <AlertCircle className="w-4 h-4" />
          <span>{error || '客户不存在'}</span>
        </div>
      </div>
    )
  }

  const tabs = [
    { key: 'contacts' as const, label: '联系人', count: contacts.length, icon: Users },
    { key: 'interactions' as const, label: '交互记录', count: interactions.length, icon: Clock },
    { key: 'projects' as const, label: '关联项目', count: projects.length, icon: Briefcase },
  ]

  return (
    <div className="h-full overflow-auto">
      <div className="p-6 max-w-5xl mx-auto space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between">
          <Link to="/customers" className="inline-flex items-center text-gray-500 hover:text-gray-700 text-sm gap-1.5">
            <ArrowLeft className="w-4 h-4" /> 返回客户列表
          </Link>
        </div>

        {/* Basic Info Card */}
        <div className="bg-white rounded-lg border border-gray-200 p-5">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-xl font-semibold text-gray-900">{customer.name}</h1>
                {customer.shortName && <span className="text-sm text-gray-400">({customer.shortName})</span>}
                <span className={`px-2 py-0.5 text-xs rounded border ${levelColors[customer.level] || levelColors.normal}`}>
                  {levelLabels[customer.level] || customer.level}
                </span>
              </div>
              <div className="flex items-center gap-4 text-sm text-gray-500 mt-2">
                {customer.type && (
                  <span className="flex items-center gap-1"><Building2 className="w-3.5 h-3.5" />{typeLabels[customer.type] || customer.type}</span>
                )}
                {customer.industry && (
                  <span className="flex items-center gap-1"><Tag className="w-3.5 h-3.5" />{customer.industry}</span>
                )}
                {customer.region && (
                  <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" />{customer.region}</span>
                )}
                {customer.website && (
                  <a href={customer.website} target="_blank" rel="noopener noreferrer"
                    className="flex items-center gap-1 text-brand-500 hover:underline">
                    <Globe className="w-3.5 h-3.5" />{customer.website}
                  </a>
                )}
              </div>
            </div>
          </div>

          {/* Info grid */}
          <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-100">
            <div>
              <div className="text-xs text-gray-400 mb-1.5">关系分数</div>
              <ScoreBar score={customer.relationshipScore || 0} />
            </div>
            <div>
              <div className="text-xs text-gray-400 mb-1">最后联系</div>
              <div className="text-sm text-gray-700 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-gray-400" />
                {formatDate(customer.lastContactAt)}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-400 mb-1">下次跟进</div>
              <div className="text-sm text-gray-700 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                {formatDate(customer.nextFollowUpAt)}
              </div>
            </div>
          </div>

          {/* Tags */}
          {customer.tags && customer.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-4 pt-3 border-t border-gray-100">
              {customer.tags.map(tag => (
                <span key={tag} className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">{tag}</span>
              ))}
            </div>
          )}

          {/* Notes */}
          {customer.notes && (
            <div className="mt-4 pt-3 border-t border-gray-100">
              <div className="text-xs text-gray-400 mb-1">备注</div>
              <p className="text-sm text-gray-600 whitespace-pre-wrap">{customer.notes}</p>
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
                  <span className={`ml-1 px-1.5 py-0.5 rounded-full text-xs ${
                    activeTab === tab.key ? 'bg-brand-50 text-brand-600' : 'bg-gray-100 text-gray-500'
                  }`}>{tab.count}</span>
                </button>
              )
            })}
          </div>

          <div className="p-4">
            {/* Contacts Tab */}
            {activeTab === 'contacts' && (
              contacts.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">暂无联系人</p>
              ) : (
                <div className="space-y-2">
                  {contacts.map((c: any) => (
                    <div key={c.id} className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-brand-50 text-brand-600 rounded-full flex items-center justify-center text-sm font-medium">
                          {(c.name || '?')[0]}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium text-gray-800">{c.name}</span>
                            {c.title && <span className="text-xs text-gray-400">{c.title}</span>}
                            {c.role && (
                              <span className="text-xs px-1.5 py-0.5 bg-purple-50 text-purple-600 rounded">
                                {roleLabels[c.role] || c.role}
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-3 mt-0.5 text-xs text-gray-400">
                            {c.department && <span>{c.department}</span>}
                            {c.mobile && <span className="flex items-center gap-0.5"><Phone className="w-3 h-3" />{c.mobile}</span>}
                            {c.email && <span className="flex items-center gap-0.5"><Mail className="w-3 h-3" />{c.email}</span>}
                            {c.wechat && <span className="flex items-center gap-0.5"><MessageCircle className="w-3 h-3" />{c.wechat}</span>}
                          </div>
                        </div>
                      </div>
                      {c.importance === 'key' && <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />}
                    </div>
                  ))}
                </div>
              )
            )}

            {/* Interactions Tab */}
            {activeTab === 'interactions' && (
              interactions.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">暂无交互记录</p>
              ) : (
                <div className="space-y-3">
                  {interactions.map((i: any) => (
                    <div key={i.id} className="relative pl-6 pb-3 border-l-2 border-gray-200 last:border-transparent">
                      <div className="absolute left-[-5px] top-1 w-2 h-2 rounded-full bg-brand-500" />
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-xs px-1.5 py-0.5 bg-gray-100 rounded text-gray-600">
                          {interactionTypeLabels[i.type] || i.type}
                        </span>
                        <span className="text-xs text-gray-400">{formatDateTime(i.interactionAt)}</span>
                        {i.sentiment && (
                          <span className={`text-xs ${sentimentColors[i.sentiment] || 'text-gray-400'}`}>
                            {i.sentiment === 'positive' ? '积极' : i.sentiment === 'negative' ? '消极' : '中性'}
                          </span>
                        )}
                      </div>
                      {i.subject && <div className="text-sm font-medium text-gray-800 mb-0.5">{i.subject}</div>}
                      {i.summary && <p className="text-sm text-gray-600">{i.summary}</p>}
                      {i.keyPoints && i.keyPoints.length > 0 && (
                        <ul className="mt-1 space-y-0.5">
                          {i.keyPoints.map((kp: string, idx: number) => (
                            <li key={idx} className="text-xs text-gray-500 flex items-start gap-1">
                              <span className="text-brand-400 mt-0.5">-</span> {kp}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  ))}
                </div>
              )
            )}

            {/* Projects Tab */}
            {activeTab === 'projects' && (
              projects.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">暂无关联项目</p>
              ) : (
                <div className="space-y-2">
                  {projects.map((p: any) => (
                    <Link
                      key={p.id}
                      to={`/projects/${p.id}`}
                      className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors group"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-gray-800 group-hover:text-brand-600">{p.name}</span>
                          <span className={`text-xs px-1.5 py-0.5 rounded ${
                            p.status === 'completed' ? 'bg-emerald-50 text-emerald-600' :
                            p.status === 'execution' ? 'bg-blue-50 text-blue-600' :
                            p.status === 'opportunity' ? 'bg-yellow-50 text-yellow-600' :
                            p.status === 'cancelled' ? 'bg-red-50 text-red-600' :
                            'bg-gray-50 text-gray-600'
                          }`}>
                            {p.status === 'lead' ? '线索' : p.status === 'opportunity' ? '机会' :
                             p.status === 'execution' ? '执行中' : p.status === 'completed' ? '已完成' :
                             p.status === 'cancelled' ? '已取消' : p.status}
                          </span>
                        </div>
                        <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                          {p.estimatedValue && (
                            <span className="flex items-center gap-0.5">
                              <TrendingUp className="w-3 h-3" /> 预估 {(p.estimatedValue / 10000).toFixed(0)} 万
                            </span>
                          )}
                          {p.bidDeadline && <span>截止 {formatDate(p.bidDeadline)}</span>}
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-gray-300 group-hover:text-brand-500" />
                    </Link>
                  ))}
                </div>
              )
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
