import { useState, useEffect } from 'react'
import { BookOpen, Plus, Search, X, Sparkles, FileText, FolderOpen, Lightbulb, AlertTriangle, BarChart3, RefreshCw, AlertCircle } from 'lucide-react'
import api from '@/services/api'
import type { Knowledge, KnowledgeCategory } from '@/types'

const categories = [
  { name: '行业知识', icon: BarChart3, key: 'industry' as KnowledgeCategory },
  { name: '方案模板', icon: FileText, key: 'template' as KnowledgeCategory },
  { name: '项目案例', icon: FolderOpen, key: 'case' as KnowledgeCategory },
  { name: '销售技巧', icon: Lightbulb, key: 'sales' as KnowledgeCategory },
  { name: '经验教训', icon: AlertTriangle, key: 'lesson' as KnowledgeCategory },
]

export default function KnowledgePage() {
  const [list, setList] = useState<Knowledge[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [searchResults, setSearchResults] = useState<Knowledge[]>([])
  const [searching, setSearching] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [selectedCat, setSelectedCat] = useState<KnowledgeCategory | null>(null)
  const [form, setForm] = useState({ title: '', content: '', category: 'industry' as KnowledgeCategory, tags: '' })

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const r = await api.knowledge.list({ pageSize: 100 }) as any
      if (r?.code === 200) {
        setList(r.data?.items || [])
      } else {
        setError(r?.message || '加载失败')
        setList([])
      }
    } catch (e: any) {
      setError(e?.message || '加载知识库失败，请检查后端服务')
      setList([])
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async () => {
    if (!searchTerm.trim()) { setSearchResults([]); return }
    try {
      setSearching(true)
      setError(null)
      const r = await api.knowledge.search(searchTerm, undefined, 20) as any
      if (r?.code === 200) {
        setSearchResults(r.data?.items || r.data || [])
      } else {
        setError(r?.message || '搜索失败')
        setSearchResults([])
      }
    } catch (e: any) {
      setError(e?.message || '搜索失败')
      setSearchResults([])
    } finally {
      setSearching(false)
    }
  }

  const handleCreate = async () => {
    if (!form.title.trim() || !form.content.trim()) return
    try {
      setCreating(true)
      setError(null)
      const r = await api.knowledge.create({
        title: form.title,
        content: form.content,
        category: form.category,
        tags: form.tags.split(',').map(t => t.trim()).filter(Boolean),
      }) as any
      if (r?.code === 200) {
        setShowCreate(false)
        setForm({ title: '', content: '', category: 'industry', tags: '' })
        load()
      } else {
        setError(r?.message || '创建失败')
      }
    } catch (e: any) {
      setError(e?.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const catCount = (key: string) => list.filter(k => k.category === key).length
  const display = searchTerm && searchResults.length > 0 ? searchResults : selectedCat ? list.filter(k => k.category === selectedCat) : list

  const fmtDate = (d?: string) => {
    if (!d) return '-'
    const days = Math.floor((Date.now() - new Date(d).getTime()) / 86400000)
    return days === 0 ? '今天' : days === 1 ? '昨天' : days < 7 ? `${days}天前` : new Date(d).toLocaleDateString('zh-CN')
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">知识库</h1>
        <div className="flex items-center gap-2">
          <button onClick={load} disabled={loading}
            className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50">
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} /> 刷新
          </button>
          <button onClick={() => setShowCreate(true)} className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors flex items-center gap-1.5">
            <Plus className="w-3.5 h-3.5" /> 添加知识
          </button>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-600">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="text-xs text-red-400 hover:text-red-600">关闭</button>
        </div>
      )}

      {/* Search */}
      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)} onKeyDown={e => e.key === 'Enter' && handleSearch()}
          placeholder="搜索知识库..." className="w-full pl-10 pr-24 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder:text-gray-400" />
        <button onClick={handleSearch} disabled={searching}
          className="absolute right-1.5 top-1/2 -translate-y-1/2 px-2.5 py-1 text-xs text-white bg-brand-500 hover:bg-brand-600 rounded-md flex items-center gap-1 disabled:opacity-50">
          <Sparkles className="w-3 h-3" /> {searching ? '搜索中' : 'AI搜索'}
        </button>
      </div>

      {/* Categories */}
      <div className="flex gap-2 mb-5">
        {categories.map(cat => {
          const Icon = cat.icon
          return (
            <button key={cat.key} onClick={() => setSelectedCat(selectedCat === cat.key ? null : cat.key)}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg border text-sm transition-colors ${
                selectedCat === cat.key ? 'border-brand-300 bg-brand-50 text-brand-600' : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
              }`}>
              <Icon className={`w-4 h-4 ${selectedCat === cat.key ? 'text-brand-500' : 'text-brand-500'}`} />
              <span>{cat.name}</span>
              <span className="text-xs text-gray-400">{catCount(cat.key)}</span>
            </button>
          )
        })}
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center h-40 gap-2">
          <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
          <span className="text-sm text-gray-400">加载中...</span>
        </div>
      ) : (
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-medium text-gray-700">
              {searchTerm && searchResults.length > 0 ? `搜索结果 (${searchResults.length})`
                : selectedCat ? `${categories.find(c => c.key === selectedCat)?.name} (${display.length})`
                : `全部 (${list.length})`}
            </h2>
            {selectedCat && <button onClick={() => setSelectedCat(null)} className="text-xs text-brand-500 hover:text-brand-600">清除筛选</button>}
          </div>

          {display.length === 0 ? (
            <div className="text-center py-16">
              <BookOpen className="w-8 h-8 text-gray-300 mx-auto mb-2" />
              <p className="text-sm text-gray-500">{list.length === 0 ? '还没有知识' : '没有匹配的内容'}</p>
              {list.length === 0 && <p className="text-xs text-gray-400 mt-1">点击上方按钮添加第一条知识</p>}
            </div>
          ) : (
            <div className="grid grid-cols-3 gap-3">
              {display.map(k => {
                const cat = categories.find(c => c.key === k.category) || categories[0]
                const CatIcon = cat.icon
                return (
                  <div key={k.id} className="bg-white p-4 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer">
                    <div className="flex items-start justify-between mb-2">
                      <CatIcon className="w-4 h-4 text-brand-500" />
                      <span className="text-xs text-gray-400">{fmtDate(k.createdAt)}</span>
                    </div>
                    <h3 className="text-sm font-medium text-gray-700 mb-1 line-clamp-2">{k.title}</h3>
                    <p className="text-xs text-gray-400 mb-2.5 line-clamp-2">{k.content}</p>
                    <div className="flex items-center gap-2">
                      <span className="text-xs px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded">{cat.name}</span>
                      {k.tags?.slice(0, 1).map((t, i) => <span key={i} className="text-xs text-gray-400">#{t}</span>)}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-lg border border-gray-200 shadow-lg w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="text-sm font-semibold text-gray-900">添加知识</h2>
              <button onClick={() => setShowCreate(false)} className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg"><X className="w-4 h-4" /></button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">标题 *</label>
                <input type="text" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })}
                  placeholder="知识标题" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">分类</label>
                <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value as KnowledgeCategory })}
                  className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm">
                  {categories.map(c => <option key={c.key} value={c.key}>{c.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">内容 *</label>
                <textarea value={form.content} onChange={e => setForm({ ...form, content: e.target.value })}
                  placeholder="知识内容" rows={5} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm resize-none" />
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
