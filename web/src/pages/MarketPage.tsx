import { useEffect, useState, useRef, useCallback } from 'react'
import { useMarketStore } from '../store/marketStore'
import { KlineChart } from '../components/KlineChart'
import { OrderBook } from '../components/OrderBook'
import { TradeHistory } from '../components/TradeHistory'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import type { CandleInterval, FuturesSymbol, FuturesQuote } from '../types'
import { Search, TrendingUp, Globe, RefreshCw } from 'lucide-react'
import { market } from '../api/client'
import clsx from 'clsx'

const intervals: { value: CandleInterval; label: string }[] = [
  { value: 'M1', label: '1分' },
  { value: 'M5', label: '5分' },
  { value: 'M15', label: '15分' },
  { value: 'M30', label: '30分' },
  { value: 'H1', label: '1小时' },
  { value: 'D1', label: '日线' },
]

const exchangeNames: Record<string, { label: string; color: string }> = {
  HKEX: { label: '港交所', color: '#F0B90B' },
  CME: { label: 'CME', color: '#4F8CF7' },
  COMEX: { label: 'COMEX', color: '#FF6B6B' },
  NYMEX: { label: 'NYMEX', color: '#00C853' },
}

export function MarketPage() {
  const store = useMarketStore()
  const [futuresSymbols, setFuturesSymbols] = useState<FuturesSymbol[]>([])
  const [quotes, setQuotes] = useState<Record<string, FuturesQuote>>({})
  const [loadingSymbols, setLoadingSymbols] = useState(true)
  const [exchangeFilter, setExchangeFilter] = useState<string>('ALL')
  const [search, setSearch] = useState('')
  const pollingRef = useRef<number>()

  // Fetch symbols on mount
  useEffect(() => {
    market.getSymbols().then(data => {
      setFuturesSymbols(data)
      setLoadingSymbols(false)
      if (data.length > 0 && !store.selectedSymbol) {
        store.selectSymbol(data[0].symbol)
      }
    }).catch(() => setLoadingSymbols(false))
  }, [])

  // Poll quotes every 5 seconds
  const fetchQuotes = useCallback(async () => {
    try {
      const data = await market.getQuotes()
      if (data) setQuotes(data)
    } catch {}
  }, [])

  useEffect(() => {
    fetchQuotes()
    pollingRef.current = window.setInterval(fetchQuotes, 5000)
    return () => { if (pollingRef.current) clearInterval(pollingRef.current) }
  }, [fetchQuotes])

  // Exchange filter
  const exchangeGroups = ['ALL', ...new Set(futuresSymbols.map(s => s.exchange))]
  const filteredSymbols = futuresSymbols.filter(s =>
    (exchangeFilter === 'ALL' || s.exchange === exchangeFilter) &&
    (s.symbol.toLowerCase().includes(search.toLowerCase()) ||
     s.name.toLowerCase().includes(search.toLowerCase()))
  )

  const selectedQuote = quotes[store.selectedSymbol]
  const selectedSymbolMeta = futuresSymbols.find(s => s.symbol === store.selectedSymbol)

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      {/* Header */}
      <div className="flex items-center gap-2 mb-3">
        <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-brand to-blue-500 flex items-center justify-center">
          <TrendingUp className="w-3.5 h-3.5 text-white" />
        </div>
        <h1 className="text-lg font-bold">期货行情</h1>
        <button onClick={fetchQuotes} className="ml-auto p-1.5 rounded-lg hover:bg-gray-800 text-gray-500 hover:text-gray-300 transition-colors">
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Exchange filter tabs */}
      <div className="flex gap-1.5 overflow-x-auto pb-2 -mx-1 px-1 mb-2">
        {exchangeGroups.map(ex => {
          const exInfo = exchangeNames[ex]
          return (
            <button key={ex}
              onClick={() => setExchangeFilter(ex)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all',
                ex === exchangeFilter
                  ? 'bg-brand/15 text-brand border border-brand/30'
                  : 'text-gray-500 bg-gray-900/50 border border-transparent hover:text-gray-300 hover:border-gray-700/50',
              )}
            >
              {ex === 'ALL' ? '全部' : (exInfo?.label || ex)}
            </button>
          )
        })}
      </div>

      {/* Search */}
      <div className="relative mb-3">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500" />
        <input
          className="w-full bg-[#0C1124] border border-gray-800/60 rounded-xl pl-9 pr-3 py-2 text-sm text-gray-300 placeholder:text-gray-600 focus:outline-none focus:border-brand/40 transition-colors"
          placeholder="搜索合约代码或名称"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
      </div>

      {loadingSymbols ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin w-6 h-6 border-2 border-brand border-t-transparent rounded-full" />
        </div>
      ) : (
        <>
          {/* Symbol grid */}
          <div className="grid grid-cols-2 gap-2 mb-3">
            {filteredSymbols.slice(0, 12).map(sym => {
              const q = quotes[sym.symbol]
              const exInfo = exchangeNames[sym.exchange]
              const change = q?.change ?? 0
              const isUp = change >= 0
              const isSelected = store.selectedSymbol === sym.symbol
              return (
                <button key={sym.symbol}
                  onClick={() => store.selectSymbol(sym.symbol)}
                  className={clsx(
                    'relative text-left p-3 rounded-xl transition-all duration-150 border',
                    isSelected
                      ? 'bg-brand/8 border-brand/30 shadow-[0_0_20px_-8px_rgba(79,140,247,0.2)]'
                      : 'bg-[#0F1629] border-gray-800/40 hover:border-gray-700/60',
                  )}
                >
                  {/* Exchange badge */}
                  <span className="inline-flex items-center gap-1 text-[9px] font-medium text-gray-500 mb-1">
                    <Globe className="w-2.5 h-2.5" />
                    {exInfo?.label || sym.exchange}
                  </span>
                  <div className="flex items-center justify-between">
                    <span className={clsx('text-sm font-bold', isSelected ? 'text-white' : 'text-gray-200')}>
                      {sym.symbol}
                    </span>
                    {q && (
                      <span className={clsx('text-xs font-bold', isUp ? 'text-[#FF6B6B]' : 'text-[#00C853]')}>
                        {isUp ? '+' : ''}{change.toFixed(2)}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center justify-between mt-1">
                    <span className="text-lg font-bold text-white">
                      {q ? q.last.toFixed(2) : '—'}
                    </span>
                    {q && (
                      <span className={clsx('text-[11px] font-medium px-1.5 py-0.5 rounded', isUp ? 'text-[#FF6B6B] bg-[#FF6B6B]/10' : 'text-[#00C853] bg-[#00C853]/10')}>
                        {isUp ? '+' : ''}{q.changePercent.toFixed(2)}%
                      </span>
                    )}
                  </div>
                  {q && <div className="text-[10px] text-gray-600 mt-1">成交量 {q.volume.toLocaleString()}</div>}
                </button>
              )
            })}
          </div>

          {/* Selected symbol detail */}
          {selectedQuote && selectedSymbolMeta && (
            <>
              {/* Price banner */}
              <Card className="p-4 mb-3">
                <div className="flex justify-between items-start">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-sm font-bold text-white">{store.selectedSymbol}</span>
                      <span className="text-xs text-gray-500">{selectedSymbolMeta.name}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-800 text-gray-500">
                        {exchangeNames[selectedSymbolMeta.exchange]?.label || selectedSymbolMeta.exchange}
                      </span>
                    </div>
                    <div className="flex items-baseline gap-3">
                      <span className={clsx('text-2xl font-bold', selectedQuote.change >= 0 ? 'text-[#FF6B6B]' : 'text-[#00C853]')}>
                        {selectedQuote.last.toFixed(2)}
                      </span>
                      <span className={clsx('text-sm font-medium', selectedQuote.change >= 0 ? 'text-[#FF6B6B]' : 'text-[#00C853]')}>
                        {selectedQuote.change >= 0 ? '+' : ''}{selectedQuote.change.toFixed(2)} ({selectedQuote.changePercent >= 0 ? '+' : ''}{selectedQuote.changePercent.toFixed(2)}%)
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-gray-500">合约乘数</div>
                    <div className="text-sm font-bold text-gray-300">{selectedSymbolMeta.multiplier}</div>
                    <div className="text-[10px] text-gray-600 mt-0.5">最小跳动 {selectedSymbolMeta.minTick}</div>
                  </div>
                </div>
                {/* Bid/Ask row */}
                <div className="flex gap-4 mt-3 pt-3 border-t border-gray-800/50">
                  <div>
                    <span className="text-[10px] text-gray-500">卖一价</span>
                    <div className="text-sm font-bold text-[#00C853]">{selectedQuote.ask.toFixed(2)}</div>
                  </div>
                  <div>
                    <span className="text-[10px] text-gray-500">买一价</span>
                    <div className="text-sm font-bold text-[#FF6B6B]">{selectedQuote.bid.toFixed(2)}</div>
                  </div>
                  <div>
                    <span className="text-[10px] text-gray-500">最高</span>
                    <div className="text-sm font-medium text-gray-300">{selectedQuote.high.toFixed(2)}</div>
                  </div>
                  <div>
                    <span className="text-[10px] text-gray-500">最低</span>
                    <div className="text-sm font-medium text-gray-300">{selectedQuote.low.toFixed(2)}</div>
                  </div>
                </div>
              </Card>

              {/* Interval selector */}
              <div className="flex gap-1 mb-2">
                {intervals.map(i => (
                  <button key={i.value}
                    onClick={() => store.selectInterval(i.value)}
                    className={clsx(
                      'px-2.5 py-1 rounded text-xs font-medium transition-colors',
                      i.value === store.selectedInterval
                        ? 'bg-brand text-white'
                        : 'text-gray-500 bg-gray-900/50 hover:text-gray-300',
                    )}
                  >
                    {i.label}
                  </button>
                ))}
              </div>

              {/* K-line chart */}
              <div className="mb-3">
                <KlineChart data={store.klineData} loading={store.isKlineLoading} />
              </div>

              {/* Order Book + Trade History */}
              <div className="grid grid-cols-2 gap-3">
                <OrderBook bids={store.depthBids} asks={store.depthAsks}
                  maxBidVol={store.depthBids.reduce((m, b) => Math.max(m, b.quantity), 0)}
                  maxAskVol={store.depthAsks.reduce((m, a) => Math.max(m, a.quantity), 0)}
                  currentPrice={selectedQuote?.last ?? 0} />
                <TradeHistory trades={store.recentTrades} />
              </div>
            </>
          )}
        </>
      )}
    </div>
  )
}
