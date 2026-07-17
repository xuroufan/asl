type WSCallback = (data: any) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string = ""
  private reconnectAttempt = 0
  private maxRetries = 10
  private listeners = new Map<string, Set<WSCallback>>()
  private token: string | null = null
  private subscriptions = new Set<string>()

  connect(token: string) {
    this.token = token
    this.url = `wss://ws.hackfuture.com/ws`
    this.doConnect()
  }

  private doConnect() {
    if (!this.token) return
    this.ws = new WebSocket(this.url)
    this.ws.onopen = () => {
      this.reconnectAttempt = 0
      this.emit('connected', null)
      // Resubscribe
      for (const ch of this.subscriptions) {
        this.ws?.send(JSON.stringify({ type: 'subscribe', channel: ch }))
      }
    }
    this.ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        if (msg.channel) this.emit(msg.channel, msg.data)
        this.emit('message', msg)
      } catch { /* ignore */ }
    }
    this.ws.onclose = () => {
      this.emit('disconnected', null)
      this.scheduleReconnect()
    }
    this.ws.onerror = () => this.ws?.close()
  }

  private scheduleReconnect() {
    if (this.reconnectAttempt >= this.maxRetries) return
    this.reconnectAttempt++
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempt - 1), 30000)
    setTimeout(() => this.doConnect(), delay)
  }

  subscribe(channel: string) {
    this.subscriptions.add(channel)
    this.ws?.send(JSON.stringify({ type: 'subscribe', channel }))
  }

  unsubscribe(channel: string) {
    this.subscriptions.delete(channel)
    this.ws?.send(JSON.stringify({ type: 'unsubscribe', channel }))
  }

  on(event: string, cb: WSCallback) {
    if (!this.listeners.has(event)) this.listeners.set(event, new Set())
    this.listeners.get(event)!.add(cb)
    return () => this.listeners.get(event)?.delete(cb)
  }

  private emit(event: string, data: any) {
    this.listeners.get(event)?.forEach(cb => cb(data))
  }

  disconnect() {
    this.ws?.close()
    this.ws = null
    this.listeners.clear()
    this.subscriptions.clear()
  }
}

export const wsClient = new WebSocketClient()
