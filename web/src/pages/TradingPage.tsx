import { useState, useEffect } from 'react'
import { useTradingStore } from '../store/tradingStore'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Card } from '../components/ui/Card'
import { Modal } from '../components/ui/Modal'
import { Badge } from '../components/ui/Badge'
import { ArrowLeftRight, Wallet, TrendingUp, GripHorizontal, Zap, DollarSign, Percent } from 'lucide-react'
import { account } from '../api/client'
import clsx from 'clsx'

const orderTypes = [
  { value: 'LIMIT' as const, label: '限价', desc: '指定价格' },
  { value: 'MARKET' as const, label: '市价', desc: '最优价格' },
  { value: 'STOP' as const, label: '止损', desc: '触发后市价' },
]

const quickAmounts = [0.1, 0.25, 0.5, 0.75, 1]
const leverageOptions = [1, 2, 5, 10, 20, 50, 100]

export function TradingPage() {
  const s = useTradingStore()
  const [balance, setBalance] = useState<number>(0)
  const [leverage, setLeverage] = useState(10)
  const [activeTab, setActiveTab] = useState<'buy' | 'sell'>(s.orderSide === 'BUY' ? 'buy' : 'sell')

  useEffect(() => {
    account.balances().then(data => {
      const total = Array.isArray(data) ? data.reduce((sum, a) => sum + a.available + a.frozen, 0) : 0
      setBalance(total)
    }).catch(() => {})
  }, [])

  const handleTabSwitch = (tab: 'buy' | 'sell') => {
    setActiveTab(tab)
    s.setSide(tab === 'buy' ? 'BUY' : 'SELL')
  }

  const estPrice = s.orderType === 'MARKET' ? s.currentPrice : parseFloat(s.price) || 0
  const estQty = parseFloat(s.quantity) || 0
  const estTotal = estPrice * estQty
  const marginRequired = leverage > 0 ? estTotal / leverage : 0
  const fee = estTotal * 0.0005
  const pnlIfUp = estTotal * 0.01 * leverage

  return (
    <div className="max-w-lg mx-auto px-3 pt-3 pb-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-3">
        <div className="w-8 h-8 rounded-xl bg-brand/10 flex items-center justify-center">
          <ArrowLeftRight className="w-4 h-4 text-brand" />
        </div>
        <div>
          <h1 className="text-base font-bold">下单交易</h1>
          <div className="text-[10px] text-gray-600">{s.symbol} · 永续合约</div>
        </div>
      </div>

      {/* Balance bar */}
      <Card className="p-3 mb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-brand/10 flex items-center justify-center">
              <Wallet className="w-3.5 h-3.5 text-brand" />
            </div>
            <div>
              <div className="text-[10px] text-gray-600">可用保证金</div>
              <div className="text-sm font-bold font-mono text-white">
                ${balance.toLocaleString(undefined, {minimumFractionDigits: 2})}
              </div>
            </div>
          </div>
          <div className="text-right">
            <div className="text-[10px] text-gray-600">杠杆倍数</div>
            <select
              value={leverage}
              onChange={e => setLeverage(Number(e.target.value))}
              className="mt-0.5 bg-gray-800 border border-gray-700 rounded-lg text-xs text-gray-300 px-2 py-1 focus:outline-none focus:border-brand/40"
            >
              {leverageOptions.map(l => (
                <option key={l} value={l}>{l}x</option>
              ))}
            </select>
          </div>
        </div>
      </Card>

      {/* Buy/Sell tabs */}
      <div className="flex rounded-xl overflow-hidden mb-4 border border-gray-800/40">
        <button
          onClick={() => handleTabSwitch('buy')}
          className={clsx(
            'flex-1 py-2.5 text-sm font-bold transition-all relative',
            activeTab === 'buy'
              ? 'text-white'
              : 'text-gray-600 hover:text-gray-400 bg-transparent',
          )}
          style={{
            background: activeTab === 'buy'
              ? 'linear-gradient(135deg, rgba(255,107,107,0.15), rgba(255,107,107,0.05))'
              : 'transparent',
          }}
        >
          {activeTab === 'buy' && (
            <span className="absolute inset-0 border border-buy/30 rounded-xl" />
          )}
          <span className="relative z-10 flex items-center justify-center gap-1.5">
            <TrendingUp className="w-4 h-4" /> 买入做多
          </span>
        </button>
        <div className="w-px bg-gray-800/40 my-1" />
        <button
          onClick={() => handleTabSwitch('sell')}
          className={clsx(
            'flex-1 py-2.5 text-sm font-bold transition-all relative',
            activeTab === 'sell'
              ? 'text-white'
              : 'text-gray-600 hover:text-gray-400 bg-transparent',
          )}
          style={{
            background: activeTab === 'sell'
              ? 'linear-gradient(135deg, rgba(0,200,83,0.15), rgba(0,200,83,0.05))'
              : 'transparent',
          }}
        >
          {activeTab === 'sell' && (
            <span className="absolute inset-0 border border-sell/30 rounded-xl" />
          )}
          <span className="relative z-10 flex items-center justify-center gap-1.5">
            <TrendingUp className="w-4 h-4 rotate-180" /> 卖出做空
          </span>
        </button>
      </div>

      {/* Order type */}
      <div className="flex gap-2 mb-4">
        {orderTypes.map(t => (
          <button
            key={t.value}
            onClick={() => s.setType(t.value)}
            className={clsx(
              'flex-1 py-2.5 rounded-xl text-xs font-medium transition-all text-center',
              t.value === s.orderType
                ? 'bg-surface border border-gray-700 text-white'
                : 'text-gray-500 bg-transparent hover:text-gray-300 border border-transparent',
            )}
          >
            <div>{t.label}</div>
            <div className={clsx('text-[9px] mt-0.5', t.value === s.orderType ? 'text-gray-500' : 'text-gray-700')}>{t.desc}</div>
          </button>
        ))}
      </div>

      {/* Order form */}
      <div className="space-y-3 mb-4">
        {s.orderType !== 'MARKET' && (
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs text-gray-500">价格 (USDT)</label>
              {s.currentPrice > 0 && (
                <button
                  onClick={() => s.setPrice(s.currentPrice.toString())}
                  className="text-[10px] text-brand hover:text-brand-light transition-colors"
                >
                  最新价 ${s.currentPrice.toFixed(2)}
                </button>
              )}
            </div>
            <div className="relative">
              <input
                type="number"
                placeholder="输入价格"
                value={s.price}
                onChange={e => s.setPrice(e.target.value)}
                className="w-full bg-ink-lighter border border-gray-800 rounded-xl pl-3.5 pr-8 py-2.5 text-sm text-gray-100 placeholder:text-gray-700 focus:outline-none focus:border-brand/30 transition-colors font-mono"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-600">USDT</span>
            </div>
          </div>
        )}

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="text-xs text-gray-500">数量</label>
            <div className="flex gap-1">
              {quickAmounts.map(amt => (
                <button
                  key={amt}
                  onClick={() => s.setQuantity((amt * (balance * leverage / estPrice || 1)).toFixed(4))}
                  className="px-2 py-0.5 rounded text-[9px] text-gray-600 hover:text-gray-400 bg-gray-800/50 hover:bg-gray-700/50 transition-colors"
                >
                  {amt * 100}%
                </button>
              ))}
            </div>
          </div>
          <div className="relative">
            <input
              type="number"
              placeholder="输入数量"
              value={s.quantity}
              onChange={e => s.setQuantity(e.target.value)}
              className="w-full bg-ink-lighter border border-gray-800 rounded-xl pl-3.5 pr-8 py-2.5 text-sm text-gray-100 placeholder:text-gray-700 focus:outline-none focus:border-brand/30 transition-colors font-mono"
            />
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-600">张</span>
          </div>
        </div>
      </div>

      {/* Info summary */}
      {estQty > 0 && (
        <Card className="p-3 mb-4 animate-fade-in">
          <div className="space-y-1.5">
            <div className="flex justify-between items-center">
              <span className="data-label">预估成交额</span>
              <span className="data-value">${estTotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="data-label">保证金 ({leverage}x)</span>
              <span className="data-value text-brand">${marginRequired.toFixed(2)}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="data-label">手续费</span>
              <span className="data-value text-gray-500">${fee.toFixed(2)}</span>
            </div>
            <div className="border-t border-gray-800/40 pt-1.5 flex justify-between items-center">
              <span className="data-label">涨1%盈亏</span>
              <span className={clsx('data-value font-bold', pnlIfUp >= 0 ? 'text-buy' : 'text-down')}>
                ${pnlIfUp.toFixed(2)} ({leverage}x)
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="data-label">可用占比</span>
              <span className={clsx('data-value', marginRequired / balance > 0.8 ? 'text-buy' : marginRequired / balance > 0.5 ? 'text-yellow-500' : 'text-gray-400')}>
                {balance > 0 ? ((marginRequired / balance) * 100).toFixed(1) : 0}%
              </span>
            </div>
          </div>
        </Card>
      )}

      {/* Messages */}
      {s.error && (
        <div className="flex items-center gap-2 text-xs text-buy bg-buy/8 rounded-xl px-3 py-2 mb-3 border border-buy/10">
          <Zap className="w-3.5 h-3.5 shrink-0" />
          {s.error}
        </div>
      )}
      {s.successMessage && (
        <div className="flex items-center gap-2 text-xs text-sell bg-sell/8 rounded-xl px-3 py-2 mb-3 border border-sell/10">
          <DollarSign className="w-3.5 h-3.5 shrink-0" />
          {s.successMessage}
        </div>
      )}

      {/* Submit button */}
      <button
        disabled={!parseFloat(s.quantity) || parseFloat(s.quantity) <= 0}
        onClick={s.previewOrder}
        className={clsx(
          'relative w-full h-12 rounded-2xl text-sm font-bold transition-all duration-200 overflow-hidden',
          'disabled:opacity-40 disabled:cursor-not-allowed',
          'hover:scale-[1.01] active:scale-[0.99]',
        )}
        style={{
          background: activeTab === 'buy'
            ? 'linear-gradient(135deg, #FF6B6B, #E55555)'
            : 'linear-gradient(135deg, #00C853, #00A844)',
        }}
      >
        {/* Hover shine effect */}
        <span className="absolute inset-0 bg-white/5 opacity-0 hover:opacity-100 transition-opacity" />
        <span className="relative flex items-center justify-center gap-2">
          {activeTab === 'buy' ? '买入做多' : '卖出做空'}
          <span className="text-xs opacity-80">{s.symbol}</span>
        </span>
      </button>

      {/* Confirm modal */}
      <Modal open={s.showConfirm} onClose={s.dismissConfirm} title="确认下单"
        actions={
          <div className="flex gap-2 w-full">
            <button
              onClick={s.dismissConfirm}
              className="flex-1 py-2.5 rounded-xl text-sm font-medium text-gray-400 bg-gray-800 hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              onClick={s.confirmOrderAction}
              disabled={s.isSubmitting}
              className={clsx(
                'flex-1 py-2.5 rounded-xl text-sm font-bold text-white transition-all',
                s.confirmOrder?.side === 'BUY'
                  ? 'bg-[#FF6B6B] hover:bg-[#E55555]'
                  : 'bg-[#00C853] hover:bg-[#00A844]',
              )}
            >
              {s.isSubmitting ? '提交中...' : `确认${s.confirmOrder?.side === 'BUY' ? '买入' : '卖出'}`}
            </button>
          </div>
        }>
        {s.confirmOrder && (
          <div className="space-y-2">
            {[
              ['方向', s.confirmOrder.side === 'BUY' ? '买入做多' : '卖出做空',
                s.confirmOrder.side === 'BUY' ? 'text-buy' : 'text-sell'],
              ['合约', s.confirmOrder.symbol, ''],
              ['类型', s.confirmOrder.type === 'LIMIT' ? '限价' : s.confirmOrder.type === 'MARKET' ? '市价' : '止损', ''],
              ['价格', `$${s.confirmOrder.price.toFixed(2)}`, ''],
              ['数量', `${s.confirmOrder.quantity.toFixed(4)} 张`, ''],
              ['杠杆', `${leverage}x`, ''],
            ].map(([label, value, color]) => (
              <div key={label as string} className="flex justify-between items-center py-1">
                <span className="text-xs text-gray-500">{label}</span>
                <span className={clsx('text-xs font-medium', color || 'text-gray-200')}>{value}</span>
              </div>
            ))}
            <div className="border-t border-gray-800 pt-2 mt-2 flex justify-between items-center">
              <span className="text-xs font-bold text-gray-400">总金额</span>
              <span className={clsx(
                'text-sm font-bold',
                s.confirmOrder.side === 'BUY' ? 'text-buy' : 'text-sell',
              )}>
                ${s.confirmOrder.total.toFixed(2)}
              </span>
            </div>
          </div>
        )}
      </Modal>

      {/* Submitting overlay */}
      {s.isSubmitting && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface rounded-2xl p-6 flex flex-col items-center gap-3 border border-gray-800">
            <div className="w-8 h-8 border-2 border-brand border-t-transparent rounded-full animate-spin" />
            <span className="text-xs text-gray-400">订单提交中...</span>
          </div>
        </div>
      )}
    </div>
  )
}
