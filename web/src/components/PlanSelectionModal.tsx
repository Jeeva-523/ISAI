import React, { useState } from 'react'
import { Check, X, Sparkles } from 'lucide-react'

interface PlanSelectionModalProps {
  isOpen: boolean
  currentPlan?: 'FREE' | 'PREMIUM'
  onSelectPlan: (plan: 'FREE' | 'PREMIUM') => void
  onClose: () => void
}

export const PlanSelectionModal: React.FC<PlanSelectionModalProps> = ({
  isOpen,
  currentPlan = 'FREE',
  onSelectPlan,
  onClose
}) => {
  const [selectedPlan, setSelectedPlan] = useState<'FREE' | 'PREMIUM'>(currentPlan)

  if (!isOpen) return null

  const freeFeatures = [
    { emoji: '🎵', text: 'Unlimited song listening' },
    { emoji: '🚫', text: 'No song ads' },
    { emoji: '⏭️', text: 'Unlimited skips' },
    { emoji: '🔎', text: 'Song search' },
    { emoji: '📋', text: 'Playlist creation & management' },
    { emoji: '🎧', text: 'Standard audio quality (160kbps)' },
    { emoji: '🌐', text: 'Online listening' },
    { emoji: '📝', text: 'Basic synchronized lyrics' }
  ]

  const premiumFeatures = [
    { emoji: '🎧', text: 'High Quality Audio (320kbps Lossless)' },
    { emoji: '📱', text: '2–3 Devices Sync (Spotify Connect Style)' },
    { emoji: '🎶', text: 'Listen Together / Music Room' },
    { emoji: '🔀', text: 'Advanced Queue Control & Reordering' },
    { emoji: '🎚️', text: 'Seamless Audio Crossfade' },
    { emoji: '😴', text: 'Custom Sleep Timer' },
    { emoji: '🎨', text: 'Premium Dynamic Themes' },
    { emoji: '🤖', text: 'Advanced AI Recommendations' },
    { emoji: '👤', text: 'Premium Profile Badge (VIP Crown)' }
  ]

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(10px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10000,
        animation: 'fadeIn 0.2s ease-out'
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#12131A',
          border: '1px solid rgba(200, 255, 0, 0.3)',
          borderRadius: '24px',
          padding: '28px',
          width: '92%',
          maxWidth: '520px',
          color: '#FFFFFF',
          boxShadow: '0 25px 60px rgba(0, 0, 0, 0.85)',
          maxHeight: '88vh',
          overflowY: 'auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '22px', fontWeight: 900, color: '#FFFFFF' }}>
              Choose Your Plan
            </h2>
            <p style={{ margin: 0, fontSize: '13px', color: '#9CA3AF' }}>
              Select your preferred music streaming experience
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: 'none',
              borderRadius: '50%',
              width: '34px',
              height: '34px',
              color: '#9CA3AF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer'
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Plans Container */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '22px' }}>
          {/* Plan 1: ISAI Free */}
          <div
            onClick={() => setSelectedPlan('FREE')}
            style={{
              borderRadius: '18px',
              padding: '18px',
              backgroundColor: selectedPlan === 'FREE' ? '#1E212B' : 'rgba(255, 255, 255, 0.03)',
              border: selectedPlan === 'FREE' ? '2px solid #C8FF00' : '1px solid rgba(255, 255, 255, 0.1)',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '24px' }}>🆓</span>
                <div>
                  <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 800, color: '#FFFFFF' }}>ISAI Free</h3>
                  <span style={{ fontSize: '11px', color: '#9CA3AF' }}>Full Access • Zero Cost</span>
                </div>
              </div>
              <div
                style={{
                  width: '22px',
                  height: '22px',
                  borderRadius: '50%',
                  backgroundColor: selectedPlan === 'FREE' ? '#C8FF00' : 'transparent',
                  border: selectedPlan === 'FREE' ? 'none' : '1.5px solid rgba(255, 255, 255, 0.3)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {selectedPlan === 'FREE' && <Check size={14} color="#000000" />}
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {freeFeatures.map((f, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#D1D5DB' }}>
                  <span>{f.emoji}</span>
                  <span>{f.text}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Plan 2: ISAI Premium */}
          <div
            onClick={() => setSelectedPlan('PREMIUM')}
            style={{
              borderRadius: '18px',
              padding: '18px',
              background: selectedPlan === 'PREMIUM'
                ? 'linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%)'
                : 'rgba(255, 255, 255, 0.03)',
              border: selectedPlan === 'PREMIUM' ? '2px solid #F59E0B' : '1px solid rgba(255, 255, 255, 0.1)',
              cursor: 'pointer',
              transition: 'all 0.2s',
              boxShadow: selectedPlan === 'PREMIUM' ? '0 8px 25px rgba(245, 158, 11, 0.2)' : 'none'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '24px' }}>💎</span>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 800, color: '#FFFFFF' }}>ISAI Premium</h3>
                    <span
                      style={{
                        backgroundColor: '#F59E0B',
                        color: '#000000',
                        fontSize: '10px',
                        fontWeight: 900,
                        padding: '2px 6px',
                        borderRadius: '4px'
                      }}
                    >
                      VIP
                    </span>
                  </div>
                  <span style={{ fontSize: '11px', color: '#F59E0B' }}>Everything in Free + VIP Features</span>
                </div>
              </div>
              <div
                style={{
                  width: '22px',
                  height: '22px',
                  borderRadius: '50%',
                  backgroundColor: selectedPlan === 'PREMIUM' ? '#F59E0B' : 'transparent',
                  border: selectedPlan === 'PREMIUM' ? 'none' : '1.5px solid rgba(255, 255, 255, 0.3)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {selectedPlan === 'PREMIUM' && <Check size={14} color="#000000" />}
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {premiumFeatures.map((f, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#D1D5DB' }}>
                  <span>{f.emoji}</span>
                  <span>{f.text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* CTA Button */}
        <button
          onClick={() => {
            onSelectPlan(selectedPlan)
            onClose()
          }}
          style={{
            width: '100%',
            backgroundColor: selectedPlan === 'PREMIUM' ? '#F59E0B' : '#C8FF00',
            color: '#000000',
            fontWeight: 800,
            fontSize: '15px',
            padding: '14px',
            borderRadius: '14px',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: selectedPlan === 'PREMIUM' ? '0 4px 18px rgba(245, 158, 11, 0.35)' : '0 4px 18px rgba(200, 255, 0, 0.35)'
          }}
        >
          <Sparkles size={18} />
          <span>{selectedPlan === 'PREMIUM' ? 'Continue with ISAI Premium' : 'Continue with ISAI Free'}</span>
        </button>
      </div>
    </div>
  )
}
