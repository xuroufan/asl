import { type ButtonHTMLAttributes, forwardRef } from 'react'
import clsx from 'clsx'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'danger' | 'success' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, children, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={clsx(
        'inline-flex items-center justify-center rounded-xl font-semibold transition-all duration-150',
        'disabled:opacity-40 disabled:cursor-not-allowed',
        'hover:scale-[1.02] active:scale-[0.98]',
        {
          'bg-brand text-white hover:bg-brand-dark active:bg-brand-dark shadow-sm hover:shadow-[0_0_20px_-5px_rgba(79,140,247,0.3)]': variant === 'primary',
          'text-gray-400 hover:text-gray-200 bg-transparent hover:bg-white/[0.04]': variant === 'ghost',
          'border border-gray-700 hover:border-gray-600 text-gray-300 hover:text-white bg-transparent': variant === 'outline',
        },
        {
          'text-xs px-2.5 py-1.5 gap-1': size === 'sm',
          'text-sm px-4 py-2.5 gap-1.5': size === 'md',
          'text-base px-6 py-3 gap-2': size === 'lg',
        },
        loading && 'relative !text-transparent',
        className,
      )}
      {...props}
    >
      {loading && (
        <span className="absolute inset-0 flex items-center justify-center">
          <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
        </span>
      )}
      {children}
    </button>
  ),
)

Button.displayName = 'Button'
