import { Plus, Trash2, Wifi, WifiOff, Loader2 } from 'lucide-react'
import type { AISession } from '@/types'

interface SessionListProps {
  sessions: AISession[]
  currentSessionId: string | null
  isLoading: boolean
  isConnected: boolean
  onSelectSession: (id: string) => void
  onCreateSession: () => void
  onDeleteSession: (id: string) => void
}

export function SessionList({
  sessions,
  currentSessionId,
  isLoading,
  isConnected,
  onSelectSession,
  onCreateSession,
  onDeleteSession,
}: SessionListProps) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 overflow-auto px-3 py-2 border-t border-gray-100">
        <div className="flex items-center justify-between mb-2 px-2">
          <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">对话</span>
          <button
            onClick={onCreateSession}
            disabled={isLoading}
            className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded disabled:opacity-50"
            title="新建"
          >
            <Plus className="w-3.5 h-3.5" />
          </button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-4">
            <Loader2 className="w-4 h-4 animate-spin text-brand-500" />
          </div>
        ) : (
          <div className="space-y-0.5">
            {sessions.map(s => (
              <div key={s.id} className="group relative flex items-center">
                <button
                  onClick={() => onSelectSession(s.id)}
                  className={`flex-1 px-3 py-2 rounded-lg text-left text-sm transition-colors truncate ${
                    currentSessionId === s.id
                      ? 'bg-brand-50 text-brand-600 font-medium'
                      : 'text-gray-500 hover:bg-gray-50'
                  }`}
                >
                  {s.title || '新对话'}
                </button>
                <button
                  onClick={() => onDeleteSession(s.id)}
                  className="p-1 text-gray-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
                >
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>
            ))}
            {sessions.length === 0 && (
              <p className="text-xs text-gray-400 text-center py-4">暂无对话</p>
            )}
          </div>
        )}
      </div>

      <div className="px-4 py-3 border-t border-gray-100">
        <div className="flex items-center gap-2 text-xs">
          {isConnected ? (
            <Wifi className="w-3 h-3 text-emerald-500" />
          ) : (
            <WifiOff className="w-3 h-3 text-red-400" />
          )}
          <span className="text-gray-400">{isConnected ? '已连接' : '未连接'}</span>
        </div>
      </div>
    </div>
  )
}
