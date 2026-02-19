import { Search, X } from 'lucide-react'
import { RefObject } from 'react'

interface SearchBarProps {
  searchTerm: string
  matchCount: number
  inputRef: RefObject<HTMLInputElement>
  onSearchChange: (term: string) => void
  onClose: () => void
}

export function SearchBar({ searchTerm, matchCount, inputRef, onSearchChange, onClose }: SearchBarProps) {
  return (
    <div className="px-5 py-2 bg-white border-b border-gray-200 flex items-center gap-2">
      <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
      <input
        ref={inputRef}
        type="text"
        value={searchTerm}
        onChange={e => onSearchChange(e.target.value)}
        placeholder="搜索消息..."
        className="flex-1 text-sm border-none outline-none bg-transparent placeholder:text-gray-400"
        autoFocus
      />
      {searchTerm && (
        <span className="text-xs text-gray-400 shrink-0">
          {matchCount} 条匹配
        </span>
      )}
      <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600">
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  )
}
