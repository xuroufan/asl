import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock API module
vi.mock('../../api/client', () => ({
  auth: {
    login: vi.fn(),
    register: vi.fn(),
  },
  setToken: vi.fn(),
}))

import { useAuthStore } from '../../store/authStore'
import { auth, setToken } from '../../api/client'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isLoggedIn: false, isLoading: false, error: null })
    vi.clearAllMocks()
  })

  it('starts logged out with no user', () => {
    const s = useAuthStore.getState()
    expect(s.isLoggedIn).toBe(false)
    expect(s.user).toBeNull()
    expect(s.error).toBeNull()
  })

  it('sets loading state during login', () => {
    vi.mocked(auth.login).mockImplementation(() => new Promise(() => {}))
    useAuthStore.getState().login('test', 'pass')
    expect(useAuthStore.getState().isLoading).toBe(true)
  })

  it('handles successful login', async () => {
    const mockUser = { id: '1', username: 'test', displayName: 'Test', email: 't@t.com', createdAt: 0, updatedAt: 0 }
    vi.mocked(auth.login).mockResolvedValue({
      accessToken: 'token123', refreshToken: 'refresh123', expiresIn: 3600, user: mockUser,
    })

    await useAuthStore.getState().login('test', 'pass')

    const s = useAuthStore.getState()
    expect(s.isLoggedIn).toBe(true)
    expect(s.user?.username).toBe('test')
    expect(s.isLoading).toBe(false)
    expect(s.error).toBeNull()
    expect(setToken).toHaveBeenCalledWith('token123')
  })

  it('handles login failure', async () => {
    vi.mocked(auth.login).mockRejectedValue(new Error('密码错误'))

    await useAuthStore.getState().login('test', 'wrong')

    const s = useAuthStore.getState()
    expect(s.isLoggedIn).toBe(false)
    expect(s.isLoading).toBe(false)
    expect(s.error).toBe('密码错误')
  })

  it('clears state on logout', () => {
    useAuthStore.setState({ user: { id: '1', username: 'test', displayName: 'Test', email: 't@t.com', createdAt: 0, updatedAt: 0 }, isLoggedIn: true })

    useAuthStore.getState().logout()

    const s = useAuthStore.getState()
    expect(s.isLoggedIn).toBe(false)
    expect(s.user).toBeNull()
  })

  it('clears error', () => {
    useAuthStore.setState({ error: 'some error' })
    useAuthStore.getState().clearError()
    expect(useAuthStore.getState().error).toBeNull()
  })
})
