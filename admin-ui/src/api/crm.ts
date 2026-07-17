import request from '@/utils/request'

// ====== 客户信息管理 ======
export function getCustomerList(params: any) {
  return request.get('/crm/customer/list', { params })
}
export function getCustomerDetail(customerId: number) {
  return request.get('/crm/customer/detail', { params: { customerId } })
}
export function getCustomerPortfolio(customerId: number) {
  return request.get('/crm/customer/portfolio', { params: { customerId } })
}

// ====== 客户等级管理 ======
export function getLevelDefinitions() {
  return request.get('/crm/level/definitions')
}
export function getLevelRules() {
  return request.get('/crm/level/rules')
}
export function getLevelHistory(params: any) {
  return request.get('/crm/level/history', { params })
}
export function updateCustomerLevel(customerId: number, newLevel: string, reason?: string) {
  return request.post('/crm/level/update', null, { params: { customerId, newLevel, reason } })
}

// ====== 客户标签管理 ======
export function getTagList(category?: string) {
  return request.get('/crm/tag/list', { params: { category } })
}
export function createTag(data: any) {
  return request.post('/crm/tag/create', data)
}
export function deleteTag(tagId: number) {
  return request.delete('/crm/tag/delete', { params: { tagId } })
}
export function getCustomerTags(customerId: number) {
  return request.get('/crm/tag/customer-tags', { params: { customerId } })
}
export function assignTag(customerId: number, tagId: number) {
  return request.post('/crm/tag/assign', null, { params: { customerId, tagId } })
}
export function removeTag(customerId: number, tagId: number) {
  return request.post('/crm/tag/remove', null, { params: { customerId, tagId } })
}

// ====== 客户沟通记录 ======
export function getCommunicationList(params: any) {
  return request.get('/crm/communication/list', { params })
}
export function createCommunication(data: any) {
  return request.post('/crm/communication/create', data)
}
export function getFollowUpList(params: any) {
  return request.get('/crm/communication/follow-ups', { params })
}

// ====== 客户反馈处理 ======
export function getFeedbackList(params: any) {
  return request.get('/crm/feedback/list', { params })
}
export function createFeedback(data: any) {
  return request.post('/crm/feedback/create', data)
}
export function assignFeedback(feedbackId: number, assignee: string) {
  return request.post('/crm/feedback/assign', null, { params: { feedbackId, assignee } })
}
export function updateFeedbackStatus(feedbackId: number, status: string, resolution?: string) {
  return request.post('/crm/feedback/status', null, { params: { feedbackId, status, resolution } })
}
export function getFeedbackStats() {
  return request.get('/crm/feedback/stats')
}
