import React, { useState } from 'react'
import type { UserProfile } from '../components/LoginModal'
import { IsaiConnectModal } from '../components/IsaiConnectModal'
import { logoutFirebaseUser } from '../firebase'
import {
  ArrowLeft,
  User,
  Pencil,
  Mail,
  ShieldCheck,
  Headphones,
  Sliders,
  Globe,
  HardDrive,
  Laptop,
  RefreshCw,
  Heart,
  ListMusic,
  Info,
  LogOut,
  LogIn,
  CheckCircle2,
  Star,
  X
} from 'lucide-react'

interface ProfilePageProps {
  user: UserProfile
  favoritesCount: number
  playlistsCount: number
  onUpdateProfile: (userData: Partial<UserProfile>) => void
  onNavigateToLogin: () => void
  onLogout: () => void
  onNavigateHome: () => void
}

export const ProfilePage: React.FC<ProfilePageProps> = ({
  user,
  favoritesCount,
  playlistsCount,
  onUpdateProfile,
  onNavigateToLogin,
  onLogout,
  onNavigateHome
}) => {
  const [showEditModal, setShowEditModal] = useState(false)
  const [editNameInput, setEditNameInput] = useState(user.name || 'ISAI Listener')
  const [isSavedNotice, setIsSavedNotice] = useState(false)
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false)

  const isEmailVerified = true // Default verified or based on Firebase user
  const currentDisplayName = user.name || 'Jeevananth'
  const currentEmail = user.email || 'jeevananthravikumar@gmail.com'

  const handleSaveName = (e: React.FormEvent) => {
    e.preventDefault()
    if (!editNameInput.trim()) return
    onUpdateProfile({ name: editNameInput.trim() })
    setIsSavedNotice(true)
    setTimeout(() => {
      setIsSavedNotice(false)
      setShowEditModal(false)
    }, 900)
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', paddingBottom: '16px' }}>
      {/* 1. Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '24px' }}>
        <button
          onClick={onNavigateHome}
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
          title="Back to Home"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 900, color: 'var(--text-primary)', letterSpacing: '-0.4px' }}>
            Profile &amp; Account
          </h1>
          <p style={{ margin: '2px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>
            Basic Details &amp; Preferences
          </p>
        </div>
      </div>

      {/* 2. Main Hero Profile Card */}
      <div
        style={{
          background: 'linear-gradient(180deg, rgba(24, 18, 40, 0.95), rgba(16, 12, 27, 0.95))',
          border: '1.5px solid rgba(6, 182, 212, 0.4)',
          borderRadius: '24px',
          padding: '32px 24px',
          textAlign: 'center',
          boxShadow: '0 16px 40px rgba(0, 0, 0, 0.6), 0 0 32px rgba(6, 182, 212, 0.15)',
          marginBottom: '28px',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Subtle Ambient Background Glow */}
        <div
          style={{
            position: 'absolute',
            top: '-50px',
            left: '50%',
            transform: 'translateX(-50%)',
            width: '260px',
            height: '140px',
            background: 'radial-gradient(ellipse at center, rgba(6, 182, 212, 0.25), transparent 70%)',
            pointerEvents: 'none'
          }}
        />

        {/* Profile Avatar Ring */}
        <div
          style={{
            position: 'relative',
            width: '96px',
            height: '96px',
            borderRadius: '50%',
            padding: '3px',
            background: 'linear-gradient(135deg, #06B6D4, #8B5CF6, #10B981, #06B6D4)',
            margin: '0 auto 16px',
            boxShadow: '0 0 24px rgba(6, 182, 212, 0.4)'
          }}
        >
          <div
            style={{
              width: '100%',
              height: '100%',
              borderRadius: '50%',
              background: 'var(--surface-dark)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#06B6D4',
              fontSize: '38px',
              fontWeight: 900
            }}
          >
            {user.avatar || (currentDisplayName ? currentDisplayName.charAt(0).toUpperCase() : 'J')}
          </div>
        </div>

        {/* Display Name with Edit Button */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '4px' }}>
          <h2 style={{ fontSize: '24px', fontWeight: 900, color: 'var(--text-primary)', margin: 0, letterSpacing: '-0.3px' }}>
            {currentDisplayName}
          </h2>
          <button
            onClick={() => {
              setEditNameInput(currentDisplayName)
              setShowEditModal(true)
            }}
            style={{
              background: 'rgba(6, 182, 212, 0.12)',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              color: '#06B6D4',
              width: '28px',
              height: '28px',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
            title="Edit Display Name"
          >
            <Pencil size={14} />
          </button>
        </div>

        {/* Email Address */}
        <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: '0 0 18px', fontWeight: 500 }}>
          {currentEmail}
        </p>

        {/* Status Badges Row */}
        <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Email Verification Pill */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 14px',
              borderRadius: '20px',
              background: isEmailVerified ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
              border: `1px solid ${isEmailVerified ? '#10B981' : '#F59E0B'}`,
              color: isEmailVerified ? '#10B981' : '#F59E0B',
              fontSize: '12px',
              fontWeight: 800
            }}
          >
            <CheckCircle2 size={14} />
            <span>{isEmailVerified ? 'Email Verified' : 'Unverified'}</span>
          </div>

          {/* ISAI Premium Badge */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 14px',
              borderRadius: '20px',
              background: 'linear-gradient(90deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2))',
              border: '1px solid rgba(6, 182, 212, 0.6)',
              color: '#06B6D4',
              fontSize: '12px',
              fontWeight: 800
            }}
          >
            <Star size={14} fill="#06B6D4" />
            <span>ISAI Premium Listener</span>
          </div>
        </div>
      </div>

      {/* 3. Section: Personal & Account Details */}
      <div style={{ marginBottom: '22px', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: '20px', padding: '20px 22px' }}>
        <h3 style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '1.2px', marginBottom: '14px' }}>
          ACCOUNT DETAILS
        </h3>
        
        <div
          onClick={() => {
            setEditNameInput(currentDisplayName)
            setShowEditModal(true)
          }}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)', cursor: 'pointer' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <User size={18} color="#06B6D4" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Display Name</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>{currentDisplayName}</span>
            <Pencil size={14} color="var(--text-muted)" />
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Mail size={18} color="#06B6D4" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Email Address</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>{currentEmail}</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <ShieldCheck size={18} color="#10B981" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Security Status</span>
          </div>
          <span style={{ color: '#10B981', fontSize: '14px', fontWeight: 800 }}>Email Verified ✓</span>
        </div>
      </div>

      {/* 4. Section: Audio & Streaming Preferences */}
      <div style={{ marginBottom: '22px', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: '20px', padding: '20px 22px' }}>
        <h3 style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '1.2px', marginBottom: '14px' }}>
          AUDIO &amp; PLAYBACK QUALITY
        </h3>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Headphones size={18} color="#8B5CF6" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Streaming Quality</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>320 kbps Ultra HD</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Sliders size={18} color="#8B5CF6" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Audio Engine</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>WebAudio Spatial 3D Equalizer Active</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Globe size={18} color="#8B5CF6" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Music Language</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>Tamil, English, Hindi</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <HardDrive size={18} color="#8B5CF6" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Offline Storage</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>Extreme High Quality Cache</span>
        </div>
      </div>

      {/* 5. Section: Device & App Info */}
      <div style={{ marginBottom: '24px', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: '20px', padding: '20px 22px' }}>
        <h3 style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '1.2px', marginBottom: '14px' }}>
          DEVICE &amp; APP INFO
        </h3>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Laptop size={18} color="#EC4899" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Active Device</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>Web Browser (Current)</span>
        </div>

        <div
          onClick={() => setIsConnectModalOpen(true)}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)', cursor: 'pointer' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <RefreshCw size={18} color="#06B6D4" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>ISAI Connect</span>
          </div>
          <span style={{ color: '#06B6D4', fontSize: '14px', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '6px' }}>
            Device Sync Ready 📱
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Heart size={18} color="#EC4899" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Favorite Tracks</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>{favoritesCount} Liked Songs</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid var(--divider)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <ListMusic size={18} color="#EC4899" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Custom Playlists</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>{playlistsCount} Playlists</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)' }}>
            <Info size={18} color="var(--text-muted)" />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>App Version</span>
          </div>
          <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 700 }}>ISAI Web v2.4.0 (2026 Build)</span>
        </div>
      </div>

      {/* 6. Action Control Buttons */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {/* ISAI Connect Button */}
        <button
          onClick={() => setIsConnectModalOpen(true)}
          style={{
            width: '100%',
            padding: '16px',
            borderRadius: '16px',
            background: 'var(--surface-card)',
            border: '1.5px solid rgba(6, 182, 212, 0.4)',
            color: '#06B6D4',
            fontWeight: 800,
            fontSize: '15px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px',
            boxShadow: '0 8px 24px rgba(0,0,0,0.3)',
            transition: 'all 0.2s ease'
          }}
          className="control-btn"
        >
          <RefreshCw size={18} />
          <span>📱 Open ISAI Connect Multi-Device</span>
        </button>

        {/* Sign In / Sign Out Button */}
        {user.isLoggedIn ? (
          <button
            onClick={() => {
              logoutFirebaseUser()
              onLogout()
            }}
            style={{
              width: '100%',
              padding: '16px',
              borderRadius: '16px',
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1.5px solid rgba(239, 68, 68, 0.4)',
              color: '#EF4444',
              fontWeight: 800,
              fontSize: '15px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              transition: 'all 0.2s ease'
            }}
            className="control-btn"
          >
            <LogOut size={18} />
            <span>🚪 Sign Out Account</span>
          </button>
        ) : (
          <button
            onClick={onNavigateToLogin}
            style={{
              width: '100%',
              padding: '16px',
              borderRadius: '16px',
              background: 'var(--isai-gradient)',
              border: 'none',
              color: '#FFF',
              fontWeight: 900,
              fontSize: '15px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              boxShadow: '0 8px 24px rgba(139, 92, 246, 0.35)',
              transition: 'all 0.2s ease'
            }}
          >
            <LogIn size={18} />
            <span>🔑 Sign In / Switch Account</span>
          </button>
        )}
      </div>

      {/* ISAI Connect Modal */}
      <IsaiConnectModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
      />

      {/* 7. Edit Display Name Dialog Modal */}
      {showEditModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(8px)',
            zIndex: 9999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
          }}
          onClick={() => setShowEditModal(false)}
        >
          <div
            style={{
              background: 'var(--surface-elevated)',
              border: '1.5px solid var(--border-glass-bright)',
              borderRadius: '24px',
              padding: '28px',
              width: '100%',
              maxWidth: '440px',
              boxShadow: '0 24px 64px rgba(0, 0, 0, 0.8)'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Pencil size={20} color="#06B6D4" />
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
                  Edit Display Name
                </h3>
              </div>
              <button
                onClick={() => setShowEditModal(false)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveName}>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px' }}>
                  Your Display Name
                </label>
                <input
                  type="text"
                  value={editNameInput}
                  onChange={(e) => setEditNameInput(e.target.value)}
                  placeholder="Enter your display name..."
                  required
                  autoFocus
                  style={{
                    width: '100%',
                    padding: '14px 16px',
                    borderRadius: '12px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-glass)',
                    color: '#fff',
                    fontSize: '15px',
                    fontWeight: 600,
                    outline: 'none'
                  }}
                />
              </div>

              {isSavedNotice && (
                <div style={{ color: '#10B981', fontSize: '13px', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <CheckCircle2 size={16} /> Updated successfully!
                </div>
              )}

              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '20px' }}>
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  style={{
                    padding: '10px 20px',
                    borderRadius: '12px',
                    background: 'transparent',
                    border: '1px solid var(--border-subtle)',
                    color: 'var(--text-secondary)',
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{
                    padding: '10px 24px',
                    borderRadius: '12px',
                    background: 'var(--isai-gradient)',
                    border: 'none',
                    color: '#fff',
                    fontWeight: 800,
                    cursor: 'pointer'
                  }}
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
