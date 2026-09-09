import React, { useState, useEffect } from 'react'
import type { UserProfile } from '../components/LoginModal'
import {
  registerWithEmailPassword,
  loginWithEmailPassword,
  sendPasswordReset,
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
  RefreshCw,
  KeyRound,
  Compass
} from 'lucide-react'

interface LoginPageProps {
  onLogin: (userData: Partial<UserProfile>) => void
  onNavigateBack?: () => void
  allowBack?: boolean
}

type AuthMode = 'SIGN_IN' | 'REGISTER' | 'FORGOT_PASSWORD' | 'VERIFY_EMAIL'



export const LoginPage: React.FC<LoginPageProps> = ({
  onLogin,
  onNavigateBack,
  allowBack = false
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
      setStatusMessage(`Verification link sent to ${email}. Please verify your email before continuing.`)
      setResendCooldown(30)
    } catch (err: any) {
      setErrorMessage(err?.message || 'Registration failed.')
    } finally {
      setLoading(false)
    }
  }



  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim()) {
      setErrorMessage('Please enter your email address.')
      return
    }

    setLoading(true)
    setErrorMessage(null)
    setStatusMessage(null)

    try {
      await sendPasswordReset(email)
      setStatusMessage(`Password reset link sent to ${email}. Check your inbox or spam folder.`)
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to send password reset email.')
    } finally {
      setLoading(false)
    }
  }

  const handleGuestContinue = () => {
    onLogin({
      name: 'Guest Listener',
      email: 'guest@isaimusic.com',
      isLoggedIn: true,
      isPremium: false
    })
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
        setErrorMessage('Email not verified yet. Please click the link sent to your inbox and retry.')
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
      setStatusMessage('Verification email sent again. Check your inbox and spam folder.')
      setResendCooldown(30)
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to resend verification email.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#090611',
        position: 'relative',
        overflow: 'hidden',
        padding: '32px 16px',
        fontFamily: "'Plus Jakarta Sans', system-ui, -apple-system, sans-serif"
      }}
    >
      {/* 1. Ambient Glowing Orbs */}
      <div
        style={{
          position: 'absolute',
          top: '-15%',
          left: '10%',
          width: '520px',
          height: '520px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(139, 92, 246, 0.22) 0%, rgba(139, 92, 246, 0) 70%)',
          filter: 'blur(70px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-15%',
          right: '10%',
          width: '540px',
          height: '540px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(6, 182, 212, 0.18) 0%, rgba(6, 182, 212, 0) 70%)',
          filter: 'blur(75px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />
      <div
        style={{
          position: 'absolute',
          top: '40%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: '650px',
          height: '650px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(236, 72, 153, 0.08) 0%, transparent 60%)',
          filter: 'blur(90px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />

      {/* Optional Top Back Navigation (Only when navigated from inside app) */}
      {allowBack && onNavigateBack && (
        <div style={{ position: 'absolute', top: '24px', left: '24px', zIndex: 10 }}>
          <button
            onClick={onNavigateBack}
            style={{
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              color: '#fff',
              padding: '8px 16px',
              borderRadius: '20px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '13px',
              fontWeight: 600,
              backdropFilter: 'blur(12px)',
              transition: 'all 0.2s ease'
            }}
          >
            <ArrowLeft size={16} /> Back to Player
          </button>
        </div>
      )}

      {/* 2. Main High-Fidelity Glassmorphism Card */}
      <div
        style={{
          width: '100%',
          maxWidth: '450px',
          background: 'linear-gradient(180deg, rgba(24, 18, 40, 0.85) 0%, rgba(14, 10, 24, 0.92) 100%)',
          backdropFilter: 'blur(28px)',
          WebkitBackdropFilter: 'blur(28px)',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          borderRadius: '28px',
          padding: '36px 32px',
          boxShadow: '0 30px 60px -12px rgba(0, 0, 0, 0.85), 0 0 45px rgba(139, 92, 246, 0.15)',
          position: 'relative',
          zIndex: 2
        }}
      >
        {/* Brand Identity & Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '58px',
              height: '58px',
              borderRadius: '18px',
              background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
              boxShadow: '0 8px 24px rgba(139, 92, 246, 0.45)',
              marginBottom: '14px',
              position: 'relative'
            }}
          >
            <img src="/logo.png" alt="ISAI" style={{ width: '34px', height: '34px', objectFit: 'contain' }} />
          </div>

          <h1
            style={{
              margin: '0 0 6px',
              fontSize: '24px',
              fontWeight: 900,
              letterSpacing: '-0.3px',
              color: '#FFFFFF'
            }}
          >
            ISAI <span style={{ background: 'linear-gradient(90deg, #06B6D4, #A78BFA)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Music</span>
          </h1>

          <p style={{ margin: 0, fontSize: '13px', color: 'rgba(255, 255, 255, 0.6)', fontWeight: 500 }}>
            {authMode === 'SIGN_IN' && 'Welcome back! Log in to continue your listening journey'}
            {authMode === 'REGISTER' && 'Create your account to unlock unlimited streaming'}
            {authMode === 'FORGOT_PASSWORD' && 'Enter your email to receive a password reset link'}
            {authMode === 'VERIFY_EMAIL' && 'Please verify your email address to continue'}
          </p>
        </div>

        {/* Feedback / Alert Messages */}
        {statusMessage && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: '14px',
              background: 'rgba(16, 185, 129, 0.12)',
              border: '1px solid rgba(16, 185, 129, 0.35)',
              color: '#34D399',
              fontSize: '13px',
              fontWeight: 600,
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px'
            }}
          >
            <CheckCircle2 size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>{statusMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: '14px',
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid rgba(239, 68, 68, 0.35)',
              color: '#F87171',
              fontSize: '13px',
              fontWeight: 600,
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px'
            }}
          >
            <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Tab Switcher (Sign In vs Create Account) */}
        {(authMode === 'SIGN_IN' || authMode === 'REGISTER') && (
            <div
              style={{
                display: 'flex',
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '16px',
                padding: '4px',
                marginBottom: '22px',
                border: '1px solid rgba(255, 255, 255, 0.08)'
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
                  background: authMode === 'SIGN_IN' ? 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)' : 'transparent',
                  color: authMode === 'SIGN_IN' ? '#FFFFFF' : 'rgba(255, 255, 255, 0.6)',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: authMode === 'SIGN_IN' ? '0 4px 14px rgba(139, 92, 246, 0.35)' : 'none',
                  transition: 'all 0.25s ease'
                }}
              >
                <LogIn size={15} /> Sign In
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
                  background: authMode === 'REGISTER' ? 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)' : 'transparent',
                  color: authMode === 'REGISTER' ? '#FFFFFF' : 'rgba(255, 255, 255, 0.6)',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: authMode === 'REGISTER' ? '0 4px 14px rgba(139, 92, 246, 0.35)' : 'none',
                  transition: 'all 0.25s ease'
                }}
              >
                <UserPlus size={15} /> Create Account
              </button>
            </div>
        )}

        {/* 4. SIGN IN FORM */}
        {authMode === 'SIGN_IN' && (
          <form onSubmit={handleSignIn} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '7px' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
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
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box',
                    transition: 'border-color 0.2s, box-shadow 0.2s'
                  }}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#06B6D4'
                    e.target.style.boxShadow = '0 0 0 3px rgba(6, 182, 212, 0.2)'
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = 'rgba(255, 255, 255, 0.12)'
                    e.target.style.boxShadow = 'none'
                  }}
                />
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '7px' }}>
                <label style={{ fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)' }}>
                  Password
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setAuthMode('FORGOT_PASSWORD')
                    setErrorMessage(null)
                    setStatusMessage(null)
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#06B6D4',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  Forgot password?
                </button>
              </div>
              <div style={{ position: 'relative' }}>
                <Lock size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box',
                    transition: 'border-color 0.2s, box-shadow 0.2s'
                  }}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#06B6D4'
                    e.target.style.boxShadow = '0 0 0 3px rgba(6, 182, 212, 0.2)'
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = 'rgba(255, 255, 255, 0.12)'
                    e.target.style.boxShadow = 'none'
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
                    color: 'rgba(255, 255, 255, 0.4)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                marginTop: '8px',
                padding: '14px',
                borderRadius: '16px',
                background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
                border: 'none',
                color: '#fff',
                fontWeight: 800,
                fontSize: '14px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.75 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)',
                transition: 'all 0.2s ease'
              }}
            >
              {loading ? <RefreshCw size={17} className="animate-spin" /> : <LogIn size={17} />}
              <span>{loading ? 'Signing in...' : 'Sign In'}</span>
            </button>
          </form>
        )}

        {/* 5. CREATE ACCOUNT (REGISTER) FORM */}
        {authMode === 'REGISTER' && (
          <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '6px' }}>
                Your Name
              </label>
              <div style={{ position: 'relative' }}>
                <User size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
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
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '6px' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
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
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '6px' }}>
                Password (min 6 characters)
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Create strong password"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
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
                    color: 'rgba(255, 255, 255, 0.4)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '6px' }}>
                Confirm Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat your password"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 44px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
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
                    color: 'rgba(255, 255, 255, 0.4)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showConfirmPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                marginTop: '8px',
                padding: '14px',
                borderRadius: '16px',
                background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
                border: 'none',
                color: '#fff',
                fontWeight: 800,
                fontSize: '14px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.75 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)',
                transition: 'all 0.2s ease'
              }}
            >
              {loading ? <RefreshCw size={17} className="animate-spin" /> : <UserPlus size={17} />}
              <span>{loading ? 'Creating account...' : 'Create Account'}</span>
            </button>
          </form>
        )}

        {/* 6. FORGOT PASSWORD FORM */}
        {authMode === 'FORGOT_PASSWORD' && (
          <form onSubmit={handleForgotPassword} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'rgba(255, 255, 255, 0.7)', marginBottom: '7px' }}>
                Account Email
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={17} color="rgba(255, 255, 255, 0.4)" style={{ position: 'absolute', left: '15px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your registered email"
                  required
                  style={{
                    width: '100%',
                    padding: '13px 14px 13px 44px',
                    borderRadius: '14px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    color: '#fff',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                marginTop: '6px',
                padding: '14px',
                borderRadius: '16px',
                background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
                border: 'none',
                color: '#fff',
                fontWeight: 800,
                fontSize: '14px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.75 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 8px 24px rgba(139, 92, 246, 0.4)'
              }}
            >
              {loading ? <RefreshCw size={17} className="animate-spin" /> : <KeyRound size={17} />}
              <span>{loading ? 'Sending link...' : 'Send Password Reset Link'}</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setAuthMode('SIGN_IN')
                setErrorMessage(null)
                setStatusMessage(null)
              }}
              style={{
                background: 'none',
                border: 'none',
                color: 'rgba(255, 255, 255, 0.7)',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
                textAlign: 'center',
                padding: '8px 0'
              }}
            >
              ← Back to Sign In
            </button>
          </form>
        )}

        {/* 7. VERIFY EMAIL SCREEN */}
        {authMode === 'VERIFY_EMAIL' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', textAlign: 'center' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'rgba(6, 182, 212, 0.15)',
                border: '1px solid rgba(6, 182, 212, 0.4)',
                color: '#06B6D4',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto'
              }}
            >
              <Mail size={32} />
            </div>

            <div>
              <h3 style={{ margin: '0 0 6px', fontSize: '18px', fontWeight: 800, color: '#fff' }}>
                Check Your Inbox
              </h3>
              <p style={{ margin: 0, fontSize: '13px', color: 'rgba(255, 255, 255, 0.6)', lineHeight: '1.5' }}>
                We sent a verification link to <strong style={{ color: '#fff' }}>{email}</strong>. Please click the link to verify your email.
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '10px' }}>
              <button
                type="button"
                onClick={handleCheckVerification}
                disabled={loading}
                style={{
                  padding: '14px',
                  borderRadius: '16px',
                  background: 'linear-gradient(135deg, #10B981 0%, #06B6D4 100%)',
                  border: 'none',
                  color: '#fff',
                  fontWeight: 800,
                  fontSize: '14px',
                  cursor: loading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: '0 8px 24px rgba(16, 185, 129, 0.35)'
                }}
              >
                {loading ? <RefreshCw size={17} className="animate-spin" /> : <CheckCircle2 size={17} />}
                <span>I Have Verified My Email</span>
              </button>

              <button
                type="button"
                onClick={handleResendEmail}
                disabled={resendCooldown > 0 || loading}
                style={{
                  padding: '12px',
                  borderRadius: '14px',
                  background: 'rgba(255, 255, 255, 0.06)',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  color: resendCooldown > 0 ? 'rgba(255, 255, 255, 0.4)' : '#fff',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: resendCooldown > 0 || loading ? 'not-allowed' : 'pointer'
                }}
              >
                {resendCooldown > 0 ? `Resend Email in ${resendCooldown}s` : 'Resend Verification Email'}
              </button>

              <button
                type="button"
                onClick={() => {
                  setAuthMode('SIGN_IN')
                  setErrorMessage(null)
                  setStatusMessage(null)
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'rgba(255, 255, 255, 0.5)',
                  fontSize: '12px',
                  cursor: 'pointer',
                  padding: '6px'
                }}
              >
                Back to Sign In
              </button>
            </div>
          </div>
        )}

        {/* 8. Continue as Guest Option */}
        {authMode !== 'VERIFY_EMAIL' && (
          <div style={{ marginTop: '22px', textAlign: 'center' }}>
            <button
              type="button"
              onClick={handleGuestContinue}
              style={{
                background: 'none',
                border: 'none',
                color: 'rgba(255, 255, 255, 0.55)',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'color 0.2s ease'
              }}
              onMouseEnter={(e) => (e.currentTarget.style.color = '#06B6D4')}
              onMouseLeave={(e) => (e.currentTarget.style.color = 'rgba(255, 255, 255, 0.55)')}
            >
              <Compass size={15} />
              <span>Want to explore first? Continue as Guest →</span>
            </button>
          </div>
        )}
      </div>

      {/* 9. Bottom Footer & Credits */}
      <div
        style={{
          marginTop: '28px',
          textAlign: 'center',
          color: 'rgba(255, 255, 255, 0.4)',
          fontSize: '12px',
          zIndex: 2,
          lineHeight: '1.6'
        }}
      >
        <div>Protected by Firebase Security &bull; High-Fidelity Lossless Audio</div>
        <div style={{ marginTop: '8px', fontSize: '11px', letterSpacing: '3px', textTransform: 'uppercase', color: 'rgba(255, 255, 255, 0.45)', fontWeight: 600 }}>
          Powered by
        </div>
        <div style={{ fontSize: '18px', fontWeight: 900, letterSpacing: '6px', background: 'linear-gradient(90deg, #8B5CF6, #06B6D4)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', marginTop: '2px' }}>
          J  E  E  V  A
        </div>
      </div>
    </div>
  )
}
