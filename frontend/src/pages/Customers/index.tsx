import { useState, useEffect } from 'react'
import { Plus, Search, X, Building2, ArrowRight, RefreshCw, AlertCircle } from 'lucide-react'
import api from '@/services/api'
import type { Customer, CustomerType } from '@/types'

export default function Customers() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [selected, setSelected] = useState<Customer | null>(null)
  const [form, setForm] = useState<{ name: string; type: CustomerType; industry: string; region: string }>({ name: '', type: 'government', industry: '', region: '' })

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const r = await api.customers.list({ pageSize: 100 }) as any
      if (r?.code === 200) {
        setCustomers(r.data?.items || [])
      } else {
        setError(r?.message || '加载失败')
        setCustomers([])
      }
    } catch (e: any) {
      setError(e?.message || '加载客户失败，请检查后端服务')
      setCustomers([])
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async () => {
    if (!form.name.trim()) return
    try {
      setCreating(true)
      setError(null)
      const r = await api.customers.create(form) as any
      if (r?.code === 200) {
        setShowCreate(false)
        setForm({ name: '', type: 'government', industry: '', region: '' })
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

  const filtered = customers.filter(c =>
    c.name.toLowerCase().includes(searchTerm.toLowerCase()) || c.industry?.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const typeLabel: Record<string, string> = { government: '政府', enterprise: '企业', other: '事业单位' }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">客户空间</h1>
        <div className="flex items-center gap-2">
          <button onClick={load} disabled={loading}
            className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50">
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} /> 刷新
          </button>
          <button onClick={() => setShowCreate(true)} className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors flex items-center gap-1.5">
            <Plus className="w-3.5 h-3.5" /> 新建客户
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

      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
          placeholder="搜索客户..." className="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder:text-gray-400" />
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center h-64 gap-2">
          <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
          <span className="text-sm text-gray-400">加载中...</span>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-4">
          <div className="col-span-2 space-y-1">
            {filtered.length === 0 ? (
              <div className="text-center py-16">
                <Building2 className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-500">{customers.length === 0 ? '还没有客户' : '没有匹配的客户'}</p>
                {customers.length === 0 && <p className="text-xs text-gray-400 mt-1">点击上方按钮创建第一个客户</p>}
              </div>
            ) : filtered.map(c => (
              <div key={c.id} onClick={() => setSelected(c)}
                className={`bg-white px-4 py-3 rounded-lg border flex items-center gap-3 cursor-pointer transition-colors ${
                  selected?.id === c.id ? 'border-brand-300 bg-brand-50' : 'border-gray-200 hover:bg-gray-50'
                }`}>
                <Building2 className="w-4 h-4 text-brand-500 shrink-0" />
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-medium text-gray-700">{c.name}</h3>
                  <p className="text-xs text-gray-400 mt-0.5">{typeLabel[c.type] || c.type} · {c.industry || '未知行业'} · {c.region || '未知区域'}</p>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-gray-300 shrink-0" />
              </div>
            ))}
          </div>

          {/* Profile */}
          <div className="bg-white border border-gray-200 rounded-lg h-fit sticky top-6">
            <div className="px-4 py-3 border-b border-gray-100">
              <h2 className="text-sm font-medium text-gray-900">客户画像</h2>
            </div>
            <div className="p-4">
              {selected ? (
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-brand-50 rounded-lg flex items-center justify-center">
                      <Building2 className="w-5 h-5 text-brand-500" />
                    </div>
                    <div>
                      <h3 className="text-sm font-medium text-gray-800">{selected.name}</h3>
                      <p className="text-xs text-gray-400">{typeLabel[selected.type] || selected.type}</p>
                    </div>
                  </div>
                  <div className="space-y-2.5 pt-3 border-t border-gray-100">
                    {([
                      ['行业', selected.industry],
                      ['区域', selected.region],
                      ['级别', selected.level === 'key' ? '重点客户' : selected.level === 'potential' ? '潜在客户' : '普通客户'],
                      ['关系分', `${selected.relationshipScore || 0}/100`],
                      ['最近联系', selected.lastContactAt ? new Date(selected.lastContactAt).toLocaleDateString('zh-CN') : undefined],
                    ] as [string, string | undefined][]).map(([label, val]) => (
                      <div key={label} className="flex text-sm">
                        <span className="text-gray-400 w-16 shrink-0 text-xs">{label}</span>
                        <span className="text-gray-700 text-xs">{val || '-'}</span>
                      </div>
                    ))}
                  </div>
                  {selected.tags?.length > 0 && (
                    <div className="pt-3 border-t border-gray-100">
                      <div className="flex flex-wrap gap-1">
                        {selected.tags.map((t, i) => (
                          <span key={i} className="text-xs px-1.5 py-0.5 bg-brand-50 text-brand-600 rounded">{t}</span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-xs text-gray-400 text-center py-8">选择客户查看详情</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-lg border border-gray-200 shadow-lg w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="text-sm font-semibold text-gray-900">新建客户</h2>
              <button onClick={() => setShowCreate(false)} className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg"><X className="w-4 h-4" /></button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">客户名称 *</label>
                <input type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  placeholder="输入客户名称" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">客户类型</label>
                <select value={form.type} onChange={e => setForm({ ...form, type: e.target.value as CustomerType })}
                  className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm">
                  <option value="government">政府</option>
                  <option value="enterprise">企业</option>
                  <option value="other">事业单位</option>
                </select>
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">行业</label>
                <input type="text" value={form.industry} onChange={e => setForm({ ...form, industry: e.target.value })}
                  placeholder="如：智慧城市" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1.5">区域</label>
                <input type="text" value={form.region} onChange={e => setForm({ ...form, region: e.target.value })}
                  placeholder="如：北京市" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
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
