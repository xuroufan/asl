import { useEffect, useState } from 'react'
import { usePositionStore } from '../store/positionStore'
import { PositionCard } from '../components/PositionCard'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { Wallet, TrendingUp, TrendingDown, AlertCircle } from 'lucide-react'
import clsx from 'clsx'

export function PositionsPage() {
  const { positions, summary, isLoading, error, load, closePosition } = usePositionStore()
  const [closingId, setClosingId] = useState<string | null>(null)

  useEffect(() => { load() }, [])

  const handleClose = async () => {
    if (closingId) {
      await closePosition(closingId)
      setClosingId(null)
    }
  }

  const totalPnl = summary?.unrealizedPnl ?? 0
  const totalEquity = summary?.totalEquity ?? 0
  const pnlPercent = totalEquity > 0 ? (totalPnl / totalEquity) * 100 : 0

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      <div className="flex items-center gap-2 mb-3">
        <Wallet className="w-5 h-5 text-brand" />
        <h1 className="text-lg font-bold">我的持仓</h1>
        {(summary?.positionCount ?? 0) > 0 && (
          <span className="text-xs text-gray-500 ml-auto">{summary?.positionCount} 笔持仓</span>
        )}
      </div>

      {error && (
        <div className="text-sm text-[#FF6B6B] bg-[#FF6B6B]/10 rounded-xl px-3 py-2 mb-3 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {/* Capital overview */}
      {summary && (
        <Card className="p-4 mb-4">
          <div className="flex justify-between items-start mb-3">
            <div>
              <div className="text-xs text-gray-500 mb-1">总权益</div>
              <div className="text-2xl font-bold text-white">${totalEquity.toFixed(2)}</div>
            </div>
            <div className="text-right">
              <div className="text-xs text-gray-500 mb-1">未实现盈亏</div>
              <div className={clsx('text-lg font-bold flex items-center gap-1', totalPnl >= 0 ? 'text-[#FF6B6B]' : 'text-[#00C853]')}>
                {totalPnl >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                {totalPnl >= 0 ? '+' : ''}{totalPnl.toFixed(2)}
              </div>
              <div className={clsx('text-[11px] font-medium', totalPnl >= 0 ? 'text-[#FF6B6B]' : 'text-[#00C853]')}>
                ({totalPnl >= 0 ? '+' : ''}{pnlPercent.toFixed(2)}%)
              </div>
            </div>
          </div>

          {/* PnL progress bar */}
          <div className="h-2 bg-gray-800 rounded-full overflow-hidden mb-3">
            <div className={clsx('h-full rounded-full transition-all duration-500', totalPnl >= 0 ? 'bg-gradient-to-r from-[#FF6B6B]/60 to-[#FF6B6B]' : 'bg-gradient-to-r from-[#00C853] to-[#00C853]/60')}
              style={{ width: `${Math.min(Math.abs(pnlPercent) * 5, 100)}%`, marginLeft: totalPnl < 0 ? 'auto' : '0' }} />
          </div>

          <div className="grid grid-cols-4 gap-2 text-center pt-3 border-t border-gray-800/50">
            <div>
              <div className="text-[10px] text-gray-500">可用</div>
              <div className="text-sm font-bold text-white">${summary.available.toFixed(2)}</div>
            </div>
            <div>
              <div className="text-[10px] text-gray-500">保证金</div>
              <div className="text-sm font-bold text-white">${summary.marginUsed.toFixed(2)}</div>
            </div>
            <div>
              <div className="text-[10px] text-gray-500">风险度</div>
              <div className={clsx('text-sm font-bold', summary.riskRatio > 80 ? 'text-[#FF6B6B]' : summary.riskRatio > 60 ? 'text-yellow-400' : 'text-white')}>
                {summary.riskRatio.toFixed(1)}%
              </div>
            </div>
            <div>
              <div className="text-[10px] text-gray-500">持仓数</div>
              <div className="text-sm font-bold text-white">{summary.positionCount}</div>
            </div>
          </div>

          {/* Risk gauge */}
          <div className="mt-3 pt-3 border-t border-gray-800/50">
            <div className="flex justify-between text-[10px] text-gray-600 mb-1">
              <span>风险低</span>
              <span>风险高</span>
            </div>
            <div className="h-1.5 rounded-full overflow-hidden flex">
              <div className="flex-1 bg-gradient-to-r from-[#00C853] via-yellow-400 to-[#FF6B6B]" />
            </div>
            <div className="relative h-0">
              <div className="absolute -top-2 transition-all duration-300"
                style={{ left: `${Math.min(summary.riskRatio, 100)}%`, transform: 'translateX(-50%)' }}>
                <div className="w-2 h-2 bg-white rounded-full shadow-[0_0_6px_rgba(255,255,255,0.5)]" />
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Loading */}
      {isLoading ? (
        <div className="flex items-center justify-center h-32">
          <div className="animate-spin w-6 h-6 border-2 border-brand border-t-transparent rounded-full" />
        </div>
      ) : positions.length === 0 ? (
        <div className="text-center py-16">
          <Wallet className="w-14 h-14 mx-auto mb-4 text-gray-700" />
          <div className="text-base font-medium text-gray-600 mb-1">暂无持仓</div>
          <div className="text-xs text-gray-700">去行情页选择合约开始交易</div>
        </div>
      ) : (
        <div className="space-y-3">
          {positions.map(p => (
            <PositionCard key={p.id} position={p} onClose={() => setClosingId(p.id)} />
          ))}
        </div>
      )}

      {/* Close confirm modal */}
      <Modal open={closingId !== null} onClose={() => setClosingId(null)} title="确认平仓"
        actions={
          <>
            <Button variant="ghost" onClick={() => setClosingId(null)}>取消</Button>
            <Button variant="success" onClick={handleClose}>确认平仓</Button>
          </>
        }>
        <p className="text-gray-300">确定平掉该持仓？</p>
      </Modal>
    </div>
  )
}
