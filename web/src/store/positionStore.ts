import { create } from 'zustand'
import type { Position, AccountBalance, CapitalSummary } from '../types'
import { positions, account } from '../api/client'
import { wsClient } from '../api/websocket'

interface PositionState {
  positions: Position[]
  balances: AccountBalance[]
  summary: CapitalSummary | null
  isLoading: boolean
  error: string | null
  load: () => Promise<void>
  closePosition: (id: string) => Promise<void>
}

function buildSummary(positions: Position[], balances: AccountBalance[]): CapitalSummary {
  const margin = positions.reduce((s, p) => s + p.marginUsed, 0)
  const pnl = positions.reduce((s, p) => s + p.unrealizedPnl, 0)
  const avail = balances.find(b => b.asset === 'USDT')?.available ?? 0
  const equity = avail + margin + pnl
  return {
    totalEquity: equity,
    available: avail,
    marginUsed: margin,
    unrealizedPnl: pnl,
    riskRatio: margin > 0 ? (equity / margin) * 100 : 100,
    positionCount: positions.length,
  }
}

export const usePositionStore = create<PositionState>((set, get) => {
  // Live prices
  const unsub = wsClient.on('@ticker', (data: any) => {
    const s = get()
    if (!data.symbol || !data.price) return
    const updated = s.positions.map(p => {
      if (p.symbol !== data.symbol) return p
      const pnl = (data.price - p.entryPrice) * p.quantity * (p.side === 'LONG' ? 1 : -1)
      const pnlPct = p.entryPrice > 0 ? (pnl / (p.entryPrice * p.quantity)) * 100 : 0
      return { ...p, currentPrice: data.price, unrealizedPnl: pnl, unrealizedPnlPercent: pnlPct }
    })
    set({
      positions: updated,
      summary: buildSummary(updated, s.balances),
    })
  })

  return {
    positions: [],
    balances: [],
    summary: null,
    isLoading: false,
    error: null,

    load: async () => {
      set({ isLoading: true, error: null })
      try {
        const [posList, bal] = await Promise.all([positions.list(), account.balances()])
        const summary = buildSummary(posList, bal)
        set({ positions: posList, balances: bal, summary, isLoading: false })
        // Subscribe to price channels
        for (const p of posList) {
          wsClient.subscribe(`${p.symbol.toLowerCase()}@ticker`)
        }
      } catch (e: any) {
        set({ isLoading: false, error: e.message })
      }
    },

    closePosition: async (id) => {
      const pos = get().positions.find(p => p.id === id)
      if (!pos) return
      try {
        await positions.close(id, pos.symbol)
        set(s => ({
          positions: s.positions.filter(p => p.id !== id),
          summary: buildSummary(
            s.positions.filter(p => p.id !== id),
            s.balances,
          ),
        }))
        wsClient.unsubscribe(`${pos.symbol.toLowerCase()}@ticker`)
      } catch (e: any) {
        set({ error: e.message })
      }
    },
  }
})
