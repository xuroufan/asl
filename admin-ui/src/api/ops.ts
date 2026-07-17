import request from '@/utils/request'

// ====== 服务状态监控 ======
export function getServiceList() {
  return request.get('/ops/service/list')
}
export function getServiceDashboard() {
  return request.get('/ops/service/dashboard')
}
export function getServiceInstances(serviceName: string) {
  return request.get('/ops/service/instances', { params: { serviceName } })
}

// ====== 服务发布管理 ======
export function getReleaseHistory(params: any) {
  return request.get('/ops/release/history', { params })
}
export function getReleaseDetail(releaseId: string) {
  return request.get('/ops/release/detail', { params: { releaseId } })
}
export function getReleaseStats() {
  return request.get('/ops/release/stats')
}
export function createRelease(data: any) {
  return request.post('/ops/release/create', data)
}
export function approveRelease(releaseId: string, comment: string) {
  return request.post('/ops/release/approve', null, { params: { releaseId, comment } })
}
export function executeRelease(releaseId: string) {
  return request.post('/ops/release/execute', null, { params: { releaseId } })
}
export function rollbackRelease(releaseId: string) {
  return request.post('/ops/release/rollback', null, { params: { releaseId } })
}

// ====== 配置变更管理 ======
export function getConfigList(params: any) {
  return request.get('/ops/config/list', { params })
}
export function getConfigDetail(configId: string) {
  return request.get('/ops/config/detail', { params: { configId } })
}
export function getConfigChangeHistory(configId: string) {
  return request.get('/ops/config/history', { params: { configId } })
}
export function applyConfigChange(data: any) {
  return request.post('/ops/config/apply', data)
}
export function approveConfigChange(changeId: string, approved: boolean, comment?: string) {
  return request.post('/ops/config/approve', null, { params: { changeId, approved, comment } })
}
export function compareConfigs(serviceName: string, envA: string, envB: string) {
  return request.get('/ops/config/compare', { params: { serviceName, envA, envB } })
}

// ====== 日志查询 ======
export function searchLogs(params: any) {
  return request.get('/ops/log/search', { params })
}
export function getLogContext(traceId: string, timestamp?: string) {
  return request.get('/ops/log/context', { params: { traceId, timestamp } })
}
export function getLogStats(params: any) {
  return request.get('/ops/log/stats', { params })
}

// ====== 告警管理 ======
export function getAlertList(params: any) {
  return request.get('/ops/alert/list', { params })
}
export function getAlertStats() {
  return request.get('/ops/alert/stats')
}
export function claimAlert(alertId: string) {
  return request.post('/ops/alert/claim', null, { params: { alertId } })
}
export function resolveAlert(alertId: string, resolution: string, notes?: string) {
  return request.post('/ops/alert/resolve', null, { params: { alertId, resolution, notes } })
}

// ====== 审计日志 ======
export function getAuditLogs(params: any) {
  return request.get('/ops/audit/list', { params })
}
