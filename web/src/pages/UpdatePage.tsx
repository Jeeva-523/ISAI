import React, { useState, useEffect, useRef } from 'react'

const APK_DOWNLOAD_URL = '/isai.dat'

export const UpdatePage: React.FC = () => {
  const [status, setStatus] = useState<'downloading' | 'completed'>('downloading')
  const [progress, setProgress] = useState(15)
  const [downloadedMb, setDownloadedMb] = useState('3.0')
  const hasTriggeredRef = useRef(false)

  const triggerDownload = () => {
    const link = document.createElement('a')
    link.href = APK_DOWNLOAD_URL
    link.setAttribute('download', 'isai.apk')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  useEffect(() => {
    if (!hasTriggeredRef.current) {
      hasTriggeredRef.current = true
      // Trigger automatic single APK download
      triggerDownload()

      let current = 15
      const timer = setInterval(() => {
        current += Math.floor(Math.random() * 20) + 15
        if (current >= 100) {
          clearInterval(timer)
          setProgress(100)
          setDownloadedMb('19.5')
          setStatus('completed')
        } else {
          setProgress(current)
          setDownloadedMb(((current / 100) * 19.5).toFixed(1))
        }
      }, 400)

      return () => clearInterval(timer)
    }
  }, [])

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at top, #1E1035 0%, #08080C 100%)',
        padding: '24px 16px',
        color: '#FFFFFF',
        fontFamily: "'Inter', system-ui, -apple-system, sans-serif"
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '520px',
          background: 'rgba(26, 21, 40, 0.92)',
          backdropFilter: 'blur(24px)',
          borderRadius: '28px',
          border: '1px solid rgba(139, 92, 246, 0.25)',
          padding: '36px 24px',
          boxShadow: '0 25px 60px rgba(0, 0, 0, 0.75), 0 0 40px rgba(139, 92, 246, 0.15)',
          textAlign: 'center'
        }}
      >
        {/* App Logo / Icon */}
        <div
          style={{
            width: '76px',
            height: '76px',
            margin: '0 auto 16px',
            borderRadius: '22px',
            background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 10px 25px rgba(139, 92, 246, 0.45)',
            fontSize: '34px'
          }}
        >
          🎵
        </div>

        {/* Badge */}
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '6px 14px',
            borderRadius: '20px',
            background: 'rgba(139, 92, 246, 0.15)',
            border: '1px solid rgba(139, 92, 246, 0.4)',
            marginBottom: '14px'
          }}
        >
          <span style={{ fontSize: '13px', color: '#A78BFA', fontWeight: 600 }}>
            v1.3.0 ➔ <span style={{ color: '#F43F5E', fontWeight: 700 }}>v1.3.1 Auto Update</span>
          </span>
        </div>

        <h1
          style={{
            fontSize: '23px',
            fontWeight: 800,
            margin: '0 0 8px',
            background: 'linear-gradient(135deg, #FFFFFF 30%, #DDD6FE 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent'
          }}
        >
          {status === 'downloading' ? 'Downloading Update Automatically... 🚀' : 'Update Downloaded! Ready to Install 🎉'}
        </h1>

        <p style={{ fontSize: '14px', color: '#9CA3AF', margin: '0 0 20px', lineHeight: '1.5' }}>
          {status === 'downloading'
            ? 'Pudhu version automatic-ah download aaguthu, konjam wait pannunga:'
            : 'Download complete aagiduchu! Keela irukkum steps follow panni install pannunga:'}
        </p>

        {/* Dynamic Download Progress Banner */}
        {status === 'downloading' && (
          <div
            style={{
              background: 'rgba(15, 12, 25, 0.85)',
              padding: '18px 20px',
              borderRadius: '20px',
              border: '1px solid rgba(139, 92, 246, 0.35)',
              marginBottom: '22px'
            }}
          >
            <div
              style={{
                width: '100%',
                height: '14px',
                background: 'rgba(255, 255, 255, 0.1)',
                borderRadius: '8px',
                overflow: 'hidden',
                marginBottom: '14px'
              }}
            >
              <div
                style={{
                  width: `${Math.min(progress, 100)}%`,
                  height: '100%',
                  background: 'linear-gradient(90deg, #8B5CF6, #10B981)',
                  borderRadius: '8px',
                  transition: 'width 0.35s ease'
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#A78BFA', fontWeight: 700 }}>
              <span>Downloading isai.apk ({downloadedMb} MB / 19.5 MB)</span>
              <span>{Math.min(progress, 100)}%</span>
            </div>
          </div>
        )}

        {/* Completed Status Notice */}
        {status === 'completed' && (
          <div
            style={{
              padding: '16px',
              borderRadius: '20px',
              background: 'rgba(16, 185, 129, 0.18)',
              border: '1.5px solid rgba(16, 185, 129, 0.5)',
              color: '#10B981',
              fontWeight: 700,
              fontSize: '15px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              marginBottom: '20px'
            }}
          >
            <span>✅</span>
            <span>isai.apk Downloaded Successfully (19.5 MB)</span>
          </div>
        )}

        {/* Feature Highlights Card */}
        <div
          style={{
            background: 'rgba(15, 12, 25, 0.7)',
            borderRadius: '18px',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            padding: '16px',
            textAlign: 'left',
            marginBottom: '20px'
          }}
        >
          <div style={{ fontSize: '12px', fontWeight: 700, color: '#A78BFA', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '10px' }}>
            What's New in v1.3.1
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', color: '#E5E7EB' }}>
              <span>💊</span>
              <span><strong>Playing Queue Capsule Pill:</strong> Modern Up, Down reorder & Delete buttons</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', color: '#E5E7EB' }}>
              <span>☁️</span>
              <span><strong>Cloud Dynamic UI Engine:</strong> Instant real-time design sync without app downloads</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', color: '#E5E7EB' }}>
              <span>🌐</span>
              <span><strong>Music Language Selection:</strong> Change listening languages anytime in Profile</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', color: '#E5E7EB' }}>
              <span>🎶</span>
              <span><strong>Strict Queue Filtering:</strong> Only your preferred language songs play next</span>
            </div>
          </div>
        </div>

        {/* Installation Instructions */}
        <div
          style={{
            padding: '16px',
            borderRadius: '18px',
            background: 'rgba(255, 255, 255, 0.04)',
            textAlign: 'left',
            fontSize: '13px',
            color: '#D1D5DB',
            lineHeight: '1.7',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            marginBottom: '14px'
          }}
        >
          <div style={{ fontWeight: 700, color: '#FFFFFF', fontSize: '14px', marginBottom: '6px' }}>
            📌 Epdi Install Pandrathu:
          </div>
          <div>1. Phone notification bar-la irukkum <strong>isai.apk</strong> tap pannunga (or Chrome Downloads ponga).</div>
          <div>2. Screen-la <strong>"Install" / "Update"</strong> button click pannunga.</div>
          <div>3. Installation mudinjathum <strong>"Open"</strong> click panna pudhu update active aagidum!</div>
        </div>

        {/* Play Protect Notice Card */}
        <div
          style={{
            padding: '12px 14px',
            borderRadius: '16px',
            background: 'rgba(245, 158, 11, 0.12)',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            textAlign: 'left',
            fontSize: '12px',
            color: '#FCD34D',
            lineHeight: '1.6',
            marginBottom: '20px'
          }}
        >
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
            <span>🛡️</span>
            <span>Play Protect "Blocked / Harmful" nu vantha:</span>
          </div>
          <div>Screen-la <strong>"More details"</strong> click panni, <strong>"Install anyway"</strong> kudunga. Namma ISAI app 100% Verified & Safe!</div>
        </div>

        {/* Subtle Fallback if automatic download was blocked by browser */}
        <div style={{ fontSize: '12px', color: '#9CA3AF' }}>
          Download start aagavilaya?{' '}
          <button
            onClick={triggerDownload}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#A78BFA',
              fontWeight: 600,
              textDecoration: 'underline',
              cursor: 'pointer',
              padding: 0
            }}
          >
            Inga click panni retry pannunga
          </button>
        </div>

        <div style={{ marginTop: '16px' }}>
          <a
            href="/"
            style={{ color: '#6B7280', fontSize: '12px', textDecoration: 'none' }}
          >
            ← Back to Web Player
          </a>
        </div>
      </div>
    </div>
  )
}
