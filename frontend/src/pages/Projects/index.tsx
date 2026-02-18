import { useState, useEffect } from 'react'
import { FolderKanban, Plus, Search, RefreshCw, X, AlertCircle } from 'lucide-react'
import api from '@/services/api'
import type { Project } from '@/types'

type ColStatus = 'LEAD' | 'OPPORTUNITY' | 'EXECUTION' | 'CLOSED'

const columns: { key: ColStatus; title: string; dot: string }[] = [
  { key: 'LEAD', title: '线索', dot: 'bg-gray-400' },
  { key: 'OPPORTUNITY', title: '机会', dot: 'bg-blue-500' },
  { key: 'EXECUTION', title: '执行', dot: 'bg-emerald-500' },
  { key: 'CLOSED', title: '完成', dot: 'bg-violet-500' },
]

export default function Projects() {
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ name: '', estimatedValue: '', description: '' })

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const r = await api.projects.list({ pageSize: 100 }) as any
      if (r?.code === 200) {
        setProjects(r.data?.items || [])
      } else {
        setError(r?.message || '加载失败')
        setProjects([])
      }
    } catch (e: any) {
      setError(e?.message || '加载项目失败，请检查后端服务')
      setProjects([])
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async () => {
    if (!form.name.trim()) return
    try {
      setCreating(true)
      setError(null)
      const r = await api.projects.create({
        name: form.name,
        description: form.description || undefined,
        estimatedValue: form.estimatedValue ? parseFloat(form.estimatedValue) * 10000 : undefined,
      }) as any
      if (r?.code === 200) {
        setShowCreate(false)
        setForm({ name: '', estimatedValue: '', description: '' })
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

  const grouped = projects.reduce((acc, p) => {
    const s = (p.status?.toUpperCase() as ColStatus) || 'LEAD'
    ;(acc[s] ||= []).push(p)
    return acc
  }, {} as Record<ColStatus, Project[]>)

  const filter = (s: ColStatus) => {
    const list = grouped[s] || []
    return searchTerm ? list.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase())) : list
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">项目空间</h1>
        <div className="flex items-center gap-2">
          <button onClick={load} disabled={loading}
            className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50">
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} /> 刷新
          </button>
          <button onClick={() => setShowCreate(true)} className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors flex items-center gap-1.5">
            <Plus className="w-3.5 h-3.5" /> 新建项目
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
        <input type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
          placeholder="搜索项目..." className="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder:text-gray-400" />
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center h-64 gap-2">
          <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
          <span className="text-sm text-gray-400">加载中...</span>
        </div>
      ) : projects.length === 0 ? (
        <div className="text-center py-20">
          <FolderKanban className="w-8 h-8 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-500 mb-1">还没有项目</p>
          <p className="text-xs text-gray-400">点击上方按钮创建第一个项目</p>
        </div>
      ) : (
        <div className="grid grid-cols-4 gap-3">
          {columns.map(col => {
            const items = filter(col.key)
            return (
              <div key={col.key}>
                <div className="flex items-center gap-2 mb-2 px-1">
                  <div className={`w-2 h-2 rounded-full ${col.dot}`} />
                  <span className="text-sm font-medium text-gray-700">{col.title}</span>
                  <span className="text-xs text-gray-400 ml-auto">{items.length}</span>
                </div>
                <div className="space-y-2">
                  {items.length === 0 ? (
                    <div className="border border-dashed border-gray-200 rounded-lg py-8 text-center">
                      <p className="text-xs text-gray-400">暂无</p>
                    </div>
                  ) : items.map(p => (
                    <div key={p.id} className="bg-white p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer">
                      <h3 className="text-sm font-medium text-gray-700 mb-1.5 line-clamp-2">{p.name}</h3>
                      <div className="flex items-center justify-between text-xs text-gray-400">
                        <span>{p.estimatedValue ? `¥${(p.estimatedValue / 10000).toFixed(0)}万` : '-'}</span>
                        <span>{p.bidDeadline ? new Date(p.bidDeadline).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) : ''}</span>
                      </div>
                      {p.tags?.length > 0 && (
                        <div className="flex gap-1 mt-2">
                          {p.tags.slice(0, 2).map((t, i) => (
                            <span key={i} className="text-xs px-1.5 py-0.5 bg-brand-50 text-brand-600 rounded">{t}</span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-lg border border-gray-200 shadow-lg w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="text-sm font-semibold text-gray-900">新建项目</h2>
              <button onClick={() => setShowCreate(false)} className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg"><X className="w-4 h-4" /></button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">项目名称 *</label>
                <input type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  placeholder="输入项目名称" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">预估金额（万元）</label>
                <input type="number" value={form.estimatedValue} onChange={e => setForm({ ...form, estimatedValue: e.target.value })}
                  placeholder="如 500" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">项目描述</label>
                <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })}
                  placeholder="简要描述" rows={3} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm resize-none" />
              </div>
            </div>
            <div className="flex justify-end gap-2 px-5 py-4 border-t border-gray-100">
              <button onClick={() => setShowCreate(false)} className="px-3 py-2 text-sm text-gray-500 hover:bg-gray-100 rounded-lg">取消</button>
              <button onClick={handleCreate} disabled={creating || !form.name.trim()}
                className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50">{creating ? '创建中...' : '创建'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
