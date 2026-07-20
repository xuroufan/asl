import { type ReactNode, useEffect, useRef } from 'react'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
}

export function Modal({ open, onClose, title, children, actions }: ModalProps) {
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    if (open) {
      const handler = (e: KeyboardEvent) => {
        if (e.key === 'Escape') onCloseRef.current()
      }
      document.addEventListener('keydown', handler)
      return () => document.removeEventListener('keydown', handler)
    }
  }, [open])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative w-full max-w-sm bg-surface rounded-2xl border border-gray-800 shadow-2xl">
        <div className="flex items-center justify-between px-4 pt-4 pb-3 border-b border-gray-800/40">
          <h2 className="text-sm font-bold">{title}</h2>
          <button
            onClick={onClose}
            className="w-6 h-6 rounded-lg flex items-center justify-center text-gray-600 hover:text-gray-400 hover:bg-gray-800 transition-colors"
          >
            &times;
          </button>
        </div>
        <div className="px-4 py-3">{children}</div>
        {actions && (
          <div className="px-4 pb-4 pt-2 flex gap-2">{actions}</div>
        )}
      </div>
    </div>
  )
}
