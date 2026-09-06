import React, { useState } from 'react'
import type { Song, UserPlaylist } from '@shared/models/song'
import { X, Plus, Music, Check } from 'lucide-react'

interface PlaylistModalProps {
  isOpen: boolean
  onClose: () => void
  songToAdd?: Song | null
  userPlaylists: UserPlaylist[]
  onCreatePlaylist: (name: string) => void
  onAddSongToPlaylist: (playlistId: string, song: Song) => void
}

export const PlaylistModal: React.FC<PlaylistModalProps> = ({
  isOpen,
  onClose,
  songToAdd,
  userPlaylists,
  onCreatePlaylist,
  onAddSongToPlaylist
}) => {
  const [newPlaylistName, setNewPlaylistName] = useState('')
  const [isCreating, setIsCreating] = useState(false)
  const [addedPlaylistIds, setAddedPlaylistIds] = useState<string[]>([])

  if (!isOpen) return null

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newPlaylistName.trim()) return
    onCreatePlaylist(newPlaylistName.trim())
    setNewPlaylistName('')
    setIsCreating(false)
  }

  const handleSelectPlaylist = (plId: string) => {
    if (songToAdd) {
      onAddSongToPlaylist(plId, songToAdd)
      setAddedPlaylistIds(prev => [...prev, plId])
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(16px)',
        zIndex: 2500,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px'
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '440px',
          background: 'var(--surface-elevated)',
          border: '1px solid var(--border-glass-bright)',
          borderRadius: 'var(--radius-xl)',
          padding: '28px',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.8)',
          position: 'relative'
        }}
      >
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer'
          }}
        >
          <X size={20} />
        </button>

        <h2 style={{ fontSize: '20px', fontWeight: 900, marginBottom: '4px' }}>
          {songToAdd ? 'Add to Playlist' : 'Create Playlist'}
        </h2>
        {songToAdd && (
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
            Choose a playlist for <span style={{ color: 'var(--isai-purple-light)', fontWeight: 700 }}>"{songToAdd.title}"</span>
          </p>
        )}

        {isCreating ? (
          <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '16px' }}>
            <input
              type="text"
              placeholder="Playlist name (e.g. Tamil Chill Hits)"
              value={newPlaylistName}
              onChange={(e) => setNewPlaylistName(e.target.value)}
              autoFocus
              style={{
                width: '100%',
                height: '46px',
                borderRadius: '12px',
                background: 'rgba(255, 255, 255, 0.06)',
                border: '1px solid var(--border-subtle)',
                color: '#fff',
                padding: '0 16px',
                outline: 'none',
                fontSize: '14px'
              }}
            />
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={() => setIsCreating(false)}
                className="pill-button"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="pill-button active"
              >
                Create
              </button>
            </div>
          </form>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px' }}>
            <button
              onClick={() => setIsCreating(true)}
              className="ytm-btn-new-playlist"
              style={{ padding: '14px' }}
            >
              <Plus size={18} /> Create New Playlist
            </button>

            <div style={{ maxHeight: '240px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {userPlaylists.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '24px', color: 'var(--text-muted)', fontSize: '13px' }}>
                  No custom playlists yet. Create one above!
                </div>
              ) : (
                userPlaylists.map((pl) => {
                  const isAdded = addedPlaylistIds.includes(pl.id)
                  return (
                    <div
                      key={pl.id}
                      onClick={() => handleSelectPlaylist(pl.id)}
                      className="ytm-playlist-item"
                      style={{ cursor: 'pointer', justifyContent: 'space-between', padding: '12px' }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div className="ytm-pl-icon">
                          <Music size={16} color="var(--isai-purple-light)" />
                        </div>
                        <div>
                          <div className="ytm-pl-title">{pl.name}</div>
                          <div className="ytm-pl-sub">{pl.songs?.length || 0} tracks</div>
                        </div>
                      </div>
                      {isAdded && (
                        <div style={{ background: 'var(--isai-purple-dark)', borderRadius: '50%', padding: '4px' }}>
                          <Check size={14} color="#fff" />
                        </div>
                      )}
                    </div>
                  )
                })
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
