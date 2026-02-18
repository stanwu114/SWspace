import { useState, useEffect, useRef } from 'react'

interface ThinkingIndicatorProps {
  startTime?: number
}

export function ThinkingIndicator({ startTime }: ThinkingIndicatorProps) {
  const [elapsed, setElapsed] = useState(0)
  const start = useRef(startTime || Date.now())

  useEffect(() => {
    start.current = startTime || Date.now()
    setElapsed(0)
    const timer = setInterval(() => {
      setElapsed(Math.floor((Date.now() - start.current) / 1000))
    }, 1000)
    return () => clearInterval(timer)
  }, [startTime])

  const hint = elapsed >= 15
    ? '正在进行复杂分析，请耐心等待...'
    : elapsed >= 8
    ? '正在深入思考...'
    : '思考中'

  return (
    <div className="flex items-center gap-2 text-gray-400 text-sm py-1">
      <div className="flex gap-0.5">
        <span className="w-1.5 h-1.5 bg-brand-400 rounded-full animate-bounce [animation-delay:0ms]" />
        <span className="w-1.5 h-1.5 bg-brand-400 rounded-full animate-bounce [animation-delay:150ms]" />
        <span className="w-1.5 h-1.5 bg-brand-400 rounded-full animate-bounce [animation-delay:300ms]" />
      </div>
      <span>{hint}</span>
      {elapsed > 0 && <span className="text-gray-300">{elapsed}s</span>}
    </div>
  )
}

export default ThinkingIndicator
