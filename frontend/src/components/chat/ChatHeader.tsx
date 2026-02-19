import { Search } from 'lucide-react'
import type { AgentType } from '@/types'
import { agents } from './AgentSelector'

interface ChatHeaderProps {
  currentAgentType: AgentType
  showSearch: boolean
  onToggleSearch: () => void
}

export function ChatHeader({ currentAgentType, showSearch, onToggleSearch }: ChatHeaderProps) {
  const currentAgent = agents.find(a => a.id === currentAgentType)!
  const AgentIcon = currentAgent.icon

  return (
    <div className="h-12 flex items-center px-5 bg-white border-b border-gray-200 shrink-0 gap-2">
      <AgentIcon className="w-4 h-4 text-brand-500" />
      <span className="text-sm font-medium text-gray-800">{currentAgent.name}</span>
      <span className="text-xs text-gray-400">{currentAgent.desc}</span>
      <div className="flex-1" />
      <button
        onClick={onToggleSearch}
        className={`p-1.5 rounded-md transition-colors ${
          showSearch
            ? 'bg-brand-50 text-brand-500'
            : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100'
        }`}
        title="搜索 (Ctrl+F)"
      >
        <Search className="w-3.5 h-3.5" />
      </button>
    </div>
  )
}
