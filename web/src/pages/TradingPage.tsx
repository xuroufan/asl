import { useState, useEffect } from 'react'
import { useTradingStore } from '../store/tradingStore'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Card } from '../components/ui/Card'
import { Modal } from '../components/ui/Modal'
import { Badge } from '../components/ui/Badge'
import { ArrowLeftRight, Wallet, TrendingUp, GripHorizontal } from 'lucide-react'
import { account } from '../api/client'
import clsx from 'clsx'

const orderTypes = [
  { value: 'LIMIT' as const, label: '限价', desc: '指定价格' },
  { value: 'MARKET' as const, label: '市价', desc: '最优价格' },
  { value: 'STOP' as const, label: '止损', desc: '触发后市价' },
]

const leverageOptions = [1, 2, 5, 10, 20, 50, 100]

export function TradingPage() {
  const s = useTradingStore()
  const [balance, setBalance] = useState<number>(0)
  const [leverage, setLeverage] = useState(10)
  const [symbolSearch, setSymbolSearch] = useState('')

  useEffect(() => {
    account.balances().then(data => {
      const total = Array.isArray(data) ? data.reduce((sum, a) => sum + a.available + a.frozen, 0) : 0
      setBalance(total)
    }).catch(() => {})
  }, [])

  const estPrice = s.orderType === 'MARKET' ? s.currentPrice : parseFloat(s.price) || 0
  const estQty = parseFloat(s.quantity) || 0
  const estTotal = estPrice * estQty
  const marginRequired = leverage > 0 ? estTotal / leverage : 0

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-3">
        <ArrowLeftRight className="w-5 h-5 text-brand" />
        <h1 className="text-lg font-bold">下单交易</h1>
      </div>

      {/* Account balance bar */}
      <Card className="p-3 mb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Wallet className="w-4 h-4 text-brand" />
            <span className="text-xs text-gray-500">可用资金</span>
            <span className="text-sm font-bold text-white">${balance.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-[10px] text-gray-500">杠杆</div>
              <select value={leverage} onChange={e => setLeverage(Number(e.target.value))}
                className="bg-gray-800 border border-gray-700 rounded text-xs text-gray-300 px-1.5 py-0.5 focus:outline-none">
                {leverageOptions.map(l => <option key={l} value={l}>{l}x</option>)}
              </select>
            </div>
            <div className="text-right">
              <div className="text-[10px] text-gray-500">保证金</div>
              <div className="text-xs font-bold text-gray-300">${marginRequired.toFixed(2)}</div>
            </div>
          </div>
        </div>
        <div className="mt-2 h-1 bg-gray-800 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-brand to-blue-400 rounded-full transition-all duration-300"
            style={{ width: `${Math.min((marginRequired / (balance || 1)) * 100, 100)}%` }} />
        </div>
      </Card>

      {/* Price banner */}
      <Card className="p-3 mb-3">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">{s.symbol}</span>
            <span className="text-xl font-bold text-white">${s.currentPrice.toFixed(2)}</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-[10px] text-gray-500">预估算额</div>
              <div className="text-sm font-semibold text-gray-300">${estTotal.toFixed(2)}</div>
            </div>
          </div>
        </div>
      </Card>

      {/* Side selector - bigger with clear labels */}
      <div className="grid grid-cols-2 gap-3 mb-4">
        <button onClick={() => s.setSide('BUY')}
          className={clsx(
            'relative py-4 rounded-2xl font-bold text-base transition-all overflow-hidden',
            s.orderSide === 'BUY'
              ? 'bg-gradient-to-b from-[#FF6B6B] to-[#E55555] text-white shadow-lg shadow-[#FF6B6B]/20'
              : 'bg-gray-900 text-gray-400 hover:text-gray-200 border border-gray-800',
          )}>
          <span className="relative z-10 flex items-center justify-center gap-2">
            <TrendingUp className="w-5 h-5" />
            买入做多
          </span>
          {s.orderSide === 'BUY' && (
            <div className="absolute inset-0 bg-[url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")] opacity-30" />
          )}
        </button>
        <button onClick={() => s.setSide('SELL')}
          className={clsx(
            'relative py-4 rounded-2xl font-bold text-base transition-all overflow-hidden',
            s.orderSide === 'SELL'
              ? 'bg-gradient-to-b from-[#00C853] to-[#00A844] text-white shadow-lg shadow-[#00C853]/20'
              : 'bg-gray-900 text-gray-400 hover:text-gray-200 border border-gray-800',
          )}>
          <span className="relative z-10 flex items-center justify-center gap-2">
            <TrendingUp className="w-5 h-5 rotate-180" />
            卖出做空
          </span>
          {s.orderSide === 'SELL' && (
            <div className="absolute inset-0 bg-[url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")] opacity-30" />
          )}
        </button>
      </div>

      {/* Order type selector with descriptions */}
      <div className="flex gap-2 mb-4">
        {orderTypes.map(t => (
          <button key={t.value}
            onClick={() => s.setType(t.value)}
            className={clsx(
              'flex-1 py-2.5 rounded-xl text-xs font-medium transition-all text-center',
              t.value === s.orderType
                ? 'bg-gray-800 text-white border border-gray-700'
                : 'text-gray-500 bg-gray-900/50 hover:text-gray-300 border border-transparent',
            )}>
            <div>{t.label}</div>
            <div className={clsx('text-[9px] mt-0.5', t.value === s.orderType ? 'text-gray-400' : 'text-gray-600')}>{t.desc}</div>
          </button>
        ))}
      </div>

      {/* Input fields */}
      <div className="space-y-3 mb-4">
        {s.orderType !== 'MARKET' && (
          <Input label="价格 (USDT)" placeholder="输入价格" type="number"
            value={s.price} onChange={e => s.setPrice(e.target.value)} />
        )}
        <Input label="数量" placeholder="输入数量" type="number"
          value={s.quantity} onChange={e => s.setQuantity(e.target.value)} />
      </div>

      {/* Info summary */}
      {estQty > 0 && (
        <Card className="p-3 mb-3 space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">预估成交额</span>
            <span className="text-gray-300 font-medium">${estTotal.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">保证金 ({leverage}x)</span>
            <span className="text-gray-300 font-medium">${marginRequired.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">可用占比</span>
            <span className="text-gray-300 font-medium">
              {balance > 0 ? ((marginRequired / balance) * 100).toFixed(1) : 0}%
            </span>
          </div>
        </Card>
      )}

      {/* Messages */}
      {s.error && (
        <div className="text-sm text-[#FF6B6B] bg-[#FF6B6B]/8 rounded-xl px-3 py-2 mb-3">{s.error}</div>
      )}
      {s.successMessage && (
        <div className="text-sm text-[#00C853] bg-[#00C853]/8 rounded-xl px-3 py-2 mb-3">{s.successMessage}</div>
      )}

      {/* Submit button */}
      <Button className="w-full h-12 rounded-2xl text-base font-bold" disabled={!parseFloat(s.quantity) || parseFloat(s.quantity) <= 0}
        loading={s.isSubmitting} onClick={s.previewOrder}
        style={{
          background: s.orderSide === 'BUY'
            ? 'linear-gradient(135deg, #FF6B6B, #E55555)'
            : 'linear-gradient(135deg, #00C853, #00A844)',
        }}>
        {s.orderSide === 'BUY' ? '买入做多' : '卖出做空'} • {s.symbol}
      </Button>

      {/* Confirm modal */}
      <Modal open={s.showConfirm} onClose={s.dismissConfirm} title="确认下单"
        actions={
          <>
            <Button variant="ghost" onClick={s.dismissConfirm}>取消</Button>
            <Button variant={s.confirmOrder?.side === 'BUY' ? 'danger' : 'success'}
              onClick={s.confirmOrderAction} loading={s.isSubmitting}>
              确认{s.confirmOrder?.side === 'BUY' ? '买入' : '卖出'}
            </Button>
          </>
        }>
        {s.confirmOrder && (
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-500">方向</span>
              <span className={s.confirmOrder.side === 'BUY' ? 'text-[#FF6B6B] font-bold' : 'text-[#00C853] font-bold'}>
                {s.confirmOrder.side === 'BUY' ? '买入做多' : '卖出做空'}
              </span>
            </div>
            <div className="flex justify-between"><span className="text-gray-500">合约</span><span>{s.confirmOrder.symbol}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">类型</span><span>{s.confirmOrder.type === 'LIMIT' ? '限价' : s.confirmOrder.type === 'MARKET' ? '市价' : '止损'}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">价格</span><span>${s.confirmOrder.price.toFixed(2)}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">数量</span><span>{s.confirmOrder.quantity.toFixed(4)}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">杠杆</span><span>{leverage}x</span></div>
            <div className="border-t border-gray-800 pt-2 flex justify-between font-bold">
              <span>总金额</span>
              <span className={s.confirmOrder.side === 'BUY' ? 'text-[#FF6B6B]' : 'text-[#00C853]'}>
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
