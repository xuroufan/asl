import clsx from 'clsx'

interface BadgeProps {
  value: number
  prefix?: string
  suffix?: string
  size?: 'sm' | 'md'
}

export function Badge({ value, prefix = '', suffix = '', size = 'md' }: BadgeProps) {
  const isUp = value > 0
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded font-medium',
        isUp ? 'text-buy bg-buy/10' : value < 0 ? 'text-sell bg-sell/10' : 'text-gray-400 bg-gray-800',
        size === 'sm' ? 'px-1.5 py-0.5 text-xs' : 'px-2 py-1 text-sm',
      )}
    >
      {prefix}{isUp ? '+' : ''}{value.toFixed(2)}{suffix}
    </span>
  )
}
