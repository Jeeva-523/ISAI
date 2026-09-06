import React, { useEffect } from 'react'
import { CheckCircle2, AlertCircle, Info } from 'lucide-react'

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
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => {
      onClose()
    }, 3000)
    return () => clearTimeout(timer)
  }, [toast, onClose])

  if (!toast) return null

  return (
    <div className="toast-snackbar">
      {toast.type === 'error' ? (
        <AlertCircle size={18} className="text-red-400" />
      ) : toast.type === 'info' ? (
        <Info size={18} className="text-blue-400" />
      ) : (
        <CheckCircle2 size={18} style={{ color: 'var(--isai-purple-light)' }} />
      )}
      <span>{toast.text}</span>
    </div>
  )
}
