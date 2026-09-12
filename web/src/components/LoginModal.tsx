import React, { useState, useEffect } from 'react'
import {
  registerWithEmailPassword,
  loginWithEmailPassword,
  checkEmailVerificationStatus,
  resendVerificationEmail,
  logoutFirebaseUser
} from '../firebase'

export interface UserProfile {
  name: string
  email: string
  avatar: string
  isLoggedIn: boolean
  isPremium: boolean
  selectedPlan?: string
  preferredLanguages?: string[]
}

interface LoginModalProps {
  isOpen: boolean
  onClose: () => void
  user: UserProfile
  onLogin: (userData: Partial<UserProfile>) => void
  onLogout: () => void
}

type AuthMode = 'SIGN_IN' | 'REGISTER' | 'VERIFY_EMAIL'

export const LoginModal: React.FC<LoginModalProps> = ({
  isOpen,
  onClose,
  user,
  onLogin,
  onLogout
}) => {
  const [editingName, setEditingName] = useState(user.name || 'ISAI Listener')
  const [isSavedNotice, setIsSavedNotice] = useState(false)
  const [loading, setLoading] = useState(false)

  // Auth Mode State
  const [authMode, setAuthMode] = useState<AuthMode>('SIGN_IN')
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [resendCooldown, setResendCooldown] = useState(0)

  useEffect(() => {
    let timer: any = null
    if (resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => prev - 1)
      }, 1000)
    }
    return () => clearInterval(timer)
  }, [resendCooldown])

  if (!isOpen) return null

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim() || !password.trim()) {
      setErrorMessage('Please enter both email and password.')
      return
    }

    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      const res = await loginWithEmailPassword(email, password)
      if (res.emailVerified) {
        onLogin(res)
        onClose()
      } else {
        setAuthMode('VERIFY_EMAIL')
        setErrorMessage('Your email is not verified yet. Please check your inbox/spam folder and click the verification link.')
      }
    } catch (err: any) {
      setErrorMessage(err?.message || 'Sign in failed.')
    } finally {
      setLoading(false)
    }
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!displayName.trim() || !email.trim() || !password.trim()) {
      setErrorMessage('Please fill in all required fields.')
      return
    }
    if (password.length < 6) {
      setErrorMessage('Password must be at least 6 characters.')
      return
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match. Please re-enter.')
      return
    }

    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      await registerWithEmailPassword(email, password, displayName)
      setAuthMode('VERIFY_EMAIL')
      setStatusMessage(`🎉 Account Created Successfully! A verification email has been sent to ${email}. Please check your inbox / spam folder and click the verification link.`)
    } catch (err: any) {
      setErrorMessage(err?.message || 'Registration failed.')
    } finally {
      setLoading(false)
    }
  }

  const handleCheckVerification = async () => {
    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      const isVerified = await checkEmailVerificationStatus()
      if (isVerified) {
        onLogin({
          name: displayName || email.split('@')[0],
          email: email,
          avatar: (displayName || email).slice(0, 1).toUpperCase(),
          isLoggedIn: true,
          isPremium: true
        })
        onClose()
      } else {
        setErrorMessage('Your email is not verified yet. Please click the link sent to your inbox and try again.')
      }
    } catch (err: any) {
      setErrorMessage(err?.message || 'Error checking verification status.')
    } finally {
      setLoading(false)
    }
  }

  const handleResendEmail = async () => {
    if (resendCooldown > 0) return
    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      await resendVerificationEmail()
      setStatusMessage('Verification email sent again. Please check your inbox/spam folder.')
      setResendCooldown(30)
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to resend verification email.')
    } finally {
      setLoading(false)
    }
  }

  const handleSaveUsername = (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingName.trim()) return
    onLogin({ name: editingName.trim() })
    setIsSavedNotice(true)
    setTimeout(() => setIsSavedNotice(false), 2000)
  }

  return (
    <div className="ytm-login-modal-overlay" onClick={onClose}>
      <div className="ytm-login-modal" onClick={(e) => e.stopPropagation()}>
        {/* Top Header */}
        <div className="ytm-login-header">
          <div className="ytm-login-brand">
            <div className="ytm-logo-box">
              <img src="/logo.png" alt="ISAI Logo" className="ytm-logo-img" />
            </div>
            <span className="isai-brand-font">ISAI</span>
            <span className="ytm-logo-text">HUB</span>
          </div>
        </div>

        {/* Logged In View */}
        {user.isLoggedIn ? (
          <div className="ytm-profile-view">
            <div className="ytm-profile-hero">
              <div className="ytm-profile-hero-avatar">
                {user.avatar || user.name.slice(0, 1) || 'J'}
              </div>
              <h2 className="ytm-profile-hero-name">{user.name}</h2>
              <p className="ytm-profile-hero-email">{user.email}</p>
              <div className="ytm-premium-badge">⚡ ISAI PREMIUM ACTIVE</div>
            </div>

            <form onSubmit={handleSaveUsername} className="ytm-settings-box">
              <h4 className="ytm-settings-title">⚙️ Edit Profile Username</h4>
              <div className="ytm-settings-input-group">
                <input
                  type="text"
                  value={editingName}
                  onChange={(e) => setEditingName(e.target.value)}
                  placeholder="Enter your username..."
                  className="ytm-login-input"
                  required
                />
                <button type="submit" className="ytm-btn-save-name">Save</button>
              </div>
              {isSavedNotice && <span className="ytm-saved-badge">✓ Username updated!</span>}
            </form>

            <button
              className="ytm-btn-logout"
              onClick={() => {
                logoutFirebaseUser()
                onLogout()
                onClose()
              }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          /* Authentication & Email Verification Forms */
          <div className="ytm-login-body">
            {authMode !== 'VERIFY_EMAIL' && (
              <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', justifyContent: 'center' }}>
                <button
                  type="button"
                  onClick={() => { setAuthMode('SIGN_IN'); setErrorMessage(null); setStatusMessage(null); }}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '20px',
                    border: 'none',
                    background: authMode === 'SIGN_IN' ? '#C8FF00' : 'rgba(255,255,255,0.1)',
                    color: authMode === 'SIGN_IN' ? '#0B0B0F' : '#FFF',
                    fontWeight: 800,
                    cursor: 'pointer',
                    fontSize: '13px'
                  }}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  onClick={() => { setAuthMode('REGISTER'); setErrorMessage(null); setStatusMessage(null); }}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '20px',
                    border: 'none',
                    background: authMode === 'REGISTER' ? '#C8FF00' : 'rgba(255,255,255,0.1)',
                    color: authMode === 'REGISTER' ? '#0B0B0F' : '#FFF',
                    fontWeight: 800,
                    cursor: 'pointer',
                    fontSize: '13px'
                  }}
                >
                  Create Account
                </button>
              </div>
            )}

            {authMode === 'VERIFY_EMAIL' && (
              <h2 className="ytm-login-title" style={{ textAlign: 'center' }}>Verify Your Email Address</h2>
            )}

            {statusMessage && (
              <div style={{ padding: '10px 14px', borderRadius: '12px', background: 'rgba(200, 255, 0, 0.15)', border: '1px solid #C8FF00', color: '#C8FF00', fontSize: '12px', fontWeight: 800, marginBottom: '16px' }}>
                {statusMessage}
              </div>
            )}

            {errorMessage && (
              <div style={{ padding: '10px 14px', borderRadius: '12px', background: 'rgba(255, 82, 82, 0.15)', border: '1px solid #FF5252', color: '#FF5252', fontSize: '12px', fontWeight: 800, marginBottom: '16px' }}>
                ⚠️ {errorMessage}
              </div>
            )}

            {/* SIGN IN FORM */}
            {authMode === 'SIGN_IN' && (
              <form onSubmit={handleSignIn} className="ytm-login-options">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px', textAlign: 'left' }}>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Email Address</label>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Enter your email address..."
                      required
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        borderRadius: '12px',
                        background: 'rgba(255, 255, 255, 0.08)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        color: '#fff',
                        fontSize: '14px',
                        outline: 'none',
                        marginTop: '4px'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Password</label>
                    <div style={{ position: 'relative', marginTop: '4px' }}>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter your password..."
                        required
                        style={{
                          width: '100%',
                          padding: '12px 42px 12px 16px',
                          borderRadius: '12px',
                          background: 'rgba(255, 255, 255, 0.08)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          color: '#fff',
                          fontSize: '14px',
                          outline: 'none'
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        style={{
                          position: 'absolute',
                          right: '12px',
                          top: '50%',
                          transform: 'translateY(-50%)',
                          background: 'none',
                          border: 'none',
                          color: showPassword ? '#C8FF00' : '#888',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          padding: '4px'
                        }}
                        title={showPassword ? 'Hide password' : 'Show password'}
                      >
                        {showPassword ? (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                            <line x1="1" y1="1" x2="23" y2="23"></line>
                          </svg>
                        ) : (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                          </svg>
                        )}
                      </button>
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  style={{
                    width: '100%',
                    padding: '14px',
                    borderRadius: '14px',
                    background: 'linear-gradient(135deg, #C8FF00, #9ECC00)',
                    color: '#0B0B0F',
                    fontWeight: 900,
                    fontSize: '14px',
                    border: 'none',
                    cursor: 'pointer'
                  }}
                >
                  {loading ? 'Signing In...' : 'Sign In 🔑'}
                </button>
              </form>
            )}

            {/* REGISTER FORM */}
            {authMode === 'REGISTER' && (
              <form onSubmit={handleRegister} className="ytm-login-options">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '16px', textAlign: 'left' }}>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Full Name</label>
                    <input
                      type="text"
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="Enter your name..."
                      required
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        borderRadius: '12px',
                        background: 'rgba(255, 255, 255, 0.08)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        color: '#fff',
                        fontSize: '14px',
                        outline: 'none',
                        marginTop: '4px'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Email Address</label>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Enter your email address..."
                      required
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        borderRadius: '12px',
                        background: 'rgba(255, 255, 255, 0.08)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        color: '#fff',
                        fontSize: '14px',
                        outline: 'none',
                        marginTop: '4px'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Password (min 6 chars)</label>
                    <div style={{ position: 'relative', marginTop: '4px' }}>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Create password..."
                        required
                        style={{
                          width: '100%',
                          padding: '12px 42px 12px 16px',
                          borderRadius: '12px',
                          background: 'rgba(255, 255, 255, 0.08)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          color: '#fff',
                          fontSize: '14px',
                          outline: 'none'
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        style={{
                          position: 'absolute',
                          right: '12px',
                          top: '50%',
                          transform: 'translateY(-50%)',
                          background: 'none',
                          border: 'none',
                          color: showPassword ? '#C8FF00' : '#888',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          padding: '4px'
                        }}
                        title={showPassword ? 'Hide password' : 'Show password'}
                      >
                        {showPassword ? (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                            <line x1="1" y1="1" x2="23" y2="23"></line>
                          </svg>
                        ) : (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                          </svg>
                        )}
                      </button>
                    </div>
                  </div>
                  <div>
                    <label style={{ fontSize: '12px', color: '#aaa', fontWeight: 800 }}>Confirm Password</label>
                    <div style={{ position: 'relative', marginTop: '4px' }}>
                      <input
                        type={showConfirmPassword ? 'text' : 'password'}
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Re-enter password..."
                        required
                        style={{
                          width: '100%',
                          padding: '12px 42px 12px 16px',
                          borderRadius: '12px',
                          background: 'rgba(255, 255, 255, 0.08)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          color: '#fff',
                          fontSize: '14px',
                          outline: 'none'
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        style={{
                          position: 'absolute',
                          right: '12px',
                          top: '50%',
                          transform: 'translateY(-50%)',
                          background: 'none',
                          border: 'none',
                          color: showConfirmPassword ? '#C8FF00' : '#888',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          padding: '4px'
                        }}
                        title={showConfirmPassword ? 'Hide password' : 'Show password'}
                      >
                        {showConfirmPassword ? (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                            <line x1="1" y1="1" x2="23" y2="23"></line>
                          </svg>
                        ) : (
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                          </svg>
                        )}
                      </button>
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  style={{
                    width: '100%',
                    padding: '14px',
                    borderRadius: '14px',
                    background: 'linear-gradient(135deg, #C8FF00, #9ECC00)',
                    color: '#0B0B0F',
                    fontWeight: 900,
                    fontSize: '14px',
                    border: 'none',
                    cursor: 'pointer'
                  }}
                >
                  {loading ? 'Creating Account...' : 'Create Account & Send Verification Email ✉️'}
                </button>
              </form>
            )}

            {/* EMAIL VERIFICATION GATE */}
            {authMode === 'VERIFY_EMAIL' && (
              <div style={{ textAlign: 'center', marginTop: '12px' }}>
                <p style={{ fontSize: '13px', color: '#aaa', marginBottom: '20px', lineHeight: '18px' }}>
                  A verification email has been sent to <b>{email}</b>. Please open your email app and click the link to verify your account.
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <button
                    onClick={handleCheckVerification}
                    disabled={loading}
                    style={{
                      width: '100%',
                      padding: '14px',
                      borderRadius: '14px',
                      background: 'linear-gradient(135deg, #00F0FF, #7000FF)',
                      color: '#FFF',
                      fontWeight: 900,
                      fontSize: '14px',
                      border: 'none',
                      cursor: 'pointer'
                    }}
                  >
                    {loading ? 'Checking Status...' : "I've Verified My Email 🔄"}
                  </button>

                  <button
                    onClick={handleResendEmail}
                    disabled={loading || resendCooldown > 0}
                    style={{
                      width: '100%',
                      padding: '12px',
                      borderRadius: '14px',
                      background: 'rgba(255, 255, 255, 0.08)',
                      border: '1px solid rgba(200, 255, 0, 0.4)',
                      color: '#C8FF00',
                      fontWeight: 800,
                      fontSize: '13px',
                      cursor: loading || resendCooldown > 0 ? 'not-allowed' : 'pointer'
                    }}
                  >
                    {resendCooldown > 0 ? `Resend Verification Email (${resendCooldown}s)` : 'Resend Verification Email ✉️'}
                  </button>

                  <button
                    onClick={() => {
                      logoutFirebaseUser()
                      setAuthMode('SIGN_IN')
                      setErrorMessage(null)
                      setStatusMessage(null)
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#888',
                      fontSize: '12px',
                      marginTop: '8px',
                      cursor: 'pointer'
                    }}
                  >
                    Back to Sign In / Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}


