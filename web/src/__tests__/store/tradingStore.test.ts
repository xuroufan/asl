import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../../api/client', () => ({
  orders: { place: vi.fn() },
}))

import { useTradingStore } from '../../store/tradingStore'

describe('tradingStore', () => {
  beforeEach(() => {
    useTradingStore.setState({
      symbol: 'BTCUSDT', orderSide: 'BUY', orderType: 'LIMIT',
      price: '', quantity: '', currentPrice: 50000,
      isSubmitting: false, showConfirm: false,
      confirmOrder: null, error: null, successMessage: null,
      lastOrder: null, recentOrders: [],
    })
  })

  it('defaults to BUY side and LIMIT type', () => {
    const s = useTradingStore.getState()
    expect(s.symbol).toBe('BTCUSDT')
    expect(s.orderSide).toBe('BUY')
    expect(s.orderType).toBe('LIMIT')
  })

  it('sets side and type', () => {
    useTradingStore.getState().setSide('SELL')
    expect(useTradingStore.getState().orderSide).toBe('SELL')

    useTradingStore.getState().setType('MARKET')
    expect(useTradingStore.getState().orderType).toBe('MARKET')
  })

  it('sets price and quantity', () => {
    useTradingStore.getState().setPrice('45000')
    expect(useTradingStore.getState().price).toBe('45000')

    useTradingStore.getState().setQuantity('0.5')
    expect(useTradingStore.getState().quantity).toBe('0.5')
  })

  it('previewOrder rejects invalid inputs', () => {
    useTradingStore.getState().previewOrder()
    expect(useTradingStore.getState().error).toBe('请输入有效的价格和数量')
  })

  it('previewOrder creates PendingOrder for valid inputs', () => {
    useTradingStore.setState({ price: '45000', quantity: '1.5', currentPrice: 50000 })

    useTradingStore.getState().previewOrder()

    const s = useTradingStore.getState()
    expect(s.showConfirm).toBe(true)
    expect(s.confirmOrder).not.toBeNull()
    expect(s.confirmOrder?.price).toBe(45000)
    expect(s.confirmOrder?.quantity).toBe(1.5)
    expect(s.confirmOrder?.total).toBe(67500)
  })

  it('previewOrder uses currentPrice for MARKET orders', () => {
    useTradingStore.setState({ orderType: 'MARKET', price: '', quantity: '1.0', currentPrice: 50000 })

    useTradingStore.getState().previewOrder()

    const s = useTradingStore.getState()
    expect(s.confirmOrder?.price).toBe(50000)
  })

  it('dismissConfirm resets dialog', () => {
    useTradingStore.setState({ showConfirm: true, confirmOrder: { symbol: 'BTC', side: 'BUY', type: 'LIMIT', price: 45000, quantity: 1, total: 45000 } })

    useTradingStore.getState().dismissConfirm()

    expect(useTradingStore.getState().showConfirm).toBe(false)
    expect(useTradingStore.getState().confirmOrder).toBeNull()
  })

  it('clearMessage clears errors', () => {
    useTradingStore.setState({ error: 'err', successMessage: 'ok' })
    useTradingStore.getState().clearMessage()
    expect(useTradingStore.getState().error).toBeNull()
    expect(useTradingStore.getState().successMessage).toBeNull()
  })
})
