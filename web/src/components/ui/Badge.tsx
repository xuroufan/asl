import clsx from 'clsx'

interface BadgeProps {
  value: number
  prefix?: string
  suffix?: string
  size?: 'sm' | 'md'
}

export function Badge({ value, prefix = '', suffix = '', size = 'md' }: BadgeProps) {
  const isUp = value > 0
  const isDown = value < 0
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-md font-medium font-mono transition-all',
        isUp ? 'badge-up' : isDown ? 'badge-down' : 'badge-neutral',
        size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-1 text-xs',
      )}
    >
      {isUp && <span className="mr-0.5 text-[9px]">&#9650;</span>}
      {isDown && <span className="mr-0.5 text-[9px]">&#9660;</span>}
      {prefix}{isUp ? '+' : ''}{value.toFixed(2)}{suffix}
    </span>
  )
}
