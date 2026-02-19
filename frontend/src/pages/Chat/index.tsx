import { AlertCircle } from 'lucide-react'
import { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { api } from '@/services/api'
import { wsService } from '@/services/websocket'
import { AgentSelector, agents } from '@/components/chat/AgentSelector'
import { SessionList } from '@/components/chat/SessionList'
import { ChatHeader } from '@/components/chat/ChatHeader'
import { SearchBar } from '@/components/chat/SearchBar'
import { MessageList } from '@/components/chat/MessageList'
import { ChatInput } from '@/components/chat/ChatInput'
import type { AgentType, ChatResponse, AISession } from '@/types'

export default function Chat() {
  const {
    sessions, currentSessionId, currentAgentType, messages, isStreaming,
    setSessions, setCurrentSession, setCurrentAgentType, setMessages, addMessage, appendToMessage, updateMessage, setStreaming,
  } = useChatStore()

  const [inputMessage, setInputMessage] = useState('')
  const [isConnected, setIsConnected] = useState(false)
  const [_isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sessionsLoading, setSessionsLoading] = useState(false)
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [showSearch, setShowSearch] = useState(false)
  const [thinkingStart, setThinkingStart] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const messagesRef = useRef<typeof messages>([])

  // Keep messagesRef in sync
  useEffect(() => { messagesRef.current = messages }, [messages])

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [])
  useEffect(() => { scrollToBottom() }, [messages, scrollToBottom])

  // Search filtering
  const filteredMessages = useMemo(() => {
    if (!searchTerm.trim()) return messages
    const term = searchTerm.toLowerCase()
    return messages.filter(m => m.content?.toLowerCase().includes(term))
  }, [messages, searchTerm])

  const matchCount = useMemo(() => {
    return searchTerm.trim() ? filteredMessages.length : 0
  }, [filteredMessages, searchTerm])

  // Search shortcut: Ctrl/Cmd + F
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'f') {
        e.preventDefault()
        setShowSearch(true)
        setTimeout(() => searchInputRef.current?.focus(), 50)
      }
      if (e.key === 'Escape' && showSearch) {
        setShowSearch(false)
        setSearchTerm('')
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [showSearch])

  // WebSocket connection
  useEffect(() => {
    let mounted = true
    const init = async () => {
      try {
        await wsService.connect()
        if (mounted) setIsConnected(true)
      } catch {
        if (mounted) setIsConnected(false)
      }
    }
    init()
    const unsubConnect = wsService.onConnect(() => { if (mounted) setIsConnected(true) })
    const unsubDisconnect = wsService.onDisconnect(() => { if (mounted) setIsConnected(false) })
    return () => { mounted = false; unsubConnect(); unsubDisconnect(); wsService.disconnect() }
  }, [])

  // Load sessions for current agent type
  useEffect(() => {
    const load = async () => {
      try {
        setSessionsLoading(true)
        const r = await api.agent.getSessions({ agentType: currentAgentType.toUpperCase() }) as unknown as { code: number; data?: { items?: AISession[] } | AISession[] }
        if (r?.code === 200) {
          const data = r.data
          setSessions(Array.isArray(data) ? data : data?.items || [])
        }
      } catch {
        setSessions([])
      } finally {
        setSessionsLoading(false)
      }
    }
    load()
  }, [currentAgentType, setSessions])

  // WebSocket subscription
  useEffect(() => {
    if (!currentSessionId || !isConnected) return

    const handle = (response: ChatResponse) => {
      switch (response.type) {
        case 'USER_MESSAGE_RECEIVED':
          break
        case 'AI_GENERATING':
          setStreaming(true)
          setThinkingStart(Date.now())
          addMessage({
            id: response.messageId || `ai-${Date.now()}`,
            sessionId: currentSessionId,
            role: 'assistant',
            content: '',
            createdAt: new Date().toISOString(),
          })
          break
        case 'AI_CHUNK':
          if (response.content) {
            setThinkingStart(null)
            const msgs = messagesRef.current
            const lastMsg = msgs[msgs.length - 1]
            if (lastMsg?.role === 'assistant') {
              appendToMessage(lastMsg.id, response.content)
            }
          }
          break
        case 'AI_COMPLETE':
          setStreaming(false)
          setThinkingStart(null)
          if (response.messageId) {
            const msgs = messagesRef.current
            const lastMsg = msgs[msgs.length - 1]
            if (lastMsg?.role === 'assistant' && lastMsg.id !== response.messageId) {
              updateMessage(lastMsg.id, { id: response.messageId })
            }
          }
          break
        case 'ERROR':
          setStreaming(false)
          setThinkingStart(null)
          setError(response.content || 'AI 响应出错')
          setTimeout(() => setError(null), 5000)
          break
      }
    }

    return wsService.subscribeToSession(currentSessionId, handle)
  }, [currentSessionId, isConnected, addMessage, appendToMessage, updateMessage, setStreaming])

  const loadSessionMessages = async (id: string) => {
    try {
      setIsLoading(true)
      setError(null)
      const r = await api.agent.getSessionMessages(id) as unknown as { code: number; data?: { items?: typeof messages } | typeof messages }
      if (r?.code === 200) {
        const data = r.data
        setMessages(Array.isArray(data) ? data : data?.items || [])
      } else {
        setMessages([])
      }
    } catch (e: unknown) {
      setError((e as Error)?.message || '加载消息失败')
      setMessages([])
    } finally {
      setIsLoading(false)
    }
  }

  const handleAgentChange = (type: AgentType) => {
    setCurrentAgentType(type)
    setCurrentSession(null)
    setMessages([])
    setError(null)
  }

  const handleCreateSession = async () => {
    try {
      setIsLoading(true)
      setError(null)
      const r = await api.agent.createSession(currentAgentType.toUpperCase()) as unknown as { code: number; data?: AISession; message?: string }
      if (r?.code === 200 && r.data) {
        setSessions([r.data, ...sessions])
        setCurrentSession(r.data.id)
        setMessages([])
      } else {
        setError(r?.message || '创建会话失败')
      }
    } catch (e: unknown) {
      setError((e as Error)?.message || '创建会话失败，请检查后端服务')
    } finally {
      setIsLoading(false)
    }
  }

  const handleSend = async () => {
    if (!inputMessage.trim() || isStreaming) return
    setError(null)

    let sid = currentSessionId
    if (!sid) {
      try {
        const r = await api.agent.createSession(currentAgentType.toUpperCase()) as unknown as { code: number; data?: AISession; message?: string }
        if (r?.code === 200 && r.data) {
          sid = r.data.id
          setSessions([r.data, ...sessions])
          setCurrentSession(sid)
        } else {
          setError('创建会话失败')
          return
        }
      } catch (e: unknown) {
        setError((e as Error)?.message || '创建会话失败')
        return
      }
    }

    const userMsg = {
      id: `user-${Date.now()}`,
      sessionId: sid!,
      role: 'user' as const,
      content: inputMessage,
      createdAt: new Date().toISOString(),
    }
    addMessage(userMsg)
    const msgContent = inputMessage
    setInputMessage('')

    if (isConnected) {
      wsService.sendMessage(sid!, msgContent)
    } else {
      // Fallback to REST API
      try {
        setStreaming(true)
        setThinkingStart(Date.now())
        const r = await api.agent.chat(sid!, msgContent) as unknown as { code: number; data?: { id?: string; content?: string } | string; message?: string }
        if (r?.code === 200 && r.data) {
          const data = typeof r.data === 'string' ? { content: r.data } : r.data
          addMessage({
            id: data.id || `ai-${Date.now()}`,
            sessionId: sid!,
            role: 'assistant',
            content: data.content || '',
            createdAt: new Date().toISOString(),
          })
        } else {
          setError(r?.message || 'AI 响应失败')
        }
      } catch (e: unknown) {
        setError((e as Error)?.message || 'AI 响应失败，请检查后端服务')
      } finally {
        setStreaming(false)
        setThinkingStart(null)
      }
    }
    textareaRef.current?.focus()
  }

  const handleFeedback = async (id: string, fb: 'good' | 'bad') => {
    try {
      await api.agent.feedbackMessage(id, fb)
      updateMessage(id, { feedback: fb })
    } catch { /* ignore */ }
  }

  const handleCopy = useCallback((id: string, content: string) => {
    navigator.clipboard.writeText(content).then(() => {
      setCopiedId(id)
      setTimeout(() => setCopiedId(null), 2000)
    })
  }, [])

  const handleDeleteSession = async (id: string) => {
    try {
      await api.agent.deleteSession(id)
      setSessions(sessions.filter(s => s.id !== id))
      if (currentSessionId === id) {
        setCurrentSession(null)
        setMessages([])
      }
    } catch { /* ignore */ }
  }

  const handleSelectSession = (id: string) => {
    setCurrentSession(id)
    loadSessionMessages(id)
  }

  const handleToggleSearch = () => {
    setShowSearch(!showSearch)
    if (showSearch) setSearchTerm('')
  }

  const currentAgent = agents.find(a => a.id === currentAgentType)!
  const displayMessages = searchTerm.trim() ? filteredMessages : messages

  return (
    <div className="flex h-full">
      {/* Left panel */}
      <div className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <AgentSelector currentAgentType={currentAgentType} onAgentChange={handleAgentChange} />
        <SessionList
          sessions={sessions}
          currentSessionId={currentSessionId}
          isLoading={sessionsLoading}
          isConnected={isConnected}
          onSelectSession={handleSelectSession}
          onCreateSession={handleCreateSession}
          onDeleteSession={handleDeleteSession}
        />
      </div>

      {/* Chat area */}
      <div className="flex-1 flex flex-col">
        <ChatHeader
          currentAgentType={currentAgentType}
          showSearch={showSearch}
          onToggleSearch={handleToggleSearch}
        />

        {showSearch && (
          <SearchBar
            searchTerm={searchTerm}
            matchCount={matchCount}
            inputRef={searchInputRef}
            onSearchChange={setSearchTerm}
            onClose={() => { setShowSearch(false); setSearchTerm('') }}
          />
        )}

        {/* Error banner */}
        {error && (
          <div className="mx-5 mt-3 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-600">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span className="flex-1">{error}</span>
            <button onClick={() => setError(null)} className="text-red-400 hover:text-red-600 text-xs">关闭</button>
          </div>
        )}

        <MessageList
          messages={displayMessages}
          isStreaming={isStreaming}
          thinkingStart={thinkingStart}
          copiedId={copiedId}
          currentAgentName={currentAgent.name}
          onCopy={handleCopy}
          onFeedback={handleFeedback}
        />
        <div ref={messagesEndRef} />

        <ChatInput
          inputMessage={inputMessage}
          isStreaming={isStreaming}
          isConnected={isConnected}
          textareaRef={textareaRef}
          onInputChange={setInputMessage}
          onSend={handleSend}
        />
      </div>
    </div>
  )
}
