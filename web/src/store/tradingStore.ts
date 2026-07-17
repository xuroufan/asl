import { create } from 'zustand'
import type { OrderSide, OrderType, Order, PendingOrder } from '../types'
import { orders } from '../api/client'
import { wsClient } from '../api/websocket'

interface TradingState {
  symbol: string
  orderSide: OrderSide
  orderType: OrderType
  price: string
  quantity: string
  currentPrice: number
  isSubmitting: boolean
  showConfirm: boolean
  confirmOrder: PendingOrder | null
  error: string | null
  successMessage: string | null
  lastOrder: Order | null
  recentOrders: Order[]
  setSymbol: (s: string) => void
  setSide: (s: OrderSide) => void
  setType: (t: OrderType) => void
  setPrice: (p: string) => void
  setQuantity: (q: string) => void
  previewOrder: () => void
  confirmOrderAction: () => Promise<void>
  dismissConfirm: () => void
  clearMessage: () => void
}

export const useTradingStore = create<TradingState>((set, get) => {
  // Subscribe to price updates
  const unsub = wsClient.on('@ticker', (data: any) => {
    if (data.symbol === get().symbol) {
      set({ currentPrice: data.price })
    }
  })

  return {
    symbol: 'BTCUSDT',
    orderSide: 'BUY',
    orderType: 'LIMIT',
    price: '',
    quantity: '',
    currentPrice: 0,
    isSubmitting: false,
    showConfirm: false,
    confirmOrder: null,
    error: null,
    successMessage: null,
    lastOrder: null,
    recentOrders: [],

    setSymbol: (symbol) => {
      const old = get().symbol
      if (old !== symbol) wsClient.unsubscribe(`${old.toLowerCase()}@ticker`)
      wsClient.subscribe(`${symbol.toLowerCase()}@ticker`)
      set({ symbol, price: '', quantity: '', error: null, successMessage: null })
    },

    setSide: (side) => set({ orderSide: side }),
    setType: (type) => set({ orderType: type }),
    setPrice: (price) => set({ price }),
    setQuantity: (quantity) => set({ quantity }),

    previewOrder: () => {
      const s = get()
      const price = s.orderType === 'MARKET' ? s.currentPrice : parseFloat(s.price)
      const qty = parseFloat(s.quantity)
      if (!price || price <= 0 || !qty || qty <= 0) {
        set({ error: '请输入有效的价格和数量' })
        return
      }
      set({
        showConfirm: true,
        confirmOrder: {
          symbol: s.symbol, side: s.orderSide, type: s.orderType,
          price, quantity: qty, total: price * qty,
        },
      })
    },

    confirmOrderAction: async () => {
      const s = get()
      const price = s.orderType === 'MARKET' ? s.currentPrice : parseFloat(s.price)
      const qty = parseFloat(s.quantity)
      set({ isSubmitting: true, showConfirm: false, error: null })
      try {
        const order = await orders.place({
          symbol: s.symbol, type: s.orderType, side: s.orderSide,
          price: s.orderType !== 'MARKET' ? price : null,
          quantity: qty,
        })
        set({
          isSubmitting: false, price: '', quantity: '',
          confirmOrder: null, lastOrder: order,
          successMessage: `订单已提交: ${order.id.slice(0, 8)}...`,
        })
      } catch (e: any) {
        set({ isSubmitting: false, error: e.message || '下单失败' })
      }
    },

    dismissConfirm: () => set({ showConfirm: false, confirmOrder: null }),
    clearMessage: () => set({ error: null, successMessage: null }),
  }
})
