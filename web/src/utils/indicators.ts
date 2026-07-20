import type { CandleData } from '../types'

export interface MACDPoint {
  time: number
  macd: number
  signal: number
  histogram: number
}

export interface RSIPoint {
  time: number
  rsi: number
}

/** EMA 计算 */
function ema(data: number[], period: number): number[] {
  const k = 2 / (period + 1)
  const result: number[] = [data[0]]
  for (let i = 1; i < data.length; i++) {
    result.push((data[i] - result[i - 1]) * k + result[i - 1])
  }
  return result
}

/** MACD: EMA12 - EMA26, Signal=EMA9, Histogram */
export function calculateMACD(candles: CandleData[]): MACDPoint[] {
  if (candles.length < 34) return []
  const closes = candles.map(c => c.close)
  const ema12 = ema(closes, 12)
  const ema26 = ema(closes, 26)
  const macdLine = ema12.map((v, i) => v - ema26[i])
  const signalLine = ema(macdLine, 9)
  return macdLine.map((v, i) => ({
    time: Math.floor(candles[i].timestamp / 1000),
    macd: v,
    signal: signalLine[i] || 0,
    histogram: v - (signalLine[i] || 0),
  }))
}

/** RSI(14) */
export function calculateRSI(candles: CandleData[]): RSIPoint[] {
  if (candles.length < 15) return []
  const closes = candles.map(c => c.close)
  const gains: number[] = []
  const losses: number[] = []
  for (let i = 1; i < closes.length; i++) {
    const diff = closes[i] - closes[i - 1]
    gains.push(Math.max(diff, 0))
    losses.push(Math.max(-diff, 0))
  }
  let avgG = gains.slice(0, 14).reduce((a, b) => a + b, 0) / 14
  let avgL = losses.slice(0, 14).reduce((a, b) => a + b, 0) / 14
  const rsi: number[] = [100 - 100 / (1 + avgG / (avgL || 0.001))]
  for (let i = 14; i < gains.length; i++) {
    avgG = (avgG * 13 + gains[i]) / 14
    avgL = (avgL * 13 + losses[i]) / 14
    rsi.push(100 - 100 / (1 + avgG / (avgL || 0.001)))
  }
  return rsi.map((v, i) => ({
    time: Math.floor(candles[i + 1].timestamp / 1000),
    rsi: v,
  }))
}
