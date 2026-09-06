import type { UserPlaylist } from '@shared/models/song'
import { SearchBar } from '../components/SearchBar'

export type PageTab = 'home' | 'search' | 'library' | 'login'

interface MainLayoutProps {
  currentTab: PageTab
  onSelectTab: (tab: PageTab) => void
  searchQuery: string
  onSearchChange: (q: string) => void
  onSearchClear: () => void
  onOpenLogin: () => void
  userName?: string
  userAvatar?: string
  userPlaylists?: UserPlaylist[]
  onCreatePlaylist?: () => void
  onSelectPlaylist?: (pl: UserPlaylist) => void
  children: React.ReactNode
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  currentTab,
  onSelectTab,
  searchQuery,
  onSearchChange,
  onSearchClear,
  onOpenLogin,
  userName = 'JEEVA ⚡',
  userAvatar = 'J',
  userPlaylists = [],
  onCreatePlaylist,
  onSelectPlaylist,
  children
}) => {
  return (
    <div className="app-container">
      {/* 1. YouTube Music Style Left Sidebar */}
      <aside className="ytm-sidebar">
        {/* YTM Logo Brand */}
        <div className="ytm-brand">
          <div className="ytm-logo-box">
            <img src="/logo.png" alt="ISAI Logo" className="ytm-logo-img" />
          </div>
          <div className="ytm-brand-text-group">
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              <span className="isai-brand-font">ISAI</span>
              <span className="ytm-brand-text">Music</span>
            </div>
            <span style={{ fontSize: '10px', color: '#C8FF00', fontWeight: 700, letterSpacing: '0.4px' }}>Un Isai. Un Feel. 🎧</span>
          </div>
        </div>

        {/* Primary Navigation Links */}
        <nav className="ytm-nav-section">
          <button
            className={`ytm-nav-item ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
              <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
            </svg>
            <span>Home</span>
          </button>

          <button
            className={`ytm-nav-item ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 16h-2v-2h2v2zm1.07-7.75l-.9.92C12.45 11.9 12 12.5 12 14h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.75z" />
            </svg>
            <span>Explore</span>
          </button>

          <button
            className={`ytm-nav-item ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12z" />
            </svg>
            <span>Library</span>
          </button>
        </nav>

        <div className="ytm-sidebar-divider" />

        {/* New Playlist Action Button */}
        <button className="ytm-btn-new-playlist" onClick={onCreatePlaylist}>
          <span style={{ fontSize: '18px', fontWeight: 'bold' }}>+</span> New playlist
        </button>

        {/* User Playlists List */}
        <div className="ytm-playlists-container">
          <div
            className="ytm-playlist-item active-fav"
            onClick={() => onSelectTab('library')}
            style={{ cursor: 'pointer' }}
          >
            <span className="ytm-pl-icon">📌</span>
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
              <span className="ytm-pl-icon">🎵</span>
              <div>
                <div className="ytm-pl-title">{pl.name}</div>
                <div className="ytm-pl-sub">{userName} • {pl.songs?.length || 0} tracks</div>
              </div>
            </div>
          ))}
        </div>

        {/* User Footer Profile Badge */}
        <div className="ytm-sidebar-user-footer" onClick={onOpenLogin} style={{ cursor: 'pointer' }} title="Click to Sign In / Manage Account">
          <div className="ytm-user-avatar">{userAvatar}</div>
          <span style={{ fontSize: '13px', fontWeight: 700 }}>{userName}</span>
        </div>
      </aside>

      {/* 2. Main Content Viewport */}
      <div className="main-content ytm-main-content">
        <header className="top-nav ytm-top-header">
          {/* Centered Wide Search Bar */}
          <div style={{ flex: 1, maxWidth: '640px', margin: '0 auto' }}>
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
            <button className="ytm-cast-btn" title="Cast to device">
              📡
            </button>
            <div
              className="ytm-profile-circle"
              onClick={onOpenLogin}
              style={{ cursor: 'pointer' }}
              title="Sign In / User Account"
            >
              {userAvatar}
            </div>
          </div>
        </header>

        <main className="page-wrapper ytm-page-wrapper">
          {children}
        </main>

        {/* 3. Mobile Navigation Bar */}
        <nav className="mobile-nav">
          <button
            className={`nav-link ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
          >
            <span>🏠</span>
            <span style={{ fontSize: '11px' }}>Home</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
          >
            <span>🧭</span>
            <span style={{ fontSize: '11px' }}>Explore</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
          >
            <span>📚</span>
            <span style={{ fontSize: '11px' }}>Library</span>
          </button>
        </nav>
      </div>
    </div>
  )
}
