
// ==================== 期货合约数据模型 ====================
export interface FuturesSymbol {
  symbol: string
  name: string
  exchange: string
  minTick: number
  multiplier: number
}

export interface FuturesQuote {
  symbol: string
  bid: number
  ask: number
  last: number
  high: number
  low: number
  volume: number
  change: number
  changePercent: number
  timestamp: number
}

export interface ExchangeGroup {
  name: string
  symbols: string[]
}


export interface User {
  id: string
  username: string
  displayName: string
  email: string
  createdAt: number
  updatedAt: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export interface MarketData {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  high: number
  low: number
  open: number
  close: number
  volume: number
  turnover: number
}

export type OrderSide = 'BUY' | 'SELL'
export type OrderType = 'MARKET' | 'LIMIT' | 'STOP'
export type OrderStatus = 'PENDING' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED'
export type PositionSide = 'LONG' | 'SHORT'

export interface Order {
  id: string
  symbol: string
  type: OrderType
  side: OrderSide
  status: OrderStatus
  price: number | null
  stopPrice: number | null
  quantity: number
  filledQuantity: number
  averageFilledPrice: number | null
  totalAmount: number
  fee: number
  createdAt: number
  updatedAt: number
}

export interface PlaceOrderRequest {
  symbol: string
  type: OrderType
  side: OrderSide
  price?: number | null
  stopPrice?: number | null
  quantity: number
}

export interface Position {
  id: string
  symbol: string
  side: PositionSide
  quantity: number
  entryPrice: number
  currentPrice: number
  marketValue: number
  unrealizedPnl: number
  unrealizedPnlPercent: number
  marginUsed: number
  leverage: number
  liquidationPrice: number
}

export interface CandleData {
  timestamp: number
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export type CandleInterval = 'M1' | 'M5' | 'M15' | 'M30' | 'H1' | 'H4' | 'D1' | 'W1'

export interface OrderBookLevel {
  price: number
  quantity: number
}

export interface MarketTrade {
  price: number
  quantity: number
  isBuyerMaker: boolean
  timestamp: number
}

export interface AccountBalance {
  asset: string
  available: number
  frozen: number
}

export interface CapitalSummary {
  totalEquity: number
  available: number
  marginUsed: number
  unrealizedPnl: number
  riskRatio: number
  positionCount: number
}

export interface PendingOrder {
  symbol: string
  side: OrderSide
  type: OrderType
  price: number
  quantity: number
  total: number
}

export interface WSMessage {
  type: 'CONNECTED' | 'DISCONNECTED' | 'MESSAGE' | 'ERROR'
  channel?: string
  data?: any
  error?: string
}
