import type { MarketTrade } from '../types'
import clsx from 'clsx'

interface TradeHistoryProps {
  trades: MarketTrade[]
}

export function TradeHistory({ trades }: TradeHistoryProps) {
  return (
    <div className="card p-3">
      <div className="section-title mb-2.5 px-1">成交记录</div>

      <div className="flex justify-between text-[10px] text-gray-600 mb-1.5 px-1 font-medium">
        <span className="flex-1">价格</span>
        <span className="flex-1 text-center">数量</span>
        <span className="flex-1 text-right">时间</span>
      </div>

      <div className="space-y-[1px] max-h-48 overflow-y-auto">
        {trades.slice(0, 30).map((t, i) => (
          <div
            key={`${t.timestamp}-${i}`}
            className="flex justify-between text-[11px] py-[1px] px-1 rounded-sm transition-colors hover:bg-white/[0.02]"
          >
            <span className={clsx('flex-1 font-mono', t.isBuyerMaker ? 'text-down' : 'text-up')}>
              {t.price.toFixed(2)}
            </span>
            <span className="flex-1 text-center text-gray-500 font-mono">{t.quantity.toFixed(4)}</span>
            <span className="flex-1 text-right text-gray-600 font-mono">
              {new Date(t.timestamp).toLocaleTimeString('zh-CN', {
                hour: '2-digit', minute: '2-digit', second: '2-digit',
              })}
            </span>
          </div>
        ))}
        {trades.length === 0 && (
          <div className="text-center text-gray-700 py-6 text-xs font-mono">暂无成交</div>
        )}
      </div>
    </div>
  )
}
