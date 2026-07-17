import request from '@/utils/request'

// ====== 用户管理 ======
export function getUserList(params: any) {
  return request.get('/system/user/list', { params })
}
export function getUserById(userId: number) {
  return request.get('/system/user/' + userId)
}
export function addUser(data: any) {
  return request.post('/system/user', data)
}
export function editUser(data: any) {
  return request.put('/system/user', data)
}
export function delUser(userId: number) {
  return request.delete('/system/user/' + userId)
}
export function resetUserPwd(userId: number) {
  return request.put('/system/user/resetPwd/' + userId)
}
export function changeUserStatus(data: any) {
  return request.put('/system/user/status', data)
}

// ====== 角色管理 ======
export function getRoleList(params: any) {
  return request.get('/system/role/list', { params })
}
export function getAllRoles() {
  return request.get('/system/role/all')
}
export function getRoleById(roleId: number) {
  return request.get('/system/role/' + roleId)
}
export function addRole(data: any) {
  return request.post('/system/role', data)
}
export function editRole(data: any) {
  return request.put('/system/role', data)
}
export function delRole(roleId: number) {
  return request.delete('/system/role/' + roleId)
}

// ====== 菜单管理 ======
export function getMenuList() {
  return request.get('/system/menu/list')
}
export function getMenuByRole(roleId: number) {
  return request.get('/system/menu/role/' + roleId)
}
export function getMenuById(menuId: number) {
  return request.get('/system/menu/' + menuId)
}
export function addMenu(data: any) {
  return request.post('/system/menu', data)
}
export function editMenu(data: any) {
  return request.put('/system/menu', data)
}
export function delMenu(menuId: number) {
  return request.delete('/system/menu/' + menuId)
}

// ====== 部门管理 ======
export function getDeptList() {
  return request.get('/system/dept/list')
}
export function getDeptById(deptId: number) {
  return request.get('/system/dept/' + deptId)
}
export function addDept(data: any) {
  return request.post('/system/dept', data)
}
export function editDept(data: any) {
  return request.put('/system/dept', data)
}
export function delDept(deptId: number) {
  return request.delete('/system/dept/' + deptId)
}

// ====== 字典管理 ======
export function getDictTypeList(params: any) {
  return request.get('/system/dict/type/list', { params })
}
export function getDictTypeById(dictId: number) {
  return request.get('/system/dict/type/' + dictId)
}
export function addDictType(data: any) {
  return request.post('/system/dict/type', data)
}
export function editDictType(data: any) {
  return request.put('/system/dict/type', data)
}
export function delDictType(dictId: number) {
  return request.delete('/system/dict/type/' + dictId)
}
export function getDictDataList(params: any) {
  return request.get('/system/dict/data/list', { params })
}
export function getDictDataById(dictCode: number) {
  return request.get('/system/dict/data/' + dictCode)
}
export function addDictData(data: any) {
  return request.post('/system/dict/data', data)
}
export function editDictData(data: any) {
  return request.put('/system/dict/data', data)
}
export function delDictData(dictCode: number) {
  return request.delete('/system/dict/data/' + dictCode)
}
export function getDictDataByType(dictType: string) {
  return request.get('/system/dict/data/type/' + dictType)
}

// ====== 参数配置 ======
export function getConfigList(params: any) {
  return request.get('/system/config/list', { params })
}
export function getConfigById(configId: number) {
  return request.get('/system/config/' + configId)
}
export function addConfig(data: any) {
  return request.post('/system/config', data)
}
export function editConfig(data: any) {
  return request.put('/system/config', data)
}
export function delConfig(configId: number) {
  return request.delete('/system/config/' + configId)
}

// ====== 日志管理 ======
export function getOperLogList(params: any) {
  return request.get('/monitor/operlog/list', { params })
}
export function delOperLog(operId: number) {
  return request.delete('/monitor/operlog/' + operId)
}
export function cleanOperLog() {
  return request.delete('/monitor/operlog/clean')
}
export function getLoginLogList(params: any) {
  return request.get('/monitor/loginlog/list', { params })
}
export function delLoginLog(infoId: number) {
  return request.delete('/monitor/loginlog/' + infoId)
}
export function cleanLoginLog() {
  return request.delete('/monitor/loginlog/clean')
}

// ====== 仪表盘 ======
export function getDashboardStats() {
  return request.get('/dashboard/stats')
}