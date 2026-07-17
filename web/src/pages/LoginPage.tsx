import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { TrendingUp, Eye, EyeOff } from 'lucide-react'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPwd, setShowPwd] = useState(false)
  const { login, isLoading, error } = useAuthStore()
  const navigate = useNavigate()

  const handleLogin = async () => {
    await login(username.trim(), password)
    if (useAuthStore.getState().isLoggedIn) {
      navigate('/market', { replace: true })
    }
  }

  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-[#080C1A]">
      {/* ===== Animated grid background ===== */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {/* Grid lines */}
        <svg className="absolute inset-0 w-full h-full opacity-[0.04]" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <pattern id="grid" width="60" height="60" patternUnits="userSpaceOnUse">
              <path d="M 60 0 L 0 0 0 60" fill="none" stroke="white" strokeWidth="0.5" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </svg>

        {/* Gradient orbs */}
        <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[60%] bg-brand/5 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-20%] right-[-10%] w-[40%] h-[50%] bg-[#FF6B6B]/5 rounded-full blur-[120px]" />

        {/* Horizontal line accent */}
        <div className="absolute top-[30%] left-0 right-0 h-px bg-gradient-to-r from-transparent via-brand/10 to-transparent" />
        <div className="absolute top-[70%] left-0 right-0 h-px bg-gradient-to-r from-transparent via-[#FF6B6B]/10 to-transparent" />
      </div>

      {/* ===== Main content ===== */}
      <div className="relative z-10 w-full max-w-sm px-6 animate-fade-in">
        {/* Brand */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-brand to-blue-500 rounded-2xl shadow-lg shadow-brand/20 mb-5">
            <TrendingUp className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">BlackFuture</h1>
          <p className="text-sm text-gray-500 mt-2 font-medium tracking-wide">专业期货交易终端</p>
        </div>

        {/* Login card */}
        <div className="card-glow p-7 animate-slide-up">
          <h2 className="text-lg font-bold text-gray-100 mb-6">登录</h2>

          <div className="space-y-4">
            <Input
              label="账号"
              placeholder="请输入账号"
              value={username}
              onChange={e => setUsername(e.target.value)}
              onKeyDown={e =>
                e.key === 'Enter' && document.getElementById('pwd-input')?.focus()
              }
            />

            <div className="relative">
              <Input
                id="pwd-input"
                label="密码"
                type={showPwd ? 'text' : 'password'}
                placeholder="请输入密码"
                value={password}
                onChange={e => setPassword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleLogin()}
              />
              <button
                type="button"
                onClick={() => setShowPwd(!showPwd)}
                className="absolute right-3 top-[38px] text-gray-500 hover:text-gray-300 transition-colors"
                tabIndex={-1}
              >
                {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>

            {error && (
              <div className="animate-fade-in text-sm text-[#FF6B6B] bg-[#FF6B6B]/10 rounded-lg px-3.5 py-2.5 border border-[#FF6B6B]/20 flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#FF6B6B] shrink-0" />
                {error}
              </div>
            )}

            <Button
              className="w-full mt-1"
              size="lg"
              loading={isLoading}
              disabled={!username.trim() || !password}
              onClick={handleLogin}
            >
              登录
            </Button>
          </div>
        </div>

        {/* Footer */}
        <div className="flex flex-col items-center gap-2 mt-8">
          <div className="flex items-center gap-3">
            <span className="h-px w-8 bg-gradient-to-r from-transparent via-gray-700 to-transparent" />
            <span className="text-[11px] text-gray-600 tracking-widest">模拟交易环境</span>
            <span className="h-px w-8 bg-gradient-to-r from-transparent via-gray-700 to-transparent" />
          </div>
          <p className="text-[10px] text-gray-700">无需真实资金 · 仅供学习参考</p>
        </div>
      </div>
    </div>
  )
}
