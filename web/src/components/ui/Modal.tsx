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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in"
        onClick={onClose}
      />
      {/* Panel */}
      <div className="relative w-full max-w-sm bg-surface rounded-2xl border border-gray-800 shadow-2xl animate-scale-in">
        {/* Header */}
        <div className="flex items-center justify-between px-4 pt-4 pb-3 border-b border-gray-800/40">
          <h2 className="text-sm font-bold">{title}</h2>
          <button
            onClick={onClose}
            className="w-6 h-6 rounded-lg flex items-center justify-center text-gray-600 hover:text-gray-400 hover:bg-gray-800 transition-colors"
          >
            &times;
          </button>
        </div>
        {/* Body */}
        <div className="px-4 py-3">
          {children}
        </div>
        {/* Actions */}
        {actions && (
          <div className="px-4 pb-4 pt-2 flex gap-2">
            {actions}
          </div>
        )}
      </div>
    </div>
  )
}
