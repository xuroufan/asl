import { Outlet, NavLink, useLocation } from 'react-router-dom'
import {
  BarChart3, ArrowLeftRight, Wallet, Bell, User,
} from 'lucide-react'
import clsx from 'clsx'

const navItems = [
  { to: '/market', label: '行情', icon: BarChart3 },
  { to: '/trading', label: '交易', icon: ArrowLeftRight },
  { to: '/positions', label: '持仓', icon: Wallet },
  { to: '/messages', label: '消息', icon: Bell },
  { to: '/settings', label: '我的', icon: User },
]

export function Layout() {
  const location = useLocation()

  return (
    <div className="min-h-screen flex flex-col bg-[#080C1A]">
      {/* Main content with page enter animation */}
      <main className="flex-1 pb-16" key={location.pathname}>
        <div className="page-enter">
          <Outlet />
        </div>
      </main>

      {/* Bottom navigation */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 glass">
        <div className="max-w-lg mx-auto flex items-center justify-around h-16 px-1">
          {navItems.map(item => {
            const isActive = location.pathname === item.to
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={clsx(
                  'relative flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-lg transition-all duration-150 text-[10px] font-medium',
                  isActive
                    ? 'text-brand'
                    : 'text-gray-600 hover:text-gray-400',
                )}
              >
                {/* Active indicator line */}
                {isActive && (
                  <span className="absolute -top-1 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-brand rounded-full" />
                )}
                <item.icon className={clsx('w-[22px] h-[22px]', isActive && 'drop-shadow-[0_0_6px_rgba(79,140,247,0.4)]')} />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </div>
      </nav>
    </div>
  )
}
