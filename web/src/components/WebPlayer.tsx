import React, { useState } from 'react'
import type { Song } from '@shared/models/song'

interface WebPlayerProps {
  song: Song | null
  onClose: () => void
  isFavorite?: boolean
  onToggleFavorite?: (song: Song) => void
}

export const WebPlayer: React.FC<WebPlayerProps> = ({ song, onClose, isFavorite = false, onToggleFavorite }) => {
  const [isExpanded, setIsExpanded] = useState(false)

  if (!song) return null

  // Embed URL for YouTube fallback
  const embedUrl = `https://www.youtube.com/embed/${song.videoId}?autoplay=1&playsinline=1&rel=0`
  const hasDirectAudio = Boolean(song.audioUrl)

  return (
    <div className={`web-player-dock ${isExpanded ? 'expanded' : ''}`}>
      <div className="web-player-container">
        {/* Minimized / Standard Docked Bar */}
        <div className="web-player-bar">
          <div
            className="web-player-left"
            onClick={() => setIsExpanded(!isExpanded)}
            title="Click to expand/minimize player"
          >
            <img
              src={song.thumbnailUrl || `https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg`}
              alt={song.title}
              className="web-player-thumb"
            />
            <div className="web-player-track-info">
              <span className="web-player-badge">{hasDirectAudio ? '⚡ 320 KBPS HD AUDIO' : 'NOW PLAYING'}</span>
              <h4 className="web-player-title">{song.title}</h4>
              <p className="web-player-artist">{song.channelTitle}</p>
            </div>
          </div>

          {/* If Direct Audio is available, show native sleek audio controls */}
          {hasDirectAudio && (
            <div style={{ flex: 1, maxWidth: '400px', margin: '0 12px' }}>
              <audio
                key={song.audioUrl}
                src={song.audioUrl}
                autoPlay
                controls
                style={{ width: '100%', height: '36px', outline: 'none' }}
              />
            </div>
          )}

          <div className="web-player-controls">
            {onToggleFavorite && (
              <button
                className={`player-btn fav-btn ${isFavorite ? 'active' : ''}`}
                onClick={() => onToggleFavorite(song)}
                title={isFavorite ? 'Remove Favorite' : 'Save Favorite'}
              >
                {isFavorite ? '💖' : '🤍'}
              </button>
            )}

            {!hasDirectAudio && (
              <button
                className="player-btn expand-btn"
                onClick={() => setIsExpanded(!isExpanded)}
                title={isExpanded ? 'Minimize video' : 'Expand video'}
              >
                {isExpanded ? '🔽' : '🎬'}
              </button>
            )}

            <button
              className="player-btn share-btn"
              onClick={() => {
                if (navigator.share) {
                  navigator.share({ title: song.title, text: `Listening to ${song.title} on ISAI Music!` }).catch(() => {})
                } else if (navigator.clipboard) {
                  navigator.clipboard.writeText(`Listening to ${song.title} on ISAI Music!`)
                }
              }}
              title="Share track"
            >
              🔗
            </button>

            <button className="player-btn close-btn" onClick={onClose} title="Close player">
              ✖
            </button>
          </div>
        </div>

        {/* Embedded Official YouTube Iframe Fallback (Only if direct audio is not available) */}
        {!hasDirectAudio && (
          <div className="web-player-iframe-wrapper">
            <iframe
              key={song.videoId}
              src={embedUrl}
              title={song.title}
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              className="web-player-iframe"
            />
          </div>
        )}
      </div>
    </div>
  )
}
