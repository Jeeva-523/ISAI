import { Play } from 'lucide-react'
import React, { useState } from 'react'
import type { FeaturedPlaylist } from '../data/featuredPlaylists'

interface FeaturedPlaylistCardProps {
  playlist: FeaturedPlaylist
  onClick: (playlist: FeaturedPlaylist) => void
  onPlay?: (playlist: FeaturedPlaylist) => void
  isPlaying?: boolean
}

export const FeaturedPlaylistCard: React.FC<FeaturedPlaylistCardProps> = ({
  playlist,
  onClick,
  onPlay,
  isPlaying = false
}) => {
  const [isHovered, setIsHovered] = useState(false)

  return (
    <div
      onClick={() => onClick(playlist)}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      style={{
        minWidth: '180px',
        maxWidth: '180px',
        background: isHovered ? 'rgba(32, 28, 48, 0.95)' : 'rgba(20, 18, 30, 0.75)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        border: isHovered ? '1px solid rgba(139, 92, 246, 0.45)' : '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '20px',
        padding: '14px',
        cursor: 'pointer',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        boxShadow: isHovered
          ? '0 16px 36px rgba(0, 0, 0, 0.6), 0 0 24px rgba(139, 92, 246, 0.2)'
          : '0 8px 20px rgba(0, 0, 0, 0.4)',
        transform: isHovered ? 'translateY(-4px)' : 'translateY(0)',
        transition: 'all 0.28s cubic-bezier(0.2, 0.8, 0.2, 1)',
        position: 'relative'
      }}
    >
      {/* Artwork Container */}
      <div
        style={{
          position: 'relative',
          width: '100%',
          aspectRatio: '1/1',
          borderRadius: '14px',
          overflow: 'hidden',
          boxShadow: '0 8px 18px rgba(0,0,0,0.5)'
        }}
      >
        <img
          src={playlist.coverUrl}
          alt={playlist.title}
          loading="lazy"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            transform: isHovered ? 'scale(1.06)' : 'scale(1)',
            transition: 'transform 0.4s ease'
          }}
          onError={(e) => {
            ;(e.target as HTMLImageElement).src =
              'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'
          }}
        />

        {/* Gradient Tint Overlay */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: playlist.gradient,
            opacity: isHovered ? 0.35 : 0.18,
            transition: 'opacity 0.3s ease'
          }}
        />

        {/* Language Pill Badge */}
        <div
          style={{
            position: 'absolute',
            top: '8px',
            left: '8px',
            background: 'rgba(10, 8, 18, 0.8)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            color: '#E0E7FF',
            fontSize: '10px',
            fontWeight: 800,
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
            padding: '3px 8px',
            borderRadius: '12px'
          }}
        >
          {playlist.language}
        </div>

        {/* Floating Spotify-Style Circular Play Button */}
        <div
          onClick={(e) => {
            e.stopPropagation()
            onPlay?.(playlist)
          }}
          style={{
            position: 'absolute',
            bottom: '10px',
            right: '10px',
            width: '44px',
            height: '44px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #1DB954 0%, #1ED760 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 20px rgba(29, 185, 84, 0.55)',
            transform: isHovered ? 'translateY(0) scale(1)' : 'translateY(12px) scale(0.8)',
            opacity: isHovered ? 1 : 0,
            transition: 'all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1)',
            cursor: 'pointer',
            zIndex: 3
          }}
        >
          <Play size={20} fill="#000000" color="#000000" style={{ marginLeft: '2px' }} />
        </div>
      </div>

      {/* Playlist Meta Details */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
        <h3
          style={{
            fontSize: '14px',
            fontWeight: 800,
            color: isPlaying ? 'var(--isai-lime)' : '#FFFFFF',
            lineHeight: 1.3,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 1,
            WebkitBoxOrient: 'vertical'
          }}
        >
          {playlist.title}
        </h3>
        <p
          style={{
            fontSize: '11px',
            color: '#9CA3AF',
            lineHeight: 1.35,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            minHeight: '28px'
          }}
        >
          {playlist.description}
        </p>
      </div>
    </div>
  )
}
