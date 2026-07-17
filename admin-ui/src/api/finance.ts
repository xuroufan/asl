import request from '@/utils/request'

// ====== 财务看板 ======
export function getFinanceDashboard() {
  return request.get('/finance/dashboard/stats')
}

// ====== 交易流水 ======
export function getTradeList(params: any) {
  return request.get('/finance/trade/list', { params })
}
export function exportTrades(params: any) {
  return request.get('/finance/trade/export', { params })
}

// ====== 日报月报 ======
export function getDailyReport(date?: string) {
  return request.get('/finance/report/daily', { params: { date } })
}
export function getMonthlyReport(yearMonth?: string) {
  return request.get('/finance/report/monthly', { params: { yearMonth } })
}
export function getReportHistory(params: any) {
  return request.get('/finance/report/history', { params })
}
export function generateReport(type: string) {
  return request.post('/finance/report/generate', null, { params: { type } })
}
export function auditReport(reportId: number, action: string) {
  return request.post('/finance/report/audit', null, { params: { reportId, action } })
}

// ====== 对账管理 ======
export function getReconciliationHistory(params: any) {
  return request.get('/finance/reconciliation/history', { params })
}
export function getReconciliationDiffs(reconciliationId: number) {
  return request.get('/finance/reconciliation/diffs', { params: { reconciliationId } })
}
export function runReconciliation(date: string, type: string) {
  return request.post('/finance/reconciliation/run', null, { params: { date, type } })
}
export function resolveDiff(diffId: number, resolution: string, notes?: string) {
  return request.post('/finance/reconciliation/diff/resolve', null, { params: { diffId, resolution, notes } })
}

// ====== 监管报表 ======
export function getRegulatoryReportList(params: any) {
  return request.get('/finance/regulatory/list', { params })
}
export function generateRegulatoryReport(type: string) {
  return request.post('/finance/regulatory/generate', null, { params: { type } })
}
export function exportRegulatoryReport(reportId: number, format?: string) {
  return request.get('/finance/regulatory/export', { params: { reportId, format } })
}
