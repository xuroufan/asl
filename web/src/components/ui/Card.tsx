import type { HTMLAttributes } from 'react'
import clsx from 'clsx'

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  hover?: boolean
}

export function Card({ className, hover, children, ...props }: CardProps) {
  return (
    <div
      className={clsx(
        'bg-surface rounded-xl border border-gray-800/60',
        hover && 'hover:border-gray-700/80 transition-colors',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}
