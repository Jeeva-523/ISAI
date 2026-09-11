import React, { useState } from 'react'
import { Sparkles, Check, Globe, X } from 'lucide-react'

export interface MusicLanguage {
  id: string
  name: string
  nativeName: string
  gradient: string
  icon: string
}

export const AVAILABLE_LANGUAGES: MusicLanguage[] = [
  { id: 'tamil', name: 'Tamil', nativeName: 'தமிழ்', gradient: 'linear-gradient(135deg, #EC4899, #8B5CF6)', icon: '🎵' },
  { id: 'telugu', name: 'Telugu', nativeName: 'తెలుగు', gradient: 'linear-gradient(135deg, #F59E0B, #EF4444)', icon: '🔥' },
  { id: 'hindi', name: 'Hindi', nativeName: 'हिंदी', gradient: 'linear-gradient(135deg, #8B5CF6, #3B82F6)', icon: '✨' },
  { id: 'malayalam', name: 'Malayalam', nativeName: 'മലയാളം', gradient: 'linear-gradient(135deg, #10B981, #06B6D4)', icon: '🌴' },
  { id: 'english', name: 'English', nativeName: 'Global Hits', gradient: 'linear-gradient(135deg, #6366F1, #A855F7)', icon: '🌍' },
  { id: 'kannada', name: 'Kannada', nativeName: 'ಕನ್ನಡ', gradient: 'linear-gradient(135deg, #F97316, #EAB308)', icon: '🎼' },
  { id: 'punjabi', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', gradient: 'linear-gradient(135deg, #84CC16, #10B981)', icon: '🥁' }
]

interface LanguageSelectionModalProps {
  isOpen: boolean
  onClose?: () => void
  onSave: (selectedLanguages: string[]) => void
  initialSelected?: string[]
}

export const LanguageSelectionModal: React.FC<LanguageSelectionModalProps> = ({
  isOpen,
  onClose,
  onSave,
  initialSelected = ['tamil']
}) => {
  const [selected, setSelected] = useState<string[]>(initialSelected.length > 0 ? initialSelected : ['tamil'])

  if (!isOpen) return null

  const toggleLanguage = (id: string) => {
    if (selected.includes(id)) {
      if (selected.length > 1) {
        setSelected(selected.filter((item) => item !== id))
      }
    } else {
      setSelected([...selected, id])
    }
  }

  const handleContinue = () => {
    if (selected.length > 0) {
      onSave(selected)
    }
  }

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(5, 3, 10, 0.88)',
      backdropFilter: 'blur(16px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 99999,
      padding: '20px'
    }}>
      <div style={{
        position: 'relative',
        background: 'linear-gradient(180deg, #1A1329 0%, #110B1D 100%)',
        border: '1px solid rgba(139, 92, 246, 0.35)',
        borderRadius: '24px',
        maxWidth: '560px',
        width: '100%',
        padding: '32px 28px',
        boxShadow: '0 25px 60px rgba(0, 0, 0, 0.8), 0 0 40px rgba(139, 92, 246, 0.2)'
      }}>
        {onClose && (
          <button
            onClick={onClose}
            aria-label="Close modal"
            style={{
              position: 'absolute',
              top: '20px',
              right: '20px',
              background: 'rgba(255, 255, 255, 0.08)',
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
        )}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(139, 92, 246, 0.15)',
            border: '1px solid rgba(139, 92, 246, 0.3)',
            borderRadius: '20px',
            padding: '6px 14px',
            color: 'var(--isai-purple-light)',
            fontSize: '12px',
            fontWeight: 800,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            marginBottom: '12px'
          }}>
            <Globe size={14} /> Personalize Your Music
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 900, color: 'var(--text-primary)', marginBottom: '8px' }}>
            What do you want to listen to?
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            Choose one or more music languages to personalize your home feed and daily recommendations.
          </p>
        </div>

        {/* Language Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
          gap: '12px',
          maxHeight: '360px',
          overflowY: 'auto',
          padding: '4px',
          marginBottom: '28px'
        }}>
          {AVAILABLE_LANGUAGES.map((lang) => {
            const isSelected = selected.includes(lang.id)
            return (
              <div
                key={lang.id}
                onClick={() => toggleLanguage(lang.id)}
                style={{
                  background: lang.gradient,
                  borderRadius: '16px',
                  padding: '16px 14px',
                  position: 'relative',
                  cursor: 'pointer',
                  userSelect: 'none',
                  border: isSelected ? '2px solid #ffffff' : '2px solid transparent',
                  transform: isSelected ? 'scale(1.02)' : 'scale(1)',
                  boxShadow: isSelected ? '0 10px 25px rgba(0, 0, 0, 0.5), 0 0 16px rgba(255, 255, 255, 0.3)' : '0 4px 12px rgba(0, 0, 0, 0.3)',
                  transition: 'all 0.18s ease'
                }}
              >
                {/* Check badge */}
                <div style={{
                  position: 'absolute',
                  top: '10px',
                  right: '10px',
                  width: '22px',
                  height: '22px',
                  borderRadius: '50%',
                  backgroundColor: isSelected ? '#ffffff' : 'rgba(0, 0, 0, 0.3)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {isSelected && <Check size={14} color="#110B1D" strokeWidth={3} />}
                </div>

                <div style={{ fontSize: '24px', marginBottom: '8px' }}>{lang.icon}</div>
                <div style={{ fontSize: '16px', fontWeight: 900, color: '#ffffff', textShadow: '0 2px 4px rgba(0,0,0,0.4)' }}>
                  {lang.name}
                </div>
                <div style={{ fontSize: '12px', color: 'rgba(255, 255, 255, 0.85)', fontWeight: 600 }}>
                  {lang.nativeName}
                </div>
              </div>
            )
          })}
        </div>

        {/* Continue Button */}
        <button
          onClick={handleContinue}
          disabled={selected.length === 0}
          style={{
            width: '100%',
            background: 'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)',
            color: '#ffffff',
            border: 'none',
            borderRadius: '16px',
            padding: '14px 20px',
            fontSize: '15px',
            fontWeight: 800,
            cursor: selected.length === 0 ? 'not-allowed' : 'pointer',
            opacity: selected.length === 0 ? 0.5 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)'
          }}
        >
          <Sparkles size={18} />
          Continue to ISAI Music ({selected.length} Selected)
        </button>
      </div>
    </div>
  )
}
