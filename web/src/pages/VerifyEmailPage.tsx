import React, { useEffect, useState } from 'react'
import { verifyEmailActionCode } from '../firebase'

interface VerifyEmailPageProps {
  onNavigateHome: () => void
}

export const VerifyEmailPage: React.FC<VerifyEmailPageProps> = ({ onNavigateHome }) => {
  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const oobCode = params.get('oobCode')
    const mode = params.get('mode')

    if (oobCode && (mode === 'verifyEmail' || !mode)) {
      verifyEmailActionCode(oobCode)
        .then(() => {
          setSuccess(true)
          setLoading(false)
        })
        .catch((err) => {
          setErrorMsg(err?.message || 'The verification link is invalid or has expired.')
          setLoading(false)
        })
    } else {
      setLoading(false)
      setErrorMsg('No valid verification code found in link.')
    }
  }, [])

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at center, #1E1B4B 0%, #0B0B0F 100%)',
        padding: '20px',
        color: '#FFFFFF',
        fontFamily: "'Inter', sans-serif"
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          borderRadius: '24px',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          padding: '36px 28px',
          textAlign: 'center',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.6)'
        }}
      >
        {/* Brand Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginBottom: '24px' }}>
          <img src="/logo.png" alt="ISAI Logo" style={{ width: '40px', height: '40px', objectFit: 'contain' }} />
          <span style={{ fontSize: '24px', fontWeight: 900, letterSpacing: '2px', color: '#00F0FF' }}>ISAI</span>
          <span style={{ fontSize: '20px', fontWeight: 700, color: '#FFF' }}>Music</span>
        </div>

        {loading ? (
          <div>
            <div
              style={{
                width: '48px',
                height: '48px',
                border: '3px solid rgba(0, 240, 255, 0.2)',
                borderTopColor: '#00F0FF',
                borderRadius: '50%',
                margin: '0 auto 20px',
                animation: 'spin 1s infinite linear'
              }}
            />
            <h2 style={{ fontSize: '18px', fontWeight: 800 }}>Verifying Your Email...</h2>
            <p style={{ color: '#aaa', fontSize: '13px', marginTop: '8px' }}>Please wait while we connect with Firebase Authentication.</p>
          </div>
        ) : success ? (
          <div>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'rgba(200, 255, 0, 0.15)',
                border: '2px solid #C8FF00',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '32px',
                margin: '0 auto 20px'
              }}
            >
              ✓
            </div>
            <h2 style={{ fontSize: '22px', fontWeight: 900, color: '#C8FF00', marginBottom: '8px' }}>
              Email Verified Successfully!
            </h2>
            <p style={{ color: '#DDD', fontSize: '14px', lineHeight: '20px', marginBottom: '24px' }}>
              Your ISAI Music account is now fully active. Enjoy unlimited Tamil music streaming!
            </p>
            <button
              onClick={onNavigateHome}
              style={{
                width: '100%',
                padding: '16px',
                borderRadius: '16px',
                background: 'linear-gradient(135deg, #C8FF00, #9ECC00)',
                color: '#0B0B0F',
                fontWeight: 900,
                fontSize: '15px',
                border: 'none',
                cursor: 'pointer',
                boxShadow: '0 8px 24px rgba(200, 255, 0, 0.3)'
              }}
            >
              Start Listening Now ➔
            </button>
          </div>
        ) : (
          <div>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'rgba(255, 82, 82, 0.15)',
                border: '2px solid #FF5252',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '28px',
                margin: '0 auto 20px'
              }}
            >
              ⚠️
            </div>
            <h2 style={{ fontSize: '20px', fontWeight: 900, color: '#FF5252', marginBottom: '8px' }}>
              Verification Failed
            </h2>
            <p style={{ color: '#AAA', fontSize: '13px', lineHeight: '18px', marginBottom: '24px' }}>
              {errorMsg || 'This verification link is invalid or has already been used.'}
            </p>
            <button
              onClick={onNavigateHome}
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '14px',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#FFF',
                fontWeight: 800,
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              Go to ISAI Music Home
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
