import { Search, FileText, MessageSquare, BookOpen } from 'lucide-react'
import type { AgentType } from '@/types'

interface AgentInfo {
  id: AgentType
  name: string
  desc: string
  icon: typeof Search
}

export const agents: AgentInfo[] = [
  { id: 'intel', name: '情报分析师', desc: '招标监控 · 政策解读', icon: Search },
  { id: 'doc', name: '文档写手', desc: '方案撰写 · 标书编写', icon: FileText },
  { id: 'crm', name: '客户助理', desc: '客户管理 · 跟进提醒', icon: MessageSquare },
  { id: 'knowledge', name: '知识管家', desc: '知识检索 · 案例管理', icon: BookOpen },
]

interface AgentSelectorProps {
  currentAgentType: AgentType
  onAgentChange: (type: AgentType) => void
}

export function AgentSelector({ currentAgentType, onAgentChange }: AgentSelectorProps) {
  return (
    <div className="px-3 py-3">
      <div className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2 px-2">AI 员工</div>
      <div className="space-y-0.5">
        {agents.map(a => {
          const Icon = a.icon
          return (
            <button
              key={a.id}
              onClick={() => onAgentChange(a.id)}
              className={`w-full px-3 py-2 rounded-lg text-left flex items-center gap-2.5 transition-colors text-sm ${
                currentAgentType === a.id
                  ? 'bg-brand-50 text-brand-600 font-medium'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Icon className="w-[18px] h-[18px] shrink-0 text-brand-500" />
              <span className="truncate">{a.name}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
