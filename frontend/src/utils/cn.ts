import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * 合并 Tailwind CSS 类名的工具函数
 * 处理类名冲突，保留最后一个定义的类
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
