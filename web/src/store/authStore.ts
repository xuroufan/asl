import { create } from 'zustand'
import type { User } from '../types'
import { auth, setToken } from '../api/client'

interface AuthState {
  user: User | null
  isLoggedIn: boolean
  isLoading: boolean
  error: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  clearError: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoggedIn: !!localStorage.getItem('access_token'),
  isLoading: false,
  error: null,

  login: async (username, password) => {
    set({ isLoading: true, error: null })
    try {
      const res = await auth.login({ username, password })
      setToken(res.accessToken)
      set({ user: res.user, isLoggedIn: true, isLoading: false })
    } catch (e: any) {
      set({ isLoading: false, error: e.message || '登录失败' })
    }
  },

  logout: () => {
    setToken(null)
    set({ user: null, isLoggedIn: false })
  },

  clearError: () => set({ error: null }),
}))
