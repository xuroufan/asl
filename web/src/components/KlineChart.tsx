import { useEffect, useRef, useMemo } from 'react'
import { createChart, ColorType, type IChartApi, type ISeriesApi, type CandlestickSeriesPartialOptions, type HistogramSeriesPartialOptions } from 'lightweight-charts'
import type { CandleData } from '../types'

interface KlineChartProps {
  data: CandleData[]
  loading?: boolean
}

export function KlineChart({ data, loading }: KlineChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null)
  const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    const chart = createChart(containerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#6B7A99',
        fontSize: 10,
        fontFamily: 'JetBrains Mono, monospace',
      },
      grid: {
        vertLines: { color: '#161F3A', style: 1 },
        horzLines: { color: '#161F3A', style: 1 },
      },
      crosshair: {
        mode: 0,
        vertLine: { color: '#4F8CF7', width: 1, style: 2, labelBackgroundColor: '#4F8CF7' },
        horzLine: { color: '#4F8CF7', width: 1, style: 2, labelBackgroundColor: '#4F8CF7' },
      },
      timeScale: {
        borderColor: '#1E2A44',
        timeVisible: true,
        secondsVisible: false,
        tickMarkFormatter: (time: number) => {
          const d = new Date(time * 1000)
          return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
        },
      },
      rightPriceScale: {
        borderColor: '#1E2A44',
        scaleMargins: { top: 0.05, bottom: 0.3 },
      },
      width: containerRef.current.clientWidth,
      height: 340,
      handleScroll: { vertTouchDrag: false },
    })

    // Candlestick series
    const candleSeries = chart.addCandlestickSeries({
      upColor: '#FF6B6B',
      downColor: '#00C853',
      borderUpColor: '#FF6B6B',
      borderDownColor: '#00C853',
      wickUpColor: '#FF6B6B',
      wickDownColor: '#00C853',
    } as CandlestickSeriesPartialOptions)

    // Volume series
    const volumeSeries = chart.addHistogramSeries({
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume',
    } as HistogramSeriesPartialOptions)

    chart.priceScale('volume').applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
      visible: false,
    })

    chartRef.current = chart
    candleSeriesRef.current = candleSeries
    volumeSeriesRef.current = volumeSeries

    const handleResize = () => {
      if (containerRef.current) {
        chart.applyOptions({ width: containerRef.current.clientWidth })
      }
    }
    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
      chart.remove()
    }
  }, [])

  useEffect(() => {
    if (!candleSeriesRef.current || !volumeSeriesRef.current || data.length === 0) return

    candleSeriesRef.current.setData(
      data.map(d => ({
        time: (d.timestamp / 1000) as any,
        open: d.open,
        high: d.high,
        low: d.low,
        close: d.close,
      }))
    )

    volumeSeriesRef.current.setData(
      data.map(d => ({
        time: (d.timestamp / 1000) as any,
        value: d.volume,
        color: d.close >= d.open ? 'rgba(255,107,107,0.3)' : 'rgba(0,200,83,0.3)',
      }))
    )

    chartRef.current?.timeScale().fitContent()
  }, [data])

  return (
    <div className="relative rounded-lg overflow-hidden bg-[#0C1124]/60">
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-[#080C1A]/70 z-10">
          <div className="flex items-center gap-2.5">
            <div className="animate-spin w-4 h-4 border-[2px] border-brand border-t-transparent rounded-full" />
            <span className="text-xs text-gray-500">加载中</span>
          </div>
        </div>
      )}
      <div ref={containerRef} className="w-full" />
    </div>
  )
}
