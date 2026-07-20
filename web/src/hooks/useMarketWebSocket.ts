import { useEffect, useRef } from 'react'
import { useMarketStore } from '../store/marketStore'
import type { MarketTrade } from '../types'

export function useMarketWebSocket() {
  const symbol = useMarketStore(s => s.selectedSymbol)
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectRef = useRef<ReturnType<typeof setTimeout>>()

  useEffect(() => {
    const connect = () => {
      try {
        const ws = new WebSocket(`ws://localhost:8093/ws/1`)
        
        ws.onopen = () => {
          ws.send(JSON.stringify({ action: 'subscribe', type: 'market', symbols: [symbol] }))
        }
        
        ws.onmessage = (e) => {
          try {
            const data = JSON.parse(e.data)
            const state = useMarketStore.getState()
            
            if (data.type === 'depth') {
              const bids = (data.bids || []).map((b: any) => ({
                price: Array.isArray(b) ? b[0] : b.price,
                quantity: Array.isArray(b) ? b[1] : b.volume
              }))
              const asks = (data.asks || []).map((a: any) => ({
                price: Array.isArray(a) ? a[0] : a.price,
                quantity: Array.isArray(a) ? a[1] : a.volume
              }))
              useMarketStore.setState({
                orderBook: {
                  bids, asks,
                  maxBidVol: Math.max(...bids.map(b => b.quantity), 1),
                  maxAskVol: Math.max(...asks.map(a => a.quantity), 1),
                }
              })
            } else if (data.type === 'trade') {
              const trade: MarketTrade = {
                price: data.price || 0,
                quantity: data.quantity || 0,
                timestamp: data.timestamp || Date.now(),
                isBuyerMaker: data.side === 'SELL',
              }
              useMarketStore.setState({
                recentTrades: [trade, ...state.recentTrades].slice(0, 50)
              })
            }
          } catch {}
        }
        
        ws.onclose = () => {
          reconnectRef.current = setTimeout(connect, 3000)
        }
        
        ws.onerror = () => ws.close()
        wsRef.current = ws
      } catch {}
    }
    
    connect()
    return () => {
      wsRef.current?.close()
      if (reconnectRef.current) clearTimeout(reconnectRef.current)
    }
  }, [symbol])
}
