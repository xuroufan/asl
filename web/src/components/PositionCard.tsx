import type { Position } from '../types'
import { Button } from './ui/Button'
import clsx from 'clsx'

interface PositionCardProps {
  position: Position
  onClose: () => void
}

export function PositionCard({ position, onClose }: PositionCardProps) {
  const isLong = position.side === 'LONG'
  const pnlColor = position.unrealizedPnl >= 0 ? 'text-buy' : 'text-sell'

  return (
    <div className="card p-4">
      <div className="flex justify-between items-start mb-3">
        <div className="flex items-center gap-2">
          <span className="font-bold text-base">{position.symbol}</span>
          <span className={clsx(
            'text-xs px-1.5 py-0.5 rounded font-medium',
            isLong ? 'text-buy bg-buy/10' : 'text-sell bg-sell/10',
          )}>
            {isLong ? '做多' : '做空'}
          </span>
        </div>
        <span className="text-xs text-gray-500">{position.leverage}x</span>
      </div>

      <div className="grid grid-cols-3 gap-3 text-sm mb-3">
        <div>
          <div className="text-xs text-gray-500">手数</div>
          <div className="font-medium">{position.quantity.toFixed(4)}</div>
        </div>
        <div>
          <div className="text-xs text-gray-500">开仓均价</div>
          <div className="font-medium">{position.entryPrice.toFixed(2)}</div>
        </div>
        <div>
          <div className="text-xs text-gray-500">最新价</div>
          <div className="font-medium">{position.currentPrice.toFixed(2)}</div>
        </div>
      </div>

      <div className={clsx('text-sm mb-4', pnlColor)}>
        <div className="text-xs text-gray-500 mb-0.5">浮动盈亏</div>
        <div className="font-semibold">
          {position.unrealizedPnl >= 0 ? '+' : ''}
          {position.unrealizedPnl.toFixed(2)}
          <span className="ml-2 text-xs">
            ({position.unrealizedPnlPercent >= 0 ? '+' : ''}
            {position.unrealizedPnlPercent.toFixed(2)}%)
          </span>
        </div>
      </div>

      <Button variant="success" size="sm" className="w-full" onClick={onClose}>
        平仓
      </Button>
    </div>
  )
}
