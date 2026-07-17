import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse, UserInfo, RouterVO } from '@/api/login'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('admin-token') || '')
  const refreshToken = ref<string>(localStorage.getItem('admin-refresh-token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const menus = ref<RouterVO[]>([])
  const roles = ref<string[]>([])

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('admin-token', t)
  }

  function setRefreshToken(t: string) {
    refreshToken.value = t
    localStorage.setItem('admin-refresh-token', t)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    menus.value = info.menus || []
    roles.value = info.roles || []
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    menus.value = []
    roles.value = []
    localStorage.removeItem('admin-token')
    localStorage.removeItem('admin-refresh-token')
  }

  return { token, refreshToken, userInfo, menus, roles, setToken, setRefreshToken, setUserInfo, logout }
})