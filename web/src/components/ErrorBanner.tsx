import React from 'react'

interface ErrorBannerProps {
  title?: string
  message: string
  onRetry?: () => void
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
  title = 'Playback Error',
  message,
  onRetry
}) => {
  return (
    <div className="state-box">
      <div style={{ fontSize: '38px', marginBottom: '12px' }}>⚠️</div>
      <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--neon-cyan)', marginBottom: '6px' }}>
        {title}
      </h3>
      <p style={{ fontSize: '13px', color: 'var(--text-muted)', maxWidth: '420px', lineHeight: 1.5 }}>
        {message}
      </p>
      {onRetry && (
        <button className="btn-gradient" onClick={onRetry}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="23 4 23 10 17 10"></polyline>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
          </svg>
          Retry Search
        </button>
      )}
    </div>
  )
}
