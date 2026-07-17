import { type ReactNode, useEffect } from 'react'
import clsx from 'clsx'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
}

export function Modal({ open, onClose, title, children, actions }: ModalProps) {
  useEffect(() => {
    if (open) {
      const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
      document.addEventListener('keydown', handler)
      return () => document.removeEventListener('keydown', handler)
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className={clsx(
        'relative bg-surface border border-gray-700/60 rounded-2xl shadow-2xl',
        'w-full max-w-md mx-4 p-6 animate-in fade-in zoom-in-95 duration-200'
      )}>
        <h3 className="text-lg font-bold text-gray-100 mb-4">{title}</h3>
        <div className="text-sm text-gray-300 space-y-2">{children}</div>
        {actions && <div className="mt-6 flex gap-3 justify-end">{actions}</div>}
      </div>
    </div>
  )
}
