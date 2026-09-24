import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import React, { useEffect, useRef } from 'react'

export interface ToastMessage {
  id: string
  text: string
  type?: 'success' | 'error' | 'info'
}

interface ToastProps {
  toast: ToastMessage | null
  onClose: () => void
}

export const Toast: React.FC<ToastProps> = ({ toast, onClose }) => {
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => {
      onCloseRef.current()
    }, 2500)
    return () => clearTimeout(timer)
  }, [toast?.id])

  if (!toast) return null

  return (
    <div className="toast-snackbar" onClick={() => onCloseRef.current()} style={{ cursor: 'pointer' }}>
      {toast.type === 'error' ? (
        <AlertCircle size={18} className="text-red-400" />
      ) : toast.type === 'info' ? (
        <Info size={18} className="text-blue-400" />
      ) : (
        <CheckCircle2 size={18} style={{ color: 'var(--isai-purple-light)' }} />
      )}
      <span>{toast.text}</span>
      <button
        onClick={(e) => {
          e.stopPropagation()
          onCloseRef.current()
        }}
        style={{
          background: 'transparent',
          border: 'none',
          color: 'var(--text-muted)',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          padding: '2px',
          marginLeft: '6px'
        }}
        title="Dismiss"
      >
        <X size={14} />
      </button>
    </div>
  )
}
