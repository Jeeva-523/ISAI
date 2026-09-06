import React, { useState } from 'react'
import { loginWithGoogleFirebase } from '../firebase'

export interface UserProfile {
  name: string
  email: string
  avatar: string
  isLoggedIn: boolean
  isPremium: boolean
}

interface LoginModalProps {
  isOpen: boolean
  onClose: () => void
  user: UserProfile
  onLogin: (userData: Partial<UserProfile>) => void
  onLogout: () => void
}

export const LoginModal: React.FC<LoginModalProps> = ({
  isOpen,
  onClose,
  user,
  onLogin,
  onLogout
}) => {
  const [editingName, setEditingName] = useState(user.name || 'Jeeva ⚡')
  const [isSavedNotice, setIsSavedNotice] = useState(false)
  const [loading, setLoading] = useState(false)

  if (!isOpen) return null

  const handleGoogleLogin = async () => {
    setLoading(true)
    try {
      const gUser = await loginWithGoogleFirebase()
      if (gUser) {
        onLogin({
          name: gUser.name,
          email: gUser.email,
          avatar: gUser.avatar,
          isLoggedIn: true,
          isPremium: true
        })
      }
    } catch (e) {
      console.error('Google login failed:', e)
    } finally {
      setLoading(false)
      onClose()
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
            <span className="ytm-logo-text">Music</span>
          </div>
          <button className="ytm-login-close-btn" onClick={onClose}>✕</button>
        </div>

        {/* If Logged In: Account Settings & Editable Username */}
        {user.isLoggedIn ? (
          <div className="ytm-profile-view">
            <div className="ytm-profile-hero">
              <div className="ytm-profile-hero-avatar">
                {user.avatar || user.name.slice(0, 1) || 'J'}
              </div>
              <h2 className="ytm-profile-hero-name">{user.name}</h2>
              <p className="ytm-profile-hero-email">{user.email || 'jeeva.google@gmail.com'}</p>
              <div className="ytm-premium-badge">⚡ ISAI PREMIUM ACTIVE</div>
            </div>

            {/* Account Settings: Edit Username */}
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
                <button type="submit" className="ytm-btn-save-name">
                  Save
                </button>
              </div>
              {isSavedNotice && (
                <span className="ytm-saved-badge">✓ Username updated successfully!</span>
              )}
            </form>

            <button
              className="ytm-btn-logout"
              onClick={() => {
                onLogout()
                onClose()
              }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          /* Streamlined 1-Click Google Sign In */
          <div className="ytm-login-body">
            <h2 className="ytm-login-title">Sign in to ISAI Music</h2>
            <p className="ytm-login-subtitle">
              Sign in with your Google Account for 1-click access to 50,000+ Tamil songs & playlists.
            </p>

            <div className="ytm-login-options">
              <button className="ytm-btn-google" onClick={handleGoogleLogin} disabled={loading}>
                <svg width="24" height="24" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                </svg>
                <span>{loading ? 'Signing in...' : 'Sign in with Google Account'}</span>
              </button>

              <p style={{ fontSize: '11px', color: '#888', marginTop: '16px', textAlign: 'center' }}>
                No registration details required. Your username can be customized in settings anytime!
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
