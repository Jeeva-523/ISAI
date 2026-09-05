import React from 'react'

interface EmptyStateProps {
  icon?: string
  title: string
  description: string
  suggestions?: string[]
  onSuggestionClick?: (s: string) => void
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon = '🔍',
  title,
  description,
  suggestions,
  onSuggestionClick
}) => {
  return (
    <div className="state-box">
      <div style={{ fontSize: '42px', marginBottom: '12px' }}>{icon}</div>
      <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px' }}>
        {title}
      </h3>
      <p style={{ fontSize: '13px', color: 'var(--text-muted)', maxWidth: '420px', lineHeight: 1.5 }}>
        {description}
      </p>

      {suggestions && suggestions.length > 0 && (
        <div style={{ marginTop: '20px', width: '100%' }}>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '10px' }}>Try searching:</p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'center' }}>
            {suggestions.map((s) => (
              <button
                key={s}
                className="chip-btn"
                onClick={() => onSuggestionClick && onSuggestionClick(s)}
              >
                {s}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
