import { Send, Paperclip, Loader2, Plus, ThumbsUp, ThumbsDown, Search, FileText, MessageSquare, BookOpen, AlertCircle, Wifi, WifiOff, Trash2, Copy, Check, X } from 'lucide-react'
import { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { api } from '@/services/api'
import { wsService } from '@/services/websocket'
import { MarkdownRenderer } from '@/components/chat/MarkdownRenderer'
import { ThinkingIndicator } from '@/components/chat/ThinkingIndicator'
import type { AIMessage, AgentType, ChatResponse } from '@/types'

const agents = [
  { id: 'intel' as AgentType, name: '情报分析师', desc: '招标监控 · 政策解读', icon: Search },
  { id: 'doc' as AgentType, name: '文档写手', desc: '方案撰写 · 标书编写', icon: FileText },
  { id: 'crm' as AgentType, name: '客户助理', desc: '客户管理 · 跟进提醒', icon: MessageSquare },
  { id: 'knowledge' as AgentType, name: '知识管家', desc: '知识检索 · 案例管理', icon: BookOpen },
]

export default function Chat() {
  const {
    sessions, currentSessionId, currentAgentType, messages, isStreaming,
    setSessions, setCurrentSession, setCurrentAgentType, setMessages, addMessage, appendToMessage, updateMessage, setStreaming,
  } = useChatStore()

  const [inputMessage, setInputMessage] = useState('')
  const [isConnected, setIsConnected] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sessionsLoading, setSessionsLoading] = useState(false)
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [showSearch, setShowSearch] = useState(false)
  const [thinkingStart, setThinkingStart] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const messagesRef = useRef<AIMessage[]>([])

  // Keep messagesRef in sync to avoid stale closure in WebSocket handler
  useEffect(() => { messagesRef.current = messages }, [messages])

  const scrollToBottom = useCallback(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [])
  useEffect(() => { scrollToBottom() }, [messages, scrollToBottom])

  // Search filtering and highlight
  const filteredMessages = useMemo(() => {
    if (!searchTerm.trim()) return messages
    const term = searchTerm.toLowerCase()
    return messages.filter(m => m.content?.toLowerCase().includes(term))
  }, [messages, searchTerm])

  const matchCount = useMemo(() => {
    if (!searchTerm.trim()) return 0
    return filteredMessages.length
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
        const r = await api.agent.getSessions({ agentType: currentAgentType.toUpperCase() }) as any
        if (r?.code === 200) setSessions(r.data?.items || r.data || [])
      } catch {
        setSessions([])
      } finally {
        setSessionsLoading(false)
      }
    }
    load()
  }, [currentAgentType, setSessions])

  // WebSocket subscription - uses ref to avoid stale closure
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
      const r = await api.agent.getSessionMessages(id) as any
      if (r?.code === 200) setMessages(r.data?.items || r.data || [])
      else setMessages([])
    } catch (e: any) {
      setError(e?.message || '加载消息失败')
      setMessages([])
    } finally {
      setIsLoading(false)
    }
  }

  const handleAgentChange = (t: AgentType) => {
    setCurrentAgentType(t)
    setCurrentSession(null)
    setMessages([])
    setError(null)
  }

  const handleCreateSession = async () => {
    try {
      setIsLoading(true)
      setError(null)
      const r = await api.agent.createSession(currentAgentType.toUpperCase()) as any
      if (r?.code === 200 && r.data) {
        setSessions([r.data, ...sessions])
        setCurrentSession(r.data.id)
        setMessages([])
      } else {
        setError(r?.message || '创建会话失败')
      }
    } catch (e: any) {
      setError(e?.message || '创建会话失败，请检查后端服务')
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
        const r = await api.agent.createSession(currentAgentType.toUpperCase()) as any
        if (r?.code === 200 && r.data) {
          sid = r.data.id
          setSessions([r.data, ...sessions])
          setCurrentSession(sid)
        } else {
          setError('创建会话失败')
          return
        }
      } catch (e: any) {
        setError(e?.message || '创建会话失败')
        return
      }
    }

    const userMsg: AIMessage = {
      id: `user-${Date.now()}`,
      sessionId: sid!,
      role: 'user',
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
        const r = await api.agent.chat(sid!, msgContent) as any
        if (r?.code === 200 && r.data) {
          addMessage({
            id: r.data.id || `ai-${Date.now()}`,
            sessionId: sid!,
            role: 'assistant',
            content: r.data.content || r.data,
            createdAt: new Date().toISOString(),
          })
        } else {
          setError(r?.message || 'AI 响应失败')
        }
      } catch (e: any) {
        setError(e?.message || 'AI 响应失败，请检查后端服务')
      } finally {
        setStreaming(false)
        setThinkingStart(null)
      }
    }
    textareaRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  const handleFeedback = async (id: string, fb: 'good' | 'bad') => {
    try {
      await api.agent.feedbackMessage(id, fb)
      updateMessage(id, { feedback: fb })
    } catch {}
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
    } catch {}
  }

  const currentAgent = agents.find(a => a.id === currentAgentType)!
  const AgentIcon = currentAgent.icon
  const displayMessages = searchTerm.trim() ? filteredMessages : messages

  return (
    <div className="flex h-full">
      {/* Left panel */}
      <div className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-3 py-3">
          <div className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2 px-2">AI 员工</div>
          <div className="space-y-0.5">
            {agents.map(a => {
              const Icon = a.icon
              return (
                <button key={a.id} onClick={() => handleAgentChange(a.id)}
                  className={`w-full px-3 py-2 rounded-lg text-left flex items-center gap-2.5 transition-colors text-sm ${
                    currentAgentType === a.id ? 'bg-brand-50 text-brand-600 font-medium' : 'text-gray-600 hover:bg-gray-50'
                  }`}>
                  <Icon className={`w-[18px] h-[18px] shrink-0 ${currentAgentType === a.id ? 'text-brand-500' : 'text-brand-500'}`} />
                  <span className="truncate">{a.name}</span>
                </button>
              )
            })}
          </div>
        </div>

        <div className="flex-1 overflow-auto px-3 py-2 border-t border-gray-100">
          <div className="flex items-center justify-between mb-2 px-2">
            <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">对话</span>
            <button onClick={handleCreateSession} disabled={isLoading}
              className="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded disabled:opacity-50" title="新建">
              <Plus className="w-3.5 h-3.5" />
            </button>
          </div>
          {sessionsLoading ? (
            <div className="flex justify-center py-4">
              <div className="w-4 h-4 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
            </div>
          ) : (
            <div className="space-y-0.5">
              {sessions.map(s => (
                <div key={s.id} className="group relative flex items-center">
                  <button onClick={() => { setCurrentSession(s.id); loadSessionMessages(s.id) }}
                    className={`flex-1 px-3 py-2 rounded-lg text-left text-sm transition-colors truncate ${
                      currentSessionId === s.id ? 'bg-brand-50 text-brand-600 font-medium' : 'text-gray-500 hover:bg-gray-50'
                    }`}>{s.title || '新对话'}</button>
                  <button onClick={() => handleDeleteSession(s.id)}
                    className="p-1 text-gray-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
              ))}
              {sessions.length === 0 && <p className="text-xs text-gray-400 text-center py-4">暂无对话</p>}
            </div>
          )}
        </div>

        <div className="px-4 py-3 border-t border-gray-100">
          <div className="flex items-center gap-2 text-xs">
            {isConnected ? <Wifi className="w-3 h-3 text-emerald-500" /> : <WifiOff className="w-3 h-3 text-red-400" />}
            <span className="text-gray-400">{isConnected ? '已连接' : '未连接'}</span>
          </div>
        </div>
      </div>

      {/* Chat area */}
      <div className="flex-1 flex flex-col">
        <div className="h-12 flex items-center px-5 bg-white border-b border-gray-200 shrink-0 gap-2">
          <AgentIcon className="w-4 h-4 text-brand-500" />
          <span className="text-sm font-medium text-gray-800">{currentAgent.name}</span>
          <span className="text-xs text-gray-400">{currentAgent.desc}</span>
          <div className="flex-1" />
          <button
            onClick={() => { setShowSearch(!showSearch); if (showSearch) setSearchTerm('') }}
            className={`p-1.5 rounded-md transition-colors ${showSearch ? 'bg-brand-50 text-brand-500' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100'}`}
            title="搜索 (Ctrl+F)"
          >
            <Search className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Search bar */}
        {showSearch && (
          <div className="px-5 py-2 bg-white border-b border-gray-200 flex items-center gap-2">
            <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
            <input
              ref={searchInputRef}
              type="text"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder="搜索消息..."
              className="flex-1 text-sm border-none outline-none bg-transparent placeholder:text-gray-400"
              autoFocus
            />
            {searchTerm && (
              <span className="text-xs text-gray-400 shrink-0">
                {matchCount} 条匹配
              </span>
            )}
            <button onClick={() => { setShowSearch(false); setSearchTerm('') }} className="p-1 text-gray-400 hover:text-gray-600">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Error banner */}
        {error && (
          <div className="mx-5 mt-3 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-600">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span className="flex-1">{error}</span>
            <button onClick={() => setError(null)} className="text-red-400 hover:text-red-600 text-xs">关闭</button>
          </div>
        )}

        <div className="flex-1 overflow-auto p-5">
          <div className="max-w-2xl mx-auto space-y-3">
            {messages.length === 0 && !isLoading && (
              <div className="bg-white rounded-lg border border-gray-200 p-4">
                <p className="text-sm text-gray-600">您好！我是<strong>{currentAgent.name}</strong>，{currentAgent.desc}。请问有什么可以帮您的？</p>
              </div>
            )}
            {displayMessages.map(msg => (
              <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] rounded-lg px-4 py-3 text-sm ${
                  msg.role === 'user' ? 'bg-brand-500 text-white' : 'bg-white text-gray-700 border border-gray-200'
                }`}>
                  {msg.role === 'user' ? (
                    <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                  ) : (
                    <>
                      {/* AI thinking state: no content yet and still streaming */}
                      {isStreaming && !msg.content && msg === messages[messages.length - 1] && (
                        <ThinkingIndicator startTime={thinkingStart || undefined} />
                      )}
                      {/* Markdown rendered content */}
                      {msg.content && (
                        <MarkdownRenderer content={msg.content} />
                      )}
                      {/* Streaming cursor */}
                      {isStreaming && msg.content && msg === messages[messages.length - 1] && (
                        <span className="inline-block w-1 h-4 ml-0.5 bg-brand-300 rounded-sm animate-pulse align-middle" />
                      )}
                    </>
                  )}
                  {/* Action buttons for completed AI messages */}
                  {msg.role === 'assistant' && !isStreaming && msg.content && (
                    <div className="flex items-center gap-1 mt-2 pt-2 border-t border-gray-100">
                      <button
                        onClick={() => handleCopy(msg.id, msg.content)}
                        className={`p-1 rounded hover:bg-gray-50 transition-colors ${copiedId === msg.id ? 'text-emerald-500' : 'text-gray-300 hover:text-gray-500'}`}
                        title="复制"
                      >
                        {copiedId === msg.id ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                      </button>
                      <button onClick={() => handleFeedback(msg.id, 'good')}
                        className={`p-1 rounded hover:bg-gray-50 ${msg.feedback === 'good' ? 'text-brand-500' : 'text-gray-300'}`}>
                        <ThumbsUp className="w-3 h-3" />
                      </button>
                      <button onClick={() => handleFeedback(msg.id, 'bad')}
                        className={`p-1 rounded hover:bg-gray-50 ${msg.feedback === 'bad' ? 'text-red-400' : 'text-gray-300'}`}>
                        <ThumbsDown className="w-3 h-3" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex justify-center py-4">
                <div className="w-5 h-5 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        </div>

        <div className="px-5 py-3 bg-white border-t border-gray-200">
          <div className="max-w-2xl mx-auto flex items-end gap-2">
            <button className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg shrink-0">
              <Paperclip className="w-4 h-4" />
            </button>
            <div className="flex-1 relative">
              <textarea ref={textareaRef} value={inputMessage} onChange={e => setInputMessage(e.target.value)} onKeyDown={handleKeyDown}
                placeholder={isConnected ? '输入消息... (Enter 发送)' : '未连接到服务器，使用 REST 模式...'}
                rows={1} disabled={isStreaming}
                className="w-full px-3 py-2.5 pr-11 border border-gray-200 rounded-lg text-sm resize-none placeholder:text-gray-400" />
              <button onClick={handleSend} disabled={!inputMessage.trim() || isStreaming}
                className="absolute right-2 bottom-1.5 w-7 h-7 bg-brand-500 text-white rounded-md flex items-center justify-center hover:bg-brand-600 disabled:opacity-30 transition-all">
                {isStreaming ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
