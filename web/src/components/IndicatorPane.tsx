import { useEffect, useRef } from 'react'
import { createChart, ColorType, type IChartApi, type ISeriesApi } from 'lightweight-charts'
import type { CandleData } from '../types'
import { calculateMACD, calculateRSI } from '../utils/indicators'

interface Props {
  type: 'MACD' | 'RSI'
  data: CandleData[]
}

export function IndicatorPane({ type, data }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<any>[]>([])

  useEffect(() => {
    if (!containerRef.current) return
    const chart = createChart(containerRef.current, {
      layout: { background: { type: ColorType.Solid, color: 'transparent' }, textColor: '#6B7A99', fontSize: 10, fontFamily: 'JetBrains Mono, monospace' },
      grid: { vertLines: { color: '#161F3A', style: 1 }, horzLines: { color: '#161F3A', style: 1 } },
      crosshair: { mode: 0, vertLine: { color: '#4F8CF7', width: 1, style: 2, labelBackgroundColor: '#4F8CF7' }, horzLine: { color: '#4F8CF7', width: 1, style: 2, labelBackgroundColor: '#4F8CF7' } },
      timeScale: { borderColor: '#1E2A44', visible: true, timeVisible: true, secondsVisible: false },
      rightPriceScale: { borderColor: '#1E2A44', scaleMargins: { top: 0.15, bottom: 0.15 } },
      width: containerRef.current.clientWidth,
      height: 130,
      handleScroll: { vertTouchDrag: false },
      handleScale: { axisPressedMouse: false },
    })
    chartRef.current = chart
    const handleResize = () => { if (containerRef.current) chart.applyOptions({ width: containerRef.current.clientWidth }) }
    window.addEventListener('resize', handleResize)
    return () => { window.removeEventListener('resize', handleResize); chart.remove() }
  }, [])

  useEffect(() => {
    const chart = chartRef.current
    if (!chart || data.length < 35) return

    // Remove old series
    seriesRef.current.forEach(s => { try { chart.removeSeries(s) } catch {} })
    seriesRef.current = []

    if (type === 'MACD') {
      const macdData = calculateMACD(data)
      if (macdData.length < 2) return

      const s1 = chart.addLineSeries({ color: '#4F8CF7', lineWidth: 1.5 })
      s1.setData(macdData.map(d => ({ time: d.time, value: d.macd })))
      seriesRef.current.push(s1)

      const s2 = chart.addLineSeries({ color: '#F59E0B', lineWidth: 1.5 })
      s2.setData(macdData.map(d => ({ time: d.time, value: d.signal })))
      seriesRef.current.push(s2)

      const s3 = chart.addHistogramSeries({ color: '#4F8CF7', priceFormat: { type: 'volume' } })
      s3.setData(macdData.map(d => ({
        time: d.time, value: Math.abs(d.histogram),
        color: d.histogram >= 0 ? 'rgba(79,140,247,0.4)' : 'rgba(245,158,11,0.4)',
      })))
      seriesRef.current.push(s3)
    } else {
      const rsiData = calculateRSI(data)
      if (rsiData.length < 2) return

      const s1 = chart.addLineSeries({ color: '#8E5CD8', lineWidth: 1.5 })
      s1.setData(rsiData.map(d => ({ time: d.time, value: d.rsi })))
      seriesRef.current.push(s1)
    }

    chart.timeScale().fitContent()
  }, [data, type])

  return <div className="rounded-lg overflow-hidden bg-[#0C1124]/40 border-t border-gray-800/30">
    <div className="flex items-center gap-2 px-3 pt-1.5 pb-0.5">
      <span className="text-[9px] font-semibold tracking-wider text-gray-600 uppercase">{type}</span>
      {type === 'RSI' && <span className="text-[9px] text-gray-700">超买70 超卖30</span>}
    </div>
    <div ref={containerRef} className="w-full" />
  </div>
}
