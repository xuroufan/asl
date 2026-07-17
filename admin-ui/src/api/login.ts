import request from '@/utils/request'

export interface LoginData {
  username: string
  password: string
  code?: string
  uuid?: string
}

export interface RouterVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  redirect?: string
  meta: {
    title: string
    icon: string
    hideMenu?: boolean
    roles?: string[]
  }
  children?: RouterVO[]
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  menus: RouterVO[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserInfo
}

export function loginApi(data: LoginData) {
  return request.post<any, { code: number; msg: string; data: LoginResponse }>('/auth/login', data)
}

export function getUserInfoApi() {
  return request.get<any, { code: number; msg: string; data: UserInfo }>('/auth/userinfo')
}

export function refreshTokenApi(refreshToken: string) {
  return request.post<any, any>('/auth/refresh', null, { params: { refreshToken } })
}


export function registerApi(data: { username: string; email: string; password: string; confirmPassword: string; inviteCode?: string }) {
  return request.post<any, { code: number; msg: string; data: LoginResponse }>('/auth/register', data)
}

export function googleOauthApi(idToken: string, email?: string, name?: string) {
  return request.post<any, { code: number; msg: string; data: LoginResponse }>('/auth/oauth/google', { idToken, email, name })
}

export function logoutApi() {
  return request.post<any, any>('/auth/logout')
}