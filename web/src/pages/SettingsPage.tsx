import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { User, Globe, Moon, Bell, Shield, Info, ChevronRight, LogOut } from 'lucide-react'

const settings = [
  { icon: Globe, label: '语言', trailing: '简体中文' },
  { icon: Moon, label: '主题', trailing: '跟随系统' },
  { icon: Bell, label: '通知设置' },
  { icon: Shield, label: '安全设置' },
]

export function SettingsPage() {
  const { logout } = useAuthStore()
  const navigate = useNavigate()

  return (
    <div className="max-w-lg mx-auto px-4 pt-4 pb-4">
      {/* User info */}
      <div className="flex items-center gap-4 mb-6 pt-2">
        <div className="w-14 h-14 rounded-full bg-brand/15 flex items-center justify-center">
          <User className="w-7 h-7 text-brand" />
        </div>
        <div>
          <div className="text-lg font-bold">期货交易用户</div>
          <div className="text-sm text-gray-500">ID: UH****1234</div>
        </div>
      </div>

      {/* Settings */}
      <Card className="divide-y divide-gray-800/60 mb-4">
        <div className="px-4 py-2 text-xs text-gray-500 font-medium">设置</div>
        {settings.map((item, i) => (
          <button
            key={i}
            className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-white/[0.02] transition-colors text-left"
          >
            <item.icon className="w-5 h-5 text-gray-400 shrink-0" />
            <span className="flex-1 text-sm text-gray-200">{item.label}</span>
            {'trailing' in item && (
              <span className="text-xs text-gray-500">{item.trailing}</span>
            )}
            <ChevronRight className="w-4 h-4 text-gray-600" />
          </button>
        ))}
      </Card>

      <Card className="divide-y divide-gray-800/60 mb-4">
        <div className="px-4 py-2 text-xs text-gray-500 font-medium">其他</div>
        <button className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-white/[0.02] transition-colors text-left">
          <Info className="w-5 h-5 text-gray-400 shrink-0" />
          <span className="flex-1 text-sm text-gray-200">关于</span>
          <span className="text-xs text-gray-500">v1.0.0</span>
          <ChevronRight className="w-4 h-4 text-gray-600" />
        </button>
      </Card>

      <Button
        variant="ghost"
        className="w-full !text-buy hover:!bg-buy/5"
        onClick={() => { logout(); navigate('/login') }}
      >
        <LogOut className="w-4 h-4" />
        退出登录
      </Button>
    </div>
  )
}
