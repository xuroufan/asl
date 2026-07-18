import { useEffect, useState, useRef, useCallback } from 'react'
import { useMarketStore } from '../store/marketStore'
import { MarketChartSkeleton, CardSkeleton } from '../components/LoadingSkeleton'
import { KlineChart } from '../components/KlineChart'
import { IndicatorPane } from '../components/IndicatorPane'
import { OrderBook } from '../components/OrderBook'
import { TradeHistory } from '../components/TradeHistory'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import type { CandleInterval, FuturesSymbol, FuturesQuote } from '../types'
import { Search, TrendingUp, Globe, RefreshCw, Star, ChevronDown } from 'lucide-react'
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
  const [showSymbolList, setShowSymbolList] = useState(true)
  const [activeIndicator, setActiveIndicator] = useState<'MACD' | 'RSI' | null>(null)

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

  // Filtered symbols
  const exchanges = ['ALL', ...new Set(futuresSymbols.map(s => s.exchange))]
  const filtered = futuresSymbols.filter(s => {
    if (exchangeFilter !== 'ALL' && s.exchange !== exchangeFilter) return false
    if (search) return s.symbol.toLowerCase().includes(search.toLowerCase()) || s.name?.toLowerCase().includes(search.toLowerCase())
    return true
  })

  const selectedSymbol = futuresSymbols.find(s => s.symbol === store.selectedSymbol)
  const quote = store.selectedSymbol ? quotes[store.selectedSymbol] : undefined

  return (
    <div className="max-w-2xl mx-auto px-3 pt-3 pb-20 sm:pb-4">
      {/* ===== Top bar: selected symbol info ===== */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowSymbolList(!showSymbolList)}
            className="flex items-center gap-2"
          >
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-bold">{selectedSymbol?.symbol || '--'}</h1>
                <ChevronDown className={clsx('w-3.5 h-3.5 text-gray-500 transition-transform', showSymbolList && 'rotate-180')} />
              </div>
              <div className="text-[10px] text-gray-600">{selectedSymbol?.name || '选择合约'}</div>
            </div>
          </button>
          {quote && (
            <div className="flex items-center gap-3">
              <span className={clsx(
                'text-xl font-bold font-mono tracking-tight',
                quote.change >= 0 ? 'text-buy' : 'text-down',
              )}>
                {quote.lastPrice.toFixed(2)}
              </span>
              <Badge value={quote.change} prefix="" suffix="%" />
            </div>
          )}
        </div>
        <button className="p-2 rounded-lg hover:bg-brand/5 text-gray-500 hover:text-brand transition-colors">
          <Star className="w-4 h-4" />
        </button>
      </div>

      {/* ===== Collapsible symbol list ===== */}
      {showSymbolList && (
        <Card className="mb-3 overflow-hidden animate-fade-in">
          {/* Search + Filter */}
          <div className="p-3 border-b border-gray-800/40">
            <div className="flex items-center gap-2 mb-2.5">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-600" />
                <input
                  type="text"
                  placeholder="搜索合约..."
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  className="w-full bg-ink-light border border-gray-800 rounded-lg pl-9 pr-3 py-2 text-xs text-gray-200 placeholder:text-gray-600 focus:outline-none focus:border-brand/40 transition-colors"
                />
              </div>
              <button
                onClick={fetchQuotes}
                className="p-2 rounded-lg hover:bg-brand/5 text-gray-500 hover:text-brand transition-colors"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </button>
            </div>
            {/* Exchange tabs */}
            <div className="flex gap-1.5 overflow-x-auto pb-0.5 scrollbar-none">
              {exchanges.map(ex => (
                <button
                  key={ex}
                  onClick={() => setExchangeFilter(ex)}
                  className={clsx(
                    'shrink-0 px-3 py-1.5 sm:px-2.5 sm:py-1 rounded-lg text-[10px] font-medium transition-all min-h-[32px]',
                    ex === exchangeFilter
                      ? 'bg-brand/10 text-brand border border-brand/20'
                      : 'text-gray-500 hover:text-gray-300 bg-transparent border border-transparent',
                  )}
                >
                  {ex === 'ALL' ? '全部' : exchangeNames[ex]?.label || ex}
                </button>
              ))}
            </div>
          </div>
          {/* Symbol items */}
          <div className="max-h-48 overflow-y-auto overscroll-contain divide-y divide-gray-800/20">
            {loadingSymbols ? (
              <CardSkeleton rows={5} />
            ) : filtered.length === 0 ? (
              <div className="text-center text-gray-700 py-8 text-xs">无匹配合约</div>
            ) : (
              filtered.map(sym => {
                const q = quotes[sym.symbol]
                const isSelected = store.selectedSymbol === sym.symbol
                const change = q ? q.change : 0
                return (
                  <button
                    key={sym.symbol}
                    onClick={() => { store.selectSymbol(sym.symbol); setShowSymbolList(false) }}
                    className={clsx(
                      'w-full flex items-center gap-3 p-2.5 transition-colors text-left',
                      isSelected ? 'bg-brand/5' : 'hover:bg-white/[0.02]',
                    )}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-medium text-gray-200">{sym.symbol}</span>
                        <span className="text-[9px] text-gray-600 bg-gray-800/60 rounded px-1 py-0.5">
                          {exchangeNames[sym.exchange]?.label || sym.exchange}
                        </span>
                      </div>
                      <div className="text-[10px] text-gray-600 truncate mt-0.5">{sym.name}</div>
                    </div>
                    {q ? (
                      <div className="text-right">
                        <div className={clsx('text-xs font-mono font-medium', change >= 0 ? 'text-buy' : 'text-down')}>
                          {q.lastPrice.toFixed(2)}
                        </div>
                        <div className={clsx('text-[10px] font-mono', change >= 0 ? 'text-buy' : 'text-down')}>
                          {change >= 0 ? '+' : ''}{change.toFixed(2)}%
                        </div>
                      </div>
                    ) : (
                      <div className="text-right">
                        <div className="text-xs text-gray-600 font-mono">--</div>
                      </div>
                    )}
                  </button>
                )
              })
            )}
          </div>
        </Card>
      )}

      {/* ===== K-line chart area ===== */}
      <Card className="mb-3 overflow-hidden">
        {/* Chart header */}
        <div className="flex items-center justify-between px-3 pt-3 pb-2">
          <div className="section-title">K线图表</div>
          <div className="flex gap-1">
            {intervals.map(int => (
              <button
                key={int.value}
                onClick={() => store.setInterval(int.value)}
                className={clsx(
                  'px-2 py-1 rounded-md text-[10px] font-medium transition-all',
                  store.currentInterval === int.value
                    ? 'bg-brand/10 text-brand border border-brand/20'
                    : 'text-gray-600 hover:text-gray-400 border border-transparent',
                )}
              >
                {int.label}
              </button>
            ))}
          </div>
          {/* Indicator toggles */}
          <div className="flex gap-1">
            {(['MACD', 'RSI'] as const).map(ind => (
              <button
                key={ind}
                onClick={() => setActiveIndicator(activeIndicator === ind ? null : ind)}
                className={'px-2 py-1 rounded-md text-[10px] font-medium transition-all' + (activeIndicator === ind ? ' bg-brand/10 text-brand border border-brand/20' : ' text-gray-600 hover:text-gray-400 border border-transparent')}
              >
                {ind}
              </button>
            ))}
          </div>
        </div>
        {/* Chart */}
        <div className="px-2 pb-2">
          <KlineChart data={store.candles} loading={store.loadingCandles} />
        </div>
      </Card>

      {/* Indicator pane */}
      {activeIndicator && (
        <Card className="mb-3 overflow-hidden animate-fade-in">
          <IndicatorPane type={activeIndicator} data={store.candles} />
        </Card>
      )}
    
      {/* ===== OrderBook + TradeHistory 2-column ===== */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3">
        <OrderBook
          bids={store.orderBook?.bids || []}
          asks={store.orderBook?.asks || []}
          maxBidVol={store.orderBook?.maxBidVol || 1}
          maxAskVol={store.orderBook?.maxAskVol || 1}
          currentPrice={quote?.lastPrice || 0}
        />
        <TradeHistory trades={store.recentTrades || []} />
      </div>
    </div>
  )
}
