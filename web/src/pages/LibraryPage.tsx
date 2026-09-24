import { Download, Grid, Heart, History, Library, List as ListIcon, Music, Play, Plus, Trash2 } from 'lucide-react'
import React, { useState } from 'react'
import { PlaylistCard } from '../components/PlaylistCard'
import { SongCard } from '../components/SongCard'
import { SongListItem } from '../components/SongListItem'
import type { Song, UserPlaylist } from '@shared/models/song'

interface LibraryPageProps {
  likedSongs: Song[]
  userPlaylists: UserPlaylist[]
  currentSong: Song | null
  isPlaying: boolean
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong: (song: Song, queue?: Song[]) => void
  onAddToPlaylist: (song: Song) => void
  onAddToQueue?: (song: Song) => void
  onPlayNext?: (song: Song) => void
  onCreatePlaylist: () => void
  onSelectPlaylistDetail?: (title: string, subtitle: string, songs: Song[], coverUrl?: string) => void
  onNavigateToSearch?: () => void
  listeningHistory?: Song[]
  onClearHistory?: () => void
}

type LibraryTab = 'playlists' | 'liked' | 'recent' | 'artists' | 'downloaded'

export const LibraryPage: React.FC<LibraryPageProps> = ({
  likedSongs,
  userPlaylists,
  currentSong,
  isPlaying,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onAddToQueue,
  onPlayNext,
  onCreatePlaylist,
  onSelectPlaylistDetail,
  onNavigateToSearch,
  listeningHistory = [],
  onClearHistory
}) => {
  const [activeTab, setActiveTab] = useState<LibraryTab>('liked')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('list')

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto', paddingBottom: '8px' }}>
      {/* Header with Title and View Switcher */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Library color="var(--isai-purple-light)" size={28} />
          <h1 style={{ fontSize: '28px', fontWeight: 900 }}>Your Library</h1>
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button
            className="control-btn"
            onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
            title={`Switch to ${viewMode === 'grid' ? 'List' : 'Grid'} view`}
          >
            {viewMode === 'grid' ? <ListIcon size={20} /> : <Grid size={20} />}
          </button>

          <button
            className="pill-button active"
            onClick={onCreatePlaylist}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 16px' }}
          >
            <Plus size={16} /> New Playlist
          </button>
        </div>
      </div>

      {/* Filter Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '10px',
          marginBottom: '28px',
          borderBottom: '1px solid var(--border-subtle)',
          paddingBottom: '12px',
          overflowX: 'auto',
          flexWrap: 'wrap'
        }}
      >
        <button
          className={`pill-button ${activeTab === 'liked' ? 'active' : ''}`}
          onClick={() => setActiveTab('liked')}
        >
          💖 Liked Songs ({likedSongs.length})
        </button>

        <button
          className={`pill-button ${activeTab === 'playlists' ? 'active' : ''}`}
          onClick={() => setActiveTab('playlists')}
        >
          🎵 Playlists ({userPlaylists.length + 1})
        </button>

        <button
          className={`pill-button ${activeTab === 'recent' ? 'active' : ''}`}
          onClick={() => setActiveTab('recent')}
        >
          🕒 Recent History ({listeningHistory.length})
        </button>

        <button
          className={`pill-button ${activeTab === 'artists' ? 'active' : ''}`}
          onClick={() => setActiveTab('artists')}
        >
          🎙️ Followed Artists
        </button>

        <button
          className={`pill-button ${activeTab === 'downloaded' ? 'active' : ''}`}
          onClick={() => setActiveTab('downloaded')}
        >
          📥 Offline Downloads
        </button>
      </div>

      {/* Tab Content 1: Liked Songs */}
      {activeTab === 'liked' &&
        (likedSongs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
            <Heart size={56} style={{ margin: '0 auto 16px', opacity: 0.3, color: 'var(--isai-pink)' }} />
            <h3 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)' }}>No Liked Songs Yet</h3>
            <p style={{ fontSize: '14px', marginTop: '6px', marginBottom: '20px' }}>
              Songs you mark with a heart icon will automatically appear in your Liked Music playlist.
            </p>
            <button className="pill-button active" onClick={onNavigateToSearch}>
              Explore Music
            </button>
          </div>
        ) : viewMode === 'grid' ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '20px' }}>
            {likedSongs.map((song) => (
              <SongCard
                key={song.videoId}
                song={song}
                isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                isFavorite={true}
                onToggleFavorite={onToggleFavorite}
                onPlay={onPlaySong}
                onAddToPlaylist={onAddToPlaylist}
                onAddToQueue={onAddToQueue}
                onPlayNext={onPlayNext}
              />
            ))}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {likedSongs.map((song, idx) => (
              <SongListItem
                key={song.videoId}
                song={song}
                index={idx}
                isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                isFavorite={true}
                onPlay={onPlaySong}
                onToggleFavorite={onToggleFavorite}
                onAddToPlaylist={onAddToPlaylist}
                onAddToQueue={onAddToQueue}
                onPlayNext={onPlayNext}
              />
            ))}
          </div>
        ))}

      {/* Tab Content 2: Playlists */}
      {activeTab === 'playlists' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '20px' }}>
          {/* Default Auto Liked Music Playlist Card */}
          <PlaylistCard
            playlist={{
              id: 'pl-liked',
              title: 'Liked Music',
              subtitle: `Auto playlist • ${likedSongs.length} tracks`,
              gradient: 'linear-gradient(135deg, #EC4899, #8B5CF6)'
            }}
            onSelect={() => onSelectPlaylistDetail?.('Liked Music', 'Auto Playlist', likedSongs)}
          />

          {/* User Created Playlists */}
          {userPlaylists.map((pl) => (
            <PlaylistCard
              key={pl.id}
              playlist={{
                id: pl.id,
                title: pl.name,
                subtitle: `${pl.songs?.length || 0} tracks`,
                gradient: 'linear-gradient(135deg, #8B5CF6, #3B82F6)'
              }}
              onSelect={() => onSelectPlaylistDetail?.(pl.name, 'Custom Playlist', pl.songs || [])}
            />
          ))}
        </div>
      )}

      {/* Tab Content 3: Recent History */}
      {activeTab === 'recent' &&
        (listeningHistory.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
            <History size={56} style={{ margin: '0 auto 16px', opacity: 0.3, color: 'var(--isai-cyan)' }} />
            <h3 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)' }}>
              No Listening History Yet
            </h3>
            <p style={{ fontSize: '14px', marginTop: '6px', marginBottom: '20px' }}>
              Tracks you play will be recorded here so you can easily pick up where you left off.
            </p>
            <button className="pill-button active" onClick={onNavigateToSearch}>
              Start Listening
            </button>
          </div>
        ) : (
          <div>
            {/* Recent History Action Bar */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '18px',
                padding: '12px 18px',
                background: 'var(--surface-card)',
                borderRadius: 'var(--radius-lg)',
                border: '1px solid var(--border-subtle)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <History size={18} color="var(--isai-cyan)" />
                <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {listeningHistory.length} recently played songs
                </span>
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  className="pill-button active"
                  onClick={() => onPlaySong(listeningHistory[0], listeningHistory)}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '6px 14px', fontSize: '13px' }}
                >
                  <Play size={14} fill="#fff" /> Play All
                </button>
                {onClearHistory && (
                  <button
                    onClick={onClearHistory}
                    style={{
                      background: 'rgba(239, 68, 68, 0.12)',
                      border: '1px solid rgba(239, 68, 68, 0.35)',
                      color: '#EF4444',
                      borderRadius: '20px',
                      padding: '6px 14px',
                      fontSize: '13px',
                      fontWeight: 700,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px'
                    }}
                  >
                    <Trash2 size={14} /> Clear History
                  </button>
                )}
              </div>
            </div>

            {viewMode === 'grid' ? (
              <div
                style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '20px' }}
              >
                {listeningHistory.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={(s) => onPlaySong(s, listeningHistory)}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                {listeningHistory.map((song, idx) => (
                  <SongListItem
                    key={song.videoId}
                    song={song}
                    index={idx}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onPlay={(s) => onPlaySong(s, listeningHistory)}
                    onToggleFavorite={onToggleFavorite}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>
            )}
          </div>
        ))}

      {/* Tab Content 4: Artists */}
      {activeTab === 'artists' && (
        <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
          <Music size={48} style={{ margin: '0 auto 16px', opacity: 0.3 }} />
          <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>
            Follow Your Favorite Artists
          </h3>
          <p style={{ fontSize: '14px', marginTop: '4px' }}>
            Click the Follow button on an artist page to see their latest releases here.
          </p>
        </div>
      )}

      {/* Tab Content 4: Downloaded */}
      {activeTab === 'downloaded' && (
        <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
          <Download size={48} style={{ margin: '0 auto 16px', opacity: 0.3 }} />
          <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Offline Downloads Ready</h3>
          <p style={{ fontSize: '14px', marginTop: '4px' }}>
            All cached songs are stored locally in your browser for seamless playback.
          </p>
        </div>
      )}
    </div>
  )
}
