import { useState, useEffect } from 'react'
import { TrendingUp, Search, RefreshCw, CheckCircle, Eye, AlertCircle, ExternalLink, Star, StarOff, MapPin, Calendar } from 'lucide-react'
import api from '@/services/api'

interface BiddingItem {
  id: string
  title: string
  region?: string
  industry?: string
  budget?: number
  deadline?: string
  publishDate?: string
  source?: string
  sourceUrl?: string
  matched?: boolean
  starred?: boolean
  read?: boolean
  summary?: string
  linkedProjectId?: string
}

interface BiddingStats {
  todayNew?: number
  matched?: number
  unread?: number
  upcoming?: number
  [key: string]: unknown
}

export default function Bidding() {
  const [filter, setFilter] = useState('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [items, setItems] = useState<BiddingItem[]>([])
  const [stats, setStats] = useState<BiddingStats>({})

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const [itemsRes, statsRes] = await Promise.all([
        api.bidding.listItems({ pageSize: 50 }) as any,
        api.bidding.getStatistics() as any,
      ])
      if (itemsRes?.code === 200) setItems(itemsRes.data?.items || [])
      if (statsRes?.code === 200) setStats(statsRes.data || {})
    } catch (e: any) {
      setError(e?.message || '加载招标信息失败')
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = () => { load() }

  const handleToggleStar = async (id: string) => {
    try {
      await api.bidding.toggleStar(id)
      setItems(prev => prev.map(item =>
        item.id === id ? { ...item, starred: !item.starred } : item
      ))
    } catch {}
  }

  const handleMarkRead = async (id: string) => {
    try {
      await api.bidding.markAsRead(id)
      setItems(prev => prev.map(item =>
        item.id === id ? { ...item, read: true } : item
      ))
    } catch {}
  }

  const filters = [
    { key: 'all', label: '全部' },
    { key: 'matched', label: '已匹配' },
    { key: 'starred', label: '收藏' },
    { key: 'unread', label: '未读' },
  ]

  const statItems = [
    { label: '今日新增', value: stats.todayNew ?? 0, icon: TrendingUp },
    { label: '已匹配', value: stats.matched ?? 0, icon: CheckCircle },
    { label: '未读', value: stats.unread ?? 0, icon: Eye },
    { label: '即将截止', value: stats.upcoming ?? 0, icon: AlertCircle },
  ]

  const filteredItems = items.filter(item => {
    if (filter === 'matched' && !item.matched) return false
    if (filter === 'starred' && !item.starred) return false
    if (filter === 'unread' && item.read) return false
    if (searchTerm && !item.title?.toLowerCase().includes(searchTerm.toLowerCase())) return false
    return true
  })

  const fmtDate = (d?: string) => {
    if (!d) return '-'
    return new Date(d).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }

  const fmtBudget = (v?: number) => {
    if (!v) return '-'
    return v >= 10000 ? `¥${(v / 10000).toFixed(0)}万` : `¥${v.toLocaleString()}`
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">招标监控</h1>
        <button onClick={handleRefresh} disabled={loading}
          className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50">
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} /> 刷新
        </button>
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

      {/* Filters + Search */}
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
            placeholder="搜索招标信息..." className="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder:text-gray-400" />
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center h-40 gap-2">
          <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
          <span className="text-sm text-gray-400">加载中...</span>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="text-center py-16">
          <TrendingUp className="w-8 h-8 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-500 mb-1">{items.length === 0 ? '暂无招标信息' : '没有匹配的信息'}</p>
          <p className="text-xs text-gray-400">{items.length === 0 ? '配置数据源后将自动监控' : '尝试调整筛选条件'}</p>
        </div>
      ) : (
        <div className="space-y-2">
          {filteredItems.map(item => (
            <div key={item.id}
              onClick={() => handleMarkRead(item.id)}
              className={`bg-white p-4 rounded-lg border transition-colors cursor-pointer ${
                item.read ? 'border-gray-200 hover:bg-gray-50' : 'border-brand-200 bg-brand-50/30 hover:bg-brand-50/50'
              }`}>
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    {!item.read && <span className="w-1.5 h-1.5 bg-brand-500 rounded-full shrink-0" />}
                    <h3 className="text-sm font-medium text-gray-800 truncate">{item.title}</h3>
                    {item.matched && <span className="text-xs px-1.5 py-0.5 bg-emerald-50 text-emerald-600 rounded shrink-0">匹配</span>}
                  </div>
                  {item.summary && <p className="text-xs text-gray-400 mb-2 line-clamp-2">{item.summary}</p>}
                  <div className="flex items-center gap-4 text-xs text-gray-400">
                    {item.region && <span className="flex items-center gap-1"><MapPin className="w-3 h-3" />{item.region}</span>}
                    {item.deadline && <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />截止: {fmtDate(item.deadline)}</span>}
                    {item.budget != null && <span>预算: {fmtBudget(item.budget)}</span>}
                    {item.source && <span>{item.source}</span>}
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <button onClick={e => { e.stopPropagation(); handleToggleStar(item.id) }}
                    className={`p-1.5 rounded hover:bg-gray-100 ${item.starred ? 'text-amber-500' : 'text-gray-300'}`}>
                    {item.starred ? <Star className="w-3.5 h-3.5 fill-current" /> : <StarOff className="w-3.5 h-3.5" />}
                  </button>
                  {item.sourceUrl && (
                    <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer"
                      onClick={e => e.stopPropagation()}
                      className="p-1.5 rounded text-gray-300 hover:text-gray-600 hover:bg-gray-100">
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
