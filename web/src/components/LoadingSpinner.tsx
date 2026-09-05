import React from 'react'

interface LoadingSpinnerProps {
  message?: string
  subMessage?: string
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  message = 'Discovering Tamil songs...',
  subMessage
}) => {
  return (
    <div className="state-box">
      <div className="spinner"></div>
      <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '4px' }}>
        {message}
      </h3>
      {subMessage && (
        <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
          {subMessage}
        </p>
      )}
    </div>
  )
}
