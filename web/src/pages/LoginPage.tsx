import React, { useState, useEffect } from 'react'
import type { UserProfile } from '../components/LoginModal'
import {
  registerWithEmailPassword,
  loginWithEmailPassword,
  checkEmailVerificationStatus,
  resendVerificationEmail
} from '../firebase'
import {
  ArrowLeft,
  Mail,
  Lock,
  Eye,
  EyeOff,
  User,
  LogIn,
  UserPlus,
  CheckCircle2,
  AlertCircle,
  RefreshCw
} from 'lucide-react'

interface LoginPageProps {
  onLogin: (userData: Partial<UserProfile>) => void
  onNavigateBack: () => void
}

type AuthMode = 'SIGN_IN' | 'REGISTER' | 'VERIFY_EMAIL'

export const LoginPage: React.FC<LoginPageProps> = ({
  onLogin,
  onNavigateBack
}) => {
  const [authMode, setAuthMode] = useState<AuthMode>('SIGN_IN')
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
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
      } else {
        setAuthMode('VERIFY_EMAIL')
        setErrorMessage('Your email is not verified yet. Please check your inbox and click the verification link.')
      }
    } catch (err: any) {
      setErrorMessage(err?.message || 'Sign in failed. Please check your credentials.')
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
      setErrorMessage('Passwords do not match. Please check and try again.')
      return
    }

    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      await registerWithEmailPassword(email, password, displayName)
      setAuthMode('VERIFY_EMAIL')
      setStatusMessage(`Verification email sent to ${email}. Please verify your email before continuing.`)
      setResendCooldown(30)
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
      const verified = await checkEmailVerificationStatus()
      if (verified) {
        onLogin({
          email,
          name: displayName || 'ISAI Listener',
          isLoggedIn: true,
          isPremium: true
        })
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

  return (
    <div style={{ maxWidth: '520px', margin: '0 auto', paddingBottom: '20px' }}>
      {/* Top Back Navigation */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '24px' }}>
        <button
          onClick={onNavigateBack}
          style={{
            background: 'rgba(255, 255, 255, 0.06)',
            border: '1px solid var(--border-subtle)',
            color: 'var(--text-primary)',
            width: '42px',
            height: '42px',
            borderRadius: '50%',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.2s ease',
            boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
          }}
          className="control-btn"
          title="Back"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 style={{ margin: 0, fontSize: '22px', fontWeight: 900, color: 'var(--text-primary)', letterSpacing: '-0.3px' }}>
            {authMode === 'SIGN_IN' ? 'Sign In' : authMode === 'REGISTER' ? 'Create Account' : 'Verify Email'}
          </h1>
          <p style={{ margin: '2px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>
            Access your ISAI library &amp; multi-device sync
          </p>
        </div>
      </div>

      {/* Main Glass Card */}
      <div
        style={{
          background: 'linear-gradient(180deg, rgba(24, 18, 40, 0.95), rgba(16, 12, 27, 0.95))',
          border: '1.5px solid var(--border-glass-bright)',
          borderRadius: '24px',
          padding: '32px 28px',
          boxShadow: '0 20px 48px rgba(0, 0, 0, 0.7), 0 0 32px rgba(139, 92, 246, 0.15)',
          position: 'relative'
        }}
      >
        {/* Brand Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '24px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '12px',
              background: 'var(--isai-gradient)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 16px rgba(139, 92, 246, 0.4)'
            }}
          >
            <img src="/logo.png" alt="ISAI" style={{ width: '26px', height: '26px', objectFit: 'contain' }} />
          </div>
          <span style={{ fontSize: '22px', fontWeight: 900, letterSpacing: '0.5px', color: '#fff' }}>
            ISAI <span style={{ color: '#06B6D4' }}>Music</span>
          </span>
        </div>

        {/* Tab Switcher (Sign In vs Create Account) */}
        {authMode !== 'VERIFY_EMAIL' && (
          <div
            style={{
              display: 'flex',
              background: 'rgba(255, 255, 255, 0.04)',
              borderRadius: '16px',
              padding: '4px',
              marginBottom: '24px',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <button
              type="button"
              onClick={() => {
                setAuthMode('SIGN_IN')
                setErrorMessage(null)
                setStatusMessage(null)
              }}
              style={{
                flex: 1,
                padding: '10px 16px',
                borderRadius: '12px',
                border: 'none',
                background: authMode === 'SIGN_IN' ? 'var(--isai-gradient)' : 'transparent',
                color: authMode === 'SIGN_IN' ? '#fff' : 'var(--text-secondary)',
                fontWeight: 800,
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                transition: 'all 0.2s ease'
              }}
            >
              <LogIn size={16} /> Sign In
            </button>
            <button
              type="button"
              onClick={() => {
                setAuthMode('REGISTER')
                setErrorMessage(null)
                setStatusMessage(null)
              }}
              style={{
                flex: 1,
                padding: '10px 16px',
                borderRadius: '12px',
                border: 'none',
                background: authMode === 'REGISTER' ? 'var(--isai-gradient)' : 'transparent',
                color: authMode === 'REGISTER' ? '#fff' : 'var(--text-secondary)',
                fontWeight: 800,
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                transition: 'all 0.2s ease'
              }}
            >
              <UserPlus size={16} /> Create Account
            </button>
          </div>
        )}

        {/* Feedback Messages */}
        {statusMessage && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: '14px',
              background: 'rgba(16, 185, 129, 0.15)',
              border: '1px solid #10B981',
              color: '#10B981',
              fontSize: '13px',
              fontWeight: 700,
              marginBottom: '18px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <CheckCircle2 size={18} />
            <span>{statusMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: '14px',
              background: 'rgba(239, 68, 68, 0.15)',
              border: '1px solid #EF4444',
              color: '#EF4444',
              fontSize: '13px',
              fontWeight: 700,
              marginBottom: '18px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <AlertCircle size={18} />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* 1. SIGN IN FORM */}
        {authMode === 'SIGN_IN' && (
          <form onSubmit={handleSignIn} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 14px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none'
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
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
                    right: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                marginTop: '10px',
                padding: '15px',
                borderRadius: '16px',
                background: 'var(--isai-gradient)',
                border: 'none',
                color: '#fff',
                fontWeight: 900,
                fontSize: '15px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)'
              }}
            >
              {loading ? <RefreshCw size={18} className="animate-spin" /> : <LogIn size={18} />}
              <span>{loading ? 'Signing in...' : 'Sign In 🔑'}</span>
            </button>
          </form>
        )}

        {/* 2. CREATE ACCOUNT (REGISTER) FORM */}
        {authMode === 'REGISTER' && (
          <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Your Name
              </label>
              <div style={{ position: 'relative' }}>
                <User size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="e.g. Jeeva"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 14px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none'
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 14px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none'
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Password (min 6 characters)
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
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
                    right: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Confirm Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
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
                    right: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                marginTop: '10px',
                padding: '15px',
                borderRadius: '16px',
                background: 'var(--isai-gradient)',
                border: 'none',
                color: '#fff',
                fontWeight: 900,
                fontSize: '15px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)'
              }}
            >
              {loading ? <RefreshCw size={18} className="animate-spin" /> : <UserPlus size={18} />}
              <span>{loading ? 'Creating account...' : 'Create Account ⚡'}</span>
            </button>
          </form>
        )}

        {/* 3. VERIFY EMAIL VIEW */}
        {authMode === 'VERIFY_EMAIL' && (
          <div style={{ textAlign: 'center', padding: '12px 0' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'rgba(6, 182, 212, 0.15)',
                border: '2px solid #06B6D4',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 16px',
                color: '#06B6D4'
              }}
            >
              <Mail size={32} />
            </div>

            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>
              Check Your Email Inbox
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '24px' }}>
              We sent a verification link to <strong style={{ color: '#fff' }}>{email}</strong>. Once you have clicked the link in your email, click the button below to continue.
            </p>

            <button
              type="button"
              onClick={handleCheckVerification}
              disabled={loading}
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '14px',
                background: 'var(--isai-gradient)',
                border: 'none',
                color: '#fff',
                fontWeight: 800,
                fontSize: '15px',
                cursor: 'pointer',
                marginBottom: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              <CheckCircle2 size={18} />
              <span>I Have Verified My Email</span>
            </button>

            <button
              type="button"
              onClick={handleResendEmail}
              disabled={loading || resendCooldown > 0}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '14px',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--border-subtle)',
                color: resendCooldown > 0 ? 'var(--text-muted)' : 'var(--text-primary)',
                fontWeight: 700,
                fontSize: '13px',
                cursor: resendCooldown > 0 ? 'default' : 'pointer'
              }}
            >
              {resendCooldown > 0 ? `Resend email in ${resendCooldown}s` : 'Resend Verification Email'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
