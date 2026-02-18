import { useState, useEffect } from 'react'
import { FileText, Plus, Search, BookOpen, Star, Layers, X, RefreshCw, AlertCircle, Eye, Award } from 'lucide-react'
import api from '@/services/api'

interface CaseItem {
  id: string
  title: string
  projectName?: string
  customerName?: string
  industry?: string
  region?: string
  contractValue?: number
  status?: string
  isFeatured?: boolean
  author?: string
  summary?: string
  tags?: string[]
  viewCount?: number
  referenceCount?: number
  createdAt?: string
  updatedAt?: string
}

interface CaseStats {
  total?: number
  published?: number
  draft?: number
  industries?: number
  [key: string]: unknown
}

export default function Cases() {
  const [filter, setFilter] = useState('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cases, setCases] = useState<CaseItem[]>([])
  const [stats, setStats] = useState<CaseStats>({})
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ title: '', industry: '', summary: '', tags: '' })

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const [casesRes, statsRes] = await Promise.all([
        api.cases.list({ pageSize: 50 }) as any,
        api.cases.getStatistics() as any,
      ])
      if (casesRes?.code === 200) setCases(casesRes.data?.items || [])
      if (statsRes?.code === 200) setStats(statsRes.data || {})
    } catch (e: any) {
      setError(e?.message || '加载案例失败')
      setCases([])
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async () => {
    if (!form.title.trim()) return
    try {
      setCreating(true)
      setError(null)
      const r = await api.cases.create({
        title: form.title,
        industry: form.industry || undefined,
        summary: form.summary || undefined,
        tags: form.tags ? form.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
      }) as any
      if (r?.code === 200) {
        setShowCreate(false)
        setForm({ title: '', industry: '', summary: '', tags: '' })
        load()
      } else {
        setError(r?.message || '创建案例失败')
      }
    } catch (e: any) {
      setError(e?.message || '创建案例失败')
    } finally {
      setCreating(false)
    }
  }

  const handleToggleFeatured = async (id: string) => {
    try {
      await api.cases.toggleFeatured(id)
      setCases(prev => prev.map(c => c.id === id ? { ...c, isFeatured: !c.isFeatured } : c))
    } catch {}
  }

  const filters = [
    { key: 'all', label: '全部' },
    { key: 'published', label: '已发布' },
    { key: 'draft', label: '草稿' },
    { key: 'featured', label: '精选' },
  ]

  const statItems = [
    { label: '总案例', value: stats.total ?? cases.length, icon: FileText },
    { label: '已发布', value: stats.published ?? 0, icon: BookOpen },
    { label: '草稿', value: stats.draft ?? 0, icon: Star },
    { label: '行业覆盖', value: stats.industries ?? 0, icon: Layers },
  ]

  const filteredCases = cases.filter(c => {
    if (filter === 'published' && c.status !== 'PUBLISHED') return false
    if (filter === 'draft' && c.status !== 'DRAFT') return false
    if (filter === 'featured' && !c.isFeatured) return false
    if (searchTerm && !c.title?.toLowerCase().includes(searchTerm.toLowerCase())) return false
    return true
  })

  const fmtDate = (d?: string) => {
    if (!d) return '-'
    const days = Math.floor((Date.now() - new Date(d).getTime()) / 86400000)
    return days === 0 ? '今天' : days === 1 ? '昨天' : days < 7 ? `${days}天前` : new Date(d).toLocaleDateString('zh-CN')
  }

  const fmtValue = (v?: number) => {
    if (!v) return ''
    return v >= 10000 ? `¥${(v / 10000).toFixed(0)}万` : `¥${v.toLocaleString()}`
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">案例库</h1>
        <div className="flex items-center gap-2">
          <button onClick={load} disabled={loading}
            className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50">
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} /> 刷新
          </button>
          <button onClick={() => setShowCreate(true)}
            className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors flex items-center gap-1.5">
            <Plus className="w-3.5 h-3.5" /> 新建案例
          </button>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-600">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} className="ml-auto text-xs text-red-400 hover:text-red-600">关闭</button>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-4 gap-3 mb-5">
        {statItems.map((s, i) => (
          <div key={i} className="bg-white border border-gray-200 rounded-lg px-4 py-3.5">
            <div className="flex items-center gap-2 mb-2">
              <s.icon className="w-4 h-4 text-brand-500" />
              <span className="text-xs text-gray-400">{s.label}</span>
            </div>
            <div className="text-xl font-semibold text-gray-900">{s.value}</div>
          </div>
        ))}
      </div>

      {/* Filter + Search */}
      <div className="flex items-center gap-3 mb-5">
        <div className="flex gap-0.5 bg-gray-100 rounded-lg p-0.5">
          {filters.map(f => (
            <button key={f.key} onClick={() => setFilter(f.key)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                filter === f.key ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}>{f.label}</button>
          ))}
        </div>
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
            placeholder="搜索案例..." className="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder:text-gray-400" />
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center h-40 gap-2">
          <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
          <span className="text-sm text-gray-400">加载中...</span>
        </div>
      ) : filteredCases.length === 0 ? (
        <div className="text-center py-16">
          <FileText className="w-8 h-8 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-500 mb-1">{cases.length === 0 ? '暂无案例' : '没有匹配的案例'}</p>
          <p className="text-xs text-gray-400">{cases.length === 0 ? '创建第一个案例开始沉淀知识' : '尝试调整筛选条件'}</p>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-3">
          {filteredCases.map(c => (
            <div key={c.id} className="bg-white p-4 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer">
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-2">
                  {c.isFeatured && <Award className="w-3.5 h-3.5 text-amber-500" />}
                  <span className={`text-xs px-1.5 py-0.5 rounded ${
                    c.status === 'PUBLISHED' ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-500'
                  }`}>{c.status === 'PUBLISHED' ? '已发布' : '草稿'}</span>
                </div>
                <div className="flex items-center gap-1">
                  <button onClick={e => { e.stopPropagation(); handleToggleFeatured(c.id) }}
                    className={`p-1 rounded hover:bg-gray-100 ${c.isFeatured ? 'text-amber-500' : 'text-gray-300'}`}>
                    <Star className={`w-3 h-3 ${c.isFeatured ? 'fill-current' : ''}`} />
                  </button>
                </div>
              </div>
              <h3 className="text-sm font-medium text-gray-800 mb-1 line-clamp-2">{c.title}</h3>
              {c.summary && <p className="text-xs text-gray-400 mb-2 line-clamp-2">{c.summary}</p>}
              <div className="flex items-center gap-3 text-xs text-gray-400">
                {c.industry && <span>{c.industry}</span>}
                {c.contractValue != null && <span>{fmtValue(c.contractValue)}</span>}
                <span className="ml-auto">{fmtDate(c.createdAt)}</span>
              </div>
              {c.tags && c.tags.length > 0 && (
                <div className="flex gap-1 mt-2">
                  {c.tags.slice(0, 3).map((t, i) => (
                    <span key={i} className="text-xs px-1.5 py-0.5 bg-brand-50 text-brand-600 rounded">{t}</span>
                  ))}
                </div>
              )}
              <div className="flex items-center gap-3 mt-2 pt-2 border-t border-gray-100 text-xs text-gray-400">
                {c.viewCount != null && <span className="flex items-center gap-1"><Eye className="w-3 h-3" />{c.viewCount}</span>}
                {c.customerName && <span>{c.customerName}</span>}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-lg border border-gray-200 shadow-lg w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="text-sm font-semibold text-gray-900">新建案例</h2>
              <button onClick={() => setShowCreate(false)} className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg"><X className="w-4 h-4" /></button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">案例标题 *</label>
                <input type="text" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })}
                  placeholder="输入案例标题" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">行业</label>
                <input type="text" value={form.industry} onChange={e => setForm({ ...form, industry: e.target.value })}
                  placeholder="如：智慧城市" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">摘要</label>
                <textarea value={form.summary} onChange={e => setForm({ ...form, summary: e.target.value })}
                  placeholder="案例简要描述" rows={3} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm resize-none" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">标签</label>
                <input type="text" value={form.tags} onChange={e => setForm({ ...form, tags: e.target.value })}
                  placeholder="逗号分隔" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
            </div>
            <div className="flex justify-end gap-2 px-5 py-4 border-t border-gray-100">
              <button onClick={() => setShowCreate(false)} className="px-3 py-2 text-sm text-gray-500 hover:bg-gray-100 rounded-lg">取消</button>
              <button onClick={handleCreate} disabled={creating || !form.title.trim()}
                className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50">{creating ? '创建中...' : '创建'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
