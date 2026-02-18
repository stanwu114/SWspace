import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import type { AISession, AIMessage, AgentType } from '@/types'

interface ChatState {
  // 状态
  sessions: AISession[]
  currentSessionId: string | null
  currentAgentType: AgentType
  messages: AIMessage[]
  isStreaming: boolean
  
  // 操作
  setSessions: (sessions: AISession[]) => void
  setCurrentSession: (sessionId: string | null) => void
  setCurrentAgentType: (agentType: AgentType) => void
  setMessages: (messages: AIMessage[]) => void
  addMessage: (message: AIMessage) => void
  updateMessage: (messageId: string, updates: Partial<AIMessage>) => void
  appendToMessage: (messageId: string, content: string) => void
  setStreaming: (streaming: boolean) => void
  
  // 获取当前会话
  getCurrentSession: () => AISession | undefined
}

export const useChatStore = create<ChatState>()(
  devtools(
    persist(
      (set, get) => ({
        // 初始状态
        sessions: [],
        currentSessionId: null,
        currentAgentType: 'intel',
        messages: [],
        isStreaming: false,

        // 操作方法
        setSessions: (sessions) => set({ sessions }),
        
        setCurrentSession: (sessionId) => set({ currentSessionId: sessionId }),
        
        setCurrentAgentType: (agentType) => set({ currentAgentType: agentType }),
        
        setMessages: (messages) => set({ messages }),
        
        addMessage: (message) => set((state) => ({ 
          messages: [...state.messages, message] 
        })),
        
        updateMessage: (messageId, updates) => set((state) => ({
          messages: state.messages.map((m) =>
            m.id === messageId ? { ...m, ...updates } : m
          ),
        })),
        
        appendToMessage: (messageId, content) => set((state) => ({
          messages: state.messages.map((m) =>
            m.id === messageId ? { ...m, content: m.content + content } : m
          ),
        })),
        
        setStreaming: (streaming) => set({ isStreaming: streaming }),
        
        getCurrentSession: () => {
          const { sessions, currentSessionId } = get()
          return sessions.find((s) => s.id === currentSessionId)
        },
      }),
      {
        name: 'chat-storage',
        partialize: (state) => ({
          currentSessionId: state.currentSessionId,
          currentAgentType: state.currentAgentType,
        }),
      }
    ),
    { name: 'ChatStore' }
  )
)
