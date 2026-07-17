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
        'inline-flex items-center justify-center rounded-lg font-semibold transition-all duration-150',
        'disabled:opacity-40 disabled:cursor-not-allowed',
        {
          'bg-brand text-white hover:bg-brand-dark active:bg-brand-dark': variant === 'primary',
          'bg-buy text-white hover:bg-red-500 active:bg-red-500': variant === 'danger',
          'bg-sell text-white hover:bg-green-500 active:bg-green-500': variant === 'success',
          'bg-transparent text-gray-300 hover:text-white hover:bg-white/5': variant === 'ghost',
          'border border-gray-700 text-gray-300 hover:text-white hover:border-gray-500': variant === 'outline',
        },
        {
          'px-3 py-1.5 text-sm gap-1.5': size === 'sm',
          'px-5 py-2.5 text-sm gap-2': size === 'md',
          'px-6 py-3 text-base gap-2': size === 'lg',
        },
        className,
      )}
      {...props}
    >
      {loading && (
        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      )}
      {children}
    </button>
  ),
)
Button.displayName = 'Button'
