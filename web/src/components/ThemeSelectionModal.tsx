import { Check, Palette, X } from 'lucide-react'
import React from 'react'

export type WebThemeMode = 'dark' | 'light' | 'amoled' | 'cyberpunk'

export interface ThemeOption {
  id: WebThemeMode
  title: string
  subtitle: string
  icon: string
  bgPreview: string
  accentPreview: string
}

export const THEME_OPTIONS: ThemeOption[] = [
  {
    id: 'dark',
    title: 'Midnight Dark 🌙',
    subtitle: 'Sleek deep midnight dark mode',
    icon: '🌙',
    bgPreview: '#0F141C',
    accentPreview: '#8B5CF6'
  },
  {
    id: 'light',
    title: 'Clean Snow ☀️',
    subtitle: 'Crisp, bright light mode with dark text',
    icon: '☀️',
    bgPreview: '#FFFFFF',
    accentPreview: '#7C3AED'
  },
  {
    id: 'amoled',
    title: 'AMOLED Black 🖤',
    subtitle: '100% pitch black for maximum battery saving',
    icon: '🖤',
    bgPreview: '#000000',
    accentPreview: '#10B981'
  },
  {
    id: 'cyberpunk',
    title: 'Cyberpunk Neon 🔮',
    subtitle: 'Cosmic purple aesthetic with neon glow',
    icon: '🔮',
    bgPreview: '#0B0418',
    accentPreview: '#E879F9'
  }
]

interface ThemeSelectionModalProps {
  isOpen: boolean
  currentTheme: WebThemeMode
  onClose: () => void
  onSelectTheme: (theme: WebThemeMode) => void
}

export const ThemeSelectionModal: React.FC<ThemeSelectionModalProps> = ({
  isOpen,
  currentTheme,
  onClose,
  onSelectTheme
}) => {
  if (!isOpen) return null

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(5, 3, 10, 0.85)',
        backdropFilter: 'blur(16px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 99999,
        padding: '20px'
      }}
    >
      <div
        style={{
          position: 'relative',
          background: 'var(--surface-dark)',
          border: '1px solid var(--border-glass-bright)',
          borderRadius: '24px',
          maxWidth: '520px',
          width: '100%',
          padding: '28px 24px',
          boxShadow: '0 25px 60px rgba(0, 0, 0, 0.8), 0 0 40px var(--isai-gradient-glow)'
        }}
      >
        <button
          onClick={onClose}
          aria-label="Close modal"
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            background: 'var(--surface-card)',
            border: 'none',
            borderRadius: '50%',
            width: '32px',
            height: '32px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--text-secondary)',
            cursor: 'pointer'
          }}
        >
          <X size={18} />
        </button>

        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 14px',
              borderRadius: '20px',
              background: 'rgba(124, 58, 237, 0.15)',
              border: '1px solid rgba(124, 58, 237, 0.3)',
              color: 'var(--isai-purple)',
              fontSize: '11px',
              fontWeight: 800,
              letterSpacing: '1px',
              textTransform: 'uppercase',
              marginBottom: '10px'
            }}
          >
            <Palette size={14} />
            APP THEMES
          </div>
          <h2 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '6px' }}>
            Choose App Appearance
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            Personalize ISAI with dynamic colors and dark modes
          </p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '22px' }}>
          {THEME_OPTIONS.map((theme) => {
            const isSelected = currentTheme === theme.id
            return (
              <div
                key={theme.id}
                onClick={() => onSelectTheme(theme.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '14px 16px',
                  borderRadius: '16px',
                  background: isSelected ? 'var(--surface-card-hover)' : 'var(--surface-card)',
                  border: isSelected ? '2px solid var(--isai-purple)' : '1px solid var(--border-subtle)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                  <div
                    style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '12px',
                      background: theme.bgPreview,
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      position: 'relative'
                    }}
                  >
                    <span style={{ fontSize: '18px' }}>{theme.icon}</span>
                    <div
                      style={{
                        position: 'absolute',
                        bottom: '-2px',
                        right: '-2px',
                        width: '12px',
                        height: '12px',
                        borderRadius: '50%',
                        background: theme.accentPreview,
                        border: '2px solid var(--surface-dark)'
                      }}
                    />
                  </div>
                  <div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-primary)' }}>{theme.title}</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{theme.subtitle}</div>
                  </div>
                </div>

                {isSelected ? (
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: 'var(--isai-purple)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#FFFFFF'
                    }}
                  >
                    <Check size={14} strokeWidth={3} />
                  </div>
                ) : (
                  <div
                    style={{
                      width: '20px',
                      height: '20px',
                      borderRadius: '50%',
                      border: '2px solid var(--text-disabled)'
                    }}
                  />
                )}
              </div>
            )
          })}
        </div>

        <button
          onClick={onClose}
          style={{
            width: '100%',
            height: '48px',
            borderRadius: '24px',
            background: 'var(--isai-gradient)',
            border: 'none',
            color: '#FFFFFF',
            fontSize: '15px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 8px 20px rgba(124, 58, 237, 0.3)'
          }}
        >
          Apply &amp; Done ✨
        </button>
      </div>
    </div>
  )
}
