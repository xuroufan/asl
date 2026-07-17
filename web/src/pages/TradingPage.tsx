import { useState } from 'react'
import { useTradingStore } from '../store/tradingStore'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Card } from '../components/ui/Card'
import { Modal } from '../components/ui/Modal'
import { Badge } from '../components/ui/Badge'
import { ArrowLeftRight } from 'lucide-react'
import clsx from 'clsx'

const orderTypes = [
  { value: 'LIMIT' as const, label: '限价' },
  { value: 'MARKET' as const, label: '市价' },
  { value: 'STOP' as const, label: '止损' },
]

export function TradingPage() {
  const s = useTradingStore()
  const [symbolSearch, setSymbolSearch] = useState('')

  const estPrice = s.orderType === 'MARKET' ? s.currentPrice : parseFloat(s.price) || 0
  const estQty = parseFloat(s.quantity) || 0
  const estTotal = estPrice * estQty

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-4">
        <ArrowLeftRight className="w-5 h-5 text-brand" />
        <h1 className="text-lg font-bold">交易</h1>
        <span className="text-sm text-gray-500 ml-auto">{s.symbol}</span>
      </div>

      {/* Price banner */}
      <Card className="p-4 mb-4">
        <div className="flex justify-between">
          <div>
            <div className="text-xs text-gray-500">最新价</div>
            <div className="text-2xl font-bold text-white">
              ${s.currentPrice.toFixed(2)}
            </div>
          </div>
          <div className="text-right">
            <div className="text-xs text-gray-500">预估成交额</div>
            <div className="text-base font-semibold text-gray-300">
              ${estTotal.toFixed(2)}
            </div>
          </div>
        </div>
      </Card>

      {/* Side selector */}
      <div className="grid grid-cols-2 gap-2 mb-4">
        <button
          onClick={() => s.setSide('BUY')}
          className={clsx(
            'py-3 rounded-xl font-bold text-sm transition-all',
            s.orderSide === 'BUY'
              ? 'bg-buy text-white shadow-lg shadow-buy/20'
              : 'bg-gray-900 text-gray-400 hover:text-gray-200',
          )}
        >
          买入
        </button>
        <button
          onClick={() => s.setSide('SELL')}
          className={clsx(
            'py-3 rounded-xl font-bold text-sm transition-all',
            s.orderSide === 'SELL'
              ? 'bg-sell text-white shadow-lg shadow-sell/20'
              : 'bg-gray-900 text-gray-400 hover:text-gray-200',
          )}
        >
          卖出
        </button>
      </div>

      {/* Type selector */}
      <div className="flex gap-1 mb-4">
        {orderTypes.map(t => (
          <button
            key={t.value}
            onClick={() => s.setType(t.value)}
            className={clsx(
              'flex-1 py-2 rounded-lg text-xs font-medium transition-colors',
              t.value === s.orderType
                ? 'bg-gray-800 text-white'
                : 'text-gray-500 hover:text-gray-300',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Input fields */}
      <div className="space-y-3 mb-4">
        {s.orderType !== 'MARKET' && (
          <Input
            label="价格 (USDT)"
            placeholder="输入价格"
            type="number"
            value={s.price}
            onChange={e => s.setPrice(e.target.value)}
          />
        )}
        <Input
          label="数量"
          placeholder="输入数量"
          type="number"
          value={s.quantity}
          onChange={e => s.setQuantity(e.target.value)}
        />
      </div>

      {/* Messages */}
      {s.error && (
        <div className="text-sm text-buy bg-buy/10 rounded-lg px-3 py-2 mb-3">{s.error}</div>
      )}
      {s.successMessage && (
        <div className="text-sm text-sell bg-sell/10 rounded-lg px-3 py-2 mb-3">{s.successMessage}</div>
      )}

      {/* Submit button */}
      <Button
        className="w-full"
        variant={s.orderSide === 'BUY' ? 'danger' : 'success'}
        size="lg"
        loading={s.isSubmitting}
        disabled={!parseFloat(s.quantity) || parseFloat(s.quantity) <= 0}
        onClick={s.previewOrder}
      >
        {s.orderSide === 'BUY' ? '买入' : '卖出'}
      </Button>

      {/* Confirm modal */}
      <Modal
        open={s.showConfirm}
        onClose={s.dismissConfirm}
        title="确认下单"
        actions={
          <>
            <Button variant="ghost" onClick={s.dismissConfirm}>取消</Button>
            <Button
              variant={s.confirmOrder?.side === 'BUY' ? 'danger' : 'success'}
              onClick={s.confirmOrderAction}
              loading={s.isSubmitting}
            >
              确认{s.confirmOrder?.side === 'BUY' ? '买入' : '卖出'}
            </Button>
          </>
        }
      >
        {s.confirmOrder && (
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-500">方向</span>
              <span className={s.confirmOrder.side === 'BUY' ? 'text-buy font-bold' : 'text-sell font-bold'}>
                {s.confirmOrder.side === 'BUY' ? '买入' : '卖出'}
              </span>
            </div>
            <div className="flex justify-between"><span className="text-gray-500">合约</span><span>{s.confirmOrder.symbol}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">类型</span><span>{s.confirmOrder.type}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">价格</span><span>${s.confirmOrder.price.toFixed(2)}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">数量</span><span>{s.confirmOrder.quantity.toFixed(4)}</span></div>
            <div className="border-t border-gray-800 pt-2 flex justify-between font-bold">
              <span>总金额</span>
              <span className={s.confirmOrder.side === 'BUY' ? 'text-buy' : 'text-sell'}>
                ${s.confirmOrder.total.toFixed(2)}
              </span>
            </div>
          </div>
        )}
      </Modal>

      {/* Submitting overlay */}
      {s.isSubmitting && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40">
          <div className="card p-6 flex items-center gap-3">
            <div className="animate-spin w-5 h-5 border-2 border-brand border-t-transparent rounded-full" />
            <span className="text-sm text-gray-300">提交中...</span>
          </div>
        </div>
      )}
    </div>
  )
}
