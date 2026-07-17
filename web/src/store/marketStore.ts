import { create } from 'zustand'
import type { MarketData, CandleData, CandleInterval, OrderBookLevel, MarketTrade } from '../types'
import { market } from '../api/client'
import { wsClient } from '../api/websocket'

interface MarketState {
  marketDataList: MarketData[]
  selectedSymbol: string
  selectedInterval: CandleInterval
  klineData: CandleData[]
  depthBids: OrderBookLevel[]
  depthAsks: OrderBookLevel[]
  recentTrades: MarketTrade[]
  isLoading: boolean
  isKlineLoading: boolean
  error: string | null
  load: () => Promise<void>
  selectSymbol: (s: string) => void
  selectInterval: (i: CandleInterval) => void
}

export const useMarketStore = create<MarketState>((set, get) => {
  // Subscribe to websocket events
  const unsubTicker = wsClient.on('@ticker', (data: any) => {
    const s = get()
    const updated = s.marketDataList.map(md =>
      md.symbol === data.symbol
        ? { ...md, price: data.price, change: data.change, changePercent: data.changePercent }
        : md
    )
    set({ marketDataList: updated })
  })

  const unsubDepth = wsClient.on('@depth', (data: any) => {
    set({
      depthBids: (data.bids || []).slice(0, 5),
      depthAsks: (data.asks || []).slice(0, 5),
    })
  })

  const unsubTrade = wsClient.on('@trade', (data: any) => {
    const trade: MarketTrade = {
      price: data.price,
      quantity: data.quantity,
      isBuyerMaker: data.isBuyerMaker,
      timestamp: data.timestamp,
    }
    set(s => ({ recentTrades: [trade, ...s.recentTrades].slice(0, 50) }))
  })

  return {
    marketDataList: [],
    selectedSymbol: 'BTCUSDT',
    selectedInterval: 'H1' as CandleInterval,
    klineData: [],
    depthBids: [],
    depthAsks: [],
    recentTrades: [],
    isLoading: false,
    isKlineLoading: false,
    error: null,

    load: async () => {
      set({ isLoading: true, error: null })
      try {
        const data = await market.getTickers()
        set({ marketDataList: data, isLoading: false })
        const sym = get().selectedSymbol
        wsClient.subscribe(`${sym.toLowerCase()}@ticker`)
        wsClient.subscribe(`${sym.toLowerCase()}@depth`)
        wsClient.subscribe(`${sym.toLowerCase()}@trade`)
        get().selectSymbol(sym)
      } catch (e: any) {
        set({ isLoading: false, error: e.message })
      }
    },

    selectSymbol: (symbol) => {
      const old = get().selectedSymbol
      if (old !== symbol) {
        wsClient.unsubscribe(`${old.toLowerCase()}@ticker`)
        wsClient.unsubscribe(`${old.toLowerCase()}@depth`)
        wsClient.unsubscribe(`${old.toLowerCase()}@trade`)
      }
      set({ selectedSymbol: symbol, klineData: [] })
      wsClient.subscribe(`${symbol.toLowerCase()}@ticker`)
      wsClient.subscribe(`${symbol.toLowerCase()}@depth`)
      wsClient.subscribe(`${symbol.toLowerCase()}@trade`)
      // Load kline
      set({ isKlineLoading: true })
      market.getCandles(symbol, get().selectedInterval).then(data => {
        set({ klineData: data.sort((a, b) => a.timestamp - b.timestamp), isKlineLoading: false })
      }).catch(() => set({ isKlineLoading: false }))
    },

    selectInterval: (interval) => {
      set({ selectedInterval: interval, isKlineLoading: true })
      const sym = get().selectedSymbol
      market.getCandles(sym, interval).then(data => {
        set({ klineData: data.sort((a, b) => a.timestamp - b.timestamp), isKlineLoading: false })
      }).catch(() => set({ isKlineLoading: false }))
    },
  }
})
