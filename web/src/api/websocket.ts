
let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let isConnected = false

// ==================== Event listener system ====================
type WSCallback = (data: any) => void
const listeners = new Map<string, Set<WSCallback>>()

function emit(event: string, data: any) {
  listeners.get(event)?.forEach(cb => cb(data))
}
// ===============================================================
 
export function connectWebSocket(userId?: number) {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
  
  const uid = userId || 1
  const url = `ws://localhost:8088/ws/${uid}`
  
  try {
    ws = new WebSocket(url)
  } catch (e) {
    console.warn('WS connect failed, will retry:', e)
    scheduleReconnect(uid)
    return
  }
  
  ws.onopen = () => {
    isConnected = true
    console.log('✅ WS connected:', url)
  }
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      emit('message', data)
      if (data.channel) emit(data.channel, data.data || data)
      if (data.type === 'ticker' || data.channel?.endsWith('@ticker')) emit('@ticker', data)
      if (data.type === 'quote') emit('@ticker', data)
      if (data.type === 'trade' || data.channel?.endsWith('@trade')) emit('@trade', data)
      if (data.type === 'depth' || data.channel?.endsWith('@depth')) emit('@depth', data)
    } catch {}
  }
  
  ws.onclose = () => {
    isConnected = false
    scheduleReconnect(uid)
  }
  
  ws.onerror = () => {
    ws?.close()
  }
}

function scheduleReconnect(userId: number) {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(() => connectWebSocket(userId), 5000)
}

export function disconnectWebSocket() {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) { ws.close(); ws = null }
  isConnected = false
}

export function getWSStatus() { return isConnected }

// ==================== wsClient (stores compatibility) ====================
export const wsClient = {
  on(event: string, cb: WSCallback) {
    if (!listeners.has(event)) listeners.set(event, new Set())
    listeners.get(event)!.add(cb)
    return () => listeners.get(event)?.delete(cb)
  },
  subscribe(channel: string) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'subscribe', channel }))
    }
  },
  unsubscribe(channel: string) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'unsubscribe', channel }))
    }
  },
}
