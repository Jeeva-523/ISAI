import React from 'react'
import type { UserPlaylist } from '@shared/models/song'
import { SearchBar } from '../components/SearchBar'
import { Home, Search, Library, Plus, Music, Heart, User, Bell } from 'lucide-react'

export type PageTab = 'home' | 'search' | 'library' | 'profile' | 'login' | 'artist-detail' | 'playlist-detail'

interface MainLayoutProps {
  currentTab: PageTab
  onSelectTab: (tab: PageTab) => void
  searchQuery: string
  onSearchChange: (q: string) => void
  onSearchClear: () => void
  onOpenLogin?: () => void
  userName?: string
  userAvatar?: string
  userPlaylists?: UserPlaylist[]
  onCreatePlaylist?: () => void
  onSelectPlaylist?: (pl: UserPlaylist) => void
  hasPlayer?: boolean
  children: React.ReactNode
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  currentTab,
  onSelectTab,
  searchQuery,
  onSearchChange,
  onSearchClear,
  onOpenLogin: _onOpenLogin,
  userName = 'JEEVA ⚡',
  userAvatar = 'J',
  userPlaylists = [],
  onCreatePlaylist,
  onSelectPlaylist,
  hasPlayer = false,
  children
}) => {
  return (
    <div className={`app-container ${hasPlayer ? 'has-player' : ''}`}>
      {/* 1. Desktop Left Sidebar */}
      <aside className="ytm-sidebar">
        {/* ISAI Brand Logo */}
        <div className="ytm-brand">
          <div className="ytm-logo-box">
            <img src="/logo.png" alt="ISAI HUB Logo" className="ytm-logo-img" />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className="isai-brand-font">ISAI</span>
              <span className="ytm-brand-text">HUB</span>
            </div>
            <span style={{ fontSize: '10px', color: 'var(--isai-purple-light)', fontWeight: 800, letterSpacing: '0.8px' }}>
              LISTEN • FEEL • LIVE 🎧
            </span>
          </div>
        </div>

        {/* Primary Navigation Links */}
        <nav className="ytm-nav-section">
          <button
            className={`ytm-nav-item ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
          >
            <Home size={20} />
            <span>Home</span>
          </button>

          <button
            className={`ytm-nav-item ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
          >
            <Search size={20} />
            <span>Explore</span>
          </button>

          <button
            className={`ytm-nav-item ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
          >
            <Library size={20} />
            <span>Library</span>
          </button>
        </nav>

        <div className="ytm-sidebar-divider" />

        {/* New Playlist Action Button */}
        <button className="ytm-btn-new-playlist" onClick={onCreatePlaylist}>
          <Plus size={18} /> New playlist
        </button>

        {/* User Playlists List */}
        <div className="ytm-playlists-container">
          <div
            className="ytm-playlist-item"
            onClick={() => onSelectTab('library')}
            style={{ cursor: 'pointer' }}
          >
            <div className="ytm-pl-icon" style={{ background: 'rgba(236, 72, 153, 0.15)' }}>
              <Heart size={16} color="var(--isai-pink)" fill="var(--isai-pink)" />
            </div>
            <div>
              <div className="ytm-pl-title">Liked Music</div>
              <div className="ytm-pl-sub">Auto playlist</div>
            </div>
          </div>

          {/* Dynamic User Created Playlists */}
          {userPlaylists.map((pl) => (
            <div
              key={pl.id}
              className="ytm-playlist-item"
              onClick={() => {
                onSelectPlaylist?.(pl)
                onSelectTab('library')
              }}
              style={{ cursor: 'pointer' }}
            >
              <div className="ytm-pl-icon">
                <Music size={16} color="var(--isai-purple-light)" />
              </div>
              <div>
                <div className="ytm-pl-title">{pl.name}</div>
                <div className="ytm-pl-sub">{pl.songs?.length || 0} tracks</div>
              </div>
            </div>
          ))}
        </div>

        {/* User Footer Profile Badge */}
        <div
          className={`ytm-sidebar-user-footer ${currentTab === 'profile' || currentTab === 'login' ? 'active' : ''}`}
          onClick={() => onSelectTab('profile')}
          style={{ cursor: 'pointer' }}
          title="Settings & Profile"
        >
          <div className="ytm-user-avatar">{userAvatar}</div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '13px', fontWeight: 700 }}>{userName}</span>
            <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>⚙️ Settings</span>
          </div>
        </div>
      </aside>

      {/* 2. Main Content Viewport */}
      <div className={`main-content ${hasPlayer ? 'has-player' : ''}`}>
        <header className="top-nav">
          <div style={{ flex: 1, maxWidth: '620px' }}>
            <SearchBar
              value={searchQuery}
              onChange={(q) => {
                onSearchChange(q)
                if (currentTab !== 'search') onSelectTab('search')
              }}
              onClear={onSearchClear}
              placeholder="Search songs, albums, artists, podcasts"
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <button className="control-btn" title="Notifications">
              <Bell size={20} />
            </button>
            <div
              className={`ytm-profile-circle ${currentTab === 'profile' || currentTab === 'login' ? 'active' : ''}`}
              onClick={() => onSelectTab('profile')}
              title="Settings & Profile"
              style={{ cursor: 'pointer' }}
            >
              {userAvatar}
            </div>
          </div>
        </header>

        <main className="ytm-page-wrapper">
          {children}
        </main>

        {/* 3. Mobile Tab Navigation Bar */}
        <nav className="mobile-nav">
          <button
            className={`nav-link ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
          >
            <Home size={20} />
            <span style={{ fontSize: '11px', fontWeight: 600 }}>Home</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
          >
            <Search size={20} />
            <span style={{ fontSize: '11px', fontWeight: 600 }}>Explore</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
          >
            <Library size={20} />
            <span style={{ fontSize: '11px', fontWeight: 600 }}>Library</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'profile' || currentTab === 'login' ? 'active' : ''}`}
            onClick={() => onSelectTab('profile')}
          >
            <User size={20} />
            <span style={{ fontSize: '11px', fontWeight: 600 }}>Settings</span>
          </button>
        </nav>
      </div>
    </div>
  )
}
