import { ThumbsUp, ThumbsDown, Copy, Check } from 'lucide-react'
import { AIMessage } from '@/types'
import { MarkdownRenderer } from './MarkdownRenderer'
import { ThinkingIndicator } from './ThinkingIndicator'

interface MessageListProps {
  messages: AIMessage[]
  isStreaming: boolean
  thinkingStart: number | null
  copiedId: string | null
  currentAgentName: string
  onCopy: (id: string, content: string) => void
  onFeedback: (id: string, feedback: 'good' | 'bad') => void
}

export function MessageList({
  messages,
  isStreaming,
  thinkingStart,
  copiedId,
  currentAgentName,
  onCopy,
  onFeedback,
}: MessageListProps) {
  return (
    <div className="flex-1 overflow-auto p-5">
      <div className="max-w-2xl mx-auto space-y-3">
        {messages.length === 0 && (
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-600">
              您好！我是<strong>{currentAgentName}</strong>，有什么可以帮您的？
            </p>
          </div>
        )}
        {messages.map(msg => (
          <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[80%] rounded-lg px-4 py-3 text-sm ${
                msg.role === 'user'
                  ? 'bg-brand-500 text-white'
                  : 'bg-white text-gray-700 border border-gray-200'
              }`}
            >
              {msg.role === 'user' ? (
                <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
              ) : (
                <>
                  {isStreaming && !msg.content && msg === messages[messages.length - 1] && (
                    <ThinkingIndicator startTime={thinkingStart || undefined} />
                  )}
                  {msg.content && <MarkdownRenderer content={msg.content} />}
                  {isStreaming && msg.content && msg === messages[messages.length - 1] && (
                    <span className="inline-block w-1 h-4 ml-0.5 bg-brand-300 rounded-sm animate-pulse align-middle" />
                  )}
                </>
              )}
              {msg.role === 'assistant' && !isStreaming && msg.content && (
                <div className="flex items-center gap-1 mt-2 pt-2 border-t border-gray-100">
                  <button
                    onClick={() => onCopy(msg.id, msg.content)}
                    className={`p-1 rounded hover:bg-gray-50 transition-colors ${
                      copiedId === msg.id ? 'text-emerald-500' : 'text-gray-300 hover:text-gray-500'
                    }`}
                    title="复制"
                  >
                    {copiedId === msg.id ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                  </button>
                  <button
                    onClick={() => onFeedback(msg.id, 'good')}
                    className={`p-1 rounded hover:bg-gray-50 ${
                      msg.feedback === 'good' ? 'text-brand-500' : 'text-gray-300'
                    }`}
                  >
                    <ThumbsUp className="w-3 h-3" />
                  </button>
                  <button
                    onClick={() => onFeedback(msg.id, 'bad')}
                    className={`p-1 rounded hover:bg-gray-50 ${
                      msg.feedback === 'bad' ? 'text-red-400' : 'text-gray-300'
                    }`}
                  >
                    <ThumbsDown className="w-3 h-3" />
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
        <div className="h-0" />
      </div>
    </div>
  )
}
