import type { OrderBookLevel } from '../types'
import clsx from 'clsx'

interface OrderBookProps {
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
  maxBidVol: number
  maxAskVol: number
  currentPrice: number
}

export function OrderBook({ bids, asks, maxBidVol, maxAskVol, currentPrice }: OrderBookProps) {
  return (
    <div className="card p-3">
      <div className="section-title mb-2.5 px-1">盘口深度</div>

      {/* Header */}
      <div className="flex justify-between text-[10px] text-gray-600 mb-1.5 px-1">
        <span>价格</span>
        <span>数量</span>
      </div>

      {/* Asks (sells) */}
      <div className="space-y-[1px] mb-2">
        {[...asks].reverse().map((a, i) => {
          const pct = (a.quantity / maxAskVol) * 100
          return (
            <div key={`a-${i}`} className="relative flex justify-between text-[11px] py-[1px] px-1">
              {/* Volume bar */}
              <div
                className="absolute right-0 top-0 bottom-0 rounded-sm opacity-20"
                style={{ width: `${pct}%`, background: 'linear-gradient(270deg, #00C853, transparent)' }}
              />
              <span className={clsx('relative z-10 font-mono', a.price > currentPrice ? 'text-down' : 'text-gray-400')}>
                {a.price.toFixed(2)}
              </span>
              <span className="relative z-10 text-gray-500 font-mono">{a.quantity.toFixed(4)}</span>
            </div>
          )
        })}
      </div>

      {/* Current price */}
      <div className="flex justify-center py-2 border-y border-gray-800/60 -mx-1 mb-2">
        <span className={clsx(
          'text-lg font-bold font-mono tracking-tight',
          currentPrice > 0 ? 'text-white' : 'text-gray-500',
        )}>
          {currentPrice > 0 ? currentPrice.toFixed(2) : '--'}
        </span>
      </div>

      {/* Bids (buys) */}
      <div className="space-y-[1px]">
        {bids.map((b, i) => {
          const pct = (b.quantity / maxBidVol) * 100
          return (
            <div key={`b-${i}`} className="relative flex justify-between text-[11px] py-[1px] px-1">
              <div
                className="absolute right-0 top-0 bottom-0 rounded-sm opacity-20"
                style={{ width: `${pct}%`, background: 'linear-gradient(270deg, #FF6B6B, transparent)' }}
              />
              <span className={clsx('relative z-10 font-mono', b.price < currentPrice ? 'text-up' : 'text-gray-400')}>
                {b.price.toFixed(2)}
              </span>
              <span className="relative z-10 text-gray-500 font-mono">{b.quantity.toFixed(4)}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
