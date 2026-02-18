import {
  FolderKanban, Users, FileText, TrendingUp,
  Calendar, ArrowRight, CheckCircle2, Sparkles,
  Search as SearchIcon, BookOpen, MessageSquare,
  AlertCircle, RefreshCw,
} from 'lucide-react'
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/services/api'
import type { Project, Reminder } from '@/types'

interface DashboardStats {
  activeProjects: number
  activeCustomers: number
  monthlyDocuments: number
  estimatedValue: number
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState<DashboardStats>({ activeProjects: 0, activeCustomers: 0, monthlyDocuments: 0, estimatedValue: 0 })
  const [todayReminders, setTodayReminders] = useState<Reminder[]>([])
  const [recentProjects, setRecentProjects] = useState<Project[]>([])

  useEffect(() => { load() }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError(null)
      const results = await Promise.allSettled([
        api.projects.list({ pageSize: 10 }) as any,
        api.customers.list({ pageSize: 10 }) as any,
        api.reminders.today() as any,
      ])

      const [projectsRes, customersRes, remindersRes] = results

      if (projectsRes.status === 'fulfilled' && projectsRes.value?.code === 200) {
        const projects = projectsRes.value.data?.items || []
        setRecentProjects(projects)
        setStats(prev => ({
          ...prev,
          activeProjects: projectsRes.value.data?.total || projects.length,
          estimatedValue: projects.reduce((sum: number, p: Project) => sum + (p.estimatedValue || 0), 0),
        }))
      }

      if (customersRes.status === 'fulfilled' && customersRes.value?.code === 200) {
        setStats(prev => ({ ...prev, activeCustomers: customersRes.value.data?.total || 0 }))
      }

      if (remindersRes.status === 'fulfilled' && remindersRes.value?.code === 200) {
        setTodayReminders(remindersRes.value.data?.items || remindersRes.value.data || [])
      }

      // Check if all requests failed
      const allFailed = results.every(r => r.status === 'rejected')
      if (allFailed) {
        setError('无法连接到后端服务，请确认服务已启动')
      }
    } catch (e: any) {
      setError(e?.message || '加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCompleteReminder = async (id: string) => {
    try {
      await api.reminders.complete(id)
      setTodayReminders(prev => prev.filter(r => r.id !== id))
    } catch {}
  }

  const fmt = (v: number) => v >= 10000 ? `¥${(v / 10000).toFixed(0)}万` : `¥${v.toLocaleString()}`
  const greeting = () => { const h = new Date().getHours(); return h < 6 ? '夜深了' : h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好' }

  if (loading) return (
    <div className="flex flex-col items-center justify-center h-full gap-2">
      <div className="w-6 h-6 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
      <span className="text-sm text-gray-400">加载中...</span>
    </div>
  )

  const statItems = [
    { icon: FolderKanban, label: '进行中项目', value: stats.activeProjects, to: '/projects' },
    { icon: Users, label: '活跃客户', value: stats.activeCustomers, to: '/customers' },
    { icon: FileText, label: '本月文档', value: stats.monthlyDocuments },
    { icon: TrendingUp, label: '预估金额', value: fmt(stats.estimatedValue) },
  ]

  const agentItems = [
    { key: 'intel', name: '情报分析师', desc: '招标监控 / 政策解读', icon: SearchIcon },
    { key: 'doc', name: '文档写手', desc: '方案撰写 / 标书编写', icon: FileText },
    { key: 'crm', name: '客户助理', desc: '客户管理 / 跟进提醒', icon: MessageSquare },
    { key: 'know', name: '知识管家', desc: '知识检索 / 案例管理', icon: BookOpen },
  ]

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">{greeting()}</h1>
          <p className="text-sm text-gray-400 mt-0.5">
            {new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })}
          </p>
        </div>
        <button onClick={load} className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors" title="刷新">
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-600">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span className="flex-1">{error}</span>
          <button onClick={load} className="text-xs text-red-500 hover:text-red-700 font-medium">重试</button>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-4 gap-3 mb-6">
        {statItems.map((s, i) => (
          <div key={i} onClick={s.to ? () => navigate(s.to!) : undefined}
            className={`bg-white border border-gray-200 rounded-lg px-4 py-3.5 ${s.to ? 'cursor-pointer hover:bg-gray-50' : ''} transition-colors`}>
            <div className="flex items-center gap-2 mb-2">
              <s.icon className="w-4 h-4 text-brand-500" />
              <span className="text-xs text-gray-400">{s.label}</span>
            </div>
            <div className="text-xl font-semibold text-gray-900">{typeof s.value === 'number' ? s.value : s.value}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {/* Today's reminders */}
        <div className="col-span-2 bg-white border border-gray-200 rounded-lg">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h2 className="text-sm font-medium text-gray-900 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-brand-500" /> 今日待办
            </h2>
            <span className="text-xs text-gray-400">{todayReminders.length} 项</span>
          </div>
          <div className="p-2">
            {todayReminders.length > 0 ? (
              todayReminders.map(r => (
                <div key={r.id} className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-gray-50 transition-colors">
                  <input type="checkbox" onChange={() => handleCompleteReminder(r.id)}
                    className="w-4 h-4 rounded border-gray-300 text-brand-500 focus:ring-brand-200 cursor-pointer" />
                  <span className="flex-1 text-sm text-gray-700">{r.title}</span>
                  <span className="text-xs text-gray-400">{new Date(r.remindAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
              ))
            ) : (
              <div className="text-center py-10">
                <CheckCircle2 className="w-6 h-6 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-400">今日暂无待办</p>
              </div>
            )}
          </div>
        </div>

        {/* AI Team */}
        <div className="bg-white border border-gray-200 rounded-lg">
          <div className="px-4 py-3 border-b border-gray-100">
            <h2 className="text-sm font-medium text-gray-900 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-brand-500" /> AI 团队
            </h2>
          </div>
          <div className="p-2">
            {agentItems.map(a => (
              <div key={a.key} onClick={() => navigate('/chat')}
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors group">
                <a.icon className="w-4 h-4 text-brand-500 shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-gray-700">{a.name}</div>
                  <div className="text-xs text-gray-400 truncate">{a.desc}</div>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-gray-300 opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent projects */}
      {recentProjects.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-lg mt-4">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h2 className="text-sm font-medium text-gray-900 flex items-center gap-2">
              <FolderKanban className="w-4 h-4 text-brand-500" /> 进行中的项目
            </h2>
            <button onClick={() => navigate('/projects')} className="text-xs text-brand-500 hover:text-brand-600 flex items-center gap-1">
              查看全部 <ArrowRight className="w-3 h-3" />
            </button>
          </div>
          <div className="p-2">
            {recentProjects.slice(0, 5).map(p => (
              <div key={p.id} onClick={() => navigate(`/projects/${p.id}`)}
                className="flex items-center justify-between px-3 py-2.5 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors">
                <div className="min-w-0">
                  <div className="text-sm font-medium text-gray-700 truncate">{p.name}</div>
                  {p.bidDeadline && <div className="text-xs text-gray-400">截止: {new Date(p.bidDeadline).toLocaleDateString('zh-CN')}</div>}
                </div>
                <div className="text-sm font-medium text-gray-600 shrink-0 ml-4">{p.estimatedValue ? fmt(p.estimatedValue) : '-'}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
