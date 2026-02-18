import { Client, IMessage } from '@stomp/stompjs'
import type { ChatResponse } from '@/types'

type MessageHandler = (response: ChatResponse) => void
type ConnectionHandler = () => void

class WebSocketService {
  private client: Client | null = null
  private messageHandlers: Map<string, MessageHandler[]> = new Map()
  private onConnectHandlers: ConnectionHandler[] = []
  private onDisconnectHandlers: ConnectionHandler[] = []
  private reconnectAttempts = 0
  private maxReconnectDelay = 60000

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        brokerURL: 'ws://localhost:8080/ws/agent',
        reconnectDelay: this.getReconnectDelay(),
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected')
          this.reconnectAttempts = 0
          this.onConnectHandlers.forEach(handler => handler())
          // Re-subscribe to all active sessions after reconnect
          this.resubscribeAll()
          resolve()
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected')
          this.reconnectAttempts++
          // Update reconnect delay with exponential backoff
          if (this.client) {
            this.client.reconnectDelay = this.getReconnectDelay()
          }
          this.onDisconnectHandlers.forEach(handler => handler())
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          reject(new Error(frame.headers.message || 'WebSocket connection failed'))
        },
        onWebSocketError: (event) => {
          console.warn('WebSocket error, will retry:', event)
        },
      })

      this.client.activate()
    })
  }

  private getReconnectDelay(): number {
    // Exponential backoff: 5s, 10s, 20s, 40s, max 60s
    const delay = Math.min(5000 * Math.pow(2, this.reconnectAttempts), this.maxReconnectDelay)
    return delay
  }

  private resubscribeAll(): void {
    if (!this.client?.active) return
    // Re-subscribe handlers stored from previous sessions
    for (const [sessionId, handlers] of this.messageHandlers.entries()) {
      if (handlers.length > 0) {
        const destination = `/topic/chat/${sessionId}`
        this.client.subscribe(destination, (message: IMessage) => {
          try {
            const response = JSON.parse(message.body) as ChatResponse
            handlers.forEach(h => h(response))
          } catch (error) {
            console.error('Failed to parse message:', error)
          }
        })
      }
    }
  }

  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate()
    }
    this.messageHandlers.clear()
  }

  subscribeToSession(sessionId: string, handler: MessageHandler): () => void {
    if (!this.client?.active) {
      console.error('WebSocket not connected')
      return () => {}
    }

    const destination = `/topic/chat/${sessionId}`
    
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const response = JSON.parse(message.body) as ChatResponse
        handler(response)
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    })

    // Store handler for potential reconnection
    if (!this.messageHandlers.has(sessionId)) {
      this.messageHandlers.set(sessionId, [])
    }
    this.messageHandlers.get(sessionId)!.push(handler)

    // Return unsubscribe function
    return () => {
      subscription.unsubscribe()
      const handlers = this.messageHandlers.get(sessionId)
      if (handlers) {
        const index = handlers.indexOf(handler)
        if (index > -1) handlers.splice(index, 1)
        if (handlers.length === 0) this.messageHandlers.delete(sessionId)
      }
    }
  }

  sendMessage(sessionId: string, content: string, context?: Record<string, unknown>): void {
    if (!this.client?.active) {
      console.error('WebSocket not connected')
      return
    }

    this.client.publish({
      destination: '/app/chat',
      body: JSON.stringify({
        sessionId,
        content,
        context,
      }),
    })
  }

  createSession(
    agentType: string,
    projectId?: string,
    customerId?: string,
    documentId?: string,
    context?: Record<string, unknown>
  ): void {
    if (!this.client?.active) {
      console.error('WebSocket not connected')
      return
    }

    this.client.publish({
      destination: '/app/session/create',
      body: JSON.stringify({
        agentType,
        projectId,
        customerId,
        documentId,
        context,
      }),
    })
  }

  onConnect(handler: ConnectionHandler): () => void {
    this.onConnectHandlers.push(handler)
    return () => {
      const index = this.onConnectHandlers.indexOf(handler)
      if (index > -1) this.onConnectHandlers.splice(index, 1)
    }
  }

  onDisconnect(handler: ConnectionHandler): () => void {
    this.onDisconnectHandlers.push(handler)
    return () => {
      const index = this.onDisconnectHandlers.indexOf(handler)
      if (index > -1) this.onDisconnectHandlers.splice(index, 1)
    }
  }

  isConnected(): boolean {
    return this.client?.active ?? false
  }
}

export const wsService = new WebSocketService()
export default wsService
