import { type InputHTMLAttributes, forwardRef } from 'react'
import clsx from 'clsx'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, id, ...props }, ref) => (
    <div className="w-full">
      {label && (
        <label htmlFor={id} className="block text-xs text-gray-500 mb-1.5 font-medium">{label}</label>
      )}
      <input
        ref={ref}
        id={id}
        className={clsx(
          'w-full bg-ink-lighter border rounded-xl px-3.5 py-2.5 text-sm text-gray-100',
          'placeholder:text-gray-700 transition-all duration-150',
          'focus:outline-none focus:border-brand/30 focus:shadow-[0_0_12px_-4px_rgba(79,140,247,0.15)]',
          error ? 'border-buy/50 focus:border-buy/60' : 'border-gray-800 hover:border-gray-700',
          className,
        )}
        {...props}
      />
      {error && (
        <p className="mt-1 text-[10px] text-buy">{error}</p>
      )}
    </div>
  ),
)

Input.displayName = 'Input'
