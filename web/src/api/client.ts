import type { FuturesSymbol, FuturesQuote,
  LoginRequest, LoginResponse, FuturesSymbol, FuturesQuote, MarketData, PlaceOrderRequest, Order,
  Position, CandleData, CandleInterval, OrderBookLevel, AccountBalance,
} from '../types'

const API_BASE = '/api/v1'

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

let accessToken: string | null = localStorage.getItem('access_token')

export function setToken(token: string | null) {
  accessToken = token
  if (token) localStorage.setItem('access_token', token)
  else localStorage.removeItem('access_token')
}

export function getToken() { return accessToken }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (!res.ok) {
    if (res.status === 401) {
      setToken(null)
      window.location.href = '/login'
    }
    throw new ApiError(res.status, await res.text().catch(() => 'Request failed'))
  }
  const json = await res.json()
  // 兼容后端统一响应格式 { code, msg, data }
  if (json.code !== undefined && json.data !== undefined) {
    return json.data as T
  }
  return json as T
}

// ==================== Auth ====================
// 登录走 admin 服务（后端统一认证入口）
export const auth = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const res = await fetch(`${API_BASE}/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    if (!res.ok) {
      const err = await res.text().catch(() => '')
      throw new Error(err || '登录失败')
    }
    const json = await res.json()
    // 兼容 { code, msg, data: LoginResponse }
    if (json.code === 200 && json.data) {
      return json.data as LoginResponse
    }
    throw new Error(json.msg || '登录失败')
  },
}

// ==================== Market ====================
export const market = {
  getTickers: () => request<MarketData[]>('/market/all-quotes'),
  getCandles: (symbol: string, interval: CandleInterval, limit = 200) =>
    request<CandleData[]>(`/market/kline?symbol=${symbol}&interval=${interval}&limit=${limit}`),
  getOrderBook: (symbol: string, depth = 10) =>
    request<{ bids: OrderBookLevel[]; asks: OrderBookLevel[] }>(`/market/depth?symbol=${symbol}&depth=${depth}`),
  getSymbols: () => request<FuturesSymbol[]>('/market/symbols'),
  getQuotes: () => request<Record<string, FuturesQuote>>('/market/all-quotes'),
}

// ==================== Orders ====================
export const orders = {
  place: (data: PlaceOrderRequest) =>
    request<Order>('/order/place', { method: 'POST', body: JSON.stringify(data) }),
  cancel: (orderId: string, symbol: string) =>
    request<Order>('/order/cancel', { method: 'POST', body: JSON.stringify({ orderId, symbol }) }),
  history: (symbol?: string, page = 1, size = 20) =>
    request<{ items: Order[]; total: number }>(
      `/order/history?${symbol ? `symbol=${symbol}&` : ''}page=${page}&size=${size}`
    ),
}

// ==================== Positions ====================
export const positions = {
  list: (symbol?: string) =>
    request<Position[]>(`/trade/positions${symbol ? `?symbol=${symbol}` : ''}`),
  close: (positionId: string, symbol: string) =>
    request<Position>('/trade/close-position', { method: 'POST', body: JSON.stringify({ positionId, symbol }) }),
}

// ==================== Account ====================
export const account = {
  balances: () => request<AccountBalance[]>('/account/overview'),
}
