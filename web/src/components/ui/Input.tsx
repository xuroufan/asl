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
        <label htmlFor={id} className="block text-sm text-gray-400 mb-1.5">{label}</label>
      )}
      <input
        ref={ref}
        id={id}
        className={clsx(
          'w-full bg-gray-900 border rounded-lg px-3.5 py-2.5 text-sm text-gray-100',
          'placeholder:text-gray-600 transition-colors',
          'focus:outline-none focus:ring-2 focus:ring-brand/50 focus:border-brand',
          error ? 'border-buy' : 'border-gray-700 hover:border-gray-600',
          className,
        )}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-buy">{error}</p>}
    </div>
  ),
)
Input.displayName = 'Input'
