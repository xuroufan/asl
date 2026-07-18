import { Outlet, NavLink, useLocation } from 'react-router-dom'
import {
  BarChart3, ArrowLeftRight, Wallet, Bell, User,
} from 'lucide-react'
import clsx from 'clsx'
import { useTranslation } from '../i18n/useTranslation'
import { LanguageSwitcher } from './LanguageSwitcher'

const navItems = [
  { to: '/market', label: '行情', icon: BarChart3, tKey: 'nav.market' },
  { to: '/trading', label: '交易', icon: ArrowLeftRight, tKey: 'nav.trading' },
  { to: '/positions', label: '持仓', icon: Wallet, tKey: 'nav.positions' },
  { to: '/messages', label: '消息', icon: Bell, tKey: 'nav.messages' },
  { to: '/settings', label: '我的', icon: User, tKey: 'nav.settings' },
]

export function Layout() {
  const location = useLocation()
  const { t } = useTranslation()

  return (
    <div className="min-h-screen flex flex-col bg-ink">
      {/* Ambient background glow */}
      <div className="bg-ambient" />

      {/* Top status bar with page indicator */}
      <header className="sticky top-0 z-30 glass-strong px-4 py-3">
        <div className="max-w-lg mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-brand/15 flex items-center justify-center">
              <BarChart3 className="w-4 h-4 text-brand" />
            </div>
            <span className="text-sm font-bold text-gradient">BlackFuture</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#00C853]/10 border border-[#00C853]/20">
              <span className="w-1.5 h-1.5 rounded-full bg-sell animate-breathe" />
              <span className="text-[10px] text-sell font-medium">已连接</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 pb-16" key={location.pathname}>
        <div className="page-enter">
          <Outlet />
        </div>
      </main>

      {/* Bottom navigation with glass effect */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 glass-strong border-t border-gray-800/40">
        <div className="max-w-lg mx-auto flex items-center justify-around h-16 px-1">
          {navItems.map(item => {
            const isActive = location.pathname === item.to
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={clsx(
                  'relative flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-xl transition-all duration-200 text-[10px] font-medium',
                  isActive
                    ? 'text-brand'
                    : 'text-gray-600 hover:text-gray-400',
                )}
              >
                {/* Active glow indicator */}
                {isActive && (
                  <>
                    <span className="absolute -top-px left-1/2 -translate-x-1/2 w-8 h-0.5 bg-brand rounded-full" />
                    <span className="absolute inset-0 rounded-xl bg-brand/5" />
                  </>
                )}
                <item.icon className={clsx(
                  'w-[22px] h-[22px] relative',
                  isActive && 'drop-shadow-[0_0_8px_rgba(79,140,247,0.5)]',
                )} />
                <span className="relative">{item.label}</span>
              </NavLink>
            )
          })}
        </div>
      </nav>
    </div>
  )
}
