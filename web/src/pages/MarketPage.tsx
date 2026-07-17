import { useEffect, useState } from 'react'
import { useMarketStore } from '../store/marketStore'
import { KlineChart } from '../components/KlineChart'
import { OrderBook } from '../components/OrderBook'
import { TradeHistory } from '../components/TradeHistory'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import type { CandleInterval } from '../types'
import { Search, TrendingUp } from 'lucide-react'
import clsx from 'clsx'

const intervals: { value: CandleInterval; label: string }[] = [
  { value: 'M1', label: '1分' },
  { value: 'M5', label: '5分' },
  { value: 'M15', label: '15分' },
  { value: 'M30', label: '30分' },
  { value: 'H1', label: '1小时' },
  { value: 'D1', label: '日线' },
]

const symbols = [
  { key: 'BTCUSDT', label: 'BTC/USDT' },
  { key: 'ETHUSDT', label: 'ETH/USDT' },
  { key: 'SOLUSDT', label: 'SOL/USDT' },
  { key: 'XRPUSDT', label: 'XRP/USDT' },
  { key: 'DOGEUSDT', label: 'DOGE/USDT' },
]

export function MarketPage() {
  const { selectedSymbol, selectedInterval, klineData, depthBids, depthAsks,
    recentTrades, marketDataList, isLoading, isKlineLoading, load, selectSymbol, selectInterval } = useMarketStore()

  const [search, setSearch] = useState('')

  useEffect(() => { load() }, [])

  const selectedMd = marketDataList.find(m => m.symbol === selectedSymbol)
  const maxBidVol = depthBids.reduce((m, b) => Math.max(m, b.quantity), 0)
  const maxAskVol = depthAsks.reduce((m, a) => Math.max(m, a.quantity), 0)

  const filteredSymbols = symbols.filter(s =>
    s.label.toLowerCase().includes(search.toLowerCase()) ||
    s.key.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-4">
        <div className="w-5 h-5 bg-brand rounded flex items-center justify-center">
          <TrendingUp className="w-3 h-3 text-white" />
        </div>
        <h1 className="text-lg font-bold">期货</h1>
        <div className="ml-auto relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500" />
          <input
            className="bg-gray-900 border border-gray-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-gray-300 placeholder:text-gray-600 focus:outline-none focus:border-brand/50 w-32"
            placeholder="搜索合约"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Symbol tabs */}
      <div className="flex gap-1 overflow-x-auto pb-2 -mx-1 px-1">
        {symbols.map(s => (
          <button
            key={s.key}
            onClick={() => selectSymbol(s.key)}
            className={clsx(
              'px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors',
              s.key === selectedSymbol
                ? 'bg-brand text-white'
                : 'text-gray-400 bg-gray-900 hover:text-gray-200',
            )}
          >
            {s.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin w-6 h-6 border-2 border-brand border-t-transparent rounded-full" />
        </div>
      ) : (
        <>
          {/* Price overview */}
          {selectedMd && (
            <Card className="p-4 mb-3">
              <div className="flex justify-between items-start">
                <div>
                  <div className="text-xs text-gray-500 mb-0.5">{selectedMd.name}</div>
                  <div className="text-2xl font-bold text-white">
                    ${selectedMd.price.toFixed(2)}
                  </div>
                </div>
                <div className="text-right">
                  <Badge value={selectedMd.changePercent} suffix="%" />
                  {selectedMd.change !== 0 && (
                    <div className={clsx(
                      'text-xs mt-1',
                      selectedMd.change > 0 ? 'text-buy' : 'text-sell',
                    )}>
                      {selectedMd.change > 0 ? '+' : ''}{selectedMd.change.toFixed(2)}
                    </div>
                  )}
                </div>
              </div>
            </Card>
          )}

          {/* Interval selector */}
          <div className="flex gap-1 mb-2">
            {intervals.map(i => (
              <button
                key={i.value}
                onClick={() => selectInterval(i.value)}
                className={clsx(
                  'px-2.5 py-1 rounded text-xs font-medium transition-colors',
                  i.value === selectedInterval
                    ? 'bg-brand text-white'
                    : 'text-gray-500 bg-gray-900 hover:text-gray-300',
                )}
              >
                {i.label}
              </button>
            ))}
          </div>

          {/* K-line chart */}
          <div className="mb-3">
            <KlineChart data={klineData} loading={isKlineLoading} />
          </div>

          {/* Order Book + Trade History */}
          <div className="grid grid-cols-2 gap-3">
            <OrderBook
              bids={depthBids}
              asks={depthAsks}
              maxBidVol={maxBidVol}
              maxAskVol={maxAskVol}
              currentPrice={selectedMd?.price ?? 0}
            />
            <TradeHistory trades={recentTrades} />
          </div>
        </>
      )}
    </div>
  )
}
