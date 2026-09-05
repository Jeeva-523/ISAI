import React from 'react'
import { SearchBar } from '../components/SearchBar'

export type PageTab = 'home' | 'search' | 'library'

interface MainLayoutProps {
  currentTab: PageTab
  onSelectTab: (tab: PageTab) => void
  searchQuery: string
  onSearchChange: (q: string) => void
  onSearchClear: () => void
  children: React.ReactNode
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  currentTab,
  onSelectTab,
  searchQuery,
  onSearchChange,
  onSearchClear,
  children
}) => {
  return (
    <div className="app-container">
      {/* 1. Desktop / Laptop Sidebar */}
      <aside className="sidebar">
        <div className="brand-logo">
          <img src="/logo.svg" alt="ISAI Logo" width="36" height="36" />
          <div>
            <div className="brand-title">ISAI</div>
            <div className="brand-badge">DISCOVERY</div>
          </div>
        </div>

        <nav className="nav-links">
          <button
            className={`nav-link ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
              <polyline points="9 22 9 12 15 12 15 22"></polyline>
            </svg>
            Home
          </button>

          <button
            className={`nav-link ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            Search
          </button>

          <button
            className={`nav-link ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
            </svg>
            Favorites
          </button>
        </nav>

        <div style={{ marginTop: 'auto', padding: '16px', background: 'rgba(255,255,255,0.03)', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--neon-cyan)', marginBottom: '4px' }}>⚡ ISAI Platform</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Tamil Music & 320 KBPS Studio HD Discovery</div>
        </div>
      </aside>

      {/* 2. Main Content Viewport */}
      <div className="main-content">
        <header className="top-nav">
          <SearchBar
            value={searchQuery}
            onChange={(q) => {
              onSearchChange(q)
              if (currentTab !== 'search') onSelectTab('search')
            }}
            onClear={onSearchClear}
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>🎵 Tamil Vibes</span>
          </div>
        </header>

        <main className="page-wrapper">
          {children}
        </main>

        {/* 3. Mobile Navigation Bar (Visible on mobile/tablet) */}
        <nav className="mobile-nav">
          <button
            className={`nav-link ${currentTab === 'home' ? 'active' : ''}`}
            onClick={() => onSelectTab('home')}
            style={{ flexDirection: 'column', gap: '4px', padding: '6px' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            </svg>
            <span style={{ fontSize: '11px' }}>Home</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'search' ? 'active' : ''}`}
            onClick={() => onSelectTab('search')}
            style={{ flexDirection: 'column', gap: '4px', padding: '6px' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <span style={{ fontSize: '11px' }}>Search</span>
          </button>

          <button
            className={`nav-link ${currentTab === 'library' ? 'active' : ''}`}
            onClick={() => onSelectTab('library')}
            style={{ flexDirection: 'column', gap: '4px', padding: '6px' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
            </svg>
            <span style={{ fontSize: '11px' }}>Favorites</span>
          </button>
        </nav>
      </div>
    </div>
  )
}
