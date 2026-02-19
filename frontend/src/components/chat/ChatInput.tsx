import { Send, Paperclip, Loader2 } from 'lucide-react'
import { RefObject } from 'react'

interface ChatInputProps {
  inputMessage: string
  isStreaming: boolean
  isConnected: boolean
  textareaRef: RefObject<HTMLTextAreaElement>
  onInputChange: (value: string) => void
  onSend: () => void
}

export function ChatInput({
  inputMessage,
  isStreaming,
  isConnected,
  textareaRef,
  onInputChange,
  onSend,
}: ChatInputProps) {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      onSend()
    }
  }

  return (
    <div className="px-5 py-3 bg-white border-t border-gray-200">
      <div className="max-w-2xl mx-auto flex items-end gap-2">
        <button className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg shrink-0">
          <Paperclip className="w-4 h-4" />
        </button>
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={inputMessage}
            onChange={e => onInputChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isConnected ? '输入消息... (Enter 发送)' : '未连接到服务器，使用 REST 模式...'}
            rows={1}
            disabled={isStreaming}
            className="w-full px-3 py-2.5 pr-11 border border-gray-200 rounded-lg text-sm resize-none placeholder:text-gray-400"
          />
          <button
            onClick={onSend}
            disabled={!inputMessage.trim() || isStreaming}
            className="absolute right-2 bottom-1.5 w-7 h-7 bg-brand-500 text-white rounded-md flex items-center justify-center hover:bg-brand-600 disabled:opacity-30 transition-all"
          >
            {isStreaming ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Send className="w-3.5 h-3.5" />
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
