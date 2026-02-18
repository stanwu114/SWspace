import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FolderKanban,
  Users,
  BookOpen,
  MessageSquare,
  Settings,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  FileText,
} from 'lucide-react'
import { useState, useEffect } from 'react'

const nav = [
  { path: '/dashboard', label: '工作台', icon: LayoutDashboard },
  { path: '/projects', label: '项目', icon: FolderKanban },
  { path: '/customers', label: '客户', icon: Users },
  { path: '/chat', label: 'AI 对话', icon: MessageSquare },
  { path: '/knowledge', label: '知识库', icon: BookOpen },
  { path: '/bidding', label: '招标监控', icon: TrendingUp },
  { path: '/cases', label: '案例库', icon: FileText },
]

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const [isElectron, setIsElectron] = useState(false)
  const [isMac, setIsMac] = useState(false)
  const location = useLocation()

  useEffect(() => {
    // Detect if running in Electron
    const electron = !!(window as any).electron || navigator.userAgent.includes('Electron')
    setIsElectron(electron)
    // Detect macOS
    setIsMac(navigator.platform.toUpperCase().includes('MAC'))
  }, [])

  // Add extra top padding for macOS traffic light buttons in Electron
  const needsTrafficLightPadding = isElectron && isMac

  return (
    <aside className={`flex flex-col bg-white border-r border-gray-200 transition-all duration-200 ${collapsed ? 'w-16' : 'w-56'}`}>
      {/* Draggable region for Electron window - accounts for macOS traffic lights */}
      {needsTrafficLightPadding && (
        <div 
          className="h-8 w-full shrink-0" 
          style={{ WebkitAppRegion: 'drag' } as React.CSSProperties}
        />
      )}

      {/* Logo */}
      <div className={`h-24 flex items-center border-b border-gray-100 overflow-hidden ${
        needsTrafficLightPadding ? '' : ''
      }`}>
        {!collapsed ? (
          <img
            src="/images/logo.png"
            alt="AIspace"
            className="h-full w-full object-cover select-none"
            style={{ objectPosition: 'left center' }}
          />
        ) : (
          <div
            className="w-9 h-9 mx-auto flex items-center justify-center select-none"
            style={{
              background: 'linear-gradient(135deg, #2563eb, #3b82f6)',
              borderRadius: '10px',
            }}
          >
            <span className="text-white text-xs font-bold">AI</span>
          </div>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 py-2 px-2 space-y-0.5 overflow-auto">
        {nav.map((item) => {
          const Icon = item.icon
          const active = location.pathname.startsWith(item.path)
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                active
                  ? 'bg-brand-50 text-brand-600 font-medium'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              } ${collapsed ? 'justify-center px-0' : ''}`}
            >
              <Icon className={`w-[18px] h-[18px] shrink-0 ${active ? 'text-brand-500' : ''}`} />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </NavLink>
          )
        })}
      </nav>

      {/* Bottom */}
      <div className="px-2 py-2 border-t border-gray-100 space-y-0.5">
        <NavLink
          to="/settings"
          className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
            location.pathname.startsWith('/settings')
              ? 'bg-brand-50 text-brand-600 font-medium'
              : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
          } ${collapsed ? 'justify-center px-0' : ''}`}
        >
          <Settings className="w-[18px] h-[18px] shrink-0" />
          {!collapsed && <span>设置</span>}
        </NavLink>

        <button
          onClick={() => setCollapsed(!collapsed)}
          className={`flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-gray-400 hover:text-gray-600 hover:bg-gray-50 transition-colors ${collapsed ? 'justify-center px-0' : ''}`}
        >
          {collapsed ? <ChevronRight className="w-[18px] h-[18px]" /> : <ChevronLeft className="w-[18px] h-[18px]" />}
          {!collapsed && <span>收起</span>}
        </button>
      </div>
    </aside>
  )
}
