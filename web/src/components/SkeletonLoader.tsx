import React from 'react'

export const SkeletonSongCard: React.FC = () => {
  return (
    <div className="song-card" style={{ cursor: 'default' }}>
      <div className="song-thumbnail-wrap skeleton-box" style={{ width: '100%', height: '160px' }} />
      <div className="skeleton-box" style={{ height: '16px', width: '80%', marginBottom: '8px' }} />
      <div className="skeleton-box" style={{ height: '12px', width: '50%' }} />
    </div>
  )
}

export const SkeletonSongRow: React.FC = () => {
  return (
    <div className="song-list-row" style={{ opacity: 0.6 }}>
      <div className="skeleton-box" style={{ width: '20px', height: '16px' }} />
      <div className="skeleton-box" style={{ width: '44px', height: '44px', borderRadius: '8px' }} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <div className="skeleton-box" style={{ width: '180px', height: '14px' }} />
        <div className="skeleton-box" style={{ width: '100px', height: '10px' }} />
      </div>
      <div className="skeleton-box" style={{ width: '100px', height: '12px' }} />
      <div className="skeleton-box" style={{ width: '40px', height: '12px' }} />
      <div className="skeleton-box" style={{ width: '24px', height: '24px', borderRadius: '50%' }} />
    </div>
  )
}
