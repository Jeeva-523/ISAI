import React from 'react'

export interface Genre {
  id: string
  name: string
  query: string
  gradient: string
  icon: string
}

interface GenreTileProps {
  genre: Genre
  onSelect: (query: string) => void
}

export const GenreTile: React.FC<GenreTileProps> = ({ genre, onSelect }) => {
  return (
    <div
      className="genre-tile"
      style={{ background: genre.gradient }}
      onClick={() => onSelect(genre.query)}
    >
      <h3 className="genre-tile-title">{genre.name}</h3>
      <div className="genre-tile-icon">{genre.icon}</div>
    </div>
  )
}
