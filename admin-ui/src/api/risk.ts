import request from '@/utils/request'

// ====== 风控规则配置 ======
export function getRiskConfigList() {
  return request.get('/risk/config/list')
}
export function updateRiskConfig(data: any) {
  return request.post('/risk/config/update', data)
}
export function refreshRiskConfig() {
  return request.post('/risk/config/refresh')
}

// ====== 风控监控大屏 ======
export function getRiskDashboard() {
  return request.get('/risk/monitor/dashboard')
}
export function getUserRiskStatus(userId: number) {
  return request.get('/risk/monitor/user-status', { params: { userId } })
}

// ====== 强平记录 ======
export function getLiquidationRecords(params: any) {
  return request.get('/risk/liquidation/records', { params })
}

// ====== 风控预警 ======
export function getRiskAlertList(params: any) {
  return request.get('/risk/alert/list', { params })
}

// ====== 风控报表 ======
export function getDailyReport(date?: string) {
  return request.get('/risk/report/daily', { params: { date } })
}
export function getWeeklyReport() {
  return request.get('/risk/report/weekly')
}
export function exportReport(type: string) {
  return request.get('/risk/report/export', { params: { type } })
}
