import { useEffect, useState } from 'react'
import { usePositionStore } from '../store/positionStore'
import { PositionCard } from '../components/PositionCard'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { Wallet } from 'lucide-react'
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

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      <div className="flex items-center gap-2 mb-4">
        <Wallet className="w-5 h-5 text-brand" />
        <h1 className="text-lg font-bold">持仓</h1>
      </div>

      {error && (
        <div className="text-sm text-buy bg-buy/10 rounded-lg px-3 py-2 mb-3">{error}</div>
      )}

      {/* Capital overview */}
      {summary && (
        <Card className="p-4 mb-4">
          <div className="flex justify-between mb-3">
            <div>
              <div className="text-xs text-gray-500">总权益</div>
              <div className="text-xl font-bold text-white">${summary.totalEquity.toFixed(2)}</div>
            </div>
            <div className="text-right">
              <div className="text-xs text-gray-500">未实现盈亏</div>
              <div className={clsx(
                'text-base font-bold',
                summary.unrealizedPnl >= 0 ? 'text-buy' : 'text-sell',
              )}>
                {summary.unrealizedPnl >= 0 ? '+' : ''}{summary.unrealizedPnl.toFixed(2)}
              </div>
            </div>
          </div>
          <div className="grid grid-cols-4 gap-2 text-center border-t border-gray-800 pt-3">
            <div>
              <div className="text-xs text-gray-500">可用</div>
              <div className="text-sm font-medium">{summary.available.toFixed(2)}</div>
            </div>
            <div>
              <div className="text-xs text-gray-500">保证金</div>
              <div className="text-sm font-medium">{summary.marginUsed.toFixed(2)}</div>
            </div>
            <div>
              <div className="text-xs text-gray-500">风险度</div>
              <div className={clsx(
                'text-sm font-medium',
                summary.riskRatio > 80 ? 'text-buy' : summary.riskRatio > 60 ? 'text-yellow-400' : 'text-gray-300',
              )}>
                {summary.riskRatio.toFixed(1)}%
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-500">持仓数</div>
              <div className="text-sm font-medium">{summary.positionCount}</div>
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
        <div className="text-center text-gray-600 py-16">
          <Wallet className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <div className="text-sm">暂无持仓</div>
        </div>
      ) : (
        <div className="space-y-3">
          {positions.map(p => (
            <PositionCard
              key={p.id}
              position={p}
              onClose={() => setClosingId(p.id)}
            />
          ))}
        </div>
      )}

      {/* Close confirm modal */}
      <Modal
        open={closingId !== null}
        onClose={() => setClosingId(null)}
        title="确认平仓"
        actions={
          <>
            <Button variant="ghost" onClick={() => setClosingId(null)}>取消</Button>
            <Button variant="success" onClick={handleClose}>确认平仓</Button>
          </>
        }
      >
        <p className="text-gray-300">确定平掉该持仓？</p>
      </Modal>
    </div>
  )
}
